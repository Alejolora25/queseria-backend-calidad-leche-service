package com.queseria.calidadleche.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.queseria.calidadleche.infrastructure.security.JsonAccessDeniedHandler;
import com.queseria.calidadleche.infrastructure.security.JsonBearerAuthenticationEntryPoint;
import com.queseria.calidadleche.infrastructure.security.JwtRolesConverter;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

  @Bean
  SecurityWebFilterChain securityWebFilterChain(
      ServerHttpSecurity http,
      ReactiveJwtAuthenticationConverter jwtAuthenticationConverter,
      JsonBearerAuthenticationEntryPoint authenticationEntryPoint,
      JsonAccessDeniedHandler accessDeniedHandler,
      CorsConfigurationSource corsConfigurationSource
  ) {
    return http
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .logout(ServerHttpSecurity.LogoutSpec::disable)
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint(authenticationEntryPoint)
            .accessDeniedHandler(accessDeniedHandler))
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .pathMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
            .pathMatchers(HttpMethod.GET, "/actuator/health").permitAll()
            .pathMatchers("/api/v1/usuarios/**").hasRole("ADMIN")
            .pathMatchers(HttpMethod.GET,
                "/api/v1/proveedores/**",
                "/api/v1/muestras/**",
                "/api/v1/analiticas/**"
            ).hasAnyRole("ADMIN", "OPERADOR", "LECTOR")
            .pathMatchers(HttpMethod.POST,
                "/api/v1/proveedores/**",
                "/api/v1/muestras/**"
            ).hasAnyRole("ADMIN", "OPERADOR")
            .pathMatchers(HttpMethod.PUT, "/api/v1/proveedores/**")
            .hasAnyRole("ADMIN", "OPERADOR")
            .pathMatchers(HttpMethod.PATCH, "/api/v1/proveedores/**")
            .hasAnyRole("ADMIN", "OPERADOR")
            .pathMatchers(
                "/api/v1/proveedores/**",
                "/api/v1/muestras/**",
                "/api/v1/analiticas/**"
            ).denyAll()
            .anyExchange().denyAll())
        .oauth2ResourceServer(oauth2 -> oauth2
            .authenticationEntryPoint(authenticationEntryPoint)
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecretKey jwtSecretKey(JwtProperties properties, Environment environment) {
    boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    properties.validate(production);

    return new SecretKeySpec(
        properties.secret().getBytes(StandardCharsets.UTF_8),
        "HmacSHA256"
    );
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey secretKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
  }

  @Bean
  ReactiveJwtDecoder reactiveJwtDecoder(SecretKey secretKey, JwtProperties properties) {
    NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
    return decoder;
  }

  @Bean
  JwtRolesConverter jwtRolesConverter() {
    return new JwtRolesConverter();
  }

  @Bean
  ReactiveJwtAuthenticationConverter jwtAuthenticationConverter(JwtRolesConverter rolesConverter) {
    ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(rolesConverter);
    return converter;
  }

  @Bean
  JsonBearerAuthenticationEntryPoint authenticationEntryPoint() {
    return new JsonBearerAuthenticationEntryPoint();
  }

  @Bean
  JsonAccessDeniedHandler accessDeniedHandler() {
    return new JsonAccessDeniedHandler();
  }
}
