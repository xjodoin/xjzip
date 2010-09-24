package ca.reivax.xjzip;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ArchiveItem {

	private ZipEntry entry;
	private ArchiveFolder archiveFolder;
	private ZipFile zipFile;

	public ArchiveItem() {
	}

	public ArchiveItem(ZipFile zipFile,ZipEntry entry) {
		this.zipFile = zipFile;
		this.entry = entry;
	}

	public String getName() {
		return entry.getName();
	}

	public String getSuffix()
	{
		String name = entry.getName();
		return name.substring(name.lastIndexOf('.'));
	}

	public Long getSize() {
		return entry.getSize();
	}

	public InputStream getInputStream() throws IOException 
	{
		return zipFile.getInputStream(entry);
	}

	public void setParent(ArchiveFolder archiveFolder) {
		this.archiveFolder = archiveFolder;
	}

}
