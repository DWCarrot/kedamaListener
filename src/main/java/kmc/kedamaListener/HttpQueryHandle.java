package kmc.kedamaListener;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class HttpQueryHandle extends ChannelInboundHandlerAdapter {

	/**
	 * uri = ${index}?start=${time:yyyy-MM-dd}&end=${time:yyyy-MM-dd}
	 */
	public static String index = "/kedamaListener/PlayerCountRecord";
	
	public static Pattern[] pattern = {
			Pattern.compile("start=(\\S*)&end=(\\S*)&dest=(.*)"),
			Pattern.compile("list=(list)&dest=(.*)"),
			Pattern.compile("check=(\\w+)&dest=(.*)"),
	};
	
	public static DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
	
	public static Charset charset = Charset.forName("UTF-8");
	
	private static GetJsonRecord get = new GetJsonRecord("jsonRecord/record.json", "jsonRecord/record-%d{yyyy-MM-dd}.json");
	
	private static long lastQuery = 0;
	
	private static long minQueryInterval = 1000 * 5;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	
	
	public static void setIndex(String index) {
		HttpQueryHandle.index = index;
	}

	public static void setPattern(Pattern[] pattern) {
		HttpQueryHandle.pattern = pattern;
	}

	public static void setFormatter(DateTimeFormatter formatter) {
		HttpQueryHandle.formatter = formatter;
	}

	public static void setCharset(Charset charset) {
		HttpQueryHandle.charset = charset;
	}

	public static void setGet(GetJsonRecord get) {
		HttpQueryHandle.get = get;
	}

	
	
	public HttpQueryHandle() {
		
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(msg instanceof HttpRequest) {
			HttpRequest req = (HttpRequest) msg;
			if(req.method().equals(HttpMethod.GET)) {
				URI uri = URI.create(req.uri());
				if(uri.getPath().equals(index) && uri.getQuery() != null) {
					handle(ctx, req, uri);
					return;
				}
			}
		}
		super.channelRead(ctx, msg);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if(cause.getCause() instanceof javax.net.ssl.SSLException && cause.getCause().getMessage().equals("Received fatal alert: unknown_ca"))
			logger.debug("#=DataServer Exception={}",Thread.currentThread(), cause);
		else
			logger.warn("#=DataServer Exception={}",Thread.currentThread(), cause);
		ctx.close();
//		super.exceptionCaught(ctx, cause);
	}
	
	public HttpResponse exceptionResponse(ChannelHandlerContext ctx, HttpResponseStatus status, Throwable cause) {
		StringBuilder s1 = new StringBuilder();
		s1.append("error={\"class\":\"")
		  .append(cause.getClass().getName())
		  .append("\",\"message\":\"")
		  .append(cause.getMessage())
		  .append("\"}");
		ByteBuf buf = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(s1.toString()), charset);
		return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buf);
	}
	
	public void handle(ChannelHandlerContext ctx, HttpRequest req, URI s) {
		long h = System.currentTimeMillis() - lastQuery;
		if (h < minQueryInterval) {
			ctx.write(exceptionResponse(ctx, HttpResponseStatus.NOT_ACCEPTABLE, new Exception(
					"Query too often. This server can only handle a query every " + minQueryInterval + "ms")));
			ctx.flush();
			ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
			return;
		}
		try {
			Matcher matcher = pattern[0].matcher(s.getQuery());
			if (matcher.matches()) {
				LocalDate start;
				LocalDate end;
				String dest;
				if (matcher.group(1).isEmpty())
					start = LocalDate.of(1970, 1, 1);
				else
					start = LocalDate.parse(matcher.group(1), formatter);
				if (matcher.group(2).isEmpty())
					end = LocalDate.now();
				else
					end = LocalDate.parse(matcher.group(2), formatter);
				dest = matcher.group(3);
				if (dest.length() > 64)
					throw new Exception("\"destination\" too long");
				handle2(ctx, start, end, dest);
			} else {
				matcher = pattern[1].matcher(s.getQuery());
				if (matcher.matches()) {
					String dest = matcher.group(2);
					if (dest.length() > 64)
						throw new Exception("\"destination\" too long");
					handle1(ctx, dest);
				} else {
					matcher = pattern[2].matcher(s.getQuery());
					if(matcher.matches()) {
						String tgt = matcher.group(1);		
						String dest = matcher.group(2);
						if (dest.length() > 64)
							throw new Exception("\"destination\" too long");
						handle3(ctx, dest);
					}
					else {
						throw new Exception("Invalid query: " + s.getQuery());
					}
				}
			}
		} catch (Exception e) {
			ctx.write(exceptionResponse(ctx, HttpResponseStatus.BAD_REQUEST, e));
			ctx.flush();
		} finally {
			lastQuery = System.currentTimeMillis();
			ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
		}

	}
	
	public void handle1(ChannelHandlerContext ctx, String dest) throws IOException {
		StringBuilder res = new StringBuilder();
		if(!dest.isEmpty())
			res.append(dest).append('=');
		res.append('[');
		for(Map.Entry<LocalDate, File> p : get.listFiles().entrySet()) {
			res.append('{')
			   .append("\"time\":")
			   .append('"')
			   .append(p.getKey().format(formatter))
			   .append('"')
			   .append(',')
			   .append("\"file\":")
			   .append('"')
			   .append(p.getValue().getName())
			   .append('"')
			   .append('}')
			   .append(',');
		}
		if(res.charAt(res.length() - 1) == ',')
			res.deleteCharAt(res.length() - 1);
		res.append(']');
		ByteBuf buf = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(res), charset);
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
		HttpHeaders h = response.headers();
		h.add(HttpHeaderNames.DATE, LocalDateTime.now());
		h.add(HttpHeaderNames.CONTENT_TYPE, "text/javascript; charset=utf-8");
		ctx.write(response);
		ctx.flush();
	}
	
	public void handle2(ChannelHandlerContext ctx, LocalDate start, LocalDate end, String valueName) throws IOException {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		HttpHeaders h = response.headers();
		h.add(HttpHeaderNames.DATE, LocalDateTime.now());
		h.add(HttpHeaderNames.CONTENT_TYPE, "text/javascript; charset=utf-8");
		h.add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
//		String compress = req.headers().get(HttpHeaderNames.ACCEPT_ENCODING);	
//		HttpUtil.setKeepAlive(response.headers(), response.protocolVersion(), true);
		ctx.write(response);
		ctx.flush();
		get.setRange(start, end);
		ctx.write(get);
		ctx.flush();
	}
	
	public void handle3(ChannelHandlerContext ctx ,String valueName) throws Exception {
		StringBuilder src = new StringBuilder();
		if(valueName != null && !valueName.isEmpty())
			src.append(valueName).append('=');
		String tmp = PlayerCountRecord.getSPlayerCountRecord();
		if(tmp != null && !tmp.isEmpty())
			src.append(tmp);
		byte[] s = src.toString().getBytes(charset);
		ByteBuf buf = ctx.alloc().buffer(s.length).writeBytes(s);
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
		HttpHeaders h = response.headers();
		h.add(HttpHeaderNames.DATE, LocalDateTime.now());
		h.add(HttpHeaderNames.CONTENT_TYPE, "text/javascript; charset=utf-8");
		ctx.write(response);
		ctx.flush();
	}
	
}
