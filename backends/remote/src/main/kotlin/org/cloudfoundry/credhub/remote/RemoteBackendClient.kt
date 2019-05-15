package org.cloudfoundry.credhub.remote

import io.grpc.internal.GrpcUtil
import io.grpc.netty.NegotiationType
import io.grpc.netty.NettyChannelBuilder
import io.netty.channel.epoll.EpollDomainSocketChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.unix.DomainSocketAddress
import org.cloudfoundry.credhub.remote.grpc.CredentialServiceGrpc
import org.cloudfoundry.credhub.remote.grpc.GetByNameRequest
import org.cloudfoundry.credhub.remote.grpc.GetResponse
import java.util.concurrent.TimeUnit

class RemoteBackendClient {

    var blockingStub: CredentialServiceGrpc.CredentialServiceBlockingStub = CredentialServiceGrpc.newBlockingStub(
        NettyChannelBuilder.forAddress(DomainSocketAddress("/tmp/socket/test.sock"))
            .eventLoopGroup(EpollEventLoopGroup())
            .channelType(EpollDomainSocketChannel::class.java)
            .negotiationType(NegotiationType.PLAINTEXT)
            .keepAliveTime(GrpcUtil.DEFAULT_KEEPALIVE_TIME_NANOS, TimeUnit.NANOSECONDS)
            .build())

    fun getByNameRequest(credentialName: String, actor: String): GetResponse {
        val request = GetByNameRequest.newBuilder().setName(credentialName).setRequester(actor).build()

        return blockingStub.get(request)
    }
}
