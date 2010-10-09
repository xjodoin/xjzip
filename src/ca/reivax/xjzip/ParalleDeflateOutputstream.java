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

	private static class WorkItem implements Callable<WorkItem> {

		private ZStream z = new ZStream();

		private final byte[] buf;
		private final int off;
		private final int len;

		private final int sequence;

		public WorkItem(int sequence,byte b[], int off, int len) {
			this.sequence = sequence;
			this.buf = b;
			this.off = off;
			this.len = len;

		}

		@Override
		public WorkItem call() throws Exception {

			if (len == 0)
				return this;

			z.deflateInit(JZlib.Z_DEFAULT_COMPRESSION);
			z.next_in = buf;
			z.next_in_index = off;
			z.avail_in = len;

			z.next_out = new byte[buf.length];
			z.next_out_index = 0;
			z.avail_out = buf.length;

			int err;

			do {
				err = z.deflate(JZlib.Z_NO_FLUSH);
				if (err != JZlib.Z_OK) {
					throw new ZStreamException("deflating: " + z.msg);
				}
			} while (z.avail_in > 0 || z.avail_out == 0);

			err = z.deflate(JZlib.Z_SYNC_FLUSH);

//			return Arrays.copyOfRange(z.next_out, 0, buf.length - z.avail_out);

			return this;
		}
	}

	private ExecutorCompletionService<WorkItem> executorCompletionService;
	private boolean writerThreadActive;
	private Thread writer;
	private int count = 0;
	
	public ParalleDeflateOutputstream(OutputStream out) {
		super(out);

		executorCompletionService = new ExecutorCompletionService<WorkItem>(
				Executors.newSingleThreadExecutor(),new LinkedBlockingQueue<Future<WorkItem>>(10));

	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {

		startWriterThread();
		WorkItem workItem = new WorkItem(count,Arrays.copyOf(b, len), off, len);
		count++;
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
							
							Future<WorkItem> poll = executorCompletionService.poll(200, TimeUnit.MILLISECONDS);
							
							while(poll!=null)
							{
								WorkItem workItem = poll.get();
								System.out.println(workItem.sequence);
								out.write(workItem.z.next_out,0,(int) workItem.z.total_out);	
								poll = executorCompletionService.poll(200, TimeUnit.MILLISECONDS);
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
	                   
	                    compressor.next_in = new byte[0];
	                    compressor.next_in_index = 0;
	                    compressor.avail_in = 0;

	                    compressor.next_out = buffer;
	                    compressor.next_out_index = 0;
	                    compressor.avail_out = buffer.length;
	                    
	                    rc = compressor.deflate(JZlib.Z_FINISH);

//	                    rc = compressor.Deflate(FlushType.Finish);
//
//	                    if (rc != ZlibConstants.Z_STREAM_END && rc != ZlibConstants.Z_OK)
//	                        throw new Exception("deflating: " + compressor.Message);
	                    
	                    if (rc != JZlib.Z_STREAM_END && rc != JZlib.Z_OK) {
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
		
		while (writer.isAlive()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

}
