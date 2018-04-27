package kmc.kedamaListener;

import java.nio.CharBuffer;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class HttpTest extends ChannelInboundHandlerAdapter {
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(!(msg instanceof HttpRequest))
			return;
		HttpRequest req = (HttpRequest) msg;
		
		
		System.out.println(req.method());
		System.out.println(req.uri());
		ByteBuf buf = ctx.alloc().directBuffer();
		buf.writeCharSequence("<html>\r\n<body bgcolor=green>\r\n</body>\r\n</html>\r\n", Charset.forName("UTF-8"));
		HttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND, buf);
		resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain")
					  .set(HttpHeaderNames.CONTENT_LENGTH, buf.readableBytes());
		ctx.write(resp);
		ctx.flush();
		ctx.channel().close().sync();
//		super.channelRead(ctx, msg);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close().sync();
//		super.exceptionCaught(ctx, cause);
	}
}
