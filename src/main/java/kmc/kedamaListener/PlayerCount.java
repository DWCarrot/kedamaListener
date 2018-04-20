package kmc.kedamaListener;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class PlayerCount {
	
	private Instant timestamp;
	
	private ZonedDateTime time;
	
	private int onlineNum;
	
	private Set<String> online;
	
	private boolean continuous;
	
	public PlayerCount() {
		timestamp = null;
		onlineNum = 0;
		online = new TreeSet<>();
		continuous = false;
	}

	public synchronized Instant getTimestamp() {
		return timestamp;
	}

	public synchronized ZonedDateTime getTime() {
		return time;
	}
	
	public synchronized void setTime(Instant timestamp) {
		this.timestamp = timestamp;
		this.time = ZonedDateTime.ofInstant(timestamp, App.zone);
	}

	public synchronized void setTime(ZonedDateTime time) {
		this.time = time;
		this.timestamp = time.toInstant();
	}
	
	public synchronized int getOnlineNum() {
		return onlineNum;
	}

	public synchronized void setOnlineNum(int onlineNum) {
		this.onlineNum = onlineNum;
	}

	public synchronized Set<String> getOnline() {
		return online;
	}

	public synchronized void setOnline(Set<String> online) {
		this.online = online;
	}
	
	public boolean getContinuous() {
		return continuous;
	}

	public void setContinuous(boolean continuous) {
		this.continuous = continuous;
	}
	
	public synchronized boolean add(String player) {
		boolean res = online.add(player);
		if(res)
			onlineNum = online.size();
		return res;
	}
	
	public synchronized boolean remove(String player) {
		boolean res = online.remove(player);
		if(res)
			onlineNum = online.size();
		return res;
	}
	
	public synchronized List<String> check(List<String> playerList) {
		Set<String> newOnline = new TreeSet<>();
		Iterator<String> it = playerList.iterator();
		String player;
		while(it.hasNext()) {
			player = it.next();
			if(online.remove(player))
				it.remove();
			newOnline.add(player);
		}		
		List<String> removed = new ArrayList<>(online);
		online = newOnline;
		onlineNum = online.size();
		return removed;
	}

	
}


