# Stage 1: Build Java classes
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy source and libraries
COPY WebContent/WEB-INF/lib /app/lib
COPY src /app/src

# Create classes directory and compile Java files
RUN mkdir -p /app/classes
RUN javac -encoding UTF-8 -cp "/app/lib/*" -d /app/classes $(find /app/src -name "*.java")

# Stage 2: Run Tomcat
FROM tomcat:9.0-jdk17-temurin-jammy

# Remove default ROOT application
RUN rm -rf /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/webapps/examples /usr/local/tomcat/webapps/docs

# Copy web content and compiled classes to ROOT webapp
COPY WebContent /usr/local/tomcat/webapps/ROOT
COPY --from=build /app/classes /usr/local/tomcat/webapps/ROOT/WEB-INF/classes

# Create a startup script that handles Render's dynamic PORT
RUN echo '#!/bin/sh' > /usr/local/bin/docker-entrypoint.sh && \
    echo 'PORT=${PORT:-8080}' >> /usr/local/bin/docker-entrypoint.sh && \
    echo 'sed -i "s/port=\"8080\"/port=\"$PORT\"/g" /usr/local/tomcat/conf/server.xml' >> /usr/local/bin/docker-entrypoint.sh && \
    echo 'echo "Starting Tomcat on port $PORT..."' >> /usr/local/bin/docker-entrypoint.sh && \
    echo 'exec catalina.sh run' >> /usr/local/bin/docker-entrypoint.sh && \
    chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
