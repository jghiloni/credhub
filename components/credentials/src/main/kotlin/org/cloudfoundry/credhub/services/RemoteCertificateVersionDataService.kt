package org.cloudfoundry.credhub.services

import org.cloudfoundry.credhub.auth.UserContextHolder
import org.cloudfoundry.credhub.domain.CertificateCredentialVersion
import org.cloudfoundry.credhub.domain.CredentialVersion
import org.cloudfoundry.credhub.remote.RemoteBackendClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import io.grpc.StatusRuntimeException
import java.util.UUID

@Profile("remote")
@Service
class RemoteCertificateVersionDataService(
    private val userContextHolder: UserContextHolder,
    private val client: RemoteBackendClient
) : CertificateVersionDataService {
    override fun findActive(caName: String): CredentialVersion? {
        try {
            val credential = client.getByNameRequest(caName, userContextHolder.userContext.actor)
            credentialFactory.makeCredentialFromEntity(credentialVersionRepository
                .findLatestNonTransitionalCertificateVersion(credential.uuid))
        } catch (e: StatusRuntimeException) {
            return null
        }
    }

    override fun findByCredentialUUID(uuid: String): CredentialVersion? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findActiveWithTransitional(certificateName: String): List<CredentialVersion>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findAllVersions(uuid: UUID): List<CredentialVersion> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findAllValidVersions(uuid: UUID): List<CredentialVersion> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteVersion(versionUuid: UUID) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findVersion(versionUuid: UUID): CertificateCredentialVersion {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setTransitionalVersion(newTransitionalVersionUuid: UUID) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unsetTransitionalVersion(certificateUuid: UUID) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
