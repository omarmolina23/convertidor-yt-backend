# ---------- Etapa 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache de dependencias: primero el pom, luego el código.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# ffmpeg (conversión) + aria2 (descargas multi-conexión, más rápidas) + yt-dlp
# (descarga) + deno (runtime JS que yt-dlp usa para extraer formatos de YouTube).
# yt-dlp y deno se instalan como binarios standalone.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg aria2 ca-certificates curl unzip \
    && curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux -o /usr/local/bin/yt-dlp \
    && chmod a+rx /usr/local/bin/yt-dlp \
    && curl -L https://github.com/denoland/deno/releases/latest/download/deno-x86_64-unknown-linux-gnu.zip -o /tmp/deno.zip \
    && unzip /tmp/deno.zip -d /usr/local/bin \
    && chmod a+rx /usr/local/bin/deno \
    && rm /tmp/deno.zip \
    && apt-get purge -y curl unzip \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Usuario sin privilegios.
RUN useradd -m appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

ENV CONVERTER_YTDLP_PATH=/usr/local/bin/yt-dlp \
    CONVERTER_FFMPEG_PATH=ffmpeg \
    CONVERTER_WORK_DIR=/tmp/convertidor-yt

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
