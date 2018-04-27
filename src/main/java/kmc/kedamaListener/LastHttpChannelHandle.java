package kmc.kedamaListener;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class LastHttpChannelHandle extends ChannelInboundHandlerAdapter {

	private ChannelHandlerContext context;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private static byte[] Page404 = {'4','0','4'};
	
	public static void setPage404(String s) {
		Page404 = s.getBytes(Charset.forName("UTF-8"));
	}
	
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
		if(msg instanceof HttpMessage) {
			ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, ctx.alloc().buffer(Page404.length).writeBytes(Page404)));
			ctx.flush();
			ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
		}
//		super.channelRead(ctx, msg);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.warn("#=DataServer Exception@{}", Thread.currentThread(), cause);
//		super.exceptionCaught(ctx, cause);
	}
}
