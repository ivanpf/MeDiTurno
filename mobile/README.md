MeDiTurno - App móvil (Paciente)

App del paciente para MeDiTurno, hecha con React Native + Expo (Expo Router, JavaScript).
Reutiliza el backend (Spring Boot) y el diseño de la web (mismos colores, logo y menú del rol).
El backend y la base de datos se levantan aparte: ver el README.md de la raíz.

No funciona en Expo Go. La app usa módulos nativos (Firebase, notificaciones, reanimated,
gesture-handler). Hace falta un development build (una compilación propia de la app).


Requisitos

- Node.js 20 o superior con npm: correr Metro/Expo y la app.
- Podman con podman compose: levantar la base de datos y el backend.
- JDK 17: compilar e instalar en Android.
- Android Studio + SDK + emulador, o un teléfono con depuración USB activada.
- adb en el PATH: viene con platform-tools.


1. Ejecutar en Mac

Se usan dos terminales: una para los contenedores (base de datos + backend)
y otra para la app.

Terminal 1 - Base de datos y backend (contenedores Podman)

El backend ya viene empaquetado en la imagen, no hace falta correrlo a mano.

    podman compose up -d postgres backend

Verificar que quedó arriba (debe responder en http://localhost:8080):

    podman compose ps

Frenarlos:

    podman compose down

Terminal 2 - App móvil

    cd mobile
    npm install
    npx expo start

Primer build del development build (compila e instala la app):

    cd mobile
    npx expo run:android

Con Metro corriendo, apretá "a" para abrir Android. Si el dispositivo no conecta a Metro:

    adb reverse tcp:8081 tcp:8081


2. Ejecutar en Windows (PowerShell)

Se usan dos terminales, igual que en Mac.

Terminal 1 - Base de datos y backend (contenedores Podman)

    podman compose up -d postgres backend

Terminal 2 - App móvil

    cd mobile
    npm install
    npx expo start

Si expo no encuentra Java, definí el JDK 17 antes de compilar:

    $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"

Primer build del development build:

    cd mobile
    npx expo run:android

Con Metro corriendo, apretá "a" para abrir Android. Si el dispositivo no conecta:

    adb reverse tcp:8081 tcp:8081


3. Notificaciones push

La app recibe avisos por Firebase Cloud Messaging con el mismo proyecto de Firebase de la
autenticación. Necesita el archivo mobile/google-services.json (ya versionado); si lo cambia,
regenere la carpeta nativa con `npx expo prebuild --platform android --clean`.
El funcionamiento y cómo probarlo están en README-BACKEND-NOTIFICACIONES_PUSH.md.

En Windows, si Gradle no encuentra el SDK o el JDK, defina antes de compilar:

    $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"


4. Cómo llega la app al backend

En Android (emulador) la app usa 10.0.2.2 para llegar al localhost de tu computadora:
apunta al backend en http://10.0.2.2:8080 sin configuración extra.
Si usás un teléfono físico, redirigí también el backend:

    adb reverse tcp:8080 tcp:8080


5. Iniciar sesión

El login usa Firebase y ya viene configurado (datos públicos en app.json, en "extra").
Usá un usuario de prueba, por ejemplo:

    paciente@mediturno.com / paciente123

La lista completa de usuarios de prueba está en la sección "Autenticación (Firebase)" de CLAUDE.md.


6. Compilar para producción e instalar la APK

Opción A - EAS Build (en la nube, recomendado)

No hace falta Android Studio ni Xcode. Los perfiles están en eas.json.

    npm install -g eas-cli
    eas login
    eas build -p android --profile preview        (APK de prueba)
    eas build -p android --profile production     (AAB para Google Play)

Al terminar, EAS muestra un enlace para descargar la APK y un comando para instalarla:

    eas build:run -p android --profile preview

Opción B - Build local (sin nube)

Genera la carpeta nativa android/ y compila con Gradle.

    cd mobile
    npx expo prebuild --platform android --clean

    cd android
    ./gradlew assembleRelease        (Mac / Linux)
    .\gradlew.bat assembleRelease    (Windows PowerShell)

La APK queda en:

    mobile/android/app/build/outputs/apk/release/app-release.apk

assembleRelease necesita una firma. Para pruebas rápidas usá assembleDebug,
que ya viene firmado con la keystore de debug.

Instalar la APK en el dispositivo

    adb devices
    adb install -r android/app/build/outputs/apk/release/app-release.apk

Si ya estaba instalada, -r la reemplaza; si cambia la firma, primero desinstalá:

    adb uninstall com.mediturno.mobile


Notas

- android/ e ios/ son generados (Continuous Native Generation) y están en .gitignore:
  no se editan a mano, se configuran en app.json.
- La estructura de rutas y el menú del rol están en shared/src/navigation/rutas.js
  (el equivalente de frontend/src/routes.jsx).
