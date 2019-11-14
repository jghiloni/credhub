package org.cloudfoundry.credhub.config

class EncryptionConfiguration {
    var port: Int? = null
    var partition: String? = null
    var partitionPassword: String? = null
    var serverCa: String? = null
    var clientCertificate: String? = null
    var clientKey: String? = null

    var host: String? = null

    var endpoint: String? = null
    var ca: String? = null

    override fun toString(): String {
        return "EncryptionConfiguration{" +
            "port=" + port +
            ", partition='" + partition + '\''.toString() +
            ", partitionPassword='" + partitionPassword + '\''.toString() +
            ", serverCa='" + serverCa + '\''.toString() +
            ", clientCertificate='" + clientCertificate + '\''.toString() +
            ", clientKey='" + clientKey + '\''.toString() +
            ", host='" + host + '\''.toString() +
            ", endpoint='" + endpoint + '\''.toString() +
            ", ca='" + ca + '\''.toString() +
            '}'.toString()
    }
}
