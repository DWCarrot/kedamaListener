package kmc.kedamaListener;

import java.time.Instant;

import org.slf4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import kmc.kedamaListener.js.settings.IRCBasicSettings;

public class IRCLoginHandle extends ChannelInboundHandlerAdapter {
	
	private int state;
	private IRCBasicSettings settings;
	
	private Logger logger = App.logger;
	
	public IRCLoginHandle() {
		state = 0;
		this.settings = SettingsManager.getSettingsManager().getIrc().basic;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.write(new IRCMessage(Instant.now(), "NICK", settings.nick));
		ctx.flush();
		ctx.write(new IRCMessage(Instant.now(), "USER", settings.username, String.valueOf(settings.usermode), "*", ":" + settings.realname));
		ctx.flush();
		state = 1;
//		super.channelActive(ctx);	/**	don't fire next channel automatically	**/
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		IRCMessage ircm = (IRCMessage)msg;
		IRCUser user = ircm.getUser();
		if(ircm.getCommand().equals("ERROR") && state != 5)
			throw new IRCLoginExcption("IRCERROR: ERROR :" + ircm.getTrailing());
		switch(state) {
		case 1:
			if(ircm.getTrailing().startsWith("Nickname is already in use")) {
				throw new IRCLoginExcption("ERROR: IRC nickname is already in use");
			}
			if(user.getNick().equals("NickServ")) {
				ctx.write(new IRCMessage(Instant.now(), "PRIVMSG", "NickServ", ":IDENTIFY " + settings.password));
				ctx.flush();
				state = 2;
				break;
			}
			break;
		case 2:
			if(user.getNick().equals("NickServ")) { 
				if(ircm.getTrailing().startsWith("You are now identified")) {
					ctx.write(new IRCMessage(Instant.now(), "JOIN", settings.channel, settings.channelpw));
					ctx.flush();
					state = 3;
					break;
				}
			}
		case 3:
			if(user.getNick().equals(settings.nick)) {
				if(ircm.getCommand().equals("JOIN") && ircm.getMiddles().get(0).equals(settings.channel)) {
					state = 4;
					ctx.fireChannelActive();
					break;
				}
			}
			break;
		case 4:
			switch(ircm.getCommand()) {
			case "KICK":
				if(ircm.getMiddles().get(1).equals(settings.nick))
					throw new IRCLoginExcption("ERROR: being kicked by " + user.asString());
				break;
			case "PING":
				ctx.write(new IRCMessage(Instant.now(), "PONG", ircm.trailing));
				ctx.flush();
				break;
			case "QUIT":
				if(ircm.getUser().getNick().equals(settings.nick)) {
					state = 5;
					ctx.close();
				}
				break;
			case "PRIVMSG":
				ctx.fireChannelRead(msg);
				break;
			}
			break;
		}
//		super.channelRead(ctx, msg);	/**	don't fire next channel automatically	**/
	}
	
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.warn("#Exception @{}", Thread.currentThread(), cause);
		ctx.close();
//		super.exceptionCaught(ctx, cause);	/**	don't throw to next channel automatically	**/
	}
}
