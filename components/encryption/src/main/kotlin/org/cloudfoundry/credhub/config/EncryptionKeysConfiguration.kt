package org.cloudfoundry.credhub.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("encryption")
class EncryptionKeysConfiguration {

    var providers: List<EncryptionKeyProvider>? = null
    var isKeyCreationEnabled: Boolean = false


}
