package com.mediturno.backend.common.seguridad;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mediturno.backend.common.health.HealthController;

/** Evita que un endpoint nuevo quede sin definir qué roles lo pueden usar. */
class RolesDeEndpointsTest {

    @Test
    void todoEndpointTieneRolesPermitidosEnElMetodoOEnLaClase() throws Exception {
        ClassPathScanningCandidateComponentProvider buscador = new ClassPathScanningCandidateComponentProvider(false);
        buscador.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> sinRol = new ArrayList<>();
        for (BeanDefinition definicion : buscador.findCandidateComponents("com.mediturno.backend")) {
            Class<?> controlador = Class.forName(definicion.getBeanClassName());
            if (controlador == HealthController.class) {
                continue; // /api/health es público
            }
            boolean rolEnLaClase = controlador.isAnnotationPresent(RolesPermitidos.class);
            for (Method metodo : controlador.getDeclaredMethods()) {
                boolean esEndpoint = AnnotatedElementUtils.hasAnnotation(metodo, RequestMapping.class);
                if (esEndpoint && !rolEnLaClase && !metodo.isAnnotationPresent(RolesPermitidos.class)) {
                    sinRol.add(controlador.getSimpleName() + "." + metodo.getName());
                }
            }
        }
        assertTrue(sinRol.isEmpty(), "Endpoints sin @RolesPermitidos: " + sinRol);
    }
}
