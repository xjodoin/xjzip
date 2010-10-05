package ca.reivax.xjzip;

import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;

import de.schlichtherle.io.File;

public class FileView {

	private final File file;

	public String getName() {
		return file.getName();
	}

	public Long getSize() {
		return file.length();
	}

	public boolean isDirectory() {
		return file.isDirectory();
	}

	public boolean isFile() {
		return file.isFile();
	}

	public FileView(File file) {
		this.file = file;
	}

	public List<FileView> listFiles() {
		List<FileView> list = new ArrayList<FileView>();

		for (java.io.File child : file.listFiles()) {
			list.add(new FileView((File) child));
		}

		return list;
	}

	public File getFile() {
		return file;
	}

}
