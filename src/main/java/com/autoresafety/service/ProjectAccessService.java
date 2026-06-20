package com.autoresafety.service;

import com.autoresafety.domain.Project;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

/**
 * Centralises the project visibility/ownership rules:
 * <ul>
 *   <li>the configured admin user (default {@code admin}) can see and access every project;</li>
 *   <li>projects flagged as {@code shared} (the example/case-study projects) are visible to everyone;</li>
 *   <li>every other project is only visible to the user that created it.</li>
 * </ul>
 */
@ApplicationScoped
public class ProjectAccessService {

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "app.admin-username", defaultValue = "admin")
    String adminUsername;

    /** Username of the current request, or {@code null} when the request is anonymous. */
    public String currentUsername() {
        if (jwt == null) {
            return null;
        }
        String name = jwt.getName();
        return name == null || name.isBlank() ? null : name.trim();
    }

    public boolean isAdmin() {
        String username = currentUsername();
        return username != null && username.equalsIgnoreCase(adminUsername);
    }

    public boolean canAccess(Project project) {
        if (project == null) {
            return false;
        }
        if (isAdmin() || project.shared) {
            return true;
        }
        String username = currentUsername();
        return username != null && username.equals(project.createdBy);
    }

    /**
     * Whether the current user may modify/delete the project. Shared example projects are
     * read-only for everyone except the admin user; owned projects can be changed by their
     * creator (or the admin).
     */
    public boolean canModify(Project project) {
        if (project == null) {
            return false;
        }
        if (isAdmin()) {
            return true;
        }
        String username = currentUsername();
        return username != null && username.equals(project.createdBy);
    }

    /**
     * Throws {@link NotFoundException} when the current user is not allowed to access the project.
     * A 404 (rather than 403) is used so callers cannot probe for the existence of projects they
     * do not own.
     */
    public void assertCanAccess(Project project) {
        if (!canAccess(project)) {
            throw new NotFoundException();
        }
    }

    /**
     * Throws when the current user may not modify the project: {@link NotFoundException} when the
     * project is not even visible to the user, or {@link ForbiddenException} when it is visible
     * (e.g. a shared example) but read-only for the current user.
     */
    public void assertCanModify(Project project) {
        if (canModify(project)) {
            return;
        }
        if (!canAccess(project)) {
            throw new NotFoundException();
        }
        throw new ForbiddenException("This project is read-only for the current user.");
    }
}
