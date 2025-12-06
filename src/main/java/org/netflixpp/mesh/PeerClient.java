package org.netflixpp.mesh;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.CompletableFuture;

public class PeerClient {
    private final String host;
    private final int port;

    public PeerClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public CompletableFuture<byte[]> requestChunk(String movieId, int chunkIndex) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                private final ByteBuf buffer = Unpooled.buffer();

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    String request = MeshConstants.MSG_GET_CHUNK + " " + movieId + " " + chunkIndex;
                                    ctx.writeAndFlush(Unpooled.copiedBuffer(request.getBytes()));
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                    buffer.writeBytes(msg);

                                    // Check if we have a complete response
                                    if (buffer.readableBytes() > 0) {
                                        byte[] data = new byte[buffer.readableBytes()];
                                        buffer.readBytes(data);

                                        // Check if it's an error message
                                        String response = new String(data);
                                        if (response.startsWith("CHUNK_NOT_FOUND") || response.startsWith("INVALID_REQUEST")) {
                                            future.completeExceptionally(new RuntimeException("Peer error: " + response));
                                        } else {
                                            future.complete(data);
                                        }

                                        ctx.close();
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    future.completeExceptionally(cause);
                                    ctx.close();
                                }
                            });
                        }
                    });

            ChannelFuture f = b.connect(host, port);
            f.addListener((ChannelFutureListener) channelFuture -> {
                if (!channelFuture.isSuccess()) {
                    future.completeExceptionally(channelFuture.cause());
                    group.shutdownGracefully();
                }
            });

        } catch (Exception e) {
            future.completeExceptionally(e);
            group.shutdownGracefully();
        }

        return future;
    }
}