#!/usr/bin/env bash
#
# One-shot provisioning + deploy of the unified AutoRESafety app to Google Cloud Run.
#
# The single container serves the Angular SPA, the REST API and the in-process
# Gemini AI on one port. PostgreSQL is provided by Cloud SQL and reached through
# the Cloud SQL socket factory (no public IP required).
#
# Prerequisites:
#   - gcloud CLI installed and authenticated:  gcloud auth login
#   - Billing enabled on the target project
#   - Run from the repository root (the folder that contains the Dockerfile):
#         ./deploy/cloud-run.sh
#
# Secrets are read from your shell environment (never hard-code them here):
#   export GOOGLE_API_KEY='...'                      # Gemini API key
#   export DB_PASSWORD='...'                          # Cloud SQL app-user password
#   export AUTH_USERS='admin:secret,manager:secret2'  # login credentials
# If any is unset you will be prompted (input hidden).
#
set -euo pipefail

# ----------------------------------------------------------------------------
# Configuration — edit these for your environment
# ----------------------------------------------------------------------------
PROJECT_ID="${PROJECT_ID:-$(gcloud config get-value project 2>/dev/null)}"
REGION="${REGION:-southamerica-east1}"   # Sao Paulo
SERVICE_NAME="${SERVICE_NAME:-resafetytool}"

# Cloud SQL
SQL_INSTANCE="${SQL_INSTANCE:-resafetytool-pg}"
SQL_EDITION="${SQL_EDITION:-enterprise}"
SQL_TIER="${SQL_TIER:-db-f1-micro}"          # smallest/cheapest; bump for real load
SQL_DB_VERSION="${SQL_DB_VERSION:-POSTGRES_16}"
DB_NAME="${DB_NAME:-resafetytool}"
DB_USER="${DB_USER:-resafetytool}"

# Application env (non-secret)
GEMINI_MODEL="${GEMINI_MODEL:-gemini-3-flash-preview}"
AUTH_JWT_ISSUER="${AUTH_JWT_ISSUER:-https://resafetytool.local}"
CORS_ORIGINS="${CORS_ORIGINS:-*}"            # same-origin in prod; tighten if you add other clients

# Cloud Run sizing
MEMORY="${MEMORY:-1Gi}"
CPU="${CPU:-1}"
# ----------------------------------------------------------------------------

if [[ -z "${PROJECT_ID}" || "${PROJECT_ID}" == "(unset)" ]]; then
  echo "ERROR: PROJECT_ID is not set. Run 'gcloud config set project <id>' or export PROJECT_ID." >&2
  exit 1
fi

CONNECTION_NAME="${PROJECT_ID}:${REGION}:${SQL_INSTANCE}"
echo ">> Project:        ${PROJECT_ID}"
echo ">> Region:         ${REGION}"
echo ">> Service:        ${SERVICE_NAME}"
echo ">> Cloud SQL:      ${CONNECTION_NAME}"
echo ">> SQL edition:    ${SQL_EDITION}"
echo ">> SQL tier:       ${SQL_TIER}"

gcloud config set project "${PROJECT_ID}" >/dev/null

# ----------------------------------------------------------------------------
# Collect secrets (env first, then hidden prompt)
# ----------------------------------------------------------------------------
prompt_secret() {           # $1 = var name, $2 = description
  local var="$1" desc="$2" val="${!1:-}"
  if [[ -z "${val}" ]]; then
    read -rs -p "Enter ${desc}: " val; echo
  fi
  # Strip CR/LF/TAB. PostgreSQL SCRAM auth (SASLprep) rejects control characters,
  # so a stray newline in a pasted password breaks login with "Prohibited character".
  val="${val//$'\r'/}"
  val="${val//$'\n'/}"
  val="${val//$'\t'/}"
  if [[ -z "${val}" ]]; then
    echo "ERROR: ${desc} cannot be empty." >&2; exit 1
  fi
  printf '%s' "${val}"
}

GOOGLE_API_KEY_VALUE="$(prompt_secret GOOGLE_API_KEY 'Gemini API key (GOOGLE_API_KEY)')"
DB_PASSWORD_VALUE="$(prompt_secret DB_PASSWORD 'Cloud SQL app-user password (DB_PASSWORD)')"
AUTH_USERS_VALUE="$(prompt_secret AUTH_USERS 'login users (AUTH_USERS, e.g. admin:secret,user:secret2)')"

# ----------------------------------------------------------------------------
# 1. Enable required APIs
# ----------------------------------------------------------------------------
echo ">> Enabling APIs..."
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  cloudbuild.googleapis.com \
  secretmanager.googleapis.com \
  artifactregistry.googleapis.com

