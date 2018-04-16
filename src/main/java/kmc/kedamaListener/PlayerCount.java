package kmc.kedamaListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class PlayerCount {
	
	private long timestamp;
	
	private int onlineNum;
	
	private Set<String> online;
	
	public PlayerCount() {
		timestamp = System.currentTimeMillis();
		onlineNum = 0;
		online = new TreeSet<>();
	}

	public synchronized long getTime() {
		return timestamp;
	}

	public synchronized void setTime(long time) {
		this.timestamp = time;
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


