
FROM openjdk:8
WORKDIR /app
COPY . /app

RUN ./scripts/setup_dev_mtls.sh
RUN ./gradlew clean bootJar

FROM openjdk:8-jre-slim
WORKDIR /app
COPY \
  --from=0 \
  /app/applications/credhub-api/build/libs/credhub.jar \
  .
COPY \
  --from=0 \
  /app/applications/credhub-api/src/test/resources/key_store.jks \
  .
COPY \
  --from=0 \
  /app/applications/credhub-api/src/test/resources/auth_server_trust_store.jks \
  ./trust_store.jks
COPY \
  --from=0 \
  /app/application.yml \
  ./application.yml

EXPOSE 900
EXPOSE 8844
CMD [ \
  "java", \
  "-Dspring.config.additional-location=/app/application.yml", \
  "-Djava.security.egd=file:/dev/urandom", \
  "-Djdk.tls.ephemeralDHKeySize=4096", \
  "-Djdk.tls.namedGroups=\"secp384r1\"", \
  "-Djavax.net.ssl.trustStore=trust_store.jks", \
  "-Djavax.net.ssl.trustStorePassword=changeit", \
  "-jar", \
  "credhub.jar" \
]
