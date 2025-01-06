package com.project.cadence.utility;

import com.project.cadence.model.User;
import com.project.cadence.service.JwtService;
import com.project.cadence.service.UserService;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestInterceptor implements HandlerInterceptor {
    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) throws IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
                return false;
            }

            String accessToken = authHeader.substring(7);
            String[] sections = accessToken.split("\\.");

            // Basic token structure validation
            if (sections.length != 3) {
                sendErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid token format");
                return false;
            }

            // Decode the token to get the header and payload
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String header = new String(decoder.decode(sections[0]));
            String payload = new String(decoder.decode(sections[1]));
            String tokenSignature = sections[2];

            // Expected signature based on the header and payload of the token
            String expectedSignature = jwtService.generateSignature(header, payload);

            // If the signature is not matching then the token might have been tampered then return false
            if (!tokenSignature.equals(expectedSignature)) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Token integrity verification failed");
                return false;
            }

            // Check if the token type is access or refresh
            JSONObject payloadJson = new JSONObject(payload);
            if (payloadJson.has("type") && payloadJson.get("type").equals("access")) {
                // Extract user email from jwt payload
                String userEmail = this.jwtService.extractEmailForPayload(payload);
                if (userEmail == null || userEmail.isEmpty()) {
                    sendErrorResponse(response, HttpStatus.BAD_REQUEST, "Unable to extract user email from token");
                    return false;
                }

                // Load user from the DB
                Optional<User> optionalUser = this.userService.loadUserByEmail(userEmail);
                if (optionalUser.isEmpty()) {
                    sendErrorResponse(response, HttpStatus.NOT_FOUND, "User not found");
                    return false;
                }

                // Check if token has expired
                if (this.jwtService.isTokenExpired(payload)) {
                    sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token has expired");
                    return false;
                }

                return true;
            } else {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid token type, Access token required");
                return false;
            }
        } catch (Exception e) {
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Authentication process failed");
            log.error("An exception has occurred {}", e.getMessage(), e);
            return false;
        }
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatusCode status, String message) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status.value());

        JSONObject errorDetails = new JSONObject();
        errorDetails.put("status", status.value());
        errorDetails.put("message", message);
        errorDetails.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(errorDetails.toString());
    }
}
