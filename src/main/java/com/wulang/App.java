package com.wulang;

import com.wulang.http.channelhandler.HttpServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;


/**
 * Hello world!
 *
 */
public class App 
{
    private static int PORT=8080;

    public static void main(String[] args) throws Exception {
        final String rootDir;
        if(args.length!=0){
            rootDir=args[0]+"/ROOT";
        }else{
            rootDir="";
        }
        if(args.length>1){
            PORT=Integer.parseInt(args[1]);
        }
//        final String rootDirTest="C:\\Users\\Administrator\\Desktop\\idea\\http-server\\src\\main\\resources";
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpServerCodec());
                            p.addLast(new HttpServerHandler(rootDir));
                        }
                    });
            Channel ch = b.bind(PORT).sync().channel();

            System.err.println("Open your web browser and navigate to " + "http://127.0.0.1:" + PORT + '/');

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
