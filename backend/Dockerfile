# Imagen del backend de TicketDesk para desplegar en Render (Root Directory: backend).
# Etapa 1: compila el proyecto con Maven. Etapa 2: ejecuta solo el JAR resultante.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Memoria acotada para el plan gratuito (512 MB de RAM) y zona horaria de Argentina,
# para que las fechas guardadas coincidan con las que ve el usuario.
ENV JAVA_TOOL_OPTIONS="-Xmx300m -XX:+UseSerialGC -Duser.timezone=America/Argentina/Buenos_Aires"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
