package ca.reivax.xjzip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;
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

//		InputStream resourceAsStream = TestParallel.class
//				.getResourceAsStream("ParallelDeflateOutputStream.cs");
//
		FileInputStream fileInputStream = new FileInputStream("/home/xjodoin/Bureau/perfo.ods");
		
		java.io.FileOutputStream byteArrayOutputStream = new java.io.FileOutputStream("test.compress");
		ParallelDeflateOutputStream deflaterOutputStream = new ParallelDeflateOutputStream(
				byteArrayOutputStream);

		{
			byte[] buf = new byte[128];
			int count = 0;
			while ((count = fileInputStream.read(buf)) != -1) {

				deflaterOutputStream.write(buf, 0, count);
			}
		}

		deflaterOutputStream.close();

		InflaterInputStream inflaterInputStream = new InflaterInputStream(
				new FileInputStream(new File("test.compress")),new Inflater(true));

		OutputStream outputStream = new FileOutputStream(new File("testinfjava.log"));

		CRC32 crc32 = new CRC32();
		
		byte[] buf = new byte[1024];
		int count = 0;
		
		int totalcount = 0;
		
		
		int total = 0;
		
		while ((count = inflaterInputStream.read(buf)) != -1) {
			outputStream.write(buf, 0, count);
			
			totalcount+=count;
			System.out.println("Total count "+totalcount);
			crc32.update(buf, 0, count);
			
			total+=count;
			System.out.println(total);
			
		}
		
		inflaterInputStream.close();
		outputStream.close();
		
		System.out.println("CRC 32 inf "+crc32.getValue());
		
	}

}
