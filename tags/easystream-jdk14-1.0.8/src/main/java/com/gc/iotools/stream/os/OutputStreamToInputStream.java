package com.gc.iotools.stream.os;

/*
 * Copyright (c) 2008, Davide Simonetti
 * All rights reserved.
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided that the following 
 * conditions are met:
 *  * Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 *  * Neither the name of Davide Simonetti nor the names of its contributors may
 *    be used to endorse or promote products derived from this software without 
 *    specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY 
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gc.iotools.stream.base.ExecutionModel;
import com.gc.iotools.stream.base.ExecutorServiceFactory;

import EDU.oswego.cs.dl.util.concurrent.Callable;
import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.FutureResult;

/**
 * TODO: code example
 * 
 * @since 0ct 3, 2008
 * @author dvd.smnt
 * @version $Revision: 1 $
 */
public abstract class OutputStreamToInputStream extends OutputStream {

	private final class DataConsumerCallable implements Callable {

		private final InputStream inputstream;

		DataConsumerCallable(final String callerId, final InputStream istream) {
			this.inputstream = istream;
		}

		public Object call() throws Exception {
			Object processResult;
			try {
				processResult = doRead(this.inputstream);
			} finally {
				emptyInputStream();
			}
			return processResult;
		}

		/**
		 * 
		 */
		private void emptyInputStream() {
			boolean closed = false;
			try {
				while ((this.inputstream.read()) >= 0) {
					// empty block
				}
			} catch (final IOException e) {
				if ((e.getMessage() != null)
						&& (e.getMessage().indexOf("closed") > 0)) {
					OutputStreamToInputStream.LOG
							.debug("Stream already closed");
					closed = true;
				} else {
					OutputStreamToInputStream.LOG.error(
							"IOException while empty InputStream a "
									+ "thread can be locked", e);
				}
			} catch (final Throwable t) {
				OutputStreamToInputStream.LOG.error(
						"Error while empty InputStream a "
								+ "thread can be locked", t);
			}
			tryCloseIs(closed);
		}

		private void tryCloseIs(final boolean closed) {
			if (!closed) {
				try {
					this.inputstream.close();
				} catch (final Throwable e) {
					OutputStreamToInputStream.LOG.error(
							"Error closing Inputstream", e);
				}
			}
		}
	}

	private static final Log LOG = LogFactory
			.getLog(OutputStreamToInputStream.class);

	private boolean closeCalled = false;
	private final boolean joinOnClose;
	private final FutureResult writingResult;
	private final PipedOutputStream wrappedPipedOS;

	public OutputStreamToInputStream() throws IOException {
		this(true, ExecutionModel.THREAD_PER_INSTANCE);
	}

	public OutputStreamToInputStream(final boolean joinOnClose,
			final ExecutionModel em) throws IOException {
		this(joinOnClose, ExecutorServiceFactory.getExecutor(em));
	}

	public OutputStreamToInputStream(final boolean joinOnClose,
			final Executor executor) throws IOException {
		final String callerId = getCaller();
		this.wrappedPipedOS = new PipedOutputStream();
		final PipedInputStream pipedIS = new PipedInputStream(
				this.wrappedPipedOS);

		final DataConsumerCallable executingProcess = new DataConsumerCallable(
				callerId, pipedIS);
		this.joinOnClose = joinOnClose;
		this.writingResult = new FutureResult();
		try {
			executor.execute(this.writingResult.setter(executingProcess));
		} catch (final InterruptedException e) {
			final IllegalStateException e1 = new IllegalStateException(
					"Executor interrupted.s");
			e1.initCause(e);
			throw e1;
		}
	}

	public final void close() throws IOException {
		if (!this.closeCalled) {
			this.closeCalled = true;
			this.wrappedPipedOS.close();
			if (this.joinOnClose) {
				// waiting for thread to finish..
				try {
					this.writingResult.get();
				} catch (final InvocationTargetException e) {
					final IOException e1 = new IOException(
							"Problem producing data");
					e1.initCause(e.getCause());
					throw e1;
				} catch (final Exception e) {
					final IOException e1 = new IOException(
							"Problem producing data");
					e1.initCause(e);
					throw e1;
				}
			}
		}
	}

	public final void flush() throws IOException {
		this.wrappedPipedOS.flush();
	}

	public final Object getResults() throws InterruptedException,
			InvocationTargetException {
		if (!this.closeCalled) {
			throw new IllegalStateException("Method close() must be called"
					+ " before getResults");
		}
		return this.writingResult.get();
	}

	public final void write(final byte[] bytes) throws IOException {
		this.wrappedPipedOS.write(bytes);
	}

	public final void write(final byte[] bytes, final int offset,
			final int length) throws IOException {
		this.wrappedPipedOS.write(bytes, offset, length);
	}

	public final void write(final int bytetowr) throws IOException {
		this.wrappedPipedOS.write(bytetowr);
	}

	private String getCaller() {
		final Exception exception = new Exception();
		final StackTraceElement[] stes = exception.getStackTrace();
		final StackTraceElement caller = stes[3];
		final String result = getClass().getName().substring(
				getClass().getPackage().getName().length() + 1)
				+ "callBy:" + caller.toString();
		OutputStreamToInputStream.LOG.debug("Open [" + result + "]");
		return result;
	}

	protected abstract Object doRead(InputStream istream) throws Exception;

}
