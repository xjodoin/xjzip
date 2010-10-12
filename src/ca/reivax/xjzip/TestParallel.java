package ca.reivax.xjzip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZInputStream;
import com.jcraft.jzlib.ZOutputStream;

import de.schlichtherle.io.FileInputStream;
import de.schlichtherle.io.FileOutputStream;

public class TestParallel {

	public static void main(String[] args) throws IOException {

		InputStream resourceAsStream = TestParallel.class
				.getResourceAsStream("ParallelDeflateOutputStream.cs");

		java.io.FileOutputStream byteArrayOutputStream = new java.io.FileOutputStream("C:/testparajava.xml");
		ParallelDeflateOutputStream deflaterOutputStream = new ParallelDeflateOutputStream(
				byteArrayOutputStream);

		{
			byte[] buf = new byte[128];
			int count = 0;
			while ((count = resourceAsStream.read(buf)) != -1) {

				deflaterOutputStream.write(buf, 0, count);
			}
		}

		deflaterOutputStream.close();

		InflaterInputStream inflaterInputStream = new InflaterInputStream(
				new FileInputStream(new File("c:/testparajava.xml")),new Inflater(true));

		OutputStream outputStream = new FileOutputStream(new File("testinfjava.xml"));

		byte[] buf = new byte[128];
		int count = 0;
		while ((count = inflaterInputStream.read(buf)) != -1) {

			outputStream.write(buf, 0, count);
		}
		
		inflaterInputStream.close();
		outputStream.close();
		
	}

}
