package kmc.kedamaListener;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import kmc.kedamaListener.IRCMessage;

public class IRCMessageDecoder extends MessageToMessageDecoder<ByteBuf> {
	
	private Charset charset = Charset.forName("UTF-8");
	
	private boolean record = true;
	
	private Logger logger = App.logger;
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		String s = msg.toString(charset);
		if(s == null || s.equals(""))
			return;
		IRCMessage ircMsg = IRCMessage.fromString(s);
		if(record)
			logger.info(ircMsg.asString(new StringBuilder().append('[').append(ircMsg.time).append("|=>]")));
		out.add(ircMsg);
	}

	public void enableLog() {
		record = true;
	}
	
	public void disableLog() {
		record = false;
	}
	
}
