package com.pocauth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class ConfiguracionSeguridad {

    private final FiltroAutenticacionFirebase filtroFirebase;

    public ConfiguracionSeguridad(FiltroAutenticacionFirebase filtroFirebase) {
        this.filtroFirebase = filtroFirebase;
    }

    @Bean
    public SecurityFilterChain cadenaFiltros(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint puntoEntradaNoAutenticado = (request, response, authException) ->
                response.sendError(401, "Falta o es invalido el header Authorization");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(puntoEntradaNoAutenticado))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/usuarios").permitAll()
                        .requestMatchers("/", "/login-test.html").permitAll()
                        .requestMatchers("/solo-admin", "/usuarios/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(filtroFirebase, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
