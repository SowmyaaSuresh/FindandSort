FROM eclipse-temurin:17-jdk-jammy

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb x11vnc novnc websockify x11-utils bash \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY src ./src
RUN mkdir -p out && javac -encoding UTF-8 -d out src/findsort/FindAndSortApp.java

COPY start.sh ./start.sh
RUN chmod 755 ./start.sh

EXPOSE 10000
CMD ["/bin/bash", "/app/start.sh"]
