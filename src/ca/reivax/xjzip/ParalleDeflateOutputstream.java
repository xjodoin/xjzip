package ca.reivax.xjzip;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;
import com.jcraft.jzlib.ZStreamException;

public class ParalleDeflateOutputstream extends FilterOutputStream {

	private static class WorkItem implements Callable<byte[]> {

		private ZStream z = new ZStream();

		private final byte[] b;
		private final int off;
		private final int len;

		public WorkItem(byte b[], int off, int len) {
			this.b = b;
			this.off = off;
			this.len = len;

		}

		@Override
		public byte[] call() throws Exception {

			if (len == 0)
				return new byte[0];

			z.next_in = b;
			z.next_in_index = off;
			z.avail_in = len;

			z.next_out = new byte[b.length];
			z.next_out_index = 0;
			z.avail_out = b.length;

			int err;

			do {
				err = z.deflate(JZlib.Z_NO_FLUSH);
				if (err != JZlib.Z_OK) {
					throw new ZStreamException("deflating: " + z.msg);
				}
			} while (z.avail_in > 0 || z.avail_out == 0);

			err = z.deflate(JZlib.Z_SYNC_FLUSH);

			return Arrays.copyOfRange(z.next_out, 0, (int) z.total_out);

		}
	}

	private ExecutorCompletionService<byte[]> executorCompletionService;
	private boolean writerThreadActive;
	private Thread writer;
	
	public ParalleDeflateOutputstream(OutputStream out) {
		super(out);

		executorCompletionService = new ExecutorCompletionService<byte[]>(
				Executors.newSingleThreadExecutor(),new LinkedBlockingQueue<Future<byte[]>>(10));

	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {

		startWriterThread();
		
		WorkItem workItem = new WorkItem(b, off, len);
		executorCompletionService.submit(workItem);
	}

	private void startWriterThread() {

		if(!writerThreadActive)
		{
			writerThreadActive = true;
			writer = new Thread()
			{
				@Override
				public void run() 
				{
					try {
					
						while (writerThreadActive) {
							
							Future<byte[]> poll = executorCompletionService.poll(200, TimeUnit.MILLISECONDS);
							
							if(poll!=null)
							{
								out.write(poll.get());	
							}
							
						}
						
						 // Finish:
	                    // After writing a series of buffers, closing each one with
	                    // Flush.Sync, we now write the final one as Flush.Finish, and
	                    // then stop.
	                    byte[] buffer = new byte[128];
	                    ZStream compressor = new ZStream();
	                    int rc = compressor.deflateInit(JZlib.Z_DEFAULT_COMPRESSION, false);
//	                    compressor.InputBuffer = null;
//	                    compressor.NextIn = 0;
//	                    compressor.AvailableBytesIn = 0;
//	                    compressor.OutputBuffer = buffer;
//	                    compressor.NextOut = 0;
//	                    compressor.AvailableBytesOut = buffer.Length;
	                   
	                    compressor.next_in_index = 0;
	                    compressor.avail_in = 0;

	                    compressor.next_out = buffer;
	                    compressor.next_out_index = 0;
	                    compressor.avail_out = buffer.length;
	                    
	                    rc = compressor.deflate(JZlib.Z_FINISH);

	                    if (rc != JZlib.Z_OK) {
	    					throw new ZStreamException("deflating: " + compressor.msg);
	    				}
	                    
	                    if (buffer.length - compressor.avail_out > 0)
	                    {
	                        out.write(buffer, 0, buffer.length - compressor.avail_out);
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
		super.flush();
		writerThreadActive = false;
	}

}
