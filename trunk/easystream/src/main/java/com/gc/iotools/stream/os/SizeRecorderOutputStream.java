package com.gc.iotools.stream.os;

/*
 * Copyright (c) 2008,2009 Davide Simonetti.
 * This source code is released under the BSD Software License.
 */
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>
 * This class counts the number of bytes written to the
 * <code>OutputStream</code> passed in the constructor.
 * </p>
 * 
 * TODO: junits
 * @deprecated
 * @see StatsOutputStream
 * @author dvd.smnt
 * @since 1.0.6
 */
public class SizeRecorderOutputStream extends OutputStream {

	private boolean closeCalled;
	private final OutputStream innerOs;
	private long size = 0;

	/**
	 * Creates a new <code>SizeRecorderOutputStream</code> with the given
	 * destination stream.
	 * 
	 * @param destination
	 *            Destination stream where data are written.
	 */
	public SizeRecorderOutputStream(final OutputStream destination) {
		this.innerOs = destination;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() throws IOException {
		if (!this.closeCalled) {
			this.closeCalled = true;
			this.innerOs.close();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flush() throws IOException {
		this.innerOs.flush();
	}

	/**
	 * Returns the number of bytes written until now.
	 * 
	 * @return return the number of bytes written until now.
	 */
	public long getSize() {
		return this.size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(final byte[] b) throws IOException {
		this.innerOs.write(b);
		this.size += b.length;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(final byte[] b, final int off, final int len)
			throws IOException {
		this.innerOs.write(b, off, len);
		this.size += len;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(final int b) throws IOException {
		this.innerOs.write(b);
		this.size++;
	}

}
