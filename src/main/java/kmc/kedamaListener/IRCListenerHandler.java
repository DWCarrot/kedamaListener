package kmc.kedamaListener;

import java.lang.Thread.State;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import kmc.kedamaListener.js.settings.MCPingSettings;
import kmc.kedamaListener.js.settings.IRCBasicSettings;
import kmc.kedamaListener.js.settings.IRCLisenerSettings;
import kmc.kedamaListener.ListenerClientStatusManager;

public class IRCListenerHandler extends ChannelInboundHandlerAdapter {
	
	private PlayerCount plc;
	private PlayerCountRecord rc;
	private MCPCPing ping;
	private Thread t;
	
	private MCPingSettings mcsettings;
	private IRCLisenerSettings irclsettings;
	private IRCBasicSettings ircsettings;
	private ListenerClientStatusManager mgr;
	private Gson gson;
	
	private String replyKey;
	
	private char[] pw;
	
	private int lineLimit = 250;
	
	public IRCListenerHandler() {
		super();
		mcsettings = SettingsManager.getSettingsManager().getMcping();
		irclsettings = SettingsManager.getSettingsManager().getIrc().listener;
		ircsettings = SettingsManager.getSettingsManager().getIrc().basic;
		t = null;
		rc = PlayerCountRecord.getPlayerCountRecord();
		plc = rc.getPlayerCount();			
		replyKey = "@"+ ircsettings.username;
		mgr = ListenerClientStatusManager.getListenerClientStatusManager();
		gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
		pw = null;
	}
	
	public void ping() {
		ping = new MCPCPing(mcsettings.server.hostname, mcsettings.server.port, mcsettings.server.protocolversion, plc);
		ping.setMaxFailTimes(mcsettings.maxfailtime);
		ping.setNormalPeriod(mcsettings.normalperiod);
		ping.setRetryPeriod(mcsettings.retryperiod);
		t = new Thread(ping);
		t.start();
	}
	
	public String getPlayer(String trailing) {
		int i = trailing.indexOf(' ', 0);
		int j = trailing.indexOf(' ', i + 1);
		if(i < 0 || j < 0)
			return null;
		return trailing.substring(i + 1, j);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		IRCMessage ircMsg = (IRCMessage)msg;
		IRCUser user = ircMsg.getUser();
		if(user.getNick().equals(irclsettings.target)) {	
			if(ircMsg.getTrailing().contains(irclsettings.joinkey)) {
				
				String playername = getPlayer(ircMsg.getTrailing());
				if(plc.add(playername)) {
					plc.setTime(ircMsg.getTime());
					rc.record();
				} else {
					//TODO
				}
				if(t.getState() == State.TERMINATED) {
					mgr.status.restartpinger++;
					ping();				
				}
			}
			else
				if(ircMsg.getTrailing().contains(irclsettings.leavekey)) {
				String playername = getPlayer(ircMsg.getTrailing());
				if(plc.remove(playername)) {
					plc.setTime(ircMsg.getTime());
					rc.record();
				} else {
					//TODO 
				}
				if(t.getState() == State.TERMINATED) {
					mgr.status.restartpinger++;
					ping();					
				}
			}	
		}
		int i, j, optcode;
		if((i = ircMsg.getTrailing().indexOf(replyKey)) >= 0) {
			try {
				i = ircMsg.getTrailing().indexOf('#', i);
				j = ircMsg.getTrailing().indexOf('.', i);
				optcode = Integer.valueOf(ircMsg.trailing.substring(i + 1, j));
				execOptcode(optcode, ctx, ircMsg);
			} catch(StringIndexOutOfBoundsException | NumberFormatException e) {
				//TODO
			}
		}
		super.channelRead(ctx, msg);
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ping();
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ping.close();
		t.interrupt();
		super.channelInactive(ctx);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		App.logger.error("#Exception @" + Thread.currentThread().getName(), cause);
//		super.exceptionCaught(ctx, cause);
	}
	
	public void execOptcode(int optcode, ChannelHandlerContext ctx, IRCMessage msg) {
		String target = msg.getUser().nick;
		IRCMessage send = new IRCMessage();
		switch(optcode) {
		case 100:
			target = msg.middles.get(0);
		case 0:
			send.setTime(System.currentTimeMillis())
				.setCommand("PRIVMSG")
				.addMiddles(target)
				.setTrailing(Long.toString(send.getTime()));
			break;
		case 101:
			target = msg.middles.get(0);
		case 1:
			send.setTime(System.currentTimeMillis())
				.setCommand("PRIVMSG")
				.addMiddles(target)
				.setTrailing(gson.toJson(ListenerClientStatusManager.getListenerClientStatusManager().getListenerClientStatus()));
			break;
		case 102:
			target = msg.middles.get(0);
		case 2:
			send.setTime(System.currentTimeMillis())
				.setCommand("PRIVMSG")
				.addMiddles(target)
				.setTrailing(gson.toJson(PlayerCountRecord.getPlayerCountRecord().getPlayerCount()));
			break;	
		case 5:
			send.setTime(System.currentTimeMillis())
				.setCommand("PRIVMSG")
				.addMiddles(target)
				.setTrailing(gson.toJson(SysInfo.getSysInfo()));
			break;
		case 27:
			if(pw == null) {
				pw = OffPassword.generatePw1();
				send.setTime(System.currentTimeMillis())
					.setCommand("PRIVMSG")
					.addMiddles(target)
					.setTrailing(new String(pw));
				break;
			} else {
				int i = msg.getTrailing().indexOf("${");
				i = (i > 0 ? i + 2 : msg.getTrailing().length());
				int j = msg.getTrailing().indexOf('}', i);
				String pwp = null;
				if(j > i)
					pwp = msg.getTrailing().substring(i, j);
				if(pwp != null && pwp.equals(new String(OffPassword.encodePw1(pw)))) {
					send.setTime(System.currentTimeMillis())
						.setCommand("QUIT");
					App.logger.info("## remoted to close ##");
					App.failTimes = -1;
				} else {
					send.setTime(System.currentTimeMillis())
						.setCommand("PRIVMSG")
						.addMiddles(target)
						.setTrailing("Invalid");
				}
				OffPassword.clear(pw);
				pw = null;
			}
			break;
		default:
			send = null;
		}
		if(send != null) {
//			String s = send.getTrailing();
//			int k;
//			for(k = lineLimit; k < s.length(); k += lineLimit) {
//				send.setTrailing(s.substring(k - lineLimit, k));
//				ctx.write(send);
//			}
//			if(k != lineLimit)
//				send.setTrailing(s.substring(k - lineLimit));		//Bug:	when trail == null;
			ctx.write(send);
			ctx.flush();
		}
	}

}


