package org.cloudfoundry.credhub.remote

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
import org.cloudfoundry.credhub.remote.grpc.GetByIdRequest
import org.cloudfoundry.credhub.remote.grpc.GetByNameRequest
import org.cloudfoundry.credhub.remote.grpc.GetResponse
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit


@Service
class RemoteBackendClient {

    companion object {
        private val LOGGER = LogManager.getLogger(RemoteBackendClient::class.java)
    }

    private lateinit var group: EventLoopGroup
    private lateinit var channelType: Class<out Channel>
    private lateinit var blockingStub: CredentialServiceGrpc.CredentialServiceBlockingStub



    init {
        setChannelInfo()
        blockingStub = CredentialServiceGrpc.newBlockingStub(
            //todo: move socket address to application.yaml
            NettyChannelBuilder.forAddress(DomainSocketAddress("/tmp/socket/test.sock"))
                .eventLoopGroup(group)
                .channelType(channelType)
                .negotiationType(NegotiationType.PLAINTEXT)
                .keepAliveTime(GrpcUtil.DEFAULT_KEEPALIVE_TIME_NANOS, TimeUnit.NANOSECONDS)
                .build())
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
                //todo: look into nio as a default option
                throw RuntimeException("Unsupported OS '" + System.getProperty("os.name") + "', only Unix and Mac are supported")
            }
        }
    }
}
