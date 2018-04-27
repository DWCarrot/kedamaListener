package kmc.kedamaListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import com.google.gson.Gson;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class IRCResponseHandle extends ChannelInboundHandlerAdapter {
	
	private String head;
	
	private OffPassword pw;
	
	private Gson gson = App.gsonbuilder.create();
	
	private Logger logger = App.logger;
	
	public IRCResponseHandle() {
		head = new StringBuilder()
				.append('@')
				.append(SettingsManager.getSettingsManager().getIrc().basic.nick)
				.append(' ')
				.append(SettingsManager.getSettingsManager().getIrc().listener.excute1)
				.append(' ')
				.toString();
		pw = new OffPassword();
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		IRCMessage ircMsg = (IRCMessage)msg;
		int i, j;
		if((i = ircMsg.getTrailing().indexOf(head)) >= 0) {
			char[] s = ircMsg.getTrailing().toCharArray();
			List<String> args = new ArrayList<>(10);
			i += head.length();
			j = i;
			while(i < s.length && j < s.length) {
				if(s[i] == ' ') {
					++i;
					++j;
					continue;
				}
				if(s[j] != ' ') {
					++j;
					continue;
				}
				args.add(new String(s, i, j - i));
				i = ++j;
			}
			if(i != j)
				args.add(new String(s, i, j - i));
			try {
				response(ctx, ircMsg, args);
			} catch (IndexOutOfBoundsException e) {
				ctx.write(new IRCMessage()
						.setTime(Instant.now())
						.setCommand("PRIVMSG")
						.addMiddles(ircMsg.getUser().getNick())
						.setTrailing("Invalid options in: " + args)
					);
				ctx.flush();
			}
		}
		super.channelRead(ctx, msg);
	}
	
	public void response(ChannelHandlerContext ctx, IRCMessage msg, List<String> args) throws Exception {
		IRCMessage send = new IRCMessage();
		int level = 0;
		send.setCommand("PRIVMSG")
			.addMiddles(msg.getUser().getNick());
		for(int i = 0; i < args.size();) {
			switch(args.get(i++)) {
			case "-public":
				if((level & 0x2) != 0)
					break;
				send.setMiddles(0, SettingsManager.getSettingsManager().getIrc().basic.channel);
				break;
			case "-ping":
				if((level & 0x1) != 0)
					break;
				send.setTrailing("pong" + Instant.now());
				level |= 0x1;
				break;
			case "-status":
				if((level & 0x01) != 0)
					break;
				send.setTrailing(gson.toJson(ListenerClientStatusManager.getListenerClientStatusManager().getListenerClientStatus()));
				level |= 0x1;
				break;
			case "-show":
				if((level & 0x1) != 0)
					break;
				send.setTrailing(gson.toJson(PlayerCountRecord.getPlayerCountRecord().getPlayerCount()));
				level |= 0x1;
				break;
			case "-server":
				if((level & 0x1) != 0)
					break;
				send.setMiddles(0, msg.getUser().getNick())
					.setTrailing(gson.toJson(SysInfo.getSysInfo()));
				level |= 0x3;
			case "-check":
				if((level & 0x1) != 0)
					break;
				Pattern p = Pattern.compile(args.get(i++));
				StringBuilder s = new StringBuilder();
				s.append("find")
				 .append('(')
				 .append('@')
				 .append("online")
				 .append('=')
				 .append(PlayerCountRecord.getPlayerCountRecord().getPlayerCount().getOnlineNum())
				 .append(')')
				 .append('=')
				 .append('[');
				for(String name : PlayerCountRecord.getPlayerCountRecord().getPlayerCount().getOnline())
					if(p.matcher(name).matches())
						s.append(name).append(',');
				if(s.charAt(s.length() - 1) == ',')
					s.deleteCharAt(s.length() - 1);
				s.append(']');
				send.setTrailing(s.toString());
				level |= 0x1;
				break;
//			case "-off":
//				if((level & 0x1) != 0)
//					break;
//				if(pw.hasPw()) {
//					if(pw.check(args.get(i++))) {
//						send.getMiddles().clear();
//						send.setCommand("QUIT");
//						level |= 0x7;
//						App.failTimes = -1;
//						logger.info("#process :remoted to close");
//					} else {
//						send.setMiddles(0, msg.getUser().getNick())
//							.setTrailing("Incorrect Password");
//						level |= 0x3;
//					}
//				} else {
//					send.setMiddles(0, msg.getUser().getNick())
//						.setTrailing(pw.genOffPw());
//					level |= 0x3;
//				}
//				break;
			default:
				break;
			}
		}
		if((level & 0x4) != 0) {
			ctx.write(new IRCMessage()
						.setTime(Instant.now())
						.setCommand("PRIVMSG")
						.addMiddles(msg.getUser().getNick())
						.setTrailing("Prepared to close...")
						);
		}
		if((level & 0x1) != 0 ) {
			ctx.write(send.setTime(Instant.now()));
			ctx.flush();
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.warn("#Exception @{}", Thread.currentThread(), cause);
//		super.exceptionCaught(ctx, cause);
	}
}
