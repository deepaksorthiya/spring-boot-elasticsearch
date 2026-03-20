# Stage 1: Build Stage
FROM bellsoft/liberica-runtime-container:jdk-26-stream-musl AS builder

WORKDIR /home/app
ADD . /home/app/spring-boot-elasticsearch
RUN cd spring-boot-elasticsearch && chmod +x mvnw && ./mvnw -Dmaven.test.skip=true clean package

# Stage 2: Layer Tool Stage
FROM bellsoft/liberica-runtime-container:jdk-26-cds-slim-musl AS optimizer

WORKDIR /home/app
COPY --from=builder /home/app/spring-boot-elasticsearch/target/*.jar spring-boot-elasticsearch.jar
RUN java -Djarmode=tools -jar spring-boot-elasticsearch.jar extract --layers --launcher

# Stage 3: Final Stage
FROM bellsoft/liberica-runtime-container:jre-26-stream-musl

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
EXPOSE 8080
COPY --from=optimizer /home/app/spring-boot-elasticsearch/dependencies/ ./
COPY --from=optimizer /home/app/spring-boot-elasticsearch/spring-boot-loader/ ./
COPY --from=optimizer /home/app/spring-boot-elasticsearch/snapshot-dependencies/ ./
COPY --from=optimizer /home/app/spring-boot-elasticsearch/application/ ./