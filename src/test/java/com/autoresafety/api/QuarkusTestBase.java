package com.autoresafety.api;

import java.util.Set;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.smallrye.jwt.build.Jwt;

import org.junit.jupiter.api.BeforeAll;

abstract class QuarkusTestBase {
    static {
        // VS Code's JUnit runner does not execute Maven Surefire, so it will not inherit
        // the -Dnet.bytebuddy.experimental=true flag we set there. Quarkus/Hibernate
        // enhancement needs this when running on newer JDKs (e.g., Java 25).
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @BeforeAll
    static void authenticateAsAdmin() {
        // Project endpoints now scope visibility to the authenticated user. The tests exercise
        // the full set of projects, so authenticate as the admin user (who can see everything).
        try {
            String token = Jwt.upn("admin").groups(Set.of("User")).sign();
            RestAssured.requestSpecification = new RequestSpecBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        } catch (RuntimeException ex) {
            // In packaged integration-test mode the signing key may not be available in the
            // test JVM; leave requests unauthenticated in that case.
            RestAssured.requestSpecification = null;
        }
    }
}
