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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Test;

import com.helger.base.io.stream.StreamHelper;

import jakarta.mail.util.SharedFileInputStream;

/**
 * Test class for {@link SharedFileInputStreamDataSource}
 *
 * @author Philip Helger
 */
public final class SharedFileInputStreamDataSourceTest
{
  private File m_aTempFile;

  @After
  public void cleanup ()
  {
    if (m_aTempFile != null && m_aTempFile.exists ())
    {
      m_aTempFile.delete ();
    }
  }

  private File _createTempFile (final String sContent) throws IOException
  {
    m_aTempFile = File.createTempFile ("test-", ".dat");
    try (final FileOutputStream fos = new FileOutputStream (m_aTempFile))
    {
      fos.write (sContent.getBytes (StandardCharsets.UTF_8));
    }
    return m_aTempFile;
  }

  @Test
  public void testCreateWithValidParameters () throws IOException
  {
    final File aFile = _createTempFile ("Test content");
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      final SharedFileInputStreamDataSource ds = new SharedFileInputStreamDataSource (sfis,
                                                                                       "test.txt",
                                                                                       "text/plain",
                                                                                       true);
      assertNotNull (ds);
      assertEquals ("test.txt", ds.getName ());
      assertEquals ("text/plain", ds.getContentType ());
      assertTrue (ds.isReadMultiple ());
      assertSame (sfis, ds.getSharedFileInputStream ());
    }
  }

  @Test
  public void testCreateWithReadMultipleFalse () throws IOException
  {
    final File aFile = _createTempFile ("Test content");
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      final SharedFileInputStreamDataSource ds = new SharedFileInputStreamDataSource (sfis,
                                                                                       "test.txt",
                                                                                       "text/plain",
                                                                                       false);
      assertNotNull (ds);
      assertFalse (ds.isReadMultiple ());
    }
  }

  @Test (expected = NullPointerException.class)
  public void testCreateWithNullSharedFileInputStream () throws IOException
  {
    new SharedFileInputStreamDataSource (null, "test.txt", "text/plain", true);
  }

  @Test (expected = NullPointerException.class)
  public void testCreateWithNullName () throws IOException
  {
    final File aFile = _createTempFile ("Test content");
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      new SharedFileInputStreamDataSource (sfis, null, "text/plain", true);
    }
  }

  @Test (expected = NullPointerException.class)
  public void testCreateWithNullContentType () throws IOException
  {
    final File aFile = _createTempFile ("Test content");
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      new SharedFileInputStreamDataSource (sfis, "test.txt", null, true);
    }
  }

  @Test
  public void testGetInputStreamMultipleTimes () throws IOException
  {
    final String sContent = "Hello World";
    final File aFile = _createTempFile (sContent);
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      final SharedFileInputStreamDataSource ds = new SharedFileInputStreamDataSource (sfis,
                                                                                       "test.txt",
                                                                                       "text/plain",
                                                                                       true);

      // Read the stream multiple times - each should return the same content
      for (int i = 0; i < 3; i++)
      {
        try (final InputStream is = ds.getInputStream ())
        {
          final byte [] aRead = StreamHelper.getAllBytes (is);
          assertArrayEquals ("Iteration " + i, sContent.getBytes (StandardCharsets.UTF_8), aRead);
        }
      }
    }
  }

  @Test
  public void testGetInputStreamReturnsBufferedStream () throws IOException
  {
    final File aFile = _createTempFile ("Test content");
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      final SharedFileInputStreamDataSource ds = new SharedFileInputStreamDataSource (sfis,
                                                                                       "test.txt",
                                                                                       "text/plain",
                                                                                       true);

      try (final InputStream is = ds.getInputStream ())
      {
        assertNotNull (is);
        // Verify that the stream supports mark/reset (BufferedInputStream feature)
        assertTrue ("Stream should support mark", is.markSupported ());
      }
    }
  }

  @Test
  public void testGetInputStreamIndependentReads () throws IOException
  {
    final String sContent = "0123456789";
    final File aFile = _createTempFile (sContent);
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      final SharedFileInputStreamDataSource ds = new SharedFileInputStreamDataSource (sfis,
                                                                                       "test.txt",
                                                                                       "text/plain",
                                                                                       true);

      // Open two streams and read them independently
      try (final InputStream is1 = ds.getInputStream (); final InputStream is2 = ds.getInputStream ())
      {
        // Read first 5 bytes from is1
        final byte [] buf1 = new byte [5];
        assertEquals (5, is1.read (buf1));
        assertArrayEquals ("01234".getBytes (StandardCharsets.UTF_8), buf1);

        // Read all bytes from is2
        final byte [] buf2 = StreamHelper.getAllBytes (is2);
        assertArrayEquals (sContent.getBytes (StandardCharsets.UTF_8), buf2);

        // Continue reading from is1
        final byte [] buf3 = StreamHelper.getAllBytes (is1);
        assertArrayEquals ("56789".getBytes (StandardCharsets.UTF_8), buf3);
      }
    }
  }

  @Test
  public void testGetOutputStreamThrowsException () throws IOException
  {
    final File aFile = _createTempFile ("Test content");
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      final SharedFileInputStreamDataSource ds = new SharedFileInputStreamDataSource (sfis,
                                                                                       "test.txt",
                                                                                       "text/plain",
                                                                                       true);

      try
      {
        ds.getOutputStream ();
        fail ("Expected UnsupportedOperationException");
      }
      catch (final UnsupportedOperationException e)
      {
        assertEquals ("SharedFileInputStreamDataSource is read-only", e.getMessage ());
      }
    }
  }

  @Test
  public void testGetContentType () throws IOException
  {
    final File aFile = _createTempFile ("Test content");
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      final SharedFileInputStreamDataSource ds = new SharedFileInputStreamDataSource (sfis,
                                                                                       "test.txt",
                                                                                       "application/pkcs7-mime",
                                                                                       true);
      assertEquals ("application/pkcs7-mime", ds.getContentType ());
    }
  }

  @Test
  public void testGetName () throws IOException
  {
    final File aFile = _createTempFile ("Test content");
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      final SharedFileInputStreamDataSource ds = new SharedFileInputStreamDataSource (sfis,
                                                                                       "message.dat",
                                                                                       "text/plain",
                                                                                       true);
      assertEquals ("message.dat", ds.getName ());
    }
  }

  @Test
  public void testGetCharsetReturnsNull () throws IOException
  {
    final File aFile = _createTempFile ("Test content");
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      final SharedFileInputStreamDataSource ds = new SharedFileInputStreamDataSource (sfis,
                                                                                       "test.txt",
                                                                                       "text/plain; charset=UTF-8",
                                                                                       true);
      // getCharset should return null as per implementation
      assertNull (ds.getCharset ());
    }
  }

  @Test
  public void testGetContentTransferEncodingReturnsNull () throws IOException
  {
    final File aFile = _createTempFile ("Test content");
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      final SharedFileInputStreamDataSource ds = new SharedFileInputStreamDataSource (sfis,
                                                                                       "test.txt",
                                                                                       "text/plain",
                                                                                       true);
      // getContentTransferEncoding should return null as per implementation
      assertNull (ds.getContentTransferEncoding ());
    }
  }

  @Test
  public void testLargeFileMultipleReads () throws IOException
  {
    // Test with a larger file to ensure buffering works correctly
    final StringBuilder sb = new StringBuilder ();
    for (int i = 0; i < 10000; i++)
      sb.append ("Line ").append (i).append ("\n");

    final String sContent = sb.toString ();
    final File aFile = _createTempFile (sContent);
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      final SharedFileInputStreamDataSource ds = new SharedFileInputStreamDataSource (sfis,
                                                                                       "large.txt",
                                                                                       "text/plain",
                                                                                       true);

      // Read multiple times
      for (int i = 0; i < 2; i++)
      {
        try (final InputStream is = ds.getInputStream ())
        {
          final byte [] aRead = StreamHelper.getAllBytes (is);
          assertEquals ("Iteration " + i, sContent.length (), aRead.length);
        }
      }
    }
  }

  @Test
  public void testBufferedStreamMarkAndReset () throws IOException
  {
    final String sContent = "Hello World";
    final File aFile = _createTempFile (sContent);
    try (final SharedFileInputStream sfis = new SharedFileInputStream (aFile))
    {
      final SharedFileInputStreamDataSource ds = new SharedFileInputStreamDataSource (sfis,
                                                                                       "test.txt",
                                                                                       "text/plain",
                                                                                       true);

      try (final InputStream is = ds.getInputStream ())
      {
        assertTrue (is.markSupported ());

        // Read first 5 bytes
        final byte [] buf1 = new byte [5];
        assertEquals (5, is.read (buf1));
        assertArrayEquals ("Hello".getBytes (StandardCharsets.UTF_8), buf1);

        // Mark current position
        is.mark (100);

        // Read next 6 bytes
        final byte [] buf2 = new byte [6];
        assertEquals (6, is.read (buf2));
        assertArrayEquals (" World".getBytes (StandardCharsets.UTF_8), buf2);

        // Reset to mark
        is.reset ();

        // Read again from mark position
        final byte [] buf3 = new byte [6];
        assertEquals (6, is.read (buf3));
        assertArrayEquals (" World".getBytes (StandardCharsets.UTF_8), buf3);
      }
    }
  }
}
