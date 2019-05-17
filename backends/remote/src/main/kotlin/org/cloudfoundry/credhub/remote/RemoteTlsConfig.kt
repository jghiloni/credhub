package org.cloudfoundry.credhub.remote

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("grpc")
class RemoteTlsConfig {
    lateinit var trustedCa: String
    lateinit var clientCertificate: String
    lateinit var clientPrivateKey: String
}
