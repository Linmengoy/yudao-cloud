FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/eclipse-temurin:21-jre

RUN mkdir -p /yudao-module-aigc-billing-server
WORKDIR /yudao-module-aigc-billing-server
COPY ./target/yudao-module-aigc-billing-server.jar app.jar

ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms512m -Xmx512m"

EXPOSE 48092

CMD java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar app.jar
