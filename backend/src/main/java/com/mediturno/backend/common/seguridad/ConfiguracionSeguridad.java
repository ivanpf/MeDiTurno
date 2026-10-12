package com.mediturno.backend.common.seguridad;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registra el interceptor que aplica {@link RolesPermitidos} a los endpoints de /api. */
@Configuration
public class ConfiguracionSeguridad implements WebMvcConfigurer {

    private final InterceptorRoles interceptorRoles;

    public ConfiguracionSeguridad(InterceptorRoles interceptorRoles) {
        this.interceptorRoles = interceptorRoles;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(interceptorRoles).addPathPatterns("/api/**");
    }
}
