package kmc.kedamaListener;

import java.time.Duration;
import java.time.LocalDateTime;

import kmc.kedamaListener.js.settings.IRCSettings;

public class ListenerClientStatusManager {
	
	private static ListenerClientStatusManager obj;
	
	public static ListenerClientStatusManager getListenerClientStatusManager() {
		if(obj == null)
			obj = new ListenerClientStatusManager();
		return obj;
	}
	
	private ListenerClientStatus status;
	
	public int failtimes;
	
	private ListenerClientStatusManager() {
		status = new ListenerClientStatus();
		status.start = LocalDateTime.now();
		status.current = null;
		status.lastfail = null;
		status.running = null;
		status.restartlistener = 0;
		status.restartpinger = 0;
	}
	
	public void setFailTime() {
		status.lastfail = LocalDateTime.now();
	}
	
	public long getRunnningTime() {
		LocalDateTime thisfail = LocalDateTime.now();
		long running;
		if(status.lastfail == null)
			running = Duration.between(status.start, thisfail).toMillis();
		else
			running = Duration.between(status.lastfail, thisfail).toMillis();
		status.lastfail = thisfail;
		return running;
	}
	
	public void addClientRestart() {
		++status.restartlistener;
	}
	
	public ListenerClientStatus getListenerClientStatus() {
		status.current = LocalDateTime.now();
		status.running = Duration.between(status.start, status.current);
		return status;
	}
	
	public boolean next() {
		if(failtimes < 0)
			return false;
		IRCSettings ircs = ListenerClient.settings.irc;
		if(getRunnningTime() > (ircs.normalworking + ircs.retryperiod) * 1000L)
			failtimes = 0;
		++failtimes;
		return failtimes <= ircs.maxfailtime;	
	}

	public void allowRestart() {
		failtimes = 0;
	}
}
