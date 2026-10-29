# OmniCast TV Signage & Controller 📺📱

Solución completa de cartelería digital y control remoto de televisores Android TV en una **única APK universal**.

---

## 🚀 Características Principales

1. **Una Sola APK Universal:**
   - Detecta si el dispositivo es un Android TV o un teléfono/tablet.
   - En Android TV arranca directamente en **Modo TV (Reproductor Kiosk)**.
   - En móviles/tablets arranca en **Modo Controlador (Mando)**.

2. **Encendido Remoto (Wake-on-LAN):**
   - Envío de *Magic Packets* UDP para encender televisores en modo reposo (Standby) a través de su dirección MAC en la red Wi-Fi o cable Ethernet.

3. **Reproducción Limpia (Zero UI):**
   - ExoPlayer integrado a pantalla completa inmersiva real sin barras de estado, navegación, botones de pausa ni controles visuales.

4. **Reproducción en Bucle Infinito (Loop):**
   - Transiciones suaves y sin cortes entre repeticiones.

5. **Arranque Automático al Encender la TV:**
   - Receptor `BOOT_COMPLETED` y `QUICKBOOT_POWERON`.
   - Cada TV guarda su video y configuración localmente en memoria interna. Al prenderse o regresar de un corte de luz, el video asignado se reproduce solo sin necesidad de abrir la app manualmente ni de que el celular esté presente.

6. **Doble Fuente de Video:**
   - **Enlaces directos:** URLs HTTP/HTTPS (MP4, MKV, transmisiones HLS .m3u8).
   - **Enviados desde el celular:** Selecciona un video de tu galería o almacenamiento del teléfono y se transfiere por red local directamente al almacenamiento de la TV.

7. **Descubrimiento Automático por Red (mDNS / Bonjour):**
   - La app del teléfono detecta automáticamente todos los televisores conectados a la misma red Wi-Fi sin necesidad de configurar direcciones IP manualmente.

---

## 🛠️ Estructura del Proyecto

```
tv-controller-signage/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Configuración híbrida TV/Móvil + BootReceiver
│   │   ├── java/com/omnicast/tvsignage/
│   │   │   ├── MainActivity.kt          # Enrutador inteligente TV / Controlador
│   │   │   ├── data/
│   │   │   │   ├── AppPreferences.kt    # Persistencia de video asignado y TVs
│   │   │   │   └── TvDevice.kt          # Modelo de datos de cada pantalla
│   │   │   ├── tv/
│   │   │   │   ├── TvPlayerActivity.kt  # Reproductor ExoPlayer sin UI + bucle
│   │   │   │   ├── BootReceiver.kt      # Autoarranque al encender la TV
│   │   │   │   ├── TvHttpServer.kt      # Servidor NanoHTTPD embebido (recibe videos y URLs)
│   │   │   │   └── NsdServerHelper.kt   # Anuncio mDNS del televisor en la red
│   │   │   ├── controller/
│   │   │   │   ├── ControllerActivity.kt# Panel móvil de control y asignación
│   │   │   │   ├── TvDeviceAdapter.kt   # Lista visual de pantallas con estados
│   │   │   │   ├── TvApiClient.kt       # Cliente de red OkHttp (envío y subida)
│   │   │   │   └── NsdDiscoveryHelper.kt# Escaneo mDNS automático de TVs
│   │   │   └── utils/
│   │   │       ├── NetworkUtils.kt      # Obtención de IP y MAC
│   │   │       └── WakeOnLanHelper.kt   # Magic Packet UDP para encender TVs
│   │   └── res/                         # Layouts, recursos y temas
│   └── build.gradle.kts                 # Dependencias (Media3, NanoHTTPD, OkHttp)
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 📦 Cómo Compilar e Instalar

### 1. Compilar con Android Studio
1. Abre **Android Studio**.
2. Selecciona **Open** y navega a la carpeta:
   `C:\Users\Asus\.gemini\antigravity\scratch\tv-controller-signage`
3. Espera a que Gradle sincronice las dependencias.
4. Ve al menú superior: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. El APK generado estará en:
   `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Instalación en los Dispositivos
- **En el Teléfono / Tablet:**
  - Instala `app-debug.apk` normalmente.
  - Al abrirlo, selecciona **Modo Controlador**.
- **En los Televisores Android TV:**
  - Puedes pasar el APK mediante una memoria USB, o usando apps de la Play Store de la TV como *Send Files to TV* o mediante ADB:
    ```bash
    adb connect <IP_DE_LA_TV>
    adb install -r app-debug.apk
    ```
  - Al abrir la app en la TV, se iniciará automáticamente en **Modo Televisor**.

---

## ⚙️ Configuración Importante en los Televisores

1. **Activar Wake-on-LAN (para poder encenderlos desde el celular):**
   - En el televisor, entra a **Ajustes > Red e Internet** (o **Ajustes > Pantalla y sonido > Encendido**).
   - Activa las opciones:
     - **Control de red / Encendido remoto**
     - **Wake on Wi-Fi / Wake on LAN**
     - **Modo de inicio rápido / Standby conectado**
2. **Permisos de arranque en segundo plano:**
   - En algunos modelos con capas personalizadas (como Xiaomi o TCL), asegúrate de permitir que la app tenga permiso de *"Inicio automático"* para que el `BootReceiver` levante el video apenas prenda la pantalla.
