package org.cloudfoundry.credhub.config

enum class ProviderType private constructor(private val label: String) {

    INTERNAL("internal"),
    HSM("hsm"),
    KMS_PLUGIN("kms-plugin")
}
