package com.pocauth.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.firebase.auth.DeleteUsersResult;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;

@RestController
public class AutenticacionControlador {

    // Ver application.properties: este correo nunca se borra en el borrado masivo.
    @Value("${admin.correo-protegido}")
    private String correoAdminProtegido;

    // Ruta publica, sin autenticacion.
    @GetMapping("/")
    public Map<String, String> inicio() {
        return Map.of("estado", "ok", "mensaje", "API de prueba - Firebase Auth con Spring Boot");
    }

    // Ruta protegida: requiere un token de Firebase valido.
    @GetMapping("/me")
    public Map<String, Object> obtenerUsuarioActual() {
        FirebaseToken usuario = usuarioAutenticado();

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("uid", usuario.getUid());
        respuesta.put("email", usuario.getEmail());
        respuesta.put("claims", usuario.getClaims());
        return respuesta;
    }

    // Crea un usuario nuevo usando el Admin SDK, con un rol opcional.
    // Solo para pruebas: en un caso real esta ruta deberia requerir un admin autenticado.
    @PostMapping("/usuarios")
    public ResponseEntity<Map<String, Object>> crearUsuario(@RequestBody Map<String, String> body) throws Exception {
        String email = body.get("email");
        String password = body.get("password");
        String rol = body.get("rol");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email y password son obligatorios"));
        }

