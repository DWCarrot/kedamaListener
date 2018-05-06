package kmc.kedamaListener;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;

import com.google.gson.Gson;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import kmc.kedamaListener.js.settings.Settings;
import kmc.kedamaListener.ClientSslContextFactory;

public class ListenerClient implements ChannelFutureListener, WatchDogTimer.WorkingProcess {
	
	InetSocketAddress ircAddr;
	
	Settings settings;
	
	ListenerClientStatusManager statusMgr;
	
	EventLoopGroup group;
	
	Bootstrap b;
	
	Logger logger = App.logger;
	
	Gson gson = App.gsonbuilder.create();
	
	WatchDogTimer wdt;
	
	int failedTimes;
	
	ChannelPipeline p;
	
	Thread waiting;
	
	boolean notLoginError;
	
	IRCListenerHandler l;
	
	public ListenerClient() {
		settings = SettingsManager.getSettingsManager().getSettings();
		statusMgr = ListenerClientStatusManager.getListenerClientStatusManager();
		failedTimes = 0;
		wdt = new WatchDogTimer(settings.irc.msgtimeout * 1000, this);
		waiting = null;
	}
	
	public void init() {
		group = new NioEventLoopGroup(2);
		b = new Bootstrap();
		b.group(group)
		 .channel(NioSocketChannel.class)
		 .remoteAddress(ircAddr = new InetSocketAddress(settings.irc.host.hostname, settings.irc.host.port))
		 .handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				p = ch.pipeline();
				if(ClientSslContextFactory.getClientContext(null) != null) {
					SslContext sslctx = ClientSslContextFactory.getClientContext(null);
			        p.addLast("ssl", sslctx.newHandler(ch.alloc()));
				}
				p.addLast("in1", new LineBasedFrameDecoder(1024))
				 .addLast("in2", new IRCMessageDecoder().setWatchDogTimer(wdt))
				 .addLast("out1", new IRCMesseageEncoder())
				 .addLast("in3", new IRCLoginHandle())
				 .addLast("in4", l = new IRCListenerHandler())
				 .addLast("in5", new IRCResponseHandle())
				 .addLast("end", new LastChannelHandle());
			}			 		
		  });
	}
	
	public void start() throws InterruptedException {
		PlayerCountRecord.getPlayerCountRecord();
		logger.info("#process :launched IRCListener");
		ChannelFuture future = b.connect().sync();
		future = future.channel().closeFuture();
		future.addListener(this);
		wdt.start();
		notLoginError = false;
	}

	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		PlayerCountRecord.release();
		logger.info("#process :closed IRCListener");
		wdt.stop();
		logger.info("#state {}" ,gson.toJson(statusMgr.getListenerClientStatus()));
		if(group != null && statusMgr.next() && !(group.isShuttingDown() || group.isShutdown() || group.isTerminated())) {		
			try {
				waiting = Thread.currentThread();
				if(notLoginError)
					Thread.sleep(10 * 1000L);
				else 
					Thread.sleep(SettingsManager.getSettingsManager().getIrc().retryperiod * 1000L);
				waiting = null;
				start();
				statusMgr.addClientRestart();
				return;
			} catch(Exception e) {
				logger.warn("#Exception @{}", Thread.currentThread(), e);
				waiting = null;
			}
		}
		wdt.stop();
		PlayerCountRecord.release();
		logger.info("#state {}" ,gson.toJson(statusMgr.getListenerClientStatus()));
		group.shutdownGracefully().addListener(_future -> {logger.info("#processs :IRCListener terminated");});
		group = null;
	}
	
	public boolean isClosed() {
		return group == null;
	}
	
	public void close(GenericFutureListener<? extends Future<? super Object>> futureListener) {
		if(waiting != null) {
			waiting.interrupt();
			return;
		}
		if(group != null) {
			wdt.stop();
			if(futureListener == null)
				futureListener = _future -> {logger.info("#processs :IRCListener terminated");};
			PlayerCountRecord.release();
			logger.info("#state {}" ,gson.toJson(statusMgr.getListenerClientStatus()));
			group.shutdownGracefully().addListener(futureListener);
			group = null;
		}
	}

	@Override
	public void reboot() throws Exception {
		logger.warn("#Exception @{}", 
					Thread.currentThread(),
					new TimeoutException("haven't received any message over " + settings.irc.msgtimeout + "s")
					);
		notLoginError = true;
		l.stopPing();
		p.close().addListener(CLOSE);
	}

	@Override
	public void exceptionCaught(Throwable cause) throws Exception {
		close(null);
		wdt.stop();
	}
}
