UPDATE project_document
SET document = jsonb_set(
    document,
    '{step7ModelUpdate}',
    jsonb_set(
        COALESCE(document -> 'step7ModelUpdate', '{}'::jsonb),
        '{updatedSteps}',
        '{
          "step1": false,
          "step2": false,
          "step3": false,
          "step4": false,
          "step5": false
        }'::jsonb,
        true
    ),
    true
)
WHERE project_id = 2
   OR document #> '{step7ModelUpdate,updatedSteps}' IS NULL;