package com.gc.iotools.stream.is;

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
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;

import com.gc.iotools.stream.utils.StreamUtils;

public class RandomAccessInputStreamTest {

	@Test
	public void testFullReadAndReset() throws IOException {
		final BigDocumentIstream bis = new BigDocumentIstream(131072);
		final byte[] reference = IOUtils.toByteArray(bis);
		bis.resetToBeginning();
		final RandomAccessInputStream ris = new RandomAccessInputStream(bis);
		final byte[] test = IOUtils.toByteArray(ris);
		assertArrayEquals("simple read", reference, test);
		ris.seek(0);
		final byte[] test2 = IOUtils.toByteArray(ris);
		assertArrayEquals("simple read", reference, test2);
	}

	@Test
	public void testRead() throws IOException {
		byte[] reference = new byte[] { 10, 11, 127, 0, -127, -1, -45 };

		final RandomAccessInputStream ris = new RandomAccessInputStream(
				new ByteArrayInputStream(reference));
		byte[] read = new byte[reference.length];
		for (int i = 0; i < read.length; i++) {
			read[i] = (byte) ris.read();
		}
		assertArrayEquals("simple read", reference, read);
		final int pos = reference.length - 1;
		ris.seek(pos);
		assertEquals("last byte", reference[pos], (byte) ris.read());
		assertEquals("eof", -1, ris.read());
	}

	@Test
	public void testMarkAndReset() throws IOException {
		final BigDocumentIstream bis = new BigDocumentIstream(131072);
		final byte[] reference = IOUtils.toByteArray(bis);
		bis.resetToBeginning();
		final RandomAccessInputStream ris = new RandomAccessInputStream(bis);
		ris.read(new byte[5]);
		ris.mark(150);
		final byte[] b = new byte[100];
		ris.read(b);
		assertArrayEquals("correct position after mark", ArrayUtils.subarray(
				reference, 5, 105), b);
		ris.reset();
		final byte[] bytes = StreamUtils.read(ris, 200);
		assertArrayEquals("correct position after reset", ArrayUtils
				.subarray(reference, 5, 205), bytes);
		ris.seek(0);
		final byte[] test2 = IOUtils.toByteArray(ris);
		assertArrayEquals("full read after resetToBeginning", reference,
				test2);
	}

	@Test
	public void testSeek() throws IOException {
		final BigDocumentIstream bis = new BigDocumentIstream(4096);
		final byte[] reference = IOUtils.toByteArray(bis);
		bis.resetToBeginning();
		final RandomAccessInputStream ris = new RandomAccessInputStream(bis,
				4096);
		ris.seek(50);
		final byte[] b = new byte[5];
		ris.read(b);
		assertArrayEquals("read correct position", ArrayUtils.subarray(
				reference, 50, 55), b);
		ris.seek(0);
		final byte[] test2 = IOUtils.toByteArray(ris);
		assertArrayEquals("skip and reset read", reference, test2);
		for (int i = 0; i < reference.length; i++) {
			byte r1 = reference[i];
			ris.seek(i);
			final int read = ris.read();
			assertEquals("byte at pos [" + i + "]", r1, (byte) read);
		}
		assertEquals("eof", -1, ris.read());
	}

	@Test
	public void testSeekEOF() throws IOException {
		final BigDocumentIstream bis = new BigDocumentIstream(4080);
		final byte[] reference = IOUtils.toByteArray(bis);
		bis.resetToBeginning();
		final RandomAccessInputStream ris = new RandomAccessInputStream(bis);
		// final byte[] reference1 = IOUtils.toByteArray(ris);
		ris.seek(5);
		// assertArrayEquals("letti", reference, reference1);
		final int pos = reference.length - 1;
		ris.skip(pos + 1);
		ris.seek(5);
		ris.seek(pos);
		assertEquals("last byte", reference[pos], (byte) ris
				.read());
	}

	@Test
	public void testSkipAndReset() throws IOException {
		final BigDocumentIstream bis = new BigDocumentIstream(131072);
		final byte[] reference = IOUtils.toByteArray(bis);
		bis.resetToBeginning();
		final RandomAccessInputStream ris = new RandomAccessInputStream(bis);
		final byte[] b = new byte[5];
		ris.read(b);
		ris.skip(32768);
		ris.read(b);
		assertArrayEquals("read correct position", ArrayUtils.subarray(
				reference, 32768 + 5, 32768 + 10), b);
		ris.seek(0);
		final byte[] test2 = IOUtils.toByteArray(ris);
		assertArrayEquals("skip and reset read", reference, test2);
	}
}
