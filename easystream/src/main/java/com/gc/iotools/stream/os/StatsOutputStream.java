package com.gc.iotools.stream.os;

/*
 * Copyright (c) 2008,2009 Davide Simonetti.
 * This source code is released under the BSD Software License.
 */
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Gather some statistics on the <code>OutputStream</code> passed in the
 * constructor.
 * </p>
 * <p>
 * It can be used to read:
 * <ul>
 * <li>The size of the data written to the underlying stream.</li>
 * <li>The time spent writing the bytes.</li>
 * <li>The bandwidth of the underlying stream.</li>
 * </ul>
 * </p>
 * <p>
 * Full statistics are available after the stream has been fully processed (by
 * other parts of the application), or after invoking the method
 * {@linkplain #close()} while partial statistics are available on the fly.
 * </p>
 * 
 * @author dvd.smnt
 * @since 1.2.1
 */
public class StatsOutputStream extends OutputStream {

	private boolean closeCalled;
	private final OutputStream innerOs;
	private long size = 0;
	private long time = 0;

	/**
	 * Creates a new <code>SizeRecorderOutputStream</code> with the given
	 * destination stream.
	 * 
	 * @param destination
	 *            Destination stream where data are written.
	 */
	public StatsOutputStream(final OutputStream destination) {
		this.innerOs = destination;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		if (!this.closeCalled) {
			this.closeCalled = true;
			final long start = System.currentTimeMillis();
			this.innerOs.close();
			this.time += System.currentTimeMillis() - start;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flush() throws IOException {
		final long start = System.currentTimeMillis();
		this.innerOs.flush();
		this.time += System.currentTimeMillis() - start;
	}

	/**
	 * Returns the number of bytes written until now.
	 * 
	 * @return return the number of bytes written until now.
	 */
	public long getSize() {
		return this.size;
	}

	public long getTime(final TimeUnit tu) {
		return tu.convert(this.time, TimeUnit.MILLISECONDS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(final byte[] b) throws IOException {
		final long start = System.currentTimeMillis();
		this.innerOs.write(b);
		this.time += System.currentTimeMillis() - start;
		this.size += b.length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(final byte[] b, final int off, final int len)
			throws IOException {
		final long start = System.currentTimeMillis();
		this.innerOs.write(b, off, len);
		this.time += System.currentTimeMillis() - start;
		this.size += len;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(final int b) throws IOException {
		final long start = System.currentTimeMillis();
		this.innerOs.write(b);
		this.time += System.currentTimeMillis() - start;
		this.size++;
	}

}
