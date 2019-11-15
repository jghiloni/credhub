package org.cloudfoundry.credhub.credential

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.apache.commons.lang3.StringUtils
import org.cloudfoundry.credhub.utils.CertificateReader
import org.cloudfoundry.credhub.utils.EmptyStringToNull
import java.time.Instant
import java.util.Objects

class CertificateCredentialValue : CredentialValue{
    @JsonDeserialize(using = EmptyStringToNull::class)
    var ca: String? = null
    @JsonDeserialize(using = EmptyStringToNull::class)
    var certificate: String? = null
    @JsonDeserialize(using = EmptyStringToNull::class)
    var privateKey: String? = null
    @JsonDeserialize(using = EmptyStringToNull::class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    var caName: String? = null
        set(value) {
            field = StringUtils.prependIfMissing(value, "/")
        }
    @JsonIgnore
    var trustedCa: String? = null

    var transitional: Boolean = false
    var certificateAuthority: Boolean = false
    var selfSigned: Boolean = false
    @JsonInclude(JsonInclude.Include.NON_NULL)
    var generated: Boolean? = null

    val expiryDate: Instant?
        get() {
            return CertificateReader(certificate).notAfter
        }

    constructor(): super()

    constructor(
            ca: String?,
            certificate: String?,
            privateKey: String?,
            caName: String?,
            certificateAuthority: Boolean,
            selfSigned: Boolean,
            generated: Boolean?,
            transitional: Boolean): this(ca, certificate, privateKey, caName, null, certificateAuthority, selfSigned, generated, transitional)

    constructor(
            ca: String?,
            certificate: String?,
            privateKey: String?,
            caName: String?,
            trustedCa: String?,
            certificateAuthority: Boolean,
            selfSigned: Boolean,
            generated: Boolean?,
            transitional: Boolean): super() {

        this.ca = ca
        this.trustedCa = trustedCa
        this.certificate = certificate
        this.privateKey = privateKey
        this.transitional = transitional
        this.certificateAuthority = certificateAuthority
        this.selfSigned = selfSigned
        this.generated = generated
        this.caName = caName
    }

    fun isTransitional(): Boolean {
        return transitional
    }

    fun isCertificateAuthority(): Boolean {
        return certificateAuthority
    }

    fun isSelfSigned(): Boolean {
        return selfSigned
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that = other as CertificateCredentialValue? ?: return false
        return transitional == that.transitional &&
                certificateAuthority == that.certificateAuthority &&
                selfSigned == that.selfSigned &&
                ca == that.ca &&
                certificate == that.certificate &&
                privateKey == that.privateKey &&
                caName == that.caName
    }

    override fun hashCode(): Int {
        return Objects.hash(ca, certificate, privateKey, caName, transitional, certificateAuthority, selfSigned)
    }
}