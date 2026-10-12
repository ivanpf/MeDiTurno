package com.mediturno.backend.common.seguridad;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.mediturno.backend.usuarios.model.Usuario;

/**
 * Roles que pueden usar un endpoint. {@link InterceptorRoles} responde 403 si la persona autenticada es de otro rol.
 * Se puede poner en la clase (vale para todos sus endpoints) y en un método (reemplaza al de la clase).
 * Sin valores ({@code @RolesPermitidos}) puede usarlo cualquier persona con sesión.
 * Ejemplos: {@code @RolesPermitidos(Administrador.class)} o {@code @RolesPermitidos({Medico.class, Funcionario.class})}.
 * Todos los endpoints tienen que llevarla (lo comprueba {@code RolesDeEndpointsTest}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RolesPermitidos {

    Class<? extends Usuario>[] value() default {};
}
