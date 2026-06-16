# Convertidor YouTube — Backend

API REST en **Java 21 + Spring Boot** que convierte enlaces de YouTube a **MP3**
(audio) o **MP4** (video), con descarga **completa** o **por intervalo de tiempo**.
Orquesta `yt-dlp` + `ffmpeg` (+ `deno` como runtime JS para YouTube).

> Frontend (Flutter) en un repositorio separado: **convertidor-yt-frontend**.

> ⚖️ **Aviso legal.** Descargar contenido de YouTube puede infringir sus Términos
> de Servicio y/o derechos de autor. Úsalo solo con contenido propio, de dominio
> público o con licencia que lo permita. Proyecto con fines educativos.

---

## Arranque rápido (Docker)

Requiere solo **Docker**. Construye y corre el backend:

```bash
docker build -t convertidor-yt-backend .
docker run --rm -p 8080:8080 convertidor-yt-backend
```

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>

### Levantar backend + frontend juntos

Con el repo del frontend clonado como carpeta hermana (`../frontend`):

```bash
docker compose up --build
```

- Frontend: <http://localhost:8081>
- Backend: <http://localhost:8080>

---

## Desarrollo local (sin Docker)

Necesitas `yt-dlp` y `ffmpeg` en el `PATH` (y opcionalmente `deno`):

```bash
mvn spring-boot:run
```

Variables de entorno:

| Variable                  | Descripción                       | Default          |
|---------------------------|-----------------------------------|------------------|
| `CONVERTER_YTDLP_PATH`    | Ruta al ejecutable yt-dlp         | `yt-dlp`         |
| `CONVERTER_FFMPEG_PATH`   | Ruta al ejecutable ffmpeg         | `ffmpeg`         |
| `CONVERTER_WORK_DIR`      | Carpeta de archivos temporales    | tmp del sistema  |
| `CONVERTER_CORS_ORIGINS`  | Orígenes permitidos para CORS     | `*`              |

---

## API REST

Base: `/api/v1/conversions`

### Crear conversión — `POST /api/v1/conversions`

```json
{
  "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "format": "MP3",
  "quality": "192",
  "startTime": "00:30",
  "endTime": "01:45"
}
```

| Campo       | Obligatorio | Notas                                                        |
|-------------|-------------|--------------------------------------------------------------|
| `url`       | Sí          | Enlace válido de YouTube                                      |
| `format`    | Sí          | `MP3` o `MP4`                                                 |
| `quality`   | No          | MP3: bitrate (128/192/256/320). MP4: altura (480/720/1080)   |
| `startTime` | No          | Inicio del recorte (`MM:SS` o `HH:MM:SS`)                    |
| `endTime`   | No          | Fin del recorte. Si se omiten ambos, descarga completa       |

Respuesta `202 Accepted` con `jobId` y `statusUrl`.

### Estado — `GET /api/v1/conversions/{jobId}`

Devuelve `status` (`PENDING` → `PROCESSING` → `READY`/`FAILED`), `progress` y,
cuando está listo, `downloadUrl`.

### Descarga — `GET /api/v1/conversions/{jobId}/download`

Disponible cuando `status = READY`.

---

## Cómo funciona el recorte por intervalo

1. **Camino rápido:** `yt-dlp --download-sections` descarga solo el intervalo.
2. **Fallback robusto:** si ese paso falla (p. ej. `403` de ffmpeg con ciertas
   URLs de YouTube), descarga el medio completo con el descargador nativo de
   yt-dlp y recorta el intervalo localmente con ffmpeg.

---

## Pruebas

```bash
mvn test
```

---

## Stack

Java 21 · Spring Boot 3.4 · Maven · yt-dlp · ffmpeg · deno · springdoc-openapi ·
Docker · GitHub Actions.
