package com.eu.habbo.networking.camera;

import com.eu.habbo.networking.camera.messages.outgoing.CameraLoginComposer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CameraClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CameraClient.class);

    private static final String host = "google.com";
    private static final int port = 1232;
    public static ChannelFuture channelFuture;
    public static boolean isLoggedIn = false;
    public static boolean attemptReconnect = true;
    private static Channel channel;
    private final Bootstrap bootstrap = new Bootstrap();

    public CameraClient() {

        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        this.bootstrap.group(eventLoopGroup);
        this.bootstrap.channel(NioSocketChannel.class);
        this.bootstrap.option(ChannelOption.TCP_NODELAY, true);
        this.bootstrap.option(ChannelOption.SO_KEEPALIVE, false);
        this.bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new CameraDecoder());
                ch.pipeline().addLast(new CameraHandler());
            }
        });
        this.bootstrap.option(ChannelOption.SO_RCVBUF, 5120);
        this.bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        this.bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(5120));
        this.bootstrap.option(ChannelOption.ALLOCATOR, new UnpooledByteBufAllocator(false));
    }

    public void connect() {
        CameraClient.channelFuture = this.bootstrap.connect(host, port);

        while (!CameraClient.channelFuture.isDone()) {
        }

        if (CameraClient.channelFuture.isSuccess()) {
            CameraClient.attemptReconnect = false;
            CameraClient.channel = channelFuture.channel();
            LOGGER.info("Connected to the Camera Server. Attempting to login.");
            this.sendMessage(new CameraLoginComposer());
        } else {
            LOGGER.error("Failed to connect to the Camera Server. Server unreachable.");
            CameraClient.channel = null;
            CameraClient.channelFuture.channel().close();
            CameraClient.channelFuture = null;
            CameraClient.attemptReconnect = true;
        }
    }

    public void disconnect() {
        if (channelFuture != null) {
            try {
                channelFuture.channel().close().sync();
                channelFuture = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        channel = null;
        isLoggedIn = false;

        LOGGER.info("Disconnected from the camera server.");
    }

    public void sendMessage(CameraOutgoingMessage outgoingMessage) {
        try {
            if (isLoggedIn || outgoingMessage instanceof CameraLoginComposer) {
                outgoingMessage.compose(channel);
                channel.write(outgoingMessage.get().copy(), channel.voidPromise());
                channel.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}