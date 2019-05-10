package org.cloudfoundry.credhub.credentials


import org.cloudfoundry.credhub.domain.CredentialVersion
import org.cloudfoundry.credhub.remote.RemoteBackendClient
import org.cloudfoundry.credhub.requests.BaseCredentialGenerateRequest
import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.views.CredentialView
import org.cloudfoundry.credhub.views.DataResponse
import org.cloudfoundry.credhub.views.FindCredentialResult
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("remote")
class RemoteCredentialsHandler : CredentialsHandler {

    private var client: RemoteBackendClient = RemoteBackendClient()

    override fun findStartingWithPath(path: String, expiresWithinDays: String): List<FindCredentialResult> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun findContainingName(name: String, expiresWithinDays: String): List<FindCredentialResult> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun generateCredential(generateRequest: BaseCredentialGenerateRequest): CredentialView {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun setCredential(setRequest: BaseCredentialSetRequest<*>): CredentialView {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun deleteCredential(credentialName: String) {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getNCredentialVersions(credentialName: String, numberOfVersions: Int?): DataResponse {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllCredentialVersions(credentialName: String): DataResponse {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getCurrentCredentialVersions(credentialName: String): DataResponse {
//        println("credentialName = $credentialName")
//        val value = StringCredentialValue("some-value")
//        return DataResponse(listOf(CredentialView(
//            Instant.now(),
//            UUID.randomUUID(),
//            credentialName,
//            "some-type",
//            value
//        )))
//        inal GetByName request = EncryptRequest.newBuilder().setPlain(ByteString.copyFrom(value, Charset.forName(CHARSET))).build();
//        val response: EncryptResponse
//        try {
//            response = blockingStub.encrypt(request)
//        } catch (e: StatusRuntimeException) {
//            LOGGER.error("Error for request: " + request.getPlain(), e)
//            throw e
//        }
//
//        return EncryptedValue(key.getUuid(), response.getCipher().toByteArray(), byteArrayOf())

        val response = client.getByNameRequest(credentialName)
        println("response = $response")
        val credentialVersions = emptyList<CredentialVersion>()
        return DataResponse.fromEntity(credentialVersions)
    }

    override fun getCredentialVersionByUUID(credentialUUID: String): CredentialView {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
