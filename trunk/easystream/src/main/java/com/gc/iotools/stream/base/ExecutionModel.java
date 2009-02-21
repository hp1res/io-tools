package com.gc.iotools.stream.base;

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

/**
 * <p>
 * This class enumerates the policies for instantiating <code>Threads</code> in
 * classes of EasyStream library that needs of them.
 * </p>
 * 
 * @author dvd.smnt
 * @since 1.0
 * @see #com.gc.iotools.stream.is.InputStreamFromOutputStream(ExecutionModel em)
 * @see #com.gc.iotools.stream.os.OutputStreamToInputStream(ExecutionModel em)
 */

public enum ExecutionModel {
	/**
	 * <p>
	 * Threads are taken from a static pool.
	 * </p>
	 * <p>
	 * Some slow thread might lock up the pool and other processes might be
	 * slowed down.
	 * </p>
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor
	 */

	STATIC_THREAD_POOL,
	/**
	 * <p>
	 * One thread per instance of class. Slow but each instance can work in
	 * isolation. Also if some thread is not correctly closed there might be
	 * threads leaks.
	 * </p>
	 */
	THREAD_PER_INSTANCE,
	/**
	 * <p>
	 * Only one thread is shared by all instances (slow).
	 * </p>
	 */
	SINGLE_THREAD
}
