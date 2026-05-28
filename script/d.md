infra-server

本地构建

```
mvn -pl yudao-module-infra/yudao-module-infra-server -am package -DskipTests
```

```
java -jar .\yudao-module-infra\yudao-module-infra-server\target\yudao-module-infra-server.jar
```

system-server

本地构建

```
mvn -pl yudao-module-system/yudao-module-system-server -am package -DskipTests
```

```
java -jar .\yudao-module-system\yudao-module-system-server\target\yudao-module-system-server.jar
```
