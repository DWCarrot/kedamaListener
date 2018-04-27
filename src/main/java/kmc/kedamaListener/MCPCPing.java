package kmc.kedamaListener;

import java.util.List;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.time.Instant;

import com.google.gson.Gson;
import kmc.kedamaListener.PlayerCount;
import kmc.kedamaListener.js.mcpingreply.MCPCPingReply;

public class MCPCPing implements Runnable {
	
	Logger logger = App.logger;
	Gson gson = App.gsonbuilder.create();
	
	
	PlayerCount plc;
	PlayerCountRecord rc;
	
	InetSocketAddress addr;
	int ptcVer;
	Charset charset = Charset.forName("UTF-8");
	
	Socket socket;
	
	byte[] buf;
	int pointer;
	int length;
	static final int MaxCapability = 32768;
	
	int failCount;
	int successCount;
	
	int normalPeriod;
	int retryPeriod;
	int maxFailTimes;
	
	
	
	public void setMaxFailTimes(int maxFailTimes) {
		this.maxFailTimes = maxFailTimes;
	}

	public MCPCPing(String hostname, int port, int protocolVersion, PlayerCount playerCount) {
		addr = new InetSocketAddress(hostname, port);
		ptcVer = protocolVersion;
		buf = new byte[8192];
		pointer = 0;
		failCount = 0;
		successCount = 0;
		normalPeriod = 60;
		retryPeriod = 5;
		maxFailTimes = 5;
		rc = PlayerCountRecord.getPlayerCountRecord();
		plc = playerCount;
		
	}

	public int getFailCount() {
		return failCount;
	}
	
	public int getInterval() {
		if(failCount > 0)
			return retryPeriod;
		return normalPeriod;
	}

	public void setNormalPeriod(int normalPeriod) {
		this.normalPeriod = normalPeriod;
	}

	public void setRetryPeriod(int retryPeriod) {
		this.retryPeriod = retryPeriod;
	}

	public void setPlayerCount(PlayerCount playerCount) {
		this.plc = playerCount;
	}
	
	private void ensureCapability(int n) {
		if(pointer + n + 1 > buf.length) {
			if(pointer + n + 1 > MaxCapability)
				return;
			byte[] newBuf = new byte[pointer + n + 1];
			System.arraycopy(buf, 0, newBuf, 0, buf.length);
			buf = newBuf;
		}
	}
	
	public void writeVarInt(int value) {
		if(value >= 0 && value <= 0xFF) {
			ensureCapability(1);
			buf[pointer++] = (byte) value;
			return;
		}
		while((value & 0xFFFFFF80)!= 0) {
			ensureCapability(1);
			buf[pointer++] = (byte)(value & 0x7F | 0x80);
			value >>>= 7;
		}
		ensureCapability(1);
		buf[pointer++] = (byte)(value & 0x7F);
	}
	
	public void writeString(String s) {
		writeVarInt(s.length());
		ensureCapability(s.length());
		byte[] tmp = s.getBytes(charset);
		System.arraycopy(tmp, 0, buf, pointer, tmp.length);
		pointer += tmp.length;
	}
	
	public void writeShortBE(short value) {
		ensureCapability(2);
		buf[pointer++] = (byte) ((value & 0xFF00) >> 8);
		buf[pointer++] = (byte) (value & 0x00FF);
	}
	
	public int readVarInt() throws IOException {
		int value = 0;
		for(int b = buf[pointer++], i = 0; pointer < length; b = buf[pointer++], i += 7) {
			value |= ((b & 0x7F) << i);
			if((b & 0x80) == 0x00)
				break;
		}
		if(pointer >= length)
			throw new IOException();
		return value;
	}
	
	public String readString() throws IOException {
		int len = readVarInt();
		if(len + pointer > length)
			throw new IOException();
		String s = new String(buf, pointer, len, charset);
		pointer += len;
		return s;
	}
	
	public void writeTo(OutputStream out) throws IOException {
		int value = pointer;
		if(value >= 0 && value <= 0xFF) {
			out.write(value);
		} else {
			while((value & 0xFFFFFF80)!= 0) {
				out.write(value & 0x7F | 0x80);
				value >>>= 7;
			}
			out.write(value & 0x7F);
		}
		out.write(buf, 0, pointer);
		pointer = 0;
	}
	
	public void readFrom(InputStream in) throws IOException {
		length = 0;
		for(int i = 0, b = in.read(); b >= 0; b = in.read(), i += 7) {
			length |= ((b & 0x7F) << i);
			if((b & 0x80) == 0x00)
				break;
		}
		pointer = 0;
		ensureCapability(length);
		int n;
		for(
				n = in.read(buf, pointer, length - pointer);
				n > 0 && pointer < length;
				pointer += n, n = in.read(buf, pointer, length - pointer)
				);
		pointer = 0;
	}
	
	public void reset() {
		pointer = 0;
		length = 0;
	}
	
	public void close() {
		try {
			if(socket != null)
				socket.close();
			socket = null;
			pointer = 0;
			length = 0;
		} catch (IOException e) {
			logger.warn("#Exception @{}", Thread.currentThread(), e);
		}
	}
	
	public void ping() {
		try {
			socket = new Socket();
			socket.connect(addr, 1000);
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			reset();
			
			writeVarInt(0x00);						//	package id
			writeVarInt(ptcVer);					//	protocol version
			writeString(addr.getHostName());		//	host name
			writeShortBE((short) addr.getPort());;	//	port
			writeVarInt(0x01);						//	next state
			writeTo(out);
			out.flush();
			
			writeVarInt(0x00);						//	package id
			writeTo(out);
			out.flush();
			
			readFrom(in);
			int pkgid = readVarInt();
			if(pkgid != 0x00)
				throw new IOException("Unpredictable reply: package id = " + pkgid);
			Instant t = Instant.now();
			
			MCPCPingReply reply = gson.fromJson(readString(), MCPCPingReply.class);
			List<String> players = reply.getPlayerList();
			List<String> removes = plc.check(players);
			
			logger.info("#ping adds={} removes={}", players, removes);
			if(!(players.isEmpty() && removes.isEmpty() && successCount != 0)) {
				plc.setTime(t);
				plc.setContinuous(false);
				rc.record();
			}
			++successCount;
			failCount = 0;
		} catch (IOException e) {
			++failCount;
			logger.warn("#Exception @{}", Thread.currentThread(), e);
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				logger.warn("#Exception @{}", Thread.currentThread(), e);
			}
		}
	}

	
	
	@Override
	public void run() {
		long last;
		long next = System.currentTimeMillis();;
		try {
			while(!Thread.interrupted()) {
				last = next;
				ping();
				if(failCount >= maxFailTimes)
					throw new MCPingException("Failed too many times to ping");
				next = last + (long)(getInterval() * 1000);
				Thread.sleep(next - System.currentTimeMillis());
			}
		} catch(InterruptedException | MCPingException e) {
			logger.warn("#Exception @{}", Thread.currentThread(), e);
		}
	}
	
}
