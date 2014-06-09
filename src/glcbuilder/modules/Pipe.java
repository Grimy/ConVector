package modules;

import java.io.*;

public class Pipe {
	public static void pipe(InputStream in, OutputStream out) throws IOException {
		int n;
		byte[] buffer = new byte[4096];
		while ((n = in.read(buffer)) >= 0) {
			out.write(buffer, 0, n);
		}
		out.close();
	}
}
