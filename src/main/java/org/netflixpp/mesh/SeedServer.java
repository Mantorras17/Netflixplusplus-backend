package org.netflixpp.mesh;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;

import java.nio.file.*;

public class SeedServer {

    private final int port;
    private final Path storageRoot; // ex: storage/chunks/

    public SeedServer(int port, Path storageRoot) {
        this.port = port;
        this.storageRoot = storageRoot;
    }

    public void start() throws Exception {
        Files.createDirectories(storageRoot);
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                private final ByteBuf buffer = Unpooled.buffer();

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                    while (msg.isReadable()) buffer.writeByte(msg.readByte());
                                    if (buffer.toString(java.nio.charset.StandardCharsets.UTF_8).contains("\n")) {
                                        String req = buffer.toString(java.nio.charset.StandardCharsets.UTF_8).trim();
                                        String[] parts = req.split("\\s+");
                                        if (parts.length >= 3 && "GETCHUNK".equalsIgnoreCase(parts[0])) {
                                            String fileId = parts[1];
                                            int idx = Integer.parseInt(parts[2]);
                                            Path chunk = storageRoot.resolve(fileId + "_" + idx + ".chunk");
                                            if (Files.exists(chunk)) {
                                                byte[] data = Files.readAllBytes(chunk);
                                                ctx.writeAndFlush(Unpooled.wrappedBuffer(data));
                                            } else {
                                                ctx.writeAndFlush(Unpooled.copiedBuffer("NOTFOUND\n".getBytes()));
                                            }
                                            ctx.close();
                                        } else {
                                            ctx.writeAndFlush(Unpooled.copiedBuffer("BADREQUEST\n".getBytes()));
                                            ctx.close();
                                        }
                                    }
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    ctx.close();
                                }
                            });
                        }
                    });
            ChannelFuture f = b.bind(port).sync();
            System.out.println("SeedServer listening on " + port);
            f.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    // main para testes
    public static void main(String[] args) throws Exception {
        Path root = Paths.get("storage/chunks/");
        new SeedServer(9001, root).start();
    }
}
