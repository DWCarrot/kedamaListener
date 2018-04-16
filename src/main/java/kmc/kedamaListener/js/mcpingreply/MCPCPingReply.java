package kmc.kedamaListener.js.mcpingreply;

import java.util.ArrayList;
import java.util.List;

public class MCPCPingReply {

	public Version version;

	public Players players;
	
	public Description description;
	
	public String favicon;
	
	public List<String> getPlayerList() {
		if(players.sample == null)
			return new ArrayList<>();
		List<String> list = new ArrayList<>(players.sample.size());
		for(Sample p : players.sample)
			list.add(p.name);
		return list;
	}
}

