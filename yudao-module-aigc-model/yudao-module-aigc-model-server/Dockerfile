FROM eclipse-temurin:21-jre

RUN mkdir -p /yudao-module-aigc-model-server
WORKDIR /yudao-module-aigc-model-server
COPY ./target/yudao-module-aigc-model-server.jar app.jar

ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms512m -Xmx512m"

EXPOSE 48083

CMD java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar app.jar
