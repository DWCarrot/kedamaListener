package kmc.kedamaListener;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class MCPCPackageInputStream extends InputStream {
	
	private final static int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
	
	protected byte[] buf;
	
	protected int pos;
	
	protected int count;
	
	public MCPCPackageInputStream() {
		buf = null;
		pos = 0;
		count = 0;
	}
	
	public int fromByteArray(byte[] b, int off, int len) throws IOException {
		 if (off < 0 || len < 0 || off + len > b.length)
			 throw new IndexOutOfBoundsException();
		 int p = off;
		 int value = 0;
		 int k = 0x80;
		 for(int j = 0; (k & 0x80) == 0x80; j += 7) {
			 k = b[p++];
			 if(p > len + off)
				 return -1;
			 if(j > 31)
				 throw new IOException("VarInt too big");
			 value |= ((k & 0x7F) << j);
		 }
		 if(value < 1)
			 throw new IOException("Invalid data package head");
		 if(value > MAX_BUFFER_SIZE)
			 throw new IOException("Data package too big");
		 if(p + value > off + len)
			 return -1;
		 if(buf == null || buf.length < value)
			buf = new byte[value];
		 System.arraycopy(b, p, buf, 0, value);
		 count = value;
		 return p + value - off;
	}
	
	public boolean readFrom(InputStream in) throws IOException {
		int value = 0;
		int k = 0x80;
		for(int j = 0; (k & 0x80) == 0x80; j += 7) {
			k = in.read();
			if(k < 0)
				return false;
			if (j > 31)
				throw new IOException("VarInt too big");
			value |= ((k & 0x7F) << j);
		}
		if(value < 1)
			throw new IOException("Invalid data package head");
		if(value > MAX_BUFFER_SIZE)
			throw new IOException("Data package too big");
		if(buf == null || buf.length < value)
			buf = new byte[value];
		int len;
		count = 0;
		while(count < value) {
			len = in.read(buf, count, value - count);
			if(len < 0)
				throw new EOFException();
			count += len;
		}
		pos = 0;
		return true;
	}
	
	@Override
	public synchronized int read() throws IOException {
		if(pos < count)
			return (int)buf[pos++] & 0xFF;
		return -1;
    }
	
	@Override
	public synchronized int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }
	
	@Override
	public synchronized int read(byte b[], int off, int len) throws IOException {
        if(off < 0 || len < 0 || off + len > b.length)
        	throw new IndexOutOfBoundsException();
        len = Math.min(len, count - pos);
        System.arraycopy(buf, pos, b, len, len);
        pos += len;
        return len;
    }

	public synchronized int readVarInt() throws IOException {
		int value = 0;
		int k = 0x80;
		for(int j = 0; (k & 0x80) == 0x80; j += 7) {
			k = read();
			if(k < 0)
				throw new EOFException();
			if (j > 31)
				throw new IOException("VarInt too big");
			value |= ((k & 0x7F) << j);
		}
		return value;
	}
	
	public synchronized String readHString() throws IOException {
		int length = readVarInt();
		if(length < 0)
			throw new IOException("Invalid String Length");
		if(available() < length)
			throw new EOFException();
		String res = new String(buf, pos, length, Charset.forName("UTF-8"));
		pos += length;
		return res;
	}
	
	public synchronized char readShortBE() throws IOException {
        if (available() < 2)
        	throw new EOFException();
        int ch1 = (int)buf[pos++] & 0xFF;
		int ch2 = (int)buf[pos++] & 0xFF;
        return (char) ((ch1 << 8) | ch2);
    }
	
	public synchronized int readIntBE() throws IOException {
		if(available() < 4)
			throw new EOFException();
		int ch1 = (int)buf[pos++] & 0xFF;
        int ch2 = (int)buf[pos++] & 0xFF;
        int ch3 = (int)buf[pos++] & 0xFF;
        int ch4 = (int)buf[pos++] & 0xFF;
       
        return ((ch1 << 24) | (ch2 << 16) | (ch3 << 8) | (ch4 << 0));
    }
	
	public synchronized long readLongBE() throws IOException {
		if(available() < 8)
			throw new EOFException();
		int end = pos + 8;
		long value = 0;
		for(;pos < end; ++pos) {
			value <<= 8;
			value |= ((long)buf[pos] & 0xFF);
		}
		return value;
    }
	
	public synchronized int available() {
        return count - pos;
    }
	
}
