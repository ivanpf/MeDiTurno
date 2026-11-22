# Pasos de la demo de MeDiTurno

Los datos (nombres, cédulas, correos) se eligen en el momento: no hay datos fijos. Lo que importa es el orden y que los
datos de un paso se reutilicen en los siguientes (por ejemplo, el paciente que da de alta el administrador es el que
después se registra en la app).

**Antes de empezar:** ver `INSTRUCCIONES.md` (base cargada, Firebase limpio, backend, web y emulador funcionando).

| Paso | Dónde | Qué se muestra |
|---|---|---|
| 0 | — | Introducción |
| 1 | Web · administrador | Pantalla de inicio de sesión e ingreso |
| 2 | Web · administrador | Alta de una especialidad nueva |
| 3 | Web · administrador | Alta de un paciente en el padrón |
| 4 | Web · administrador | Alta de un médico con la especialidad nueva (le llega un correo con su contraseña temporal) |
| 5 | Web · administrador | Listado y búsqueda de especialidades: aparece la nueva. Modificarla y volver al listado: los datos ya están actualizados |
| 6 | Web · administrador | Alta masiva de consultorios con `demo/consultorios-demo.csv` |
| 7 | Web · funcionario | Alta de un turno para el médico nuevo, con su especialidad y uno de los consultorios nuevos. **Hoy, empezando una hora después de la hora de la demo** |
| 8 | Web · funcionario | Turnos semanales: el turno quedó creado |
| 9 | Web · funcionario | Cartelera: mostrar la normal, publicar anuncios y volver a verla con los anuncios nuevos |
| 10 | App móvil · paciente | Registro con los datos del padrón: primero con la **cédula incorrecta** (no deja crear la cuenta) y después con la correcta. Inicio de sesión |
| 11 | App móvil · paciente | Cartelera: aparecen los anuncios nuevos |
| 12 | Web · funcionario | Eliminar un anuncio. En la app, actualizar: ya no aparece |
| 13 | App móvil · paciente | Reservar en el turno del médico nuevo y una segunda reserva en un turno de otro médico y otra especialidad |
| 14 | Web · funcionario | Suspender el turno de la segunda reserva: al paciente le llega la notificación push |
| 15 | App móvil · paciente | Mis reservas: aparecen las dos con su estado (una vigente y otra anulada) |
| 16 | Web · funcionario | Consulta de reserva: poner el número de la reserva del médico nuevo y emitir la orden |
| 17 | Web · médico nuevo | Iniciar sesión con la contraseña que llegó por correo. En «Mis pacientes», ver la orden y su estado. Registrar la consulta con sus datos y emitir un pase |
| 18 | Web · médico nuevo | «Historial de atenciones»: la consulta figura como registrada y se ven sus datos. «Pases emitidos»: aparece el pase |
| 19 | App móvil · paciente | Mis reservas, actualizando: una suspendida y la otra atendida. Menú «Historia clínica»: se ven los datos de la consulta |
| 20 | Web · administrador | Los tres módulos de estadísticas: reservas por especialidad, turnos más demandados y ocupación de turnos |

## Notas puntuales

- **Paso 4:** el correo con la contraseña temporal llega a la casilla configurada en `MAIL_*`. Con el entorno de
  desarrollo llega a Mailpit (http://localhost:8025). El correo del médico y el del paciente tienen que ser distintos y
  no estar ya registrados en Firebase (por eso se limpia antes).
- **Paso 7:** el turno tiene que ser **del día de la demo** y empezar dentro de la hora siguiente, porque la orden (paso 16)
  solo se emite desde una hora antes de que empieza el turno hasta que termina. Elegir un turno corto: no puede pasar de
  medianoche.
- **Paso 10:** la cédula y el correo tienen que coincidir con los del padrón (paso 3). Con la cédula mal, el sistema
  responde con un mensaje genérico, para no revelar qué datos existen.
- **Paso 13:** la segunda reserva se hace en un turno que luego se suspende (paso 14); no tiene que ser el turno del
  médico nuevo. Si la especialidad exige pase, el paciente no puede reservar: elegir una que no lo exija.
- **Paso 16:** la orden se emite sobre la reserva del turno del médico nuevo, que es la que se atiende después.
- **Paso 17:** el botón «Emitir pase» aparece al terminar de registrar la consulta; también se puede emitir desde
  «Historial de atenciones».
