package com.queseria.calidadleche.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebFluxCorsConfig implements WebFluxConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
      .allowedOriginPatterns("http://localhost:4200",
                      "https://queseria-frontend.vercel.app",
                      "https://*.vercel.app")
      .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
      .allowedHeaders("*");
  }
}