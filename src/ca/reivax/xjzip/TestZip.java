package ca.reivax.xjzip;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import de.schlichtherle.util.zip.ZipEntry;

public class TestZip {
	public static void main(String[] args) throws NullPointerException,
			IOException {
		
		Chronometer chronometer = new Chronometer();
		chronometer.start();
		
		FileOutputStream out = new FileOutputStream(
				"/home/xjodoin/Bureau/test.zip");
		ZipOutputStream2 zipOutputStream2 = new ZipOutputStream2(out);
		zipOutputStream2.putNextEntry(new ZipEntry("standalone.zip"));

		FileInputStream fileInputStream = new FileInputStream(
				"/home/xjodoin/Bureau/standalone.zip");
		int count = 0;
		byte[] buf = new byte[1024];
		while ((count = fileInputStream.read(buf)) != -1) {
			zipOutputStream2.write(buf, 0, count);
		}

		zipOutputStream2.close();
		fileInputStream.close();

		chronometer.stop();
		
		System.out.println(chronometer.getSeconds());
	}
}
