package ca.reivax.xjzip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZOutputStream;

public class TestParallel {

	public static void main(String[] args) throws IOException {

		InputStream resourceAsStream = TestParallel.class
				.getResourceAsStream("archivemanager.wtkx");

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ParalleDeflateOutputstream deflaterOutputStream = new ParalleDeflateOutputstream(
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
				new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

		ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();

		byte[] buf = new byte[1024];
		int count = 0;
		while ((count = inflaterInputStream.read(buf)) != -1) {

			byteArrayOutputStream2.write(buf, 0, count);
		}
		
		inflaterInputStream.close();
		
		System.out.println(new String(byteArrayOutputStream2.toByteArray()));

	}

}
