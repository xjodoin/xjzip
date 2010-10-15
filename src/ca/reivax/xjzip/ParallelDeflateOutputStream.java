package ca.reivax.xjzip;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
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

	private static class WorkItem implements Callable<WorkItem> {

		private ZStream z = new ZStream();

		protected int bufsize = 1024;
		protected byte[] buf = new byte[bufsize];

		private ByteArrayOutputStream in = new ByteArrayOutputStream();
		private ByteArrayOutputStream out = new ByteArrayOutputStream();

		private int sequence;

		@Override
		public WorkItem call() throws Exception {

			byte[] byteArray = in.toByteArray();

			if (byteArray.length == 0)
				return this;

			z.deflateInit(JZlib.Z_DEFAULT_COMPRESSION, true);

			int err;
			z.next_in = byteArray;
			z.next_in_index = 0;
			z.avail_in = byteArray.length;

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

			return this;
		}

		public void reset() {
			in.reset();
			out.reset();
			z.deflateEnd();
		}
	}

	private ExecutorCompletionService<WorkItem> executorCompletionService;
	private ExecutorService executorService;

	private LinkedBlockingQueue<WorkItem> pool = new LinkedBlockingQueue<WorkItem>();

	private AtomicBoolean writerThreadActive = new AtomicBoolean();
	private int count = 0;
	private long bytesRead = 0;
	private long bytesWritten = 0;
	private Future<?> writeThread;

	public ParallelDeflateOutputStream(OutputStream out) {
		super(out);

		executorService = Executors.newCachedThreadPool();


		executorCompletionService = new ExecutorCompletionService<WorkItem>(
				executorService);

		for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
			pool.add(new WorkItem());
		}

	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {

		startWriterThread();

		WorkItem take;
		try {
			take = pool.take();
			take.sequence = count;
			ByteArrayOutputStream in = take.in;
			in.write(b, off, len);
			System.out.println(count);
			count++;
			bytesRead += len;
			executorCompletionService.submit(take);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private void startWriterThread() {

		if (writerThreadActive.compareAndSet(false, true)) {

			Runnable writer = new Runnable() {

				Map<Integer, WorkItem> waitingToWrite = new HashMap<Integer, ParallelDeflateOutputStream.WorkItem>();

				@Override
				public void run() {
					try {

						int writeCount = 0;

						while (true) {

							Future<WorkItem> poll = executorCompletionService
									.poll(100, TimeUnit.MILLISECONDS);

							if (poll != null) {
								WorkItem workItem = poll.get();

								if (workItem.sequence == writeCount) {
									do {
										writeCount = writeItem(writeCount,
												workItem);
									} while ((workItem = waitingToWrite
											.remove(writeCount)) != null);
								} else {
									waitingToWrite.put(workItem.sequence,
											workItem);
								}
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

				private int writeItem(int writeCount, WorkItem workItem)
						throws IOException {
					byte[] buf = workItem.out.toByteArray();

					bytesWritten += buf.length;
					out.write(buf);

					System.out.println("Sequence write " + workItem.sequence);
					System.out.println("Lenght compress " + buf.length);

					writeCount++;

					workItem.reset();
					pool.offer(workItem);

					return writeCount;
				}

			};

			writeThread = executorService.submit(writer);

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
		executorService.shutdown();
	}

	public void reset() {
		bytesRead = 0;
		bytesWritten = 0;
		count = 0;
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

		try {
			// wait the writter
			writeThread.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}

}
