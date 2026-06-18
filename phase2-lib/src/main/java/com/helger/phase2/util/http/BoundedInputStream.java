/*
 * The FreeBSD Copyright
 * Copyright 1994-2008 The FreeBSD Project. All rights reserved.
 * Copyright (C) 2013-2025 Philip Helger philip[at]helger[dot]com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE FREEBSD PROJECT ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE FREEBSD PROJECT OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation
 * are those of the authors and should not be interpreted as representing
 * official policies, either expressed or implied, of the FreeBSD Project.
 */
package com.helger.phase2.util.http;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.WillCloseWhenClosed;
import com.helger.base.io.stream.WrappedInputStream;

/**
 * Stream to read a bounded number of bytes from an input stream based on Content-Length.
 * This allows streaming of large files without loading them entirely into memory.
 * The stream reads up to the specified number of bytes and then returns EOF.
 *
 * @author Philip Helger
 */
public class BoundedInputStream extends WrappedInputStream
{
  private static final Logger LOGGER = LoggerFactory.getLogger (BoundedInputStream.class);

  /**
   * Number of bytes left to read
   */
  private long m_nBytesRemaining;
  private final long m_nTotalBytes;

  /**
   * Creates a new BoundedInputStream that will read up to the specified number of bytes.
   *
   * @param aIS
   *        The underlying input stream to read from
   * @param nContentLength
   *        The maximum number of bytes to read (from Content-Length header)
   */
  public BoundedInputStream (@NonNull @WillCloseWhenClosed final InputStream aIS, final long nContentLength)
  {
    super (aIS);
    m_nBytesRemaining = nContentLength;
    m_nTotalBytes = nContentLength;

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Created BoundedInputStream for " + nContentLength + " bytes");
  }

  @Override
  public final int read () throws IOException
  {
    if (m_nBytesRemaining <= 0)
      return -1;

    final int nResult = super.read ();
    if (nResult >= 0)
      m_nBytesRemaining--;

    return nResult;
  }

  @Override
  public final int read (@NonNull final byte [] aBuf, final int nOffset, final int nLength) throws IOException
  {
    if (m_nBytesRemaining <= 0)
      return -1;

    // Read at most the remaining bytes
    final int nBytesToRead = (int) Math.min (nLength, m_nBytesRemaining);
    final int nBytesRead = super.read (aBuf, nOffset, nBytesToRead);

    if (nBytesRead > 0)
    {
      m_nBytesRemaining -= nBytesRead;

      if (LOGGER.isTraceEnabled ())
        LOGGER.trace ("Read " + nBytesRead + " bytes, " + m_nBytesRemaining + " remaining of " + m_nTotalBytes);
    }

    return nBytesRead;
  }

  @Override
  public final long skip (final long n) throws IOException
  {
    final long nBytesToSkip = Math.min (n, m_nBytesRemaining);
    final long nBytesSkipped = super.skip (nBytesToSkip);

    if (nBytesSkipped > 0)
      m_nBytesRemaining -= nBytesSkipped;

    return nBytesSkipped;
  }

  @Override
  public final int available () throws IOException
  {
    final int nAvailable = super.available ();
    return (int) Math.min (nAvailable, m_nBytesRemaining);
  }

  /**
   * @return The number of bytes remaining to be read
   */
  public long getBytesRemaining ()
  {
    return m_nBytesRemaining;
  }

  /**
   * @return The total number of bytes this stream was configured to read
   */
  public long getTotalBytes ()
  {
    return m_nTotalBytes;
  }
}
