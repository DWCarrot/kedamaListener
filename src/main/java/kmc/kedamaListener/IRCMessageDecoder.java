package kmc.kedamaListener;

import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import kmc.kedamaListener.IRCMessage;

public class IRCMessageDecoder extends MessageToMessageDecoder<ByteBuf> {
	
	private Charset charset = Charset.forName("UTF-8");
	
	private boolean record = true;
	
	private Logger logger = LoggerFactory.getLogger(IRCMessageDecoder.class);
	
	private WatchDogTimer wdt;
	
	public IRCMessageDecoder() {
		
	}
	
	public IRCMessageDecoder setWatchDogTimer(WatchDogTimer wdt) {
		this.wdt = wdt;
		return this;
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		String s = msg.toString(charset);
		if(s == null || s.equals(""))
			return;
		IRCMessage ircMsg = IRCMessage.fromString(s);
		if(record) {
			if(wdt != null)
				wdt.reset();
			if(ircMsg.getCommand().equals("PING"))
				logger.debug("[=>|]{}", s);
			else
				logger.info("[=>|]{}", s);
		}
		out.add(ircMsg);
	}

	public void enableLog() {
		record = true;
	}
	
	public void disableLog() {
		record = false;
	}
	
}
