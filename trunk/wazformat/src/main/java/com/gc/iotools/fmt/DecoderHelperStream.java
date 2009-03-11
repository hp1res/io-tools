package com.gc.iotools.fmt;

/*
 * Copyright (c) 2008, 2009 Davide Simonetti.
 * This source code is released under the BSD Software License.
 */
import java.io.IOException;
import java.io.InputStream;

import com.gc.iotools.fmt.base.Decoder;

/**
 * Helps in mark and reset of decoded streams.
 * 
 * Mark and reset are done on baseStream. Reads are done on decodedStream.
 * 
 * @since 1.2.0
 * @author dvd.smnt
 * @see Decoder
 */
class DecoderHelperStream extends InputStream {

	private final InputStream baseStream;

	private InputStream decodedStream;

	private final Decoder decoder;

	public DecoderHelperStream(final InputStream originalStream,
			final Decoder decoder) {
		this.baseStream = originalStream;
		// this.decodedStream = decodedStream;
		this.decoder = decoder;
	}

	@Override
	public void mark(final int readlimit) {
		final int markLimit = (int) (readlimit * this.decoder.getRatio())
				+ this.decoder.getOffset()
				+ 1;
		this.baseStream.mark(markLimit);
	}

	@Override
	public boolean markSupported() {
		return this.baseStream.markSupported();
	}

	@Override
	public int read() throws IOException {
		checkInitialized();
		return this.decodedStream.read();
	}

	@Override
	public int read(final byte[] b) throws IOException {
		checkInitialized();
		return this.decodedStream.read(b);
	}

	@Override
	public int read(final byte[] b, final int off, final int len)
			throws IOException {
		checkInitialized();
		return this.decodedStream.read(b, off, len);
	}

	@Override
	public void reset() throws IOException {
		this.baseStream.reset();
		this.decodedStream = null;
	}

	private void checkInitialized() throws IOException {
		this.decodedStream = decoder.decode(baseStream);
	}

}
