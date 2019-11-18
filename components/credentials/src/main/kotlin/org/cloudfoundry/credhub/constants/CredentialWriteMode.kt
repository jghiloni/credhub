package org.cloudfoundry.credhub.constants

import com.fasterxml.jackson.annotation.JsonValue

enum class CredentialWriteMode(val mode: String) {
    OVERWRITE("overwrite"),
    NO_OVERWRITE("no-overwrite"),
    CONVERGE("converge");

    @JsonValue
    fun forJackson(): String {
        return mode
    }

    override fun toString(): String {
        return mode
    }
}
