package org.cloudfoundry.credhub.config

open class EncryptionKeyMetadata {
    var encryptionPassword: String? = null
    private var active: Boolean = false
    open var encryptionKeyName: String? = null

    var isActive: Boolean?
        get() = active
        set(active) {
            this.active = active!!
        }

    override fun toString(): String {
        return "EncryptionKeyMetadata{" +
            "encryptionPassword='" + encryptionPassword + '\''.toString() +
            ", active=" + active +
            ", encryptionKeyName='" + encryptionKeyName + '\''.toString() +
            '}'.toString()
    }
}
