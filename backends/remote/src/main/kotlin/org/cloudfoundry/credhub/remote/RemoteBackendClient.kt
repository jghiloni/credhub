package org.cloudfoundry.credhub.remote

import io.grpc.internal.GrpcUtil
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import org.cloudfoundry.credhub.ErrorMessages
import org.cloudfoundry.credhub.exceptions.EntryNotFoundException
import org.cloudfoundry.credhub.remote.grpc.CredentialServiceGrpc
import org.cloudfoundry.credhub.remote.grpc.GetByNameRequest
import org.cloudfoundry.credhub.remote.grpc.GetResponse
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit


@Service
class RemoteBackendClient(
    remoteTlsConfig: RemoteTlsConfig
) {

    private final val caFile = File(remoteTlsConfig.trustedCa)
    private final val keyFile = File(remoteTlsConfig.clientPrivateKey)
    private final val certFile = File(remoteTlsConfig.clientCertificate)

    var blockingStub: CredentialServiceGrpc.CredentialServiceBlockingStub = CredentialServiceGrpc.newBlockingStub(
        NettyChannelBuilder.forAddress("localhost", 10000)
            .useTransportSecurity()
            .sslContext(GrpcSslContexts
                .forClient()
                .trustManager(caFile)
                .keyManager(certFile, keyFile)
                .build())
            .keepAliveTime(GrpcUtil.DEFAULT_KEEPALIVE_TIME_NANOS, TimeUnit.NANOSECONDS)
            .build())

    fun getByNameRequest(credentialName: String, user: String): GetResponse {
        val request = GetByNameRequest
            .newBuilder()
            .setName(credentialName)
            .setRequester(user)
            .build()

        val getResponse: GetResponse

        try {
            getResponse = blockingStub.get(request)
        } catch (e: RuntimeException) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }

        return getResponse
    }
}
