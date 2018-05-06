package kmc.kedamaListener;

import java.lang.Thread.State;

import org.slf4j.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import kmc.kedamaListener.js.settings.MCPingSettings;
import kmc.kedamaListener.js.settings.IRCLisenerSettings;
import kmc.kedamaListener.ListenerClientStatusManager;

public class IRCListenerHandler extends ChannelInboundHandlerAdapter {
	
	private PlayerCount plc;
	private PlayerCountRecord rc;
	private MCPCPing ping;
	private Thread t;
	
	private MCPingSettings mcsettings;
	private IRCLisenerSettings irclsettings;
	private ListenerClientStatusManager mgr;
	
	private Logger logger = App.logger;
	
	public IRCListenerHandler() {
		super();
		mcsettings = SettingsManager.getSettingsManager().getMcping();
		irclsettings = SettingsManager.getSettingsManager().getIrc().listener;
		t = null;
		rc = PlayerCountRecord.getPlayerCountRecord();
		plc = rc.getPlayerCount();			
		mgr = ListenerClientStatusManager.getListenerClientStatusManager();
	}
	
	public void ping() {
		ping = new MCPCPing(mcsettings.server.hostname, mcsettings.server.port, mcsettings.server.protocolversion, plc);
		ping.setMaxFailTimes(mcsettings.maxfailtime);
		ping.setNormalPeriod(mcsettings.normalperiod);
		ping.setRetryPeriod(mcsettings.retryperiod);
		t = new Thread(ping);
		t.start();
	}
	
	public void stopPing() {
		try {
			if(ping != null) {
				ping.close();
				ping = null;
			}
			if(t != null) {
				t.interrupt();
				t = null;
			}
			logger.info("#process: pinger closed");
		} catch (Exception e) {
			logger.warn("#Exception @{}", Thread.currentThread(), e);
		}
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
					mgr.getListenerClientStatus().restartpinger++;
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
					mgr.getListenerClientStatus().restartpinger++;
					ping();					
				}
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
		if(ping != null || t != null)
			stopPing();
		super.channelInactive(ctx);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.warn("#Exception @{}", Thread.currentThread(), cause);
//		super.exceptionCaught(ctx, cause);
	}

	
}


