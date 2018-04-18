package kmc.kedamaListener;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class IRCMessage {

	protected Instant time;
	
//	protected String prefix;
	
	protected IRCUser user;
	
	protected String command;
	
	protected List<String> middles;
	
	protected String trailing;
	
	static IRCMessage fromString(String msg) throws IOException {
		return new IRCMessage(Instant.now(), msg);
	}
	
	public IRCMessage() {

	}
	
	public IRCMessage(Instant time, String cmd, String... para) {
		if(command == null && "".equals(command))
			return;
		this.time = time;
		user = null;
		command = cmd;
		trailing = "";
		middles = new ArrayList<>(para.length);
		for(String p : para) {
			if(p == null || p.isEmpty())
				continue;
			if(p.charAt(0) == ':') {
				trailing = p.substring(1);
				break;
			}
			middles.add(p);
		}			
	}
	
	public IRCMessage(Instant time, String msg) throws IOException {
		this.time = time;
		parse(msg);
	}
	
	public void parse(String msg) throws IOException {
		int j = 0, i = 0;
		if(msg.startsWith("null"))
			i = 4;
		if(msg.charAt(i) == ':') {
			j = msg.indexOf(' ', i);
			if(j < i)
				throw new IOException("Invalid IRC message: " + msg);
			user = IRCUser.generateFromPrefix(msg.substring(i + 1, j));
			i = j + 1;
		} else {
			user = null;
		}
		j = msg.indexOf(' ', i);
		if(j < 0)
			throw new IOException("Invalid IRC message: " + msg);
		command = msg.substring(i, j);
		i = j + 1;
		middles = new ArrayList<>(10);
		for(j = msg.indexOf(' ', i); msg.charAt(i) != ':'; j = msg.indexOf(' ', i)) {
			if(j < 0) {
				middles.add(msg.substring(i));
				trailing = "";
				return;
			}
			middles.add(msg.substring(i, j));
			i = j + 1;
		}
		trailing = msg.substring(i + 1);
	}

	public IRCMessage setUser(IRCUser user) {
		this.user = user;
		return this;
	}

	public IRCMessage setTime(Instant time) {
		this.time = time;
		return this;
	}

	public IRCMessage setCommand(String command) {
		this.command = command;
		return this;
	}

	public IRCMessage setMiddles(List<String> middles) {
		this.middles = middles;
		return this;
	}

	public IRCMessage setTrailing(String trailing) {
		this.trailing = trailing;
		return this;
	}
	
	public IRCMessage addMiddles(String para) {
		if(middles == null)
			middles = new ArrayList<>();
		middles.add(para);
		return this;
	}
	
	public Instant getTime() {
		return time;
	}

	public String getCommand() {
		return command;
	}

	public List<String> getMiddles() {
		return middles;
	}

	public String getTrailing() {
		return trailing;
	}
	
	public IRCUser getUser() {
		return user;
	}
	
	
	public String asString() {
		if(command == null || command.isEmpty())
			return null;
		StringBuilder s = new StringBuilder();
		if(user != null) {
			s.append(':');
			s = user.asString(s);
			s.append(' ');
		}
		s.append(command);
		if(middles != null)
			for(String m : middles)
				s.append(' ').append(m);
		if(trailing != null && !trailing.isEmpty())
			s.append(' ').append(':').append(trailing);
		return s.toString();
	}

	public StringBuilder asString(StringBuilder s) {
		if(command == null || command.isEmpty())
			return null;
		if(s == null)
			s = new StringBuilder();
		if(user != null) {
			s.append(':');
			s = user.asString(s);
			s.append(' ');
		}
		s.append(command);
		if(middles != null)
			for(String m : middles)
				s.append(' ').append(m);
		if(trailing != null && !trailing.isEmpty())
			s.append(' ').append(':').append(trailing);
		return s;
	}




}