        UserRecord.CreateRequest solicitud = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password);

        UserRecord usuarioCreado = FirebaseAuth.getInstance().createUser(solicitud);

        if (rol != null && !rol.isBlank()) {
            FirebaseAuth.getInstance().setCustomUserClaims(usuarioCreado.getUid(), Map.of("rol", rol));
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("uid", usuarioCreado.getUid());
        respuesta.put("email", usuarioCreado.getEmail());
        respuesta.put("rol", rol);

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // Asigna/reemplaza el rol de un usuario existente. Requiere rol ADMIN (ver ConfiguracionSeguridad).
    @PostMapping("/usuarios/{uid}/rol")
    public ResponseEntity<Map<String, Object>> asignarRol(@PathVariable String uid, @RequestBody Map<String, String> body) throws Exception {
        String rol = body.get("rol");

        if (rol == null || rol.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "rol es obligatorio"));
        }

        FirebaseAuth.getInstance().setCustomUserClaims(uid, Map.of("rol", rol));

        return ResponseEntity.ok(Map.of("uid", uid, "rol", rol));
    }

    // Ruta de ejemplo protegida por rol: solo usuarios con claim rol=admin pueden entrar.
    @GetMapping("/solo-admin")
    public Map<String, String> soloAdmin() {
        FirebaseToken usuario = usuarioAutenticado();
        return Map.of("mensaje", "Bienvenido, admin " + usuario.getEmail());
    }

    // Consulta los datos (incluidos los custom claims, como "rol") de cualquier usuario por su uid.
    // Requiere rol ADMIN (ver ConfiguracionSeguridad) ya que la consola de Firebase no muestra los claims.
    @GetMapping("/usuarios/{uid}")
    public ResponseEntity<Map<String, Object>> obtenerUsuario(@PathVariable String uid) throws Exception {
        UserRecord usuario = FirebaseAuth.getInstance().getUser(uid);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("uid", usuario.getUid());
        respuesta.put("email", usuario.getEmail());
        respuesta.put("claims", usuario.getCustomClaims());
        return ResponseEntity.ok(respuesta);
    }

    // Busca un usuario por correo (mas comodo que memorizar el uid). Requiere rol ADMIN.
    @GetMapping("/usuarios/por-correo")
    public ResponseEntity<Map<String, Object>> obtenerUsuarioPorCorreo(@RequestParam String correo) throws Exception {
        UserRecord usuario = FirebaseAuth.getInstance().getUserByEmail(correo);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("uid", usuario.getUid());
        respuesta.put("email", usuario.getEmail());
        respuesta.put("claims", usuario.getCustomClaims());
        return ResponseEntity.ok(respuesta);
    }

    // Deshabilita un usuario (no puede loguearse mas hasta que se lo vuelva a habilitar).
    // Util por ejemplo para dar de baja a un funcionario o medico sin borrar su historial. Requiere rol ADMIN.
    @PostMapping("/usuarios/{uid}/deshabilitar")
    public ResponseEntity<Map<String, Object>> deshabilitarUsuario(@PathVariable String uid) throws Exception {
        UserRecord.UpdateRequest solicitud = new UserRecord.UpdateRequest(uid).setDisabled(true);
        UserRecord usuario = FirebaseAuth.getInstance().updateUser(solicitud);
        return ResponseEntity.ok(Map.of("uid", usuario.getUid(), "deshabilitado", usuario.isDisabled()));
    }

    // Vuelve a habilitar un usuario previamente deshabilitado. Requiere rol ADMIN.
    @PostMapping("/usuarios/{uid}/habilitar")
    public ResponseEntity<Map<String, Object>> habilitarUsuario(@PathVariable String uid) throws Exception {
        UserRecord.UpdateRequest solicitud = new UserRecord.UpdateRequest(uid).setDisabled(false);
        UserRecord usuario = FirebaseAuth.getInstance().updateUser(solicitud);
        return ResponseEntity.ok(Map.of("uid", usuario.getUid(), "deshabilitado", usuario.isDisabled()));
    }

    // Elimina definitivamente un usuario de Firebase Auth. Requiere rol ADMIN.
    @DeleteMapping("/usuarios/{uid}")
    public ResponseEntity<Map<String, String>> eliminarUsuario(@PathVariable String uid) throws Exception {
        FirebaseAuth.getInstance().deleteUser(uid);
        return ResponseEntity.ok(Map.of("uid", uid, "estado", "eliminado"));
    }

    // Elimina TODOS los usuarios de Firebase Auth. Accion destructiva e irreversible,
    // por eso exige el query param ?confirmar=si ademas del rol ADMIN.
    @DeleteMapping("/usuarios")
    public ResponseEntity<Map<String, Object>> eliminarTodosLosUsuarios(
            @RequestParam(name = "confirmar", required = false) String confirmar) throws Exception {

        if (!"si".equalsIgnoreCase(confirmar)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Accion destructiva. Repeti la llamada con ?confirmar=si para borrar todos los usuarios."));
        }

        List<String> uids = new ArrayList<>();
        for (ExportedUserRecord usuario : FirebaseAuth.getInstance().listUsers(null).iterateAll()) {
            if (correoAdminProtegido.equalsIgnoreCase(usuario.getEmail())) continue;
            uids.add(usuario.getUid());
        }

        int totalExitosos = 0;
        int totalFallidos = 0;
        List<String> errores = new ArrayList<>();

        // deleteUsers acepta hasta 500 uids por llamada.
        for (int i = 0; i < uids.size(); i += 500) {
            List<String> lote = uids.subList(i, Math.min(i + 500, uids.size()));
            DeleteUsersResult resultado = FirebaseAuth.getInstance().deleteUsers(lote);
            totalExitosos += resultado.getSuccessCount();
            totalFallidos += resultado.getFailureCount();
            resultado.getErrors().forEach(e -> errores.add(e.getIndex() + ": " + e.getReason()));
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("totalEncontrados", uids.size());
        respuesta.put("eliminados", totalExitosos);
        respuesta.put("fallidos", totalFallidos);
        respuesta.put("errores", errores);
        respuesta.put("correoProtegido", correoAdminProtegido);
        return ResponseEntity.ok(respuesta);
    }

    // Revoca todos los refresh tokens del usuario: cualquier ID token ya emitido
    // deja de aceptarse en cuanto expire (hasta 1 hora), forzando un nuevo login.
    // Util al deshabilitar a alguien que ya tiene una sesion activa. Requiere rol ADMIN.
    @PostMapping("/usuarios/{uid}/revocar-tokens")
    public ResponseEntity<Map<String, String>> revocarTokens(@PathVariable String uid) throws Exception {
        FirebaseAuth.getInstance().revokeRefreshTokens(uid);
        return ResponseEntity.ok(Map.of("uid", uid, "estado", "tokens revocados"));
    }

    // Alta masiva de usuarios desde un archivo CSV con columnas: email,password,rol
    // (rol es opcional, puede ir vacio). Requiere rol ADMIN.
    @PostMapping("/usuarios/carga-masiva")
    public ResponseEntity<Map<String, Object>> cargaMasiva(@RequestParam("archivo") MultipartFile archivo) throws Exception {
        List<Map<String, Object>> resultados = new ArrayList<>();

        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

            String linea;
            int numeroLinea = 0;

            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (linea.isBlank()) continue;

                // "-1" evita que se descarten columnas vacias al final de la linea
                // (ej. "correo@x.com,clave," sin rol quedaria con solo 2 columnas si no).
                String[] columnas = linea.split(",", -1);
                String email = columnas.length > 0 ? columnas[0].trim() : "";
                String password = columnas.length > 1 ? columnas[1].trim() : "";
                String rol = columnas.length > 2 ? columnas[2].trim() : null;

                // Se asume que la primera linea puede ser el encabezado.
                if (numeroLinea == 1 && "email".equalsIgnoreCase(email)) continue;

                Map<String, Object> resultado = new HashMap<>();
                resultado.put("linea", numeroLinea);
                resultado.put("email", email);

                if (email.isEmpty() || password.isEmpty()) {
                    resultado.put("estado", "error");
                    resultado.put("error", "email y password son obligatorios (linea invalida o mal formada)");
                    resultados.add(resultado);
                    continue;
                }

                try {
                    UserRecord.CreateRequest solicitud = new UserRecord.CreateRequest()
                            .setEmail(email)
                            .setPassword(password);
                    UserRecord creado = FirebaseAuth.getInstance().createUser(solicitud);

                    if (rol != null && !rol.isBlank()) {
                        FirebaseAuth.getInstance().setCustomUserClaims(creado.getUid(), Map.of("rol", rol));
                    }

                    resultado.put("uid", creado.getUid());
                    resultado.put("estado", "creado");
                } catch (FirebaseAuthException | IllegalArgumentException e) {
                    // IllegalArgumentException: el SDK valida formato de email/password localmente
                    // antes de llamar a Firebase (ej. password de menos de 6 caracteres).
                    resultado.put("estado", "error");
                    resultado.put("error", e.getMessage());
                }

                resultados.add(resultado);
            }
        }

        return ResponseEntity.ok(Map.of("resultados", resultados));
    }

    private FirebaseToken usuarioAutenticado() {
        return (FirebaseToken) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    // Traduce errores del Admin SDK (ej. uid inexistente) a una respuesta JSON clara
    // en vez de dejar que se propaguen como un error 500/401 sin explicacion.
    @ExceptionHandler(FirebaseAuthException.class)
    public ResponseEntity<Map<String, String>> manejarErrorFirebase(FirebaseAuthException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Usuario no encontrado o uid invalido: " + ex.getMessage()));
    }

    // Red de seguridad: cualquier otro error no manejado explicitamente devuelve
    // JSON en vez de una respuesta vacia que el frontend no puede parsear.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> manejarErrorGenerico(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getClass().getSimpleName() + ": " + ex.getMessage()));
    }
}
