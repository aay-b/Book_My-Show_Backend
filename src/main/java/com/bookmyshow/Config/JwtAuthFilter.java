package com.bookmyshow.Config;

import com.bookmyshow.Security.CustomUserDetailsService;
import com.bookmyshow.Services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // registers this as a Spring bean so it can be inserted into the security filter chain
public class JwtAuthFilter extends OncePerRequestFilter {
    // OncePerRequestFilter guarantees the filter runs exactly once per HTTP request

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService customUserDetailsService; // loads UserDetails (password, roles/authorities) for a username from our DB.

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Spring calls this for every request before our controllers run.
        // request/response are the HTTP objects.
        // filterChain lets us pass control to the next filter when we’re done.

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // substring(7) strips "Bearer " and leaves the raw token.
            try {
                username = jwtService.extractUserName(token); // Extract the subject/username from the token
            } catch (Exception e) {
                // Invalid token, continue without authentication
                // Why just “continue without auth”?
                    // Because some endpoints are public or the client may have an expired/invalid token. We don’t want to
                    // crash; we just won’t authenticate the request. Authorization rules later will decide if access is allowed.
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Proceed only if: We actually extracted a username, and there isn’t already an Authentication in the SecurityContext
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
            // Load the full UserDetails from DB for that username: includes encoded password (not used here) and authorities (roles like ROLE_USER, ROLE_ADMIN).
            if (jwtService.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // Attach request-specific details (remote IP, session id, etc.) to the Authentication (optional but standard)
                SecurityContextHolder.getContext().setAuthentication(authToken); // Log the user in for this request by placing the Authentication in the thread-local SecurityContext.
            }
        }

        filterChain.doFilter(request, response);
        // Always pass control to the next filter (or eventually your controller).
        // This is crucial — never swallow the request; otherwise nothing else runs.
    }
}