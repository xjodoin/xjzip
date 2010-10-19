package ca.reivax.xjzip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.schlichtherle.util.zip.ZipEntry;

public class Main {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length == 1) {

			String path = args[0];

			File file = new File(path);

			if (file.exists()) {
				String name = file.getName();
				String nameWithoutExtention = name.substring(0,
						name.lastIndexOf('.'));

				FileOutputStream out = new FileOutputStream(
						nameWithoutExtention + ".zip");
				ZipOutputStream2 zipOutputStream2 = new ZipOutputStream2(out);

				zip(zipOutputStream2, file.getParentFile(), file);

				zipOutputStream2.close();
			}
		}
	}

	private static void zip(ZipOutputStream2 zipOutputStream2, File root,
			File file) throws IOException {

		if (file.isFile()) {
			zipOutputStream2.putNextEntry(new ZipEntry(root.toURI()
					.relativize(file.toURI()).getPath()));
			write(zipOutputStream2, file);
			zipOutputStream2.closeEntry();
		} else {
			zipOutputStream2.putNextEntry(new ZipEntry(root.toURI()
					.relativize(file.toURI()).getPath()));
			zipOutputStream2.closeEntry();
			File[] listFiles = file.listFiles();

			for (File subFile : listFiles) {
				zip(zipOutputStream2, root, subFile);
			}
		}

	}

	private static void write(ZipOutputStream2 zipOutputStream2, File file)
			throws IOException {
		FileInputStream fileInputStream = new FileInputStream(file);
		int count = 0;
		byte[] buf = new byte[1024];
		while ((count = fileInputStream.read(buf)) != -1) {
			zipOutputStream2.write(buf, 0, count);
		}

		fileInputStream.close();
	}

}
