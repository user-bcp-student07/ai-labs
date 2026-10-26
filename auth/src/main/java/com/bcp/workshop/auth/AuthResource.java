package com.bcp.workshop.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.Optional;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        if (request == null) {
            return error(Response.Status.BAD_REQUEST, "Request is null");
        }
        if (request.username == null || request.username.trim().isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Username is required");
        }
        if (request.password == null || request.password.trim().isEmpty()) {
            return error(Response.Status.BAD_REQUEST, "Password is required");
        }

        try {
            Optional<LoginResponse> loginResponse = authService.authenticate(request.username, request.password);
            if (loginResponse.isEmpty()) {
                return error(Response.Status.UNAUTHORIZED, "Invalid user " + request.username + " or password");
            }
            return Response.ok(loginResponse.get()).build();
        } catch (Exception error) {
            return Response.status(500).entity(Map.of(
                    "message", error.getMessage() == null ? "Unknown error" : error.getMessage(),
                    "exception", error.getClass().getName()
            )).build();
        }
    }

    private Response error(Response.Status status, String message) {
        return Response.status(status).entity(Map.of("message", message)).build();
    }
}
