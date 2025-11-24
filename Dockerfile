# 1. Usamos uma imagem leve do Java 17
FROM eclipse-temurin:17-jdk-alpine

# 2. Criamos um volume para arquivos temporários (opcional, boa prática)
VOLUME /tmp

# 3. Copiamos o JAR gerado (assumindo que o Maven gera em /target)
# DICA: O nome do jar pode variar. O '*' ajuda a pegar qualquer versão.
COPY target/*.jar app.jar

# 4. Comando para iniciar a API
ENTRYPOINT ["java","-jar","/app.jar"]