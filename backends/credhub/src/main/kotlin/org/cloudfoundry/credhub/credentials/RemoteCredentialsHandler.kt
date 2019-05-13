package org.cloudfoundry.credhub.credentials

import org.cloudfoundry.credhub.auth.UserContextHolder
import org.cloudfoundry.credhub.credential.CredentialValue
import org.cloudfoundry.credhub.credential.StringCredentialValue
import org.cloudfoundry.credhub.remote.RemoteBackendClient
import org.cloudfoundry.credhub.remote.grpc.GetByNameResponse
import org.cloudfoundry.credhub.requests.BaseCredentialGenerateRequest
import org.cloudfoundry.credhub.requests.BaseCredentialSetRequest
import org.cloudfoundry.credhub.views.CredentialView
import org.cloudfoundry.credhub.views.DataResponse
import org.cloudfoundry.credhub.views.FindCredentialResult
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
@Profile("remote")
class RemoteCredentialsHandler(private val userContextHolder: UserContextHolder) : CredentialsHandler {

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

        val actor = userContextHolder.userContext.actor
        val response = client.getByNameRequest(credentialName, actor)

        val credentialValue = getValueFromResponse(response)

        return DataResponse(listOf(CredentialView(
            Instant.now(),
            UUID.randomUUID(),
            credentialName,
            response.type,
            credentialValue
        )))
    }

    private fun getValueFromResponse(response: GetByNameResponse): CredentialValue? {
        return when (response.type) {
            "value" -> StringCredentialValue(response.data.toStringUtf8())
            else -> null
        }
    }

    override fun getCredentialVersionByUUID(credentialUUID: String): CredentialView {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
