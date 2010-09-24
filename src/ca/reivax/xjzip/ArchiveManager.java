package ca.reivax.xjzip;

import java.awt.Desktop;
import java.io.IOException;

import org.apache.pivot.collections.ArrayList;
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

import de.schlichtherle.io.File;

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
							if (selectedRow instanceof File
									&& ((File) selectedRow).isDirectory()) {
								tableView.setTableData(new ArrayList<File>(
										(File[]) ((File) selectedRow)
												.listFiles()));
							} else {
								try {
									File item = (File) selectedRow;

									java.io.File createTempFile = File
											.createTempFile("tmp",
													item.getName());
									createTempFile.deleteOnExit();

									item.copyTo(createTempFile);

									// fileWatcherService.scheduleAtFixedRate(new
									// Run, initialDelay, period, unit)

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

		File zipFile = new File(
				"C:/Documents and Settings/Xavier/Bureau/Bureau.zip");

		File[] listFiles = (File[]) zipFile.listFiles();

		tableView.setTableData(new ArrayList<File>(listFiles));

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
