package kmc.kedamaListener;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

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
import kmc.kedamaListener.js.settings.DataServerSettings;

public class DataServer {
	
	private NioEventLoopGroup group;
	
	private ServerBootstrap b;
	
	private Gson gson = App.gsonbuilder.create();
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private Instant start;
	
	private InetSocketAddress localAddress;
	
	private List<String> indexs;
	
	private SslContext sslContext;
	
	private Charset charset = Charset.forName("UTF-8");
//	public DataServer(String host, int port, List<String> indexs) {
//		localAddress = new InetSocketAddress(host, port);
//		this.indexs = indexs;
//	}
	
//	public DataServer setDataFileLocate(FileSave settings) {
//		if(settings != null) {
//			
//		}
//		return this;
//	}
	
//	public DataServer setPage404(String s) {
//		if(s != null && !s.isEmpty())
//			LastHttpChannelHandle.setPage404(s);
//		return this;
//	}
	
	public DataServer() {
	}
	
	public void init() throws JsonIOException, JsonSyntaxException, IOException {
		FileReader infile = new FileReader("serversettings.json");
		DataServerSettings serverSettings = gson.fromJson(new JsonReader(infile), DataServerSettings.class);
		
		localAddress = new InetSocketAddress(serverSettings.host, serverSettings.port);
		indexs = serverSettings.indexs;
		HttpQueryHandle.setGet(new GetJsonRecord(serverSettings.filesave.main, serverSettings.filesave.rolling));
		HttpQueryHandle.setIndex(indexs.get(0));
		LastHttpChannelHandle.setPage404(serverSettings.page404);
		sslContext = ServerSslContextFactory.getServerContext(serverSettings.ssl);
		
		group = new NioEventLoopGroup(2);
		b = new ServerBootstrap();
		b.group(group)
		 .channel(NioServerSocketChannel.class)
		 .localAddress(localAddress)
		 .childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline p = ch.pipeline();
				if(sslContext != null) {
			        p.addLast("ssl", sslContext.newHandler(ch.alloc()));
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
			ServerSslContextFactory.clear();
		}
	}
	
	public boolean isClosed() {
		return group == null;
	}
}
