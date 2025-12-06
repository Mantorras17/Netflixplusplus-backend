package org.netflixpp.mesh;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.nio.file.Files;

public class SeederHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final ChunkManager chunkManager = new ChunkManager();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        String request = msg.toString(io.netty.util.CharsetUtil.UTF_8).trim();
        System.out.println("🕸️ Mesh request: " + request);

        // Parse request: "GET_CHUNK movieId chunkIndex"
        String[] parts = request.split(" ");
        if (parts.length >= 3 && MeshConstants.MSG_GET_CHUNK.equals(parts[0])) {
            String movieId = parts[1];
            int chunkIndex = Integer.parseInt(parts[2]);

            // Serve chunk if available
            File chunkFile = chunkManager.getChunkFile(movieId, chunkIndex);
            if (chunkFile != null && chunkFile.exists()) {
                byte[] chunkData = Files.readAllBytes(chunkFile.toPath());
                ByteBuf response = Unpooled.copiedBuffer(chunkData);
                ctx.writeAndFlush(response);
                System.out.println("✅ Served chunk: " + movieId + "[" + chunkIndex + "]");
            } else {
                // Send error response
                String error = "CHUNK_NOT_FOUND " + movieId + " " + chunkIndex;
                ByteBuf response = Unpooled.copiedBuffer(error.getBytes());
                ctx.writeAndFlush(response);
                System.out.println("❌ Chunk not found: " + movieId + "[" + chunkIndex + "]");
            }
        } else {
            // Invalid request
            String error = "INVALID_REQUEST";
            ByteBuf response = Unpooled.copiedBuffer(error.getBytes());
            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("🕸️ Mesh server error: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("🕸️ New peer connected: " + ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("🕸️ Peer disconnected: " + ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }
}