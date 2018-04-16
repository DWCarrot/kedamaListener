
package kmc.kedamaListener;

public class IRCUser {
	
	protected String nick;
	
	protected String username;
	
	protected String host;
	
	static IRCUser generateFromPrefix(String prefix) {
		IRCUser user = new IRCUser();
		if(prefix != null)
			user.prase(prefix);
		return user;
	}
	
	public IRCUser() {
		nick = null;
		username = null;
		host = null;
	}
	
	public IRCUser(String nick, String username, String host) {
		this.nick = nick;
		this.username = username;
		this.host = host;
	}
	
	public void prase(String prefix) {
		nick = username = host = "";
		int i = prefix.indexOf('!');
		if(i > 0) {
			nick = prefix.substring(0, i++);
			int j = prefix.indexOf('@', i);
			if(j > i) {
				username = prefix.substring(i, j++);
				host = prefix.substring(j);
			} else {
				username = prefix.substring(i);
			}
		} else {
			nick = prefix;
		}
	}
	
	public String getNick() {
		return nick;
	}
	
	public String getServername() {
		return nick;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getHost() {
		return host;
	}
	
	public String asString() {
		StringBuilder s = new StringBuilder();
		s.append(nick);
		if(username != null && !username.isEmpty())
			s.append('!').append(username);
		if(host != null && !host.isEmpty())
			s.append('@').append(host);
		return s.toString();
	}
	
	public StringBuilder asString(StringBuilder s) {
		if(s == null)
			s = new StringBuilder();
		s.append(nick);
		if(username != null && !username.isEmpty())
			s.append('!').append(username);
		if(host != null && !host.isEmpty())
			s.append('@').append(host);
		return s;
	}
}
