package org.cloudfoundry.credhub.services;

import java.io.IOException;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;


@Component
@EnableScheduling
public class PasswordGeneratorProvider {

  private EventLoopGroup workerGroup;
  private Class<? extends Channel> channelType;
  private Class<? extends ServerChannel> serverChannelType;
  private ManagedChannel chan;
  private PasswordGeneratorService passwordGeneratorService;
  Server server;


  public PasswordGeneratorProvider() throws IOException {
    setChannelInfo();
    chan = NettyChannelBuilder
      .forAddress(new DomainSocketAddress("/socket/socketfile.sock"))
      .usePlaintext()
      .eventLoopGroup(workerGroup)
      .channelType(channelType)
      .build();
    passwordGeneratorService = new PasswordGeneratorService();
    server = NettyServerBuilder.forAddress(new DomainSocketAddress("/socket/socketfile.sock"))
      .bossEventLoopGroup(workerGroup)
      .workerEventLoopGroup(workerGroup)
      .channelType(serverChannelType)
      .addService(passwordGeneratorService)
      .build();
    System.out.println("******************starting password generator server******************\n");
    server.start();
//    server.awaitTermination();
  }

  @Scheduled(fixedDelay = 30000, initialDelay = 0)
  public void status() {
    System.out
      .println("Server status: is shutdown? " + server.isShutdown() + " is terminated? " + server.isTerminated());
    System.out.println("Server listening on address: " + server.getListenSockets() + " " + server.getPort());
  }

  private void setChannelInfo() {
    if (Epoll.isAvailable()) {
      this.workerGroup = new EpollEventLoopGroup();
      this.channelType = EpollDomainSocketChannel.class;
      this.serverChannelType = EpollServerDomainSocketChannel.class;
    } else {
      if (!KQueue.isAvailable()) {
        throw new RuntimeException("Unsupported OS '" + System.getProperty("os.name") + "', only Unix and Mac are supported");
      }
      this.workerGroup = new KQueueEventLoopGroup();
      this.channelType = KQueueDomainSocketChannel.class;
      this.serverChannelType = KQueueServerDomainSocketChannel.class;
    }

  }
}
