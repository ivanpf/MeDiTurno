package com.mediturno.backend.usuarios;

import java.util.Locale;
import com.mediturno.backend.auth.AuthService;
import com.mediturno.backend.common.dto.PerfilDTO;
import com.mediturno.backend.common.dto.PerfilResponseDTO;
import com.mediturno.backend.common.validation.FormatoUsuario;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.usuarios.model.Usuario;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Contacto propio de todos los perfiles; nunca recibe del cliente la identidad ni el rol a editar. */
@Service
public class PerfilService {
    private final UsuarioRepository usuarios;
    private final AuthService auth;

    public PerfilService(UsuarioRepository usuarios, AuthService auth) {
        this.usuarios = usuarios;
        this.auth = auth;
    }

    @Transactional(readOnly = true)
    public PerfilResponseDTO consultarPerfil(Long id) {
        return convertirAPerfilResponseDTO(buscar(id));
    }

    @Transactional
    public void actualizarPerfil(Long id, PerfilDTO datos) {
        usuarios.bloquear(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe"));
        Usuario usuario = buscar(id);
        String correo = datos.correo() == null ? "" : datos.correo().trim().toLowerCase(Locale.ROOT);
        if (correo.length() > 255 || !FormatoUsuario.correoValido(correo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo electrónico no es válido");
        }
        String telefono = opcional(datos.telefono());
        String domicilio = opcional(datos.domicilio());
        if ((telefono != null && telefono.length() > 30) || (domicilio != null && domicilio.length() > 255)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El teléfono o el domicilio supera el largo permitido");
        }
        if (!(usuario instanceof Paciente) && domicilio != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El domicilio solo corresponde al perfil de paciente");
        }
        if (usuarios.existsByCorreoIgnoreCaseAndIdUsuarioNot(correo, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya pertenece a otra persona");
        }
        boolean cambiaCorreo = !correo.equalsIgnoreCase(usuario.getCorreo());
        usuario.setCorreo(correo);
        usuario.setTelefono(telefono);
        if (usuario instanceof Paciente paciente) paciente.setDomicilio(domicilio);
        try {
            // Primero se comprueban las restricciones locales. Si Firebase falla se revierte toda la transacción.
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya pertenece a otra persona");
        }
        if (cambiaCorreo && usuario.getFirebaseUid() != null) auth.actualizarCorreo(usuario.getFirebaseUid(), correo);
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe"));
    }

    private static String opcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static PerfilResponseDTO convertirAPerfilResponseDTO(Usuario u) {
        String rol = u instanceof Paciente ? "paciente" : u instanceof Medico ? "medico"
            : u instanceof Funcionario ? "funcionario" : "admin";
        return new PerfilResponseDTO(u.getIdUsuario(), u.nombreCompleto(), u.getCedula(), u.getFechaNacimiento(),
            rol, u.getCorreo(), u.getTelefono(), u instanceof Paciente p ? p.getDomicilio() : null);
    }
}
