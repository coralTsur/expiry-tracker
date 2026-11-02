package com.coral.expirytracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the React dev server (http://localhost:5173) call the API from the browser.
 * One central rule for every endpoint under /api/**, instead of @CrossOrigin on each controller.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final String[] allowedOrigins;

	// The origin can be overridden per environment (property or env var APP_CORS_ALLOWED_ORIGINS).
	// Without an override, the Vite dev server origin is used. Several origins: comma-separated.
	public CorsConfig(@Value("${app.cors.allowed-origins:http://localhost:5173}") String[] allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins)
				// Every method our endpoints use. The default would be only GET, HEAD, POST.
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
				// POST and PUT send Content-Type: application/json, which triggers a preflight.
				.allowedHeaders("Content-Type");
		// allowCredentials is not set: no login and no cookies in the first version (spec section 2).
	}

}
