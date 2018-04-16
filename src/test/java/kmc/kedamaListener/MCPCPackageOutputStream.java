package kmc.kedamaListener;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class MCPCPackageOutputStream extends OutputStream {
	

	protected byte[] buf;
	
	protected int offset;
	
	protected int count;
	
	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
	private static final int MAX_DATASIZE_SIZE = 5; 
	
	
	private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buf.length > 0 && 2 * minCapacity < MAX_ARRAY_SIZE) {
        	byte[] newBuf = new byte[2 * minCapacity];
        	System.arraycopy(buf, MAX_DATASIZE_SIZE, newBuf, MAX_DATASIZE_SIZE, buf.length);
        	buf = newBuf;
        }
    }
	
	private int lenthVarInt(int val) {
		int len;
		for(len = 1; (val = val >>> 7) != 0; ++len);
		return len;
	}

	private synchronized void insertPackageSize() {
		int dataSize = count - MAX_DATASIZE_SIZE;
		offset = MAX_DATASIZE_SIZE - lenthVarInt(dataSize);
		for(int i = offset; ; ++i, dataSize >>>= 7) {
			if((dataSize & 0xFFFFFF80) == 0) {
				buf[i] = (byte)dataSize;
				return;
			}
			buf[i] = (byte)(dataSize & 0x7F | 0x80);
		}
	}
	
	public MCPCPackageOutputStream() {
		this(32);
	}
	
	public MCPCPackageOutputStream(int size) {
		if (size < 0)
            throw new IllegalArgumentException("Negative initial size: " + size);
        buf = new byte[size];
		count = MAX_DATASIZE_SIZE;
		offset = MAX_DATASIZE_SIZE;
	}
	
	@Override
	public synchronized void write(int b) throws IOException {
		ensureCapacity(count + 1);
        buf[count++] = (byte) b;
	}

	public synchronized void write(byte b[], int off, int len) {
        if (off < 0 || len < 0 || off + len > b.length)
            throw new IndexOutOfBoundsException();
        ensureCapacity(count + len);
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }
	
	public synchronized int size() {
        return count - MAX_DATASIZE_SIZE;
    }
	
	protected synchronized void reset() {
        count = MAX_DATASIZE_SIZE;
        offset = MAX_DATASIZE_SIZE;
    }
	
	public synchronized byte[] toByteArray() {
		insertPackageSize();
		byte[] arr = new byte[count - offset];
		System.arraycopy(buf, offset, arr, 0, count - offset);
        return arr;
    }
	
	public synchronized void writeTo(OutputStream out) throws IOException {
		insertPackageSize();
        out.write(buf, offset, count - offset);
        reset();
    }
	
	
	public synchronized void writeLongBE(long val) throws IOException {
		ensureCapacity(count + 8);
		count += 8;
		for(int m = count - 1; m >= count - 8; --m) {
			buf[m] = 0;
			buf[m] |= (byte)(val & 0xFFL);
			val >>>= 8;
		}	
	}
	
	public synchronized void writeIntBE(int val) throws IOException {
		ensureCapacity(count + 4);
		buf[count++] = (byte)((val & 0xFF000000) >>> 24);
		buf[count++] = (byte)((val & 0xFF0000) >>> 16);
		buf[count++] = (byte)((val & 0xFF00) >>> 8);
		buf[count++] = (byte)(val & 0xFF);
	}
	
	public synchronized void writeShortBE(char val) throws IOException {
		ensureCapacity(count + 2);
		buf[count++] = (byte)((val & 0xFF00) >>> 8);
		buf[count++] = (byte)(val & 0xFF);
	}
	
	public synchronized void writeVarInt(int val) throws IOException {
		if(val > 0 && val < 128) {
			ensureCapacity(count + 1);
			buf[count++] = (byte)val;
			return;
		}
		ensureCapacity(count + lenthVarInt(val));
        for(;;val >>>= 7) {
        	if((val & 0xFFFFFF80) == 0) {
				buf[count++] = (byte)val;
				return;
			}
			buf[count++] = (byte)(val & 0x7F | 0x80);
        }
	}
	
	public synchronized void writeHString(String s) throws IOException {
			writeVarInt(s.length());
			write(s.getBytes(Charset.forName("UTF-8")));
	}
	
}
