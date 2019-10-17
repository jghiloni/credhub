package org.cloudfoundry.credhub.services;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import org.cloudfoundry.credhub.generators.PasswordCredentialGenerator;


@Component
public class PasswordGeneratorProvider {
  @Autowired
  private PasswordCredentialGenerator passwordCredentialGenerator;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;
  private Class<? extends ServerChannel> channelType;
  private ManagedChannel chan;
  Server server;


  public PasswordGeneratorProvider() throws IOException, InterruptedException {
    setChannelInfo();
    PasswordGeneratorService passwordGeneratorService = new PasswordGeneratorService(passwordCredentialGenerator);
    server = NettyServerBuilder.forAddress(new DomainSocketAddress("/socket/socketfile.sock"))
      .bossEventLoopGroup(bossGroup)
      .workerEventLoopGroup(workerGroup)
      .channelType(channelType)
      .addService(passwordGeneratorService)
      .build();
    server.start();
    server.awaitTermination();
  }

  private void setChannelInfo() {
    if (Epoll.isAvailable()) {
      this.bossGroup = new EpollEventLoopGroup();
      this.workerGroup = new EpollEventLoopGroup();
      this.channelType = EpollServerDomainSocketChannel.class;
    } else {
      if (!KQueue.isAvailable()) {
        throw new RuntimeException("Unsupported OS '" + System.getProperty("os.name") + "', only Unix and Mac are supported");
      }
      this.workerGroup = new KQueueEventLoopGroup();
      this.bossGroup = new KQueueEventLoopGroup();
      this.channelType = KQueueServerDomainSocketChannel.class;
    }

  }
}
