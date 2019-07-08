package org.cloudfoundry.credhub.services

import com.google.common.collect.Lists
import org.cloudfoundry.credhub.ErrorMessages
import org.cloudfoundry.credhub.audit.AuditableCredentialVersion
import org.cloudfoundry.credhub.audit.CEFAuditRecord
import org.cloudfoundry.credhub.credential.CertificateCredentialValue
import org.cloudfoundry.credhub.domain.CertificateCredentialFactory
import org.cloudfoundry.credhub.domain.CertificateCredentialVersion
import org.cloudfoundry.credhub.domain.CertificateMetadata
import org.cloudfoundry.credhub.domain.CredentialVersion
import org.cloudfoundry.credhub.entity.Credential
import org.cloudfoundry.credhub.exceptions.EntryNotFoundException
import org.cloudfoundry.credhub.exceptions.InvalidQueryParameterException
import org.cloudfoundry.credhub.exceptions.ParameterizedValidationException
import org.cloudfoundry.credhub.requests.BaseCredentialGenerateRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class DefaultCertificateService(
    private val credentialService: CredentialService,
    private val certificateDataService: CertificateDataService,
    private val certificateVersionDataService: CertificateVersionDataService,
    private val certificateCredentialFactory: CertificateCredentialFactory,
    private val credentialVersionDataService: CredentialVersionDataService,
    private val auditRecord: CEFAuditRecord,
    @Value("\${certificates.concatenate_cas:false}") var concatenateCas: Boolean
) {

    fun save(
        existingCredentialVersion: CredentialVersion,
        credentialValue: CertificateCredentialValue,
        generateRequest: BaseCredentialGenerateRequest
    ): CredentialVersion {
        generateRequest.type = "certificate"
        if (credentialValue.isTransitional) {
            validateNoTransitionalVersionsAlreadyExist(generateRequest.name)
        }
        val version = credentialService
            .save(
                existingCredentialVersion,
                credentialValue,
                generateRequest
            ) as CertificateCredentialVersion

        if (version.isVersionTransitional) {
            createNewChildVersions(version.name)
        }
        return version
    }

    fun getAll(): List<Credential> {
        return certificateDataService.findAll()
    }

    fun getByName(name: String): List<Credential> {
        val certificate = certificateDataService.findByName(name)
            ?: throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)

        return listOf(certificate)
    }

    fun getVersions(uuid: UUID, current: Boolean): List<CredentialVersion> {
        val list: List<CredentialVersion>?
        val name: String?

        try {
            if (current) {
                val credential = findCertificateCredential(uuid)
                name = credential.name
                list = certificateVersionDataService.findActiveWithTransitional(name!!)
            } else {
                list = certificateVersionDataService.findAllVersions(uuid)
                name = if (!list.isEmpty()) list[0].name else null
            }
        } catch (e: IllegalArgumentException) {
            throw InvalidQueryParameterException(ErrorMessages.BAD_REQUEST, "uuid")
        }

        if (list!!.isEmpty() || name == null) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }

        return concatenateCas(list)
    }

    fun getAllValidVersions(uuid: UUID): List<CredentialVersion> {
        val list: List<CredentialVersion>?
        val name: String?

        try {
            list = certificateVersionDataService.findAllValidVersions(uuid)
            name = if (!list.isEmpty()) list[0].name else null
        } catch (e: IllegalArgumentException) {
            throw InvalidQueryParameterException(ErrorMessages.BAD_REQUEST, "uuid")
        }

        if (list.isEmpty()) {
            return emptyList()
        }

        if (name == null) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }

        return list
    }

    fun findAllValidMetadata(names: List<String>): List<CertificateMetadata> {
        return certificateDataService.findAllValidMetadata(names)
    }

    fun findSignedCertificates(caName: String): List<String> {
        return credentialService.findAllCertificateCredentialsByCaName(caName)
    }

    fun updateTransitionalVersion(certificateUuid: UUID, newTransitionalVersionUuid: UUID?): List<CredentialVersion> {
        val credential = findCertificateCredential(certificateUuid)

        val name = credential.name

        //todo: handle unset transitional - currently it is not setting the correct ca
        certificateVersionDataService.unsetTransitionalVersion(certificateUuid)

        if (newTransitionalVersionUuid != null) {
            val version = certificateVersionDataService.findVersion(newTransitionalVersionUuid)

            if (versionDoesNotBelongToCertificate(credential, version)) {
                throw ParameterizedValidationException(ErrorMessages.Credential.MISMATCHED_CREDENTIAL_AND_VERSION)
            }
            certificateVersionDataService.setTransitionalVersion(newTransitionalVersionUuid)
        }

        val credentialVersions = certificateVersionDataService.findActiveWithTransitional(name!!)
        auditRecord.addAllVersions(Lists.newArrayList<AuditableCredentialVersion>(credentialVersions!!))

        createNewChildVersions(name)

        return credentialVersions
    }

    fun deleteVersion(certificateUuid: UUID, versionUuid: UUID): CertificateCredentialVersion {
        val certificate = certificateDataService.findByUuid(certificateUuid)
            ?: throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)

        val versionToDelete = certificateVersionDataService.findVersion(versionUuid)

        if (versionDoesNotBelongToCertificate(certificate, versionToDelete)) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }

        if (certificateHasOnlyOneVersion(certificateUuid)) {
            throw ParameterizedValidationException(ErrorMessages.Credential.CANNOT_DELETE_LAST_VERSION)
        }

        certificateVersionDataService.deleteVersion(versionUuid)

        if (versionToDelete.isVersionTransitional) {
            createNewChildVersions(versionToDelete.name)
        }

        return versionToDelete
    }

    fun findByCredentialUuid(uuid: String): CertificateCredentialVersion {
        return certificateVersionDataService.findByCredentialUUID(uuid)
            as? CertificateCredentialVersion
            ?: throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
    }

    operator fun set(certificateUuid: UUID, value: CertificateCredentialValue): CertificateCredentialVersion {
        val credential = findCertificateCredential(certificateUuid)

        if (value.isTransitional) {
            validateNoTransitionalVersionsAlreadyExist(credential.name)
        }

        val certificateCredentialVersion = certificateCredentialFactory
            .makeNewCredentialVersion(credential, value)

        val version = credentialVersionDataService.save(certificateCredentialVersion) as CertificateCredentialVersion

        if (version.isVersionTransitional) {
            createNewChildVersions(version.name)
        }

        return version
    }

    private fun concatenateCas(credentialVersions: List<CredentialVersion>): List<CredentialVersion> {
        if (!concatenateCas) return credentialVersions
        return credentialVersions.map {
            val certificateCredentialVersion = it as? CertificateCredentialVersion ?: return credentialVersions
            if (!certificateCredentialVersion.trustedCa.isNullOrEmpty()) {
                val trustedCa = certificateCredentialVersion.trustedCa
                val ca = certificateCredentialVersion.ca
                certificateCredentialVersion.ca = listOf(ca.trim(), trustedCa.trim()).joinToString("\n")
            }
            certificateCredentialVersion
        }
    }

    private fun createNewChildVersions(parentCredentialName: String) {
        if (!concatenateCas) {
            return
        }

        /**
            on regenerate
            sign with latest non transitional

            signing ca is always ca field
            trusted ca will be transitional or latest non transitional or null

            step 0
            ca -> no transitional versions
            cert -> ca field= version1 (signing ca), trusted ca= empty (no transitional version)

            step 1 -> regenerate ca
            ca -> 2 versions, none transitional
            cert -> ca field= version1 (signing ca), trusted ca=empty (no transitional version)

            step 2 -> set new ca as transitional
            ca -> 2 versions, 2=transitional
            cert -> 2 versions, ca field= version1, trusted ca=version2(transitional)

            step 3 -> move transitional flag
            ca -> 2 versions, 1=transitional
            cert -> 2 versions, ca field= version1, trusted ca= version2(signing ca is transitional, so we use latest non transitional here)

            step 4 -> bulk-regenerate
            ca -> 2 versions, 1=transitional
            cert -> 3 versions, ca field=version2, trusted ca=version1 (transitional version)

            step 5 -> remove transitional flag
            ca -> 2 versions, none transitional
            cert -> 4 versions, ca field=version2, trusted ca= empty(no transitional version)
        */


        val signedCertificates = findSignedCertificates(parentCredentialName)
        signedCertificates.forEach {
            val credentialVersion = credentialVersionDataService.findMostRecent(it) as? CertificateCredentialVersion
            val credential = credentialVersion?.credential

            val ca = credentialVersion?.ca
            var trustedCa: CertificateCredentialVersion? = null
            val activeWithTransitional = certificateVersionDataService.findActiveWithTransitional(parentCredentialName)


            val mapIndexed = activeWithTransitional?.forEach { version ->
                val caVersion = version as CertificateCredentialVersion

                if (caVersion.certificate == ca)
                if (caVersion.isVersionTransitional)
            }
//                if (caVersion.certificate != ca) {
//                    if (!caVersion.isVersionTransitional && trustedCa == null) {
//                        // Either certificate was signed by a version that is now transitional or signing version has been deleted
//                        if (activeWithTransitional.size == 2) {
//                            trustedCa = caVersion
//                        }
//                    } else {
//                        trustedCa = caVersion
//                    }
//                }
                /**

                    +--------+--------------+---------+---------------------------+
                    | Length | Transitional | Signing | Result (Which is trusted) |
                    +--------+--------------+---------+---------------------------+
                    | 1      | True         | True    | none                      |
                    +--------+--------------+---------+---------------------------+
                    | 1      | True         | False   | transitional version      |
                    +--------+--------------+---------+---------------------------+
                    | 1      | False        | True    | none                      |
                    +--------+--------------+---------+---------------------------+
                    | 1      | False        | False   | none                      |
                    +--------+--------------+---------+---------------------------+
                    | 2      | True         | True    | non-transitional version  |
                    +--------+--------------+---------+---------------------------+
                    | 2      | True         | False   | transitional version      |
                    +--------+--------------+---------+---------------------------+
                    | 2      | False        | True    | transitional version      |
                    +--------+--------------+---------+---------------------------+
                    | 2      | False        | False   | transitional version      |
                    +--------+--------------+---------+---------------------------+

                 */


            val value = CertificateCredentialValue(
                ca,
                credentialVersion?.certificate,
                credentialVersion?.privateKey,
                credentialVersion?.caName,
                credentialVersion?.isCertificateAuthority ?: false,
                credentialVersion?.isSelfSigned ?: false,
                credentialVersion?.generated,
                credentialVersion?.isVersionTransitional ?: false
            )

            val newCredentialVersion = certificateCredentialFactory
                .makeNewCredentialVersion(credential, value)

            newCredentialVersion.trustedCa = trustedCa?.certificate

            credentialVersionDataService.save(newCredentialVersion)
        }

    }

    private fun versionDoesNotBelongToCertificate(certificate: Credential, version: CertificateCredentialVersion?): Boolean {
        return version == null || certificate.uuid != version.credential.uuid
    }

    private fun certificateHasOnlyOneVersion(certificateUuid: UUID): Boolean {
        return certificateVersionDataService.findAllVersions(certificateUuid).size == 1
    }

    private fun findCertificateCredential(certificateUuid: UUID): Credential {
        return certificateDataService.findByUuid(certificateUuid)
            ?: throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
    }

    private fun validateNoTransitionalVersionsAlreadyExist(name: String?) {
        val credentialVersions = credentialService
            .findAllByName(name!!)

        val transitionalVersionsAlreadyExist = credentialVersions.stream()
            .map { version -> version as CertificateCredentialVersion }
            .anyMatch { version -> version.isVersionTransitional }

        if (transitionalVersionsAlreadyExist) {
            throw ParameterizedValidationException(ErrorMessages.TOO_MANY_TRANSITIONAL_VERSIONS)
        }
    }
}
