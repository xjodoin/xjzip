package ca.reivax.xjzip;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.pivot.collections.ArrayList;
import org.apache.pivot.collections.List;
import org.apache.pivot.collections.Map;
import org.apache.pivot.wtk.Application;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.ComponentMouseButtonListener;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.Display;
import org.apache.pivot.wtk.Mouse.Button;
import org.apache.pivot.wtk.TableView;
import org.apache.pivot.wtk.Window;
import org.apache.pivot.wtkx.WTKXSerializer;

public class ArchiveManager implements Application {

	private Window window;

	@Override
	public void startup(Display display, Map<String, String> properties)
			throws Exception {
		WTKXSerializer wtkxSerializer = new WTKXSerializer();
		window = (Window) wtkxSerializer
				.readObject(this, "archivemanager.wtkx");

		TableView tableView = (TableView) wtkxSerializer.get("tableView");

		tableView.getComponentMouseButtonListeners().add(
				new ComponentMouseButtonListener() {

					@Override
					public boolean mouseClick(Component component,
							Button button, int x, int y, int count) {

						if (count == 2) {
							TableView tableView = (TableView) component;
							Object selectedRow = tableView.getSelectedRow();
							if (selectedRow instanceof ArchiveFolder) {
								tableView
										.setTableData(((ArchiveFolder) selectedRow)
												.getItems());
							} else if (selectedRow instanceof ArchiveItem) {
								try {
									ArchiveItem item = ((ArchiveItem) selectedRow);

									InputStream inputStream = item
											.getInputStream();
									byte[] buf = new byte[1024];

									int readCount = 0;

									File createTempFile = File.createTempFile(
											"tmp", item.getSuffix());
									createTempFile.deleteOnExit();

									FileOutputStream fileOutputStream = new FileOutputStream(
											createTempFile);

									while ((readCount = inputStream.read(buf)) != -1) {
										fileOutputStream.write(buf, 0,
												readCount);
									}

									fileOutputStream.flush();
									fileOutputStream.close();
									
//									fileWatcherService.scheduleAtFixedRate(new Run, initialDelay, period, unit)

									Desktop.getDesktop().open(createTempFile);

								} catch (IOException e) {
									e.printStackTrace();
								}

							}

						}

						return false;
					}

					@Override
					public boolean mouseDown(Component component,
							Button button, int x, int y) {
						return false;
					}

					@Override
					public boolean mouseUp(Component component, Button button,
							int x, int y) {
						return false;
					}

				});

		ZipFile zipFile = new ZipFile(
				"C:/Documents and Settings/Xavier/Mes documents/balloonica-icons-set.zip");

		Enumeration<? extends ZipEntry> entries = zipFile.entries();

		java.util.Map<String, ArchiveFolder> hiearchie = new java.util.HashMap<String, ArchiveFolder>();

		final List<ArchiveItem> tableData = new ArrayList<ArchiveItem>();

		while (entries.hasMoreElements()) {
			ZipEntry zipEntry = (ZipEntry) entries.nextElement();
			String name = zipEntry.getName();

			if (name.endsWith("/")) {

				int lastIndexOf = name.substring(0, name.length() - 1)
						.lastIndexOf('/');

				if (lastIndexOf > 0) {
					ArchiveFolder archiveFolder = new ArchiveFolder(zipFile,
							zipEntry);
					hiearchie.put(archiveFolder.getName(), archiveFolder);
					hiearchie.get(name.substring(0, lastIndexOf + 1))
							.addToItems(archiveFolder);
				} else {
					ArchiveFolder archiveFolder = new ArchiveFolder(zipFile,
							zipEntry);
					hiearchie.put(archiveFolder.getName(), archiveFolder);
					tableData.add(archiveFolder);
				}

			} else {
				int lastIndexOf = name.lastIndexOf('/');

				if (lastIndexOf > 0) {
					hiearchie.get(name.substring(0, lastIndexOf + 1))
							.addToItems(new ArchiveItem(zipFile, zipEntry));
				} else {
					tableData.add(new ArchiveItem(zipFile, zipEntry));
				}
			}
		}

		tableView.setTableData(tableData);

		window.open(display);
	}

	@Override
	public boolean shutdown(boolean optional) {
		if (window != null) {
			window.close();
		}

		return false;
	}

	@Override
	public void suspend() {
	}

	@Override
	public void resume() {
	}

	public static void main(String[] args) {
		DesktopApplicationContext.main(ArchiveManager.class, args);
	}

}
