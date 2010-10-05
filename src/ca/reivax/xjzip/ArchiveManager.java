package ca.reivax.xjzip;

import java.awt.Desktop;
import java.io.IOException;

import org.apache.pivot.collections.Map;
import org.apache.pivot.wtk.Application;
import org.apache.pivot.wtk.Component;
import org.apache.pivot.wtk.ComponentMouseButtonListener;
import org.apache.pivot.wtk.DesktopApplicationContext;
import org.apache.pivot.wtk.Display;
import org.apache.pivot.wtk.MessageType;
import org.apache.pivot.wtk.Mouse.Button;
import org.apache.pivot.wtk.Prompt;
import org.apache.pivot.wtk.TableView;
import org.apache.pivot.wtk.Window;
import org.apache.pivot.wtkx.WTKXSerializer;

import de.schlichtherle.io.File;

public class ArchiveManager implements Application, FileListener {

	private Window window;

	private FileMonitor fileMonitor = new FileMonitor(500);

	private File currentEntry;

	@Override
	public void startup(Display display, Map<String, String> properties)
			throws Exception {

		File zipFile = new File(
				"C:/Documents and Settings/Xavier/Bureau/Bureau.zip");

		fileMonitor.addListener(this);

		WTKXSerializer wtkxSerializer = new WTKXSerializer();
		window = (Window) wtkxSerializer
				.readObject(this, "archivemanager.wtkx");

		window.setTitle(zipFile.getName());

		TableView tableView = (TableView) wtkxSerializer.get("tableView");

		tableView.getComponentMouseButtonListeners().add(
				new ComponentMouseButtonListener() {

					@Override
					public boolean mouseClick(Component component,
							Button button, int x, int y, int count) {

						if (count == 2) {
							TableView tableView = (TableView) component;
							Object selectedRow = tableView.getSelectedRow();
							if (selectedRow instanceof FileView
									&& ((FileView) selectedRow).isDirectory()) {

								tableView.setTableData(((FileView) selectedRow)
										.listFiles());
							} else {
								try {
									currentEntry = ((FileView) selectedRow)
											.getFile();

									java.io.File createTempFile = File
											.createTempFile("tmp",
													currentEntry.getName());
									createTempFile.deleteOnExit();

									currentEntry.copyTo(createTempFile);

									fileMonitor.addFile(createTempFile);

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

		tableView.setTableData(new FileView(zipFile).listFiles());

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

	@Override
	public void fileChanged(java.io.File file) {

		Prompt.prompt(MessageType.INFO, "The file has been updated", window);
		currentEntry.copyFrom(file);

	}

}
