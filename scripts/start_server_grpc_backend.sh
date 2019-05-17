#!/bin/bash

set -euo pipefail

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )"/.. && pwd )"

rm -rf "$DIR/build"
"$DIR/scripts/setup_dev_mtls.sh"
"$DIR/scripts/setup_dev_grpc_certs.sh"
"$DIR/gradlew" --no-daemon downloadBouncyCastleFips
"$DIR/gradlew" --no-daemon assemble

exec "$DIR/gradlew" \
  --no-daemon \
  bootRun \
  -Djava.security.egd=file:/dev/urandom \
  -Djdk.tls.ephemeralDHKeySize=4096 \
  -Djdk.tls.namedGroups="secp384r1" \
  -Djavax.net.ssl.trustStore=src/test/resources/auth_server_trust_store.jks \
  -Djavax.net.ssl.trustStorePassword=changeit \
  -Dspring.profiles.active=dev,dev-h2,remote \
  "$@"
