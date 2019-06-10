package org.cloudfoundry.credhub.remote

import com.google.protobuf.ByteString
import io.grpc.internal.GrpcUtil
import io.grpc.netty.NegotiationType
import io.grpc.netty.NettyChannelBuilder
import io.netty.channel.Channel
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollDomainSocketChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueDomainSocketChannel
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.unix.DomainSocketAddress
import org.apache.logging.log4j.LogManager
import org.cloudfoundry.credhub.ErrorMessages
import org.cloudfoundry.credhub.exceptions.EntryNotFoundException
import org.cloudfoundry.credhub.remote.grpc.CredentialServiceGrpc
import org.cloudfoundry.credhub.remote.grpc.DeleteByNameRequest
import org.cloudfoundry.credhub.remote.grpc.DeleteResponse
import org.cloudfoundry.credhub.remote.grpc.FindContainingNameRequest
import org.cloudfoundry.credhub.remote.grpc.FindResponse
import org.cloudfoundry.credhub.remote.grpc.GetByIdRequest
import org.cloudfoundry.credhub.remote.grpc.GetByNameRequest
import org.cloudfoundry.credhub.remote.grpc.GetResponse
import org.cloudfoundry.credhub.remote.grpc.SetRequest
import org.cloudfoundry.credhub.remote.grpc.SetResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@Profile("remote")
class RemoteBackendClient(
    @Value("\${backend.socket_file}") private val socketFile: String
) {

    companion object {
        private val LOGGER = LogManager.getLogger(RemoteBackendClient::class.java)
    }

    private lateinit var group: EventLoopGroup
    private lateinit var channelType: Class<out Channel>
    private lateinit var blockingStub: CredentialServiceGrpc.CredentialServiceBlockingStub

    init {
        setChannelInfo()
        blockingStub = CredentialServiceGrpc.newBlockingStub(
            NettyChannelBuilder.forAddress(DomainSocketAddress(socketFile))
                .eventLoopGroup(group)
                .channelType(channelType)
                .negotiationType(NegotiationType.PLAINTEXT)
                .keepAliveTime(GrpcUtil.DEFAULT_KEEPALIVE_TIME_NANOS, TimeUnit.NANOSECONDS)
                .build())

        LOGGER.info("using socket file $socketFile")
    }

    fun getByNameRequest(credentialName: String, user: String): GetResponse {
        val request = GetByNameRequest
            .newBuilder()
            .setName(credentialName)
            .setRequester(user)
            .build()

        val getResponse: GetResponse

        try {
            getResponse = blockingStub.getByName(request)
        } catch (e: RuntimeException) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }

        return getResponse
    }

    fun getByIdRequest(credentialUuid: String, user: String): GetResponse {
        val request = GetByIdRequest
            .newBuilder()
            .setId(credentialUuid)
            .setRequester(user)
            .build()

        val getResponse: GetResponse

        try {
            getResponse = blockingStub.getById(request)
        } catch (e: RuntimeException) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }

        return getResponse
    }

    fun setRequest(name: String, type: String, data: ByteString, user: String): SetResponse {
        val request = SetRequest
            .newBuilder()
            .setName(name)
            .setRequester(user)
            .setType(type)
            .setData(data)
            .build()

        val setResponse: SetResponse

        try {
            setResponse = blockingStub.set(request)
        } catch (e: RuntimeException) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }

        return setResponse
    }

    fun deleteRequest(name: String, user: String): DeleteResponse {
        val request = DeleteByNameRequest
            .newBuilder()
            .setName(name)
            .setRequester(user)
            .build()

        val deleteResponse: DeleteResponse

        try {
            deleteResponse = blockingStub.delete(request)
        } catch (e: RuntimeException) {
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }

        return deleteResponse
    }

    fun findContainingNameRequest(name: String, user: String) : FindResponse {
        val request = FindContainingNameRequest
            .newBuilder()
            .setName(name)
            .setRequester(user)
            .build()

        val findResponse: FindResponse

        try {
            findResponse = blockingStub.findContainingName(request)
        }catch (e: RuntimeException){
            throw EntryNotFoundException(ErrorMessages.Credential.INVALID_ACCESS)
        }

        return findResponse
    }

    private fun setChannelInfo() {
        when {
            Epoll.isAvailable() -> {
                group = EpollEventLoopGroup()
                channelType = EpollDomainSocketChannel::class.java
                LOGGER.info("Using epoll for Netty transport.")
            }
            KQueue.isAvailable() -> {
                group = KQueueEventLoopGroup()
                channelType = KQueueDomainSocketChannel::class.java
                LOGGER.info("Using KQueue for Netty transport.")
            }
            else -> {
                throw RuntimeException("Unsupported OS '" + System.getProperty("os.name") + "', only Unix and Mac are supported")
            }
        }
    }
}
