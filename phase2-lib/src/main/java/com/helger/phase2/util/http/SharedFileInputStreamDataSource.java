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
import java.io.OutputStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.mail.datasource.IExtendedDataSource;

import jakarta.mail.util.SharedFileInputStream;

/**
 * A DataSource implementation that wraps a {@link SharedFileInputStream} (including
 * {@link TempSharedFileInputStream}) and properly handles multiple reads by creating
 * a new stream for each {@link #getInputStream()} call.
 * <p>
 * This is necessary because during AS2 message processing, the content needs to be
 * read multiple times:
 * <ul>
 * <li>First for MIC (Message Integrity Check) calculation</li>
 * <li>Then for decryption</li>
 * <li>Possibly for signature verification</li>
 * </ul>
 * <p>
 * The {@link SharedFileInputStream#newStream(long, long)} method is used to create
 * independent streams that read from the same underlying file without interfering
 * with each other.
 */
public class SharedFileInputStreamDataSource implements IExtendedDataSource
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SharedFileInputStreamDataSource.class);

  private final SharedFileInputStream m_aSharedFileIS;
  private final String m_sName;
  private final String m_sContentType;
  private final boolean m_bReadMultiple;

  /**
   * Creates a new DataSource for a SharedFileInputStream.
   *
   * @param aSharedFileIS
   *        The SharedFileInputStream to wrap. Must not be null.
   * @param sName
   *        The name for this data source. Must not be null.
   * @param sContentType
   *        The content type. Must not be null.
   * @param bReadMultiple
   *        If true, indicates this stream will be read multiple times
   */
  public SharedFileInputStreamDataSource (@NonNull final SharedFileInputStream aSharedFileIS,
                                          @NonNull final String sName,
                                          @NonNull final String sContentType,
                                          final boolean bReadMultiple)
  {
    m_aSharedFileIS = ValueEnforcer.notNull (aSharedFileIS, "SharedFileIS");
    m_sName = ValueEnforcer.notNull (sName, "Name");
    m_sContentType = ValueEnforcer.notNull (sContentType, "ContentType");
    m_bReadMultiple = bReadMultiple;

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Created SharedFileInputStreamDataSource for '" +
                    sName +
                    "' with content-type '" +
                    sContentType +
                    "'" +
                    (bReadMultiple ? " (multi-read enabled)" : ""));
  }

  /**
   * Returns a new independent InputStream that reads from the beginning of the shared file.
   * Each call to this method creates a new stream that can be read independently.
   *
   * @return A new InputStream reading from position 0. Never null.
   * @throws IOException
   *         If an I/O error occurs
   */
  @Override
  @NonNull
  public InputStream getInputStream () throws IOException
  {
    // Create a new independent stream from the shared file
    // newStream(0, -1) creates a stream reading from position 0 to EOF
    // Return SharedFileInputStream directly so MimeMultipart.parse() can detect it
    // as a SharedInputStream and avoid loading entire content into ByteArrayOutputStream
    // SharedFileInputStream is already internally buffered, no need to wrap it
    final InputStream ret = m_aSharedFileIS.newStream (0, -1);

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Created new shared stream for '" + m_sName + "' (already buffered, supports mark/reset)");

    return ret;
  }

  @Override
  @NonNull
  public OutputStream getOutputStream () throws IOException
  {
    throw new UnsupportedOperationException ("SharedFileInputStreamDataSource is read-only");
  }

  @Override
  @NonNull
  public String getContentType ()
  {
    return m_sContentType;
  }

  @Override
  @NonNull
  public String getName ()
  {
    return m_sName;
  }

  @Nullable
  public String getCharset ()
  {
    // Content-Type might contain charset, but we return null here
    // as the charset should be parsed from getContentType() if needed
    return null;
  }

  @Nullable
  public String getContentTransferEncoding ()
  {
    // Not applicable for this data source
    return null;
  }

  /**
   * @return The underlying SharedFileInputStream
   */
  @NonNull
  public SharedFileInputStream getSharedFileInputStream ()
  {
    return m_aSharedFileIS;
  }

  /**
   * @return true if this data source is configured for multiple reads
   */
  public boolean isReadMultiple ()
  {
    return m_bReadMultiple;
  }
}
