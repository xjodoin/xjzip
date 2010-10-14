package ca.reivax.xjzip;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;
import com.jcraft.jzlib.ZStreamException;

import de.schlichtherle.util.zip.DeflaterStream;

public class ParallelDeflateOutputStream extends FilterOutputStream implements
		DeflaterStream {

	private static class WorkItem implements Callable<byte[]> {

		private ZStream z = new ZStream();

		private final byte[] b;
		private final int off;
		private final int len;

		protected int bufsize = 1024;
		protected byte[] buf = new byte[bufsize];

		public WorkItem(byte b[], int off, int len) {
			this.b = b;
			this.off = off;
			this.len = len;

		}

		@Override
		public byte[] call() throws Exception {

			if (len == 0)
				return new byte[0];

			ByteArrayOutputStream out = new ByteArrayOutputStream();

			z.deflateInit(JZlib.Z_DEFAULT_COMPRESSION, true);

			int err;
			z.next_in = b;
			z.next_in_index = off;
			z.avail_in = len;

			do {
				z.next_out = buf;
				z.next_out_index = 0;
				z.avail_out = bufsize;
				err = z.deflate(JZlib.Z_NO_FLUSH);

				if (err != JZlib.Z_OK) {
					throw new ZStreamException("deflating: " + z.msg);
				}
				out.write(buf, 0, bufsize - z.avail_out);
			} while (z.avail_in > 0 || z.avail_out == 0);

			z.next_out = buf;
			z.next_out_index = 0;
			z.avail_out = bufsize;
			err = z.deflate(JZlib.Z_SYNC_FLUSH);

			out.write(buf, 0, bufsize - z.avail_out);

			return out.toByteArray();
		}
	}

	private ExecutorCompletionService<byte[]> executorCompletionService;
	private ExecutorService newFixedThreadPool;

	private AtomicBoolean writerThreadActive = new AtomicBoolean();
	private Thread writer;
	private int count = 0;
	private long bytesRead = 0;
	private long bytesWritten = 0;

	public ParallelDeflateOutputStream(OutputStream out) {
		super(out);

		newFixedThreadPool = Executors.newFixedThreadPool(Runtime.getRuntime()
				.availableProcessors());
		executorCompletionService = new ExecutorCompletionService<byte[]>(
				newFixedThreadPool);

	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {

		startWriterThread();
		WorkItem workItem = new WorkItem(Arrays.copyOf(b, len), off, len);
		count++;
		System.out.println(count);
		bytesRead += len;
		executorCompletionService.submit(workItem);
	}

	private void startWriterThread() {

		if (!writerThreadActive.get()) {
			writerThreadActive.set(true);

			writer = new Thread("Parallel Deflate writer thread") {
				@Override
				public void run() {
					try {

						int writeCount = 0;

						while (true) {

							Future<byte[]> poll = executorCompletionService
									.poll(200, TimeUnit.MILLISECONDS);

							if (poll != null) {
								byte[] buf = poll.get();
								bytesWritten += buf.length;
								out.write(buf);
								writeCount++;
								System.out
										.println("write count: " + writeCount);
							}

							if (!writerThreadActive.get()
									&& writeCount == count) {
								break;
							}
						}

						// Finish:
						// After writing a series of buffers, closing each one
						// with
						// Flush.Sync, we now write the final one as
						// Flush.Finish, and
						// then stop.
						byte[] buffer = new byte[128];
						ZStream compressor = new ZStream();
						int rc = compressor.deflateInit(
								JZlib.Z_DEFAULT_COMPRESSION, true);

						compressor.next_in = new byte[0];
						compressor.next_in_index = 0;
						compressor.avail_in = 0;

						compressor.next_out = buffer;
						compressor.next_out_index = 0;
						compressor.avail_out = buffer.length;

						rc = compressor.deflate(JZlib.Z_FINISH);

						if (rc != JZlib.Z_STREAM_END && rc != JZlib.Z_OK) {
							throw new ZStreamException("deflating: "
									+ compressor.msg);
						}

						if (buffer.length - compressor.avail_out > 0) {
							out.write(buffer, 0, buffer.length
									- compressor.avail_out);

							bytesWritten += (buffer.length - compressor.avail_out);
						}

						compressor.deflateEnd();

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			writer.start();

		}

	}

	@Override
	public void flush() throws IOException {
		finish();
		super.flush();

	}

	@Override
	public void close() throws IOException {
		super.close();
		newFixedThreadPool.shutdown();
	}

	public void reset() {
		// TODO Auto-generated method stub

	}

	public long getBytesWritten() {
		return bytesWritten;
	}

	public long getBytesRead() {
		return bytesRead;
	}

	@Override
	public void setLevel(int level) {
		// TODO Auto-generated method stub

	}

	@Override
	public void finish() throws IOException {
		writerThreadActive.set(false);

		while (writer.isAlive()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
