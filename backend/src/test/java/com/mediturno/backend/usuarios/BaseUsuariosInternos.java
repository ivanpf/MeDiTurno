package com.mediturno.backend.usuarios;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.model.Usuario;
import com.mediturno.backend.usuarios.repository.AdministradorRepository;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;

/** Base de las pruebas de usuarios internos (CU-35, CU-36, CU-45 y CU-46): arma administradores, funcionarios y médicos. */
abstract class BaseUsuariosInternos extends PostgresIntegracionTest {

    @Autowired protected UsuarioRepository usuarios;
    @Autowired protected AdministradorRepository administradores;

    private <T extends Usuario> T guardar(T u, String cedula, String nombre, String apellido, String correo,
            EstadoActivacion estado, String uid, LocalDateTime creacion) {
        u.setCedula(cedula);
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setCorreo(correo);
        u.setFechaNacimiento(LocalDate.of(1980, 1, 1));
        u.setEstado(estado);
        u.setFirebaseUid(uid);
        u.setFechaCreacion(creacion);
        return usuarios.saveAndFlush(u);
    }

    protected Administrador administrador(String cedula, String nombre, String apellido, String correo, EstadoActivacion estado) {
        return guardar(new Administrador(), cedula, nombre, apellido, correo, estado, "uid-" + correo, LocalDateTime.now());
    }

    protected Funcionario funcionarioInterno(String cedula, String nombre, String apellido, String correo, EstadoActivacion estado,
            LocalDateTime creacion) {
        return guardar(new Funcionario(), cedula, nombre, apellido, correo, estado, "uid-" + correo, creacion);
    }

    protected Medico medicoInterno(String cedula, String nombre, String apellido, String correo, String matricula,
            EstadoActivacion estado, LocalDateTime creacion, Especialidad... especialidades) {
        Medico m = new Medico();
        m.setMatricula(matricula);
        for (Especialidad e : especialidades) {
            m.getEspecialidades().add(e);
        }
        return guardar(m, cedula, nombre, apellido, correo, estado, "uid-" + correo, creacion);
    }

    protected Paciente pacienteInterno(String cedula, String nombre, String apellido, String correo) {
        Paciente p = new Paciente();
        p.setFechaAlta(LocalDateTime.now());
        return guardar(p, cedula, nombre, apellido, correo, EstadoActivacion.ACTIVO, null, LocalDateTime.now());
    }

    /** Deja inactivos a todos los administradores menos a los indicados: así se prueba qué pasa con «el último administrador activo». */
    protected void inactivarAdministradoresSalvo(Long... conservar) {
        java.util.Set<Long> ids = java.util.Set.of(conservar);
        for (Administrador a : administradores.findAll()) {
            if (!ids.contains(a.getIdUsuario())) {
                a.setEstado(EstadoActivacion.INACTIVO);
            }
        }
        administradores.flush();
    }
}
