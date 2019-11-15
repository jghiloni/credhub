package org.cloudfoundry.credhub.validators

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import org.cloudfoundry.credhub.credential.CertificateCredentialValue
import org.cloudfoundry.credhub.utils.CertificateReader
import org.cloudfoundry.credhub.utils.PrivateKeyReader

class CertificateMatchesPrivateKeyValidator : ConstraintValidator<RequireCertificateMatchesPrivateKey, Any> {

    override fun initialize(constraintAnnotation: RequireCertificateMatchesPrivateKey?) {}

    override fun isValid(value: Any, context: ConstraintValidatorContext?): Boolean {

        val certificateCredentialValue = value as CertificateCredentialValue

        if (certificateCredentialValue.certificate.isNullOrEmpty() ||
            certificateCredentialValue.privateKey.isNullOrEmpty()) {
                return true
            }

        val certificateValue = certificateCredentialValue.certificate
        val privateKeyValue = certificateCredentialValue.privateKey

        val certificateReader = CertificateReader(certificateValue)
        val certificate = certificateReader.certificate
        val certificatePublicKey = certificate.publicKey

        val publicKey = privateKeyValue?.let { PrivateKeyReader.getPublicKey(it) }

        return publicKey == certificatePublicKey
    }
}
