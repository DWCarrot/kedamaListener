package kmc.kedamaListener;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
	
//	public static Pattern[] pattern = {
//			Pattern.compile("start=(\\S*)&end=(\\S*)&dest=(.*)"),
//			Pattern.compile("list=(list)&dest=(.*)"),
//			Pattern.compile("check=(\\w+)&dest=(.*)"),
//	};
	
	public static DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
	
	public static Charset charset = Charset.forName("UTF-8");
	
	private static GetJsonRecord get = new GetJsonRecord("jsonRecord/record.json", "jsonRecord/record-%d{yyyy-MM-dd}.json");
	
	private static long lastQuery = 0;
	
	private static long minQueryInterval = 1000 * 5;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	
	
	public static void setIndex(String index) {
		HttpQueryHandle.index = index;
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
		s1.append("{\"error\":\"")
		  .append(cause.getClass().getName())
		  .append("\",\"message\":\"")
		  .append(cause.getMessage())
		  .append("\"}");
		ByteBuf buf = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(s1.toString()), charset);
		return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buf);
	}
	
	public Map<String, String>getQueryPara(String query) throws Exception {
		Map<String, String> queryPara = new HashMap<String, String>();
		int i, j, k;
		for(i = 0, j = query.indexOf('&', i); i < query.length() && j > 0; i = j + 1, j = query.indexOf('&', i)) {
			k = query.indexOf('=', i);
			if(k > i)
				queryPara.put(query.substring(i, k), query.substring(k + 1, j));
		}
		if(i < query.length()) {
			k = query.indexOf('=', i);
			if(k > i)
				queryPara.put(query.substring(i, k), query.substring(k + 1));
		}
		return queryPara;
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
			Map<String, String> queryPara = getQueryPara(s.getQuery());
			String a = null;
			String b = null;
			if ((a = queryPara.get("start")) != null && (b = queryPara.get("end")) != null) {
				LocalDate start;
				LocalDate end;
				if (a.isEmpty())
					start = LocalDate.of(1970, 1, 1);
				else
					start = LocalDate.parse(a, formatter);
				if (b.isEmpty())
					end = LocalDate.now();
				else
					end = LocalDate.parse(b, formatter);
				b = queryPara.get("jsoncallback");
				handle2(ctx, start, end, b);
			} else {
				if ((a = queryPara.get("list")) != null && a.equals("list")) {
					b = queryPara.get("jsoncallback");
					handle1(ctx, b);
				} else {
					if((a = queryPara.get("check")) != null && a.equals("now")) {
						b = queryPara.get("jsoncallback");
						handle3(ctx, b);
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
//			ctx.channel().close().addListener(ChannelFutureListener.CLOSE);
		}

	}
	
	public void handle1(ChannelHandlerContext ctx, String callback) throws IOException {
		boolean hasCallback = callback != null && !callback.isEmpty();
		StringBuilder res = new StringBuilder();
		if(hasCallback)
			res.append(callback).append('(');
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
		if(hasCallback)
			res.append(')');
		ByteBuf buf = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(res), charset);
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
		HttpHeaders h = response.headers();
		h.add(HttpHeaderNames.DATE, LocalDateTime.now());
		if(hasCallback)
			h.add(HttpHeaderNames.CONTENT_TYPE, "text/javascript");
		else
			h.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
//		h.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, ctx.channel().localAddress());
		ctx.write(response);
		ctx.flush();
	}
	
	public void handle2(ChannelHandlerContext ctx, LocalDate start, LocalDate end, String callback) throws IOException {
		boolean hasCallback = callback != null && !callback.isEmpty();
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		HttpHeaders h = response.headers();
		h.add(HttpHeaderNames.DATE, LocalDateTime.now());
		if(hasCallback)
			h.add(HttpHeaderNames.CONTENT_TYPE, "text/javascript");
		else
			h.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
		h.add(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
//		h.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, ctx.channel().localAddress());
//		String compress = req.headers().get(HttpHeaderNames.ACCEPT_ENCODING);	
//		HttpUtil.setKeepAlive(response.headers(), response.protocolVersion(), true);
		ctx.write(response);
		ctx.flush();
		get.setRange(start, end);
		get.setCallback(callback);
		ctx.write(get);
		ctx.flush();
	}
	
	public void handle3(ChannelHandlerContext ctx, String callback) throws Exception {
		boolean hasCallback = callback != null && !callback.isEmpty();
		StringBuilder src = new StringBuilder();
		if(hasCallback)
			src.append(callback).append('(');
		String tmp = PlayerCountRecord.getSPlayerCountRecord();
		if(tmp != null && !tmp.isEmpty())
			src.append(tmp);
		else
			src.append("{}");
		if(hasCallback)
			src.append(')');
		byte[] s = src.toString().getBytes(charset);
		ByteBuf buf = ctx.alloc().buffer(s.length).writeBytes(s);
		HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
		HttpHeaders h = response.headers();
		h.add(HttpHeaderNames.DATE, LocalDateTime.now());
		if(hasCallback)
			h.add(HttpHeaderNames.CONTENT_TYPE, "text/javascript");
		else
			h.add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
//		h.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, ctx.channel().localAddress());
		ctx.write(response);
		ctx.flush();
	}
	
}
