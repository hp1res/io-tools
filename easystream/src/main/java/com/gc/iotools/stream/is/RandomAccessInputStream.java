package com.gc.iotools.stream.is;

/*
 * Copyright (c) 2008,2009 Davide Simonetti
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

import com.gc.iotools.stream.base.AbstractInputStreamWrapper;
import com.gc.iotools.stream.storage.MemoryStorage;
import com.gc.iotools.stream.storage.Storage;

/**
 * <p>
 * A <code>StorageBufInputStream</code> adds functionality to another input
 * stream namely the ability to be read multiple times and to support the
 * <code>{@link #mark()}</code> and <code>{@link #reset()}</code> methods.
 * </p>
 * <p>
 * It buffers the <code>source</code> stream into a {@linkplain Storage}. The
 * implementation can be changed to fit the application needs (cache on disk
 * rather than in memory).
 * </p>
 * <p>
 * It also adds the functionality of marking an <code>InputStream</code> without
 * specifying a mark length, thus allowing a <code>reset</code> after an
 * indefinite length of bytes has been read. Check {@link #mark() } for details.
 * </p>
 * 
 * @since 1.2
 */
public class RandomAccessInputStream extends AbstractInputStreamWrapper {
	private long sourcePosition = 0;
	private long resettableIsPosition = 0;
	private long markPosition = 0;
	private final Storage storage;

	public RandomAccessInputStream(final InputStream source) {
		this(source, 32768);
	}

	public RandomAccessInputStream(final InputStream source,
			final int threshold) {
		super(source);
		this.storage = new MemoryStorage();
	}

	public RandomAccessInputStream(final InputStream source,
			final Storage storage) {
		super(source);
		this.storage = storage;
	}

	@Override
	public int available() throws IOException {
		return (int) Math.min(Math.max(resettableIsPosition - sourcePosition,
				0)
				+ this.source.available(), Integer.MAX_VALUE);
	}

	@Override
	protected void closeOnce() throws IOException {
		this.storage.cleanup();
		this.source.close();
	}

	@Override
	public int innerRead(final byte[] b, final int off, final int len)
			throws IOException {
		int n;
		if (this.sourcePosition == this.resettableIsPosition) {
			// source and external same position so read from source.
			n = super.source.read(b, off, len);
			if (n > 0) {
				this.sourcePosition += n;
				this.resettableIsPosition += n;
				this.storage.put(b, off, n);
			}
		} else if (this.resettableIsPosition < this.sourcePosition) {
			// resetIS has been called. Read from buffer;n
			final int efflen = (int) Math.min(len, this.sourcePosition
					- this.resettableIsPosition);
			n = this.storage.get(b, off, efflen);
			if (n <= 0) {
				throw new IllegalStateException(
						"Problem reading from buffer. Expecting bytes ["
								+ efflen + "] but buffer is empty.");
			}
			this.resettableIsPosition += n;
		} else {
			/*
			 * resettableIsPosition > sourcePosition. A reset() was called on
			 * the StorageBufInputStream. just read from source don't buffer.
			 */
			final int efflen = (int) Math.min(len, this.resettableIsPosition
					- this.sourcePosition);
			n = this.source.read(b, off, efflen);
			this.sourcePosition += Math.max(n, 0);
		}
		return n;
	}

	@Override
	public synchronized void mark(final int readlimit) {
		super.source.mark(readlimit);
		this.markPosition = this.sourcePosition;
	}

	@Override
	public boolean markSupported() {
		return super.source.markSupported();
	}

	@Override
	public synchronized void reset() throws IOException {
		this.source.reset();
		this.sourcePosition = this.markPosition;
	}

	public void seek(long position) {
		this.resettableIsPosition = position;
		this.storage.seek(position);
	}
}
