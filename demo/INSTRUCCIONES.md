# Cómo preparar una demo (instrucciones para personas y para agentes de IA)

Si le piden «preparar una demo para tal fecha y hora», esto es lo que hay que hacer, **todo en ese mismo momento** (no
dejar nada para el día de la demo). Los pasos de la demo están en `PASOS.md`; no hay datos fijos ni nada que generar: la
demo crea sus propios datos en vivo (especialidad, consultorios, paciente, médico y turno).

## 0. Preguntar primero: ¿local o deployada?

Estas instrucciones valen para la demo **local** (contenedores en la computadora). Si la demo se hace sobre el sistema
**deployado**, no seguir con los pasos de abajo: no se recarga la base (`down -v` es solo local) ni se toca nada de lo que
está ejecutándose en el servidor. Solo aplica la limpieza de Firebase (paso 2), y únicamente si el sistema deployado usa el
mismo proyecto de Firebase. Para el resto, avisar a la persona y decidir con ella (por ejemplo, un entorno deployado propio
para la demo); no inventar pasos.

## 1. Verificar la clave de Firebase

Tiene que existir `secrets/firebase-clave.json` (la pasa el equipo por un canal privado, nunca por Git). Sin ella nadie
puede iniciar sesión.

## 2. Limpiar las cuentas de Firebase de la demo

Borrar la base **no** borra Firebase: si el médico o el paciente de la demo ya existen allí, el alta o el registro fallan.
Pedir a la persona los dos correos que va a usar (el del médico y el del paciente; pueden ser alias del mismo correo, como
`nombre+medico@gmail.com`) y correr primero el simulacro:

```powershell
node demo/limpiar-firebase.mjs --correos medico@ejemplo.com,paciente@ejemplo.com
```

Mostrar la lista «Se BORRARÍAN» y, solo con su confirmación, repetir con `--borrar`. El proyecto de Firebase es
**compartido**: el script solo borra los correos indicados y conserva las cuentas del seed, las cuatro `@mediturno.com` y
las de otras personas, pero nunca ejecutar `--borrar` sin confirmación. Si después de esto se ensaya la demo con esos
correos, hay que limpiar otra vez antes de la demo real.

## 3. Cargar la base ahora

```powershell
podman compose down -v
podman compose up -d --build
```

Esto borra todos los datos locales y los vuelve a cargar con el seed normal (perfil `dev`); no se modifica el seed.
Las fechas del seed son relativas al día de carga, **pero eso no importa para la demo si se carga antes**: la demo usa solo
turnos futuros (el de Pediatría del martes y otro turno con números libres para las reservas) y el historial para las
estadísticas, y el turno nuevo lo crea en vivo, con la fecha real. El backend completa solo las fechas reservables
(hasta 8 semanas) al arrancar y todos los días a las 2:00; si el backend estuvo apagado o la demo es dentro de más de
una semana, alcanza con reiniciarlo el día de la demo (`podman compose restart backend`, sin perder datos).

## 4. Comprobar que todo funcione

- Backend: http://localhost:8080/api/health
- Web: http://localhost:3000
- Mailpit: http://localhost:8025 (el correo con la contraseña temporal del médico; si `MAIL_*` apunta a un SMTP real,
  llega a esa casilla)
- Emulador con la app móvil, si se muestra la parte mobile (las notificaciones push necesitan `mobile/google-services.json`).

## 5. Avisar lo que debe tener a mano la persona

- Paso 7: el turno tiene que ser del día de la demo y empezar dentro de la hora siguiente a la hora de la presentación
  (la orden se emite desde una hora antes). Con una demo a las 20:00, por ejemplo, un turno de 21:00 a 22:15. Elegir
  siempre un turno corto que no pase de medianoche.
- Qué datos va a cargar en vivo: cédula, nombre, apellido, correo y fecha de nacimiento del paciente y del médico,
  matrícula, especialidad nueva, horario del turno y los anuncios.
- Usuarios del seed que se usan: administrador `admin@mediturno.com` / `admin123` y funcionario
  `funcionario@mediturno.com` / `funcionario123`. El médico y el paciente se crean en vivo (pasos 3 y 4); no se usan
  `medico@` ni `paciente@mediturno.com` salvo como plan B.

## Archivos de esta carpeta

- `PASOS.md`: orden de la demo, por rol, con notas puntuales.
- `consultorios-demo.csv`: archivo del alta masiva de consultorios (paso 6).
- `limpiar-firebase.mjs`: borra de Firebase las cuentas de los correos indicados (simulacro por defecto).

El commit lo hace siempre la persona, no el agente.
