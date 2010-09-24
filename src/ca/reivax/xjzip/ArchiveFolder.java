package ca.reivax.xjzip;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;

public class ArchiveFolder extends ArchiveItem {
	private List<ArchiveItem> items = new ArrayList<ArchiveItem>();

	public ArchiveFolder(ZipFile zipFile,ZipEntry entry) {
		super(zipFile,entry);
	}

	public void addToItems(ArchiveItem item) 
	{
		items.add(item);
		item.setParent(this);
	}

	public void setItems(List<ArchiveItem> items) {
		this.items = items;
	}

	public List<ArchiveItem> getItems() {
		return items;
	}
	
}
