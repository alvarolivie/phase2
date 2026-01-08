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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.stream.StreamHelper;

/**
 * Test class for {@link BoundedInputStream}
 *
 * @author Philip Helger
 */
public final class BoundedInputStreamTest
{
  @Test
  public void testReadExactBytes () throws IOException
  {
    final byte [] aData = "Hello World".getBytes (StandardCharsets.UTF_8);
    try (final BoundedInputStream aIS = new BoundedInputStream (new NonBlockingByteArrayInputStream (aData), aData.length))
    {
      final byte [] aRead = StreamHelper.getAllBytes (aIS);
      assertArrayEquals (aData, aRead);
      assertEquals (0, aIS.getBytesRemaining ());
    }
  }

  @Test
  public void testReadFewerBytes () throws IOException
  {
    final byte [] aData = "Hello World".getBytes (StandardCharsets.UTF_8);
    final int nLimit = 5;
    try (final BoundedInputStream aIS = new BoundedInputStream (new NonBlockingByteArrayInputStream (aData), nLimit))
    {
      final byte [] aRead = StreamHelper.getAllBytes (aIS);
      assertEquals (nLimit, aRead.length);
      assertArrayEquals ("Hello".getBytes (StandardCharsets.UTF_8), aRead);
      assertEquals (0, aIS.getBytesRemaining ());
    }
  }

  @Test
  public void testReadMoreBytesThanAvailable () throws IOException
  {
    final byte [] aData = "Hello".getBytes (StandardCharsets.UTF_8);
    final int nLimit = 100;
    try (final BoundedInputStream aIS = new BoundedInputStream (new NonBlockingByteArrayInputStream (aData), nLimit))
    {
      final byte [] aRead = StreamHelper.getAllBytes (aIS);
      // Should only read what's available in the stream
      assertArrayEquals (aData, aRead);
      // Bytes remaining should reflect that we couldn't read all requested bytes
      assertEquals (nLimit - aData.length, aIS.getBytesRemaining ());
    }
  }

  @Test
  public void testReadZeroBytes () throws IOException
  {
    final byte [] aData = "Hello World".getBytes (StandardCharsets.UTF_8);
    try (final BoundedInputStream aIS = new BoundedInputStream (new NonBlockingByteArrayInputStream (aData), 0))
    {
      final byte [] aRead = StreamHelper.getAllBytes (aIS);
      assertEquals (0, aRead.length);
      assertEquals (0, aIS.getBytesRemaining ());
    }
  }

  @Test
  public void testReadSingleBytes () throws IOException
  {
    final byte [] aData = "ABC".getBytes (StandardCharsets.UTF_8);
    try (final BoundedInputStream aIS = new BoundedInputStream (new NonBlockingByteArrayInputStream (aData), 2))
    {
      assertEquals ('A', aIS.read ());
      assertEquals (1, aIS.getBytesRemaining ());
      assertEquals ('B', aIS.read ());
      assertEquals (0, aIS.getBytesRemaining ());
      assertEquals (-1, aIS.read ()); // EOF
    }
  }

  @Test
  public void testReadLargeContent () throws IOException
  {
    // Test with a larger payload to ensure no issues with buffer sizes
    final byte [] aData = new byte [10 * 1024 * 1024]; // 10 MB
    for (int i = 0; i < aData.length; i++)
      aData[i] = (byte) (i % 256);

    try (final BoundedInputStream aIS = new BoundedInputStream (new NonBlockingByteArrayInputStream (aData), aData.length))
    {
      final byte [] aRead = StreamHelper.getAllBytes (aIS);
      assertEquals (aData.length, aRead.length);
      assertArrayEquals (aData, aRead);
      assertEquals (0, aIS.getBytesRemaining ());
    }
  }

  @Test
  public void testSkip () throws IOException
  {
    final byte [] aData = "Hello World".getBytes (StandardCharsets.UTF_8);
    try (final BoundedInputStream aIS = new BoundedInputStream (new NonBlockingByteArrayInputStream (aData), aData.length))
    {
      final long nSkipped = aIS.skip (6);
      assertEquals (6, nSkipped);
      assertEquals (aData.length - 6, aIS.getBytesRemaining ());

      final byte [] aRead = StreamHelper.getAllBytes (aIS);
      assertArrayEquals ("World".getBytes (StandardCharsets.UTF_8), aRead);
    }
  }

  @Test
  public void testGetTotalBytes () throws IOException
  {
    final byte [] aData = "Hello World".getBytes (StandardCharsets.UTF_8);
    final int nLimit = 5;
    try (final BoundedInputStream aIS = new BoundedInputStream (new NonBlockingByteArrayInputStream (aData), nLimit))
    {
      assertEquals (nLimit, aIS.getTotalBytes ());
      aIS.read ();
      assertEquals (nLimit, aIS.getTotalBytes ()); // Total should not change
      assertEquals (nLimit - 1, aIS.getBytesRemaining ());
    }
  }
}