# ----------------------------------------------------------------------------
# 2. Cloud SQL (PostgreSQL) instance, database and user — idempotent
# ----------------------------------------------------------------------------
if ! gcloud sql instances describe "${SQL_INSTANCE}" >/dev/null 2>&1; then
  echo ">> Creating Cloud SQL instance ${SQL_INSTANCE} (this can take several minutes)..."
  gcloud sql instances create "${SQL_INSTANCE}" \
    --database-version="${SQL_DB_VERSION}" \
    --edition="${SQL_EDITION}" \
    --tier="${SQL_TIER}" \
    --region="${REGION}" \
    --storage-auto-increase
else
  echo ">> Cloud SQL instance ${SQL_INSTANCE} already exists."
fi

if ! gcloud sql databases describe "${DB_NAME}" --instance="${SQL_INSTANCE}" >/dev/null 2>&1; then
  echo ">> Creating database ${DB_NAME}..."
  gcloud sql databases create "${DB_NAME}" --instance="${SQL_INSTANCE}"
else
  echo ">> Database ${DB_NAME} already exists."
fi

if ! gcloud sql users list --instance="${SQL_INSTANCE}" --format='value(name)' | grep -qx "${DB_USER}"; then
  echo ">> Creating database user ${DB_USER}..."
  gcloud sql users create "${DB_USER}" --instance="${SQL_INSTANCE}" --password="${DB_PASSWORD_VALUE}"
else
  echo ">> Updating password for existing user ${DB_USER}..."
  gcloud sql users set-password "${DB_USER}" --instance="${SQL_INSTANCE}" --password="${DB_PASSWORD_VALUE}"
fi

# ----------------------------------------------------------------------------
# 3. Secret Manager — create or add a new version for each secret
# ----------------------------------------------------------------------------
upsert_secret() {           # $1 = secret name, $2 = value
  local name="$1" value="$2"
  if ! gcloud secrets describe "${name}" >/dev/null 2>&1; then
    echo ">> Creating secret ${name}..."
    printf '%s' "${value}" | gcloud secrets create "${name}" --data-file=- --replication-policy=automatic
  else
    echo ">> Adding new version to secret ${name}..."
    printf '%s' "${value}" | gcloud secrets versions add "${name}" --data-file=-
  fi
}

upsert_secret GOOGLE_API_KEY "${GOOGLE_API_KEY_VALUE}"
upsert_secret DB_PASSWORD    "${DB_PASSWORD_VALUE}"
upsert_secret AUTH_USERS     "${AUTH_USERS_VALUE}"

# ----------------------------------------------------------------------------
# 4. Grant the Cloud Run runtime service account the roles it needs
# ----------------------------------------------------------------------------
PROJECT_NUMBER="$(gcloud projects describe "${PROJECT_ID}" --format='value(projectNumber)')"
RUNTIME_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
echo ">> Granting roles to runtime service account ${RUNTIME_SA}..."
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${RUNTIME_SA}" --role="roles/cloudsql.client" --condition=None >/dev/null
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${RUNTIME_SA}" --role="roles/secretmanager.secretAccessor" --condition=None >/dev/null

# ----------------------------------------------------------------------------
# 5. Build (Cloud Build picks up the root Dockerfile) and deploy to Cloud Run
# ----------------------------------------------------------------------------
# Cloud SQL JDBC URL via the socket factory (no public IP needed).
DB_JDBC_URL="jdbc:postgresql:///${DB_NAME}?cloudSqlInstance=${CONNECTION_NAME}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"

echo ">> Building image from source and deploying ${SERVICE_NAME}..."
# '^@^' switches the env-var delimiter from ',' to '@' so values may contain commas/&/=.
gcloud run deploy "${SERVICE_NAME}" \
  --source . \
  --region "${REGION}" \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --memory "${MEMORY}" \
  --cpu "${CPU}" \
  --add-cloudsql-instances "${CONNECTION_NAME}" \
  --set-secrets "GOOGLE_API_KEY=GOOGLE_API_KEY:latest,DB_PASSWORD=DB_PASSWORD:latest,AUTH_USERS=AUTH_USERS:latest" \
  --set-env-vars "^@^DB_JDBC_URL=${DB_JDBC_URL}@DB_USER=${DB_USER}@DB_NAME=${DB_NAME}@GEMINI_MODEL=${GEMINI_MODEL}@AUTH_JWT_ISSUER=${AUTH_JWT_ISSUER}@CORS_ORIGINS=${CORS_ORIGINS}"

echo
echo ">> Done. Service URL:"
gcloud run services describe "${SERVICE_NAME}" --region "${REGION}" --format='value(status.url)'
