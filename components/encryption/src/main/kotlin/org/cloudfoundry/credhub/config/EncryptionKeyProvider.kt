package org.cloudfoundry.credhub.config

import java.util.ArrayList

open class EncryptionKeyProvider {
    var providerName: String? = null
    open var providerType: ProviderType? = null
    open var keys: List<EncryptionKeyMetadata> = ArrayList()
    var configuration: EncryptionConfiguration? = null

    override fun toString(): String {
        return "EncryptionKeyProvider{" +
            "providerName='" + providerName + '\''.toString() +
            ", providerType=" + providerType +
            ", keys=" + keys +
            ", configuration=" + configuration +
            '}'.toString()
    }
}
