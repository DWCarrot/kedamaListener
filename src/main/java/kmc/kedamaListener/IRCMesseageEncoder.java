package kmc.kedamaListener;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import kmc.kedamaListener.IRCMessage;

public class IRCMesseageEncoder extends MessageToMessageEncoder<IRCMessage> {

	private Charset charset = Charset.forName("UTF-8");
	
	private boolean record = true;
	
	private Logger logger = App.logger;
	
	@Override
	protected void encode(ChannelHandlerContext ctx, IRCMessage msg, List<Object> out) throws Exception {
		StringBuilder s = msg.asString(null);
		if(record)
			logger.info("[<=|]{}", s.toString());
		out.add(ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(s.append("\r\n").toString()), charset));
	}
	
	public void enableLog() {
		record = true;
	}
	
	public void disableLog() {
		record = false;
	}
	
}
