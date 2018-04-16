package kmc.kedamaListener;

import java.util.Random;

public class OffPassword {

	static char[] table = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        };
	
	static Random random = new Random(System.currentTimeMillis());
	
	private static int getIndex(char c) {
		if(c >= 'A' && c <= 'Z')
			return (int)(c - 'A');
		if(c >= 'a' && c <= 'z')
			return (int)(c - 'a') + 26;
		return (int)(c - '0') + 52;
	}
	
	public static char[] generatePw1() {
		char[] res = new char[random.nextInt(9) + 8];
		for(int i = 0; i < res.length; ++i)
			res[i] = table[random.nextInt(table.length)];
		return res;
	}
	
	public static char[] encodePw1(char[] pw) {
		char[] res = new char[getIndex(pw[pw.length - 3]) % 9 + 8];
		int times = res.length + getIndex(pw[3]) % pw.length;
		for(int i = 0; i < times; ++i)
			res[i % res.length] = table[(getIndex(res[(i + 2) % res.length]) ^ getIndex(pw[i % pw.length])) % table.length ];
		return res;
	}
	
	public static void clear(char[] pw) {
		for(int i = 0; i < pw.length; ++i)
			pw[i] = '\0';
	}
}
