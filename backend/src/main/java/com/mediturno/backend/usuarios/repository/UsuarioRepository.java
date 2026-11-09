package com.mediturno.backend.usuarios.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.usuarios.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByFirebaseUid(String firebaseUid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where lower(u.correo) = lower(:correo)")
    Optional<Usuario> buscarPorCorreoConLock(@Param("correo") String correo);

    boolean existsByCedula(String cedula);

    boolean existsByCorreoIgnoreCase(String correo);

    /** ¿Otra persona (no la indicada) usa ese correo? Para validar el cambio de correo de alguien que ya existe. */
    boolean existsByCorreoIgnoreCaseAndIdUsuarioNot(String correo, Long idUsuario);

    /** Todas las cédulas registradas, de cualquier rol: las cargas masivas comprueban así los duplicados de una sola vez. */
    @Query("select u.cedula from Usuario u")
    List<String> listarCedulas();

    /** Todos los correos registrados, de cualquier rol, en minúsculas. */
    @Query("select lower(u.correo) from Usuario u")
    List<String> listarCorreosEnMinusculas();

    /**
     * Bloquea la fila de la persona hasta el fin de la transacción (SELECT ... FOR UPDATE) sin saber todavía de qué rol es:
     * dos administradores que modifican a la misma persona a la vez no se pisan. Vacío si no existe.
     */
    @Query(value = "select id_usuario from usuario where id_usuario = :id for update", nativeQuery = true)
    Optional<Long> bloquear(@Param("id") Long id);

    /**
     * Bloquea a todos los administradores, siempre en el mismo orden. Se usa antes de contar a los activos para no dejar el
     * sistema sin ninguno: dos administradores que se inactivan entre sí a la vez se esperan en lugar de pisarse.
     */
    @Query(value = "select id_usuario from administrador order by id_usuario for update", nativeQuery = true)
    List<Long> bloquearAdministradores();

    // Cambio de rol (CU-45): como no hay una columna «rol», la persona cambia de rol al pasar de la tabla de un rol a la de
    // otro. Las filas del rol que se deja solo se pueden borrar si nada las referencia (turnos, órdenes, consultas, cambios
    // hechos...): lo que quede referenciado es actividad histórica y la base lo impide.

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from medico_especialidad where id_medico = :id", nativeQuery = true)
    void borrarEspecialidadesDeMedico(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from medico where id_usuario = :id", nativeQuery = true)
    void borrarRolMedico(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from funcionario where id_usuario = :id", nativeQuery = true)
    void borrarRolFuncionario(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from administrador where id_usuario = :id", nativeQuery = true)
    void borrarRolAdministrador(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "insert into medico (id_usuario, matricula) values (:id, :matricula)", nativeQuery = true)
    void agregarRolMedico(@Param("id") Long id, @Param("matricula") String matricula);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "insert into funcionario (id_usuario) values (:id)", nativeQuery = true)
    void agregarRolFuncionario(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "insert into administrador (id_usuario) values (:id)", nativeQuery = true)
    void agregarRolAdministrador(@Param("id") Long id);
}
