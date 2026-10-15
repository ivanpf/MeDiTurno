package com.mediturno.backend;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.common.config.FechaConfig;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.consultorios.repository.ConsultorioRepository;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.especialidades.repository.EspecialidadRepository;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.TurnoSemanalRepository;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.repository.MedicoRepository;

/**
 * Base de las pruebas que necesitan una base PostgreSQL real (se levanta una sola vez y se comparte, sin Podman). Cada
 * prueba se deshace sola al terminar. Trae datos de ejemplo para armar médicos, consultorios y turnos.
 * Los datos de ejemplo no pueden repetir los del seed (usuarios de prueba): la matrícula MAT-1001 y las cédulas 9.000.00x-x.
 */
@SpringBootTest
@Transactional
public abstract class PostgresIntegracionTest extends BasePostgresEmbebida {

    @DynamicPropertySource
    static void baseDeDatos(DynamicPropertyRegistry registro) {
        registrarBaseDeDatos(registro, "integracion");
    }

    @Autowired protected TurnoSemanalRepository turnos;
    @Autowired protected MedicoRepository medicos;
    @Autowired protected ConsultorioRepository consultorios;
    @Autowired protected EspecialidadRepository especialidades;

    /** «Hoy» en la zona de la aplicación (la misma del Clock). */
    protected static final LocalDate HOY = LocalDate.now(FechaConfig.ZONA);

    protected Especialidad especialidad(String codigo) {
        Especialidad e = new Especialidad();
        e.setCodigo(codigo);
        e.setNombre("Especialidad " + codigo);
        e.setRequierePase(false);
        e.setEstado(EstadoActivacion.ACTIVO);
        return especialidades.save(e);
    }

    protected Consultorio consultorio(String codigo, EstadoActivacion estado) {
        Consultorio c = new Consultorio();
        c.setCodigo(codigo);
        c.setNombre("Consultorio " + codigo);
        c.setUbicacion("Piso 1");
        c.setEstado(estado);
        return consultorios.save(c);
    }

    protected Medico medico(String id, EstadoActivacion estado, Especialidad... delMedico) {
        Medico m = new Medico();
        m.setCorreo(id + "@ejemplo.com");
        m.setEstado(estado);
        m.setFechaCreacion(LocalDateTime.now());
        m.setCedula(id);
        m.setNombre("Nombre" + id);
        m.setApellido("Apellido" + id);
        m.setFechaNacimiento(LocalDate.of(1980, 1, 1));
        m.setMatricula("MAT-" + id);
        for (Especialidad e : delMedico) {
            m.getEspecialidades().add(e);
        }
        return medicos.save(m);
    }

    protected TurnoSemanal turno(Medico m, Especialidad e, Consultorio c, DiaSemana dia, String inicio, String fin,
            LocalDate desde, LocalDate hasta, EstadoTurnoSemanal estado) {
        TurnoSemanal t = new TurnoSemanal();
        t.setMedico(m);
        t.setEspecialidad(e);
        t.setConsultorio(c);
        t.setDia(dia);
        t.setHoraInicio(LocalTime.parse(inicio));
        t.setHoraFin(LocalTime.parse(fin));
        t.setCupoMaximo(10);
        t.setVigenteDesde(desde);
        t.setVigenteHasta(hasta);
        t.setEstado(estado);
        return turnos.saveAndFlush(t);
    }
}
