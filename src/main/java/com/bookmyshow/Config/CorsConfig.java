package com.bookmyshow.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // registers this class as a Spring config bean.
public class CorsConfig implements WebMvcConfigurer {
    // WebMvcConfigurer lets you customize Spring MVC; here you’ll override a CORS hook.
    // NOTE: CORS = Cross-Origin Resource Sharing

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Spring calls this during startup so you can register CORS rules.
        registry.addMapping("/**") // Apply to all endpoints
                .allowedOrigins("http://localhost:5173") // Allow your frontend origin
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Specify allowed methods
                // Including OPTIONS is important because the browser sends preflight requests (OPTIONS) before certain cross-origin calls (e.g., with custom headers or non-GET methods).

                .allowedHeaders("*") // Allow all headers
                .allowCredentials(true); // If you're using cookies or authentication
    }
    /* In SecurityConfig, we called .cors(cors -> cors.and()). That turns on Spring Security’s CORS support and delegates
    to this WebMvcConfigurer configuration. Together they ensure:
            Preflights (OPTIONS) are permitted.
            Actual requests from http://localhost:5173 with Authorization work.
    */
}