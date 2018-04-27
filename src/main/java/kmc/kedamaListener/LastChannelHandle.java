package kmc.kedamaListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class LastChannelHandle extends ChannelInboundHandlerAdapter {
	
	private ChannelHandlerContext context;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public ChannelHandlerContext getContext() {
		return context;
	}
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		context = ctx;
		super.handlerAdded(ctx);
	}
	
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		context = null;
		super.handlerRemoved(ctx);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//		super.channelRead(ctx, msg);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.warn("#Exception @{}", Thread.currentThread(), cause);
//		super.exceptionCaught(ctx, cause);
	}
	

}
