package kmc.kedamaListener;


import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import kmc.kedamaListener.js.settings.FileSave;

public class DataServer {
	
	private NioEventLoopGroup group;
	
	private ServerBootstrap b;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private Instant start;
	
	private InetSocketAddress localAddress;
	
	private List<String> indexs;
	
	public DataServer(String host, int port, List<String> indexs) {
		localAddress = new InetSocketAddress(host, port);
		this.indexs = indexs;
	}
	
	public DataServer setDataFileLocate(FileSave settings) {
		if(settings != null) {
			HttpQueryHandle.setGet(new GetJsonRecord(settings.main, settings.rolling));
			HttpQueryHandle.setIndex(indexs.get(0));
		}
		return this;
	}
	
	public DataServer setPage404(String s) {
		if(s != null && !s.isEmpty())
			LastHttpChannelHandle.setPage404(s);
		return this;
	}
	
	public void init() {
		group = new NioEventLoopGroup(2);
		b = new ServerBootstrap();
		b.group(group)
		 .channel(NioServerSocketChannel.class)
		 .localAddress(localAddress)
		 .childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline p = ch.pipeline();
				if(ServerSslContextFactory.getServerContext(null) != null) {
					SslContext sslctx = ServerSslContextFactory.getServerContext(null);
			        p.addLast("ssl", sslctx.newHandler(ch.alloc()));
				}
				p.addLast("codec", new HttpServerCodec());
				p.addLast("compressor", new HttpContentCompressor());
				p.addLast("chunked", new ChunkedWriteHandler());
				p.addLast("handle_1", new HttpQueryHandle());
				p.addLast("last", new LastHttpChannelHandle());
			}
		});
	}
	
	public void start() throws InterruptedException {
		ChannelFuture future = b.bind().sync();
		logger.info("#=DataServer process=launched@[{}]", localAddress);
		start = Instant.now();
		future = future.channel().closeFuture();
	}
	
	public void close(GenericFutureListener<? extends Future<? super Object>> futureListener) {	
		if(group != null) {
			logger.info("#=dataServer process=closed");
			if(futureListener == null)
				futureListener = (_future -> {logger.info("#=DataServer process=terminated");});
			group.shutdownGracefully().addListener(futureListener);
			group = null;
		}
	}
	
	public boolean isClosed() {
		return group == null;
	}
}
