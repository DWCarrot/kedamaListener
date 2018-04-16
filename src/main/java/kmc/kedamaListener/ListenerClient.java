package kmc.kedamaListener;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import javax.net.ssl.SSLEngine;

import com.google.gson.JsonSyntaxException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.ssl.SslHandler;
import kmc.kedamaListener.js.settings.Settings;
import kmc.kedamaListener.SslContextFactory;

public class ListenerClient {
	
	InetSocketAddress ircAddr;
	
	PlayerCountRecord rc;
	
	Settings settings;
	
	SSLEngine engine;
	
	ListenerClientStatus status;
	
	public ListenerClient() {
		settings = SettingsManager.getSettingsManager().getSettings();
		status = ListenerClientStatusManager.getListenerClientStatusManager().getListenerClientStatus();
	}
	
	public void settingRecord() throws JsonSyntaxException, IOException {
		
		File file = new File(settings.recordfolder);
		if(!file.exists())
			file.mkdirs();
		rc = PlayerCountRecord.getPlayerCountRecord(file.getAbsolutePath() + File.separator + "data", settings.recordnum);
	}
	
	public void start() throws Exception {
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group)
			 .channel(NioSocketChannel.class)
			 .remoteAddress(ircAddr = new InetSocketAddress(settings.irc.host.hostname, settings.irc.host.port))
			 .handler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline p = ch.pipeline();
					if(SslContextFactory.getClientContext(null) != null) {
						engine = SslContextFactory.getClientContext(null).createSSLEngine();
				        engine.setUseClientMode(true);
				        p.addLast("ssl", new SslHandler(engine));
					}
					p.addLast("in1", new LineBasedFrameDecoder(1024))
					 .addLast("in2", new IRCMessageDecoder())
					 .addLast("out1", new IRCMesseageEncoder())
					 .addLast("in3", new IRCLoginHandle())
					 .addLast("in4", new IRCListenerHandler());
				}			 		
		 	});
			ChannelFuture future = b.connect();
			App.logger.info("#process :launched IRCListener");
			future.sync();
			future.channel().closeFuture().sync();
		} finally {
			group.shutdownGracefully().sync();
			PlayerCountRecord.release();	
			App.logger.info("#process :closed IRCListener");
		}	
	}

}
