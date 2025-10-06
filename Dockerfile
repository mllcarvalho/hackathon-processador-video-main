FROM openjdk:21-slim

RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    libopencv-dev \
    && rm -rf /var/lib/apt/lists/*

COPY target/hackathon-processador-video.jar /app/hackathon-processador-video.jar

ENV LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu:$LD_LIBRARY_PATH
