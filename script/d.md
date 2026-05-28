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

gateway-server

本地构建

```
mvn -pl yudao-gateway -am package -DskipTests
```

```java
java -jar .\yudao-gateway\target\yudao-gateway.jar
```

admin创建

```
npm run dev-server
```

member-server

```
mvn -pl yudao-module-member/yudao-module-member-server -am package -DskipTests
```

```
java -jar .\yudao-module-member\yudao-module-member-server\target\yudao-module-member-server.jar
```

pay-server

```
mvn -pl yudao-module-pay/yudao-module-pay-server -am package -DskipTests
```

```
java -jar .\yudao-module-pay\yudao-module-pay-server\target\yudao-module-pay-server.jar
```

aigc-asset-server

```
mvn -pl yudao-module-aigc-asset/yudao-module-aigc-asset-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-asset\yudao-module-aigc-asset-server\target\yudao-module-aigc-asset-server.jar
```

aigc-task-server

```
mvn -pl yudao-module-aigc-task/yudao-module-aigc-task-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-task\yudao-module-aigc-task-server\target\yudao-module-aigc-task-server.jar
```

aigc-billing-server

```
mvn -pl yudao-module-aigc-billing/yudao-module-aigc-billing-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-billing\yudao-module-aigc-billing-server\target\yudao-module-aigc-billing-server.jar
```

aigc-model-server

```
mvn -pl yudao-module-aigc-model/yudao-module-aigc-model-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-model\yudao-module-aigc-model-server\target\yudao-module-aigc-model-server.jar
```



aigc-safety-server

```
mvn -pl yudao-module-aigc-safety/yudao-module-aigc-safety-server -am package -DskipTests
```


```
java -jar .\yudao-module-aigc-safety\yudao-module-aigc-safety-server\target\yudao-module-aigc-safety-server.jar
```


aigc-gen-server

```
mvn -pl yudao-module-aigc-gen/yudao-module-aigc-gen-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-gen\yudao-module-aigc-gen-server\target\yudao-module-aigc-gen-server.jar
```

aigc-workflow-server

```
mvn -pl yudao-module-aigc-workflow/yudao-module-aigc-workflow-server -am package -DskipTests
```

```
java -jar .\yudao-module-aigc-workflow\yudao-module-aigc-workflow-server\target\yudao-module-aigc-workflow-server.jar
```
