FROM tomcat:9.0-jdk17-temurin-jammy

# Remove default ROOT and sample apps
RUN rm -rf /usr/local/tomcat/webapps/ROOT /usr/local/tomcat/webapps/examples /usr/local/tomcat/webapps/docs

# Copy web content into ROOT
COPY WebContent /usr/local/tomcat/webapps/ROOT

# Copy Java source code and compile using Tomcat's servlet-api and MySQL jars
COPY src /app/src
RUN mkdir -p /usr/local/tomcat/webapps/ROOT/WEB-INF/classes && \
    javac -encoding UTF-8 -cp "/usr/local/tomcat/lib/*:/usr/local/tomcat/webapps/ROOT/WEB-INF/lib/*" -d /usr/local/tomcat/webapps/ROOT/WEB-INF/classes $(find /app/src -name "*.java")

# Create startup script for dynamic PORT binding on Render
RUN echo '#!/bin/sh' > /usr/local/bin/docker-entrypoint.sh && \
    echo 'PORT=${PORT:-8080}' >> /usr/local/bin/docker-entrypoint.sh && \
    echo 'sed -i "s/port=\"8080\"/port=\"$PORT\"/g" /usr/local/tomcat/conf/server.xml' >> /usr/local/bin/docker-entrypoint.sh && \
    echo 'echo "Starting Tomcat on port $PORT..."' >> /usr/local/bin/docker-entrypoint.sh && \
    echo 'exec catalina.sh run' >> /usr/local/bin/docker-entrypoint.sh && \
    chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
