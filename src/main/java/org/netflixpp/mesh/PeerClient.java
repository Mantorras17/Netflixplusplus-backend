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

    public byte[] requestChunk(String fileId, int chunkIndex) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            CompletableFuture<byte[]> promise = new CompletableFuture<>();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    String req = "GETCHUNK " + fileId + " " + chunkIndex + "\n";
                                    ctx.writeAndFlush(Unpooled.copiedBuffer(req.getBytes()));
                                }
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                    byte[] arr = new byte[msg.readableBytes()];
                                    msg.readBytes(arr);
                                    promise.complete(arr);
                                    ctx.close();
                                }
                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    promise.completeExceptionally(cause);
                                    ctx.close();
                                }
                            });
                        }
                    });
            ChannelFuture f = b.connect(host, port).sync();
            byte[] data = promise.get();
            f.channel().closeFuture().sync();
            return data;
        } finally {
            group.shutdownGracefully();
        }
    }
}
