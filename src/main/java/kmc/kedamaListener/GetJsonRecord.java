package kmc.kedamaListener;

import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedInput;

public class GetJsonRecord implements ChunkedInput<HttpContent> {
	
	private File jsonRecordFolder;
	
	private DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
	
	private File mainFile;
	
	private Pattern rollingNamePattern;
	
	private LocalDate lastSearch;
	
	private Map<LocalDate, File> list;
	
	private int chunksize = 8192;
	
	private byte[] buffer;
	
	private LastHttpContent lastHttpContent = LastHttpContent.EMPTY_LAST_CONTENT;
	
	private Charset charset = Charset.forName("UTF-8");
	
	private List<File> flist;
	private Iterator<File> fit;
	private int bufferOff;
	private int bufferLen;
	private long length;
	private long process;
	private FileInputStream infile;
	private int status;
	
	private byte[] head = {'['};
	private byte[] tail = {']'};
	
	private static byte[] ohead = {'['};
	private static byte[] otail = {']'};
	
	public GetJsonRecord(String mainName, String rollingNamePattern) {
		int i = rollingNamePattern.lastIndexOf('/');
		StringBuilder r = new StringBuilder(rollingNamePattern.substring(i + 1));
		jsonRecordFolder = new File(i < 0 ? "" : rollingNamePattern.substring(0, i));
		i = r.indexOf("%d");
		int j;
		if(r.charAt(i + 2) == '{') {
			j = r.indexOf("}", i + 2);
			formatter = DateTimeFormatter.ofPattern(r.substring(i + 3, j));
			r.replace(i, j + 1, "(\\S+)");
			this.rollingNamePattern = Pattern.compile(r.toString());
		}
		this.mainFile = new File(mainName);
		lastSearch = LocalDate.of(1970, 1, 1);
		buffer = new byte[chunksize]; 
	}
	
	public Map<LocalDate, File> listFiles() {
		Map<LocalDate, File> rc = new TreeMap<>();
		if(mainFile.isFile())
			rc.put(LocalDate.now(), mainFile);
		if(jsonRecordFolder.isDirectory()) {
			String[] fs = jsonRecordFolder.list();
			Matcher m;
			LocalDate t = LocalDate.now();
			for(String f : fs) {
				m = rollingNamePattern.matcher(f);
				if(m.matches()) {
					t = LocalDate.parse(m.group(1), formatter);
					rc.put(t, new File(jsonRecordFolder.getPath() + File.separatorChar + f));
				}
			}
		}
		lastSearch = LocalDate.now();
		list = rc;
		return rc;
	}

	public List<File> getFiles(LocalDate start, LocalDate end) {
		LocalDate now = LocalDate.now();
		if(!now.isEqual(lastSearch))
			list = listFiles();
		List<File> res = new ArrayList<>(list.size());
		Object[] o = list.entrySet().toArray();
		for(int i = 0; i < o.length; ++i) {
			@SuppressWarnings("unchecked")
			Map.Entry<LocalDate, File> e = (Entry<LocalDate, File>) o[i];
			if(e.getValue().isFile()) {
				if(!(e.getKey().isBefore(start) || e.getKey().isAfter(end)))
					res.add(e.getValue());
			} else {
				list.remove(e.getKey());
			}
		}
		return res;
	}
	
	public void setRange(LocalDate start, LocalDate end) {
		flist = getFiles(start, end);
		fit = null;
		status = 4;
	}
	
	public void setCallback(String callback) {
		if(callback == null || callback.isEmpty() || callback.length() + 16 > chunksize) {
			head = ohead;
			tail = otail;
		}
		else {
			byte[] v = callback.getBytes(charset);
			head = new byte[v.length + 2];
			System.arraycopy(v, 0, head, 0, v.length);
			head[v.length + 0] = '(';
			head[v.length + 1] = '[';
			tail = new byte[] {']', ')'};
		}
	}
	
	public long getLength() {
		if(flist == null)
			return 0;
		if(length > 0)
			return length;
		length = head.length + tail.length;
		for(File f : flist) {
			length += f.length();
		}
		return length;
	}
	
	public int writeTo(ByteBuf buf) {
		int j = bufferOff;
		int w = 0;
		int wl;
		int wb;
		while(j < bufferLen) {
			if(buffer[j] == '\r' || buffer[j] == '\n') {
				wl = j - bufferOff;
				if(wl > 0) {
					wb = buf.writableBytes();
					if(wl > wb) {
						buf.writeBytes(buffer, bufferOff, wb);
						bufferOff += wb;
						w += wb;
						return w;
					}
					buf.writeBytes(buffer, bufferOff, wl);
					w += wl;
				}
				bufferOff = j + 1;
			}
			++j;
			++process;
		}
		wl = j - bufferOff;
		if(wl > 0) {
			wb = buf.writableBytes();
			if(wl > wb) {
				buf.writeBytes(buffer, bufferOff, wb);
				bufferOff += wb;
				w += wb;
				return w;
			}
			buf.writeBytes(buffer, bufferOff, wl);
			w += wl;
		}
		bufferOff = j;
		return w;
	}
	
	@Override
	public boolean isEndOfInput() throws Exception {		
		return status == 0;
	}

	@Override
	public void close() throws Exception {
		flist = null;
		fit = null;
		bufferOff = 0;
		bufferLen = -1;
		length = 0;
		process = 0;
		if(infile != null)
			infile.close();
		infile = null;
		status = 0;
	}

	@Deprecated
	@Override
	public HttpContent readChunk(ChannelHandlerContext ctx) throws Exception {
		return readChunk(ctx.alloc());
	}

	@Override
	public HttpContent readChunk(ByteBufAllocator allocator) throws Exception {
		ByteBuf buf = null;
		switch(status) {
		case 4:		//start
			fit = flist.iterator();
			buf = allocator.buffer(chunksize);
			buf.writeBytes(head);
			process += head.length;
			bufferOff = 0;
			bufferLen = -1;
			status = 3;
		case 3:		//read files
			if(buf == null)
				buf = allocator.buffer(chunksize);
			while(buf.isWritable() && status == 3) {
				if(bufferLen < 0) {
					if(fit.hasNext()) {
						infile = new FileInputStream(fit.next());
						bufferOff = bufferLen = 0;
					} else {
						System.arraycopy(tail, 0, buffer, 0, tail.length);
						bufferOff = 0;
						bufferLen = tail.length;
						if(buf.getByte(buf.writerIndex() - 1) == ',') {
							buf.setByte(buf.writerIndex() - 1, buffer[bufferOff++]);
							process++;
						}
						status = 2;
					}
				}
				if(!(bufferOff < bufferLen || infile == null)) {
					bufferLen = infile.read(buffer);
					if(bufferLen < 0) {
						infile.close();
						infile = null;
						continue;
					}
					bufferOff = 0;
				}
				writeTo(buf);
			}
			return new DefaultHttpContent(buf);
		case 2:
			status = 1;
			if(bufferOff < bufferLen) {
				buf = allocator.buffer(bufferLen - bufferOff);
				writeTo(buf);
				return new DefaultHttpContent(buf);
			}
		case 1:		//file end;	to send last content
			status = 0;
			return lastHttpContent;
		}
		return null;
	}

	@Override
	public long length() {
		return getLength();
	}

	@Override
	public long progress() {
		return process;
	}

}
