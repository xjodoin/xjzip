package ca.reivax.xjzip;

import java.io.IOException;
import java.io.OutputStream;

public class MultipleOutputStream extends OutputStream{

	private final OutputStream[] outputStreams;


	public MultipleOutputStream(OutputStream ...outputStreams ) {
		this.outputStreams = outputStreams;
	}
	
	
	@Override
	public void write(int b) throws IOException {
		for (OutputStream outputStream : outputStreams) {
			outputStream.write(b);
		}
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		for (OutputStream outputStream : outputStreams) {
			outputStream.write(b, off, len);
		}
	}
	
	@Override
	public void flush() throws IOException {
		for (OutputStream outputStream : outputStreams) {
			outputStream.flush();
		}
	}

	@Override
	public void close() throws IOException {
		for (OutputStream outputStream : outputStreams) {
			outputStream.close();
		}
	}
	
	
}
