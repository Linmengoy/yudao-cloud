FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/eclipse-temurin:21-jre

RUN mkdir -p /yudao-module-aigc-asset-server
WORKDIR /yudao-module-aigc-asset-server
COPY ./target/yudao-module-aigc-asset-server.jar app.jar

ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms512m -Xmx512m"

EXPOSE 48093

CMD java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar app.jar
