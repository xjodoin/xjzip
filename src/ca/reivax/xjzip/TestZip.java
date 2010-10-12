package ca.reivax.xjzip;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.schlichtherle.util.zip.ZipEntry;

public class TestZip {

	public static void main(String[] args) throws NullPointerException, IOException {
		ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream("C:/Documents and Settings/Xavier/Bureau/test.zip"));
		
		zipOutputStream.putNextEntry(new ZipEntry("Test.pdf"));
		
		FileInputStream fileInputStream = new FileInputStream("C:/Documents and Settings/Xavier/Bureau/Test.pdf");
		byte[] buf = new byte[1024];
		int count = 0;
		
		while((count = fileInputStream.read(buf))!= -1)
		{
			zipOutputStream.write(buf, 0, count);
		}
		
		zipOutputStream.closeEntry();
		zipOutputStream.close();
		fileInputStream.close();
		
	}
}
