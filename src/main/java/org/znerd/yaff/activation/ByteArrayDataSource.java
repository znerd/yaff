// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff.activation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.text.TextUtils;

/**
 * Implementation of an <code>XDataSource</code>, based on a byte
 * array.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public final class ByteArrayDataSource implements XDataSource {

   //-------------------------------------------------------------------------
   // Class functions
   //-------------------------------------------------------------------------

   /**
    * Produces a <code>ByteArrayDataSource</code> using the specified
    * <code>String</code> as the data contents.
    *
    * @param name
    *    the name, cannot be <code>null</code>
    *    and cannot be an empty string.
    *
    * @param data
    *    the data, as a {@link String}, cannot be <code>null</code>.
    *
    * @param contentType
    *    the MIME content type,
    *    cannot be <code>null</code>
    *    and cannot be an empty string.
    *
    * @throws IllegalArgumentException
    *    if <code>name                 == null
    *          || name.length()        == 0</code>.
    *          || data                 == null
    *          || contentType          == null
    *          || contentType.length() == 0</code>.
    */
   public static final ByteArrayDataSource fromString(String name,
                                                      String data,
                                                      String contentType)
   throws IllegalArgumentException {

      // Check preconditions
      MandatoryArgumentChecker.check("name",        name,
                                     "data",        data,
                                     "contentType", contentType);
      return new ByteArrayDataSource(name, TextUtils.toUTF8(data), contentType);
   }

   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>ByteArrayDataSource</code>.
    *
    * @param name
    *    the name, cannot be <code>null</code>
    *    and cannot be an empty string.
    *
    * @param data
    *    the data, as a byte array, cannot be <code>null</code>
    *    and must contain at least one byte.
    *
    * @param contentType
    *    the MIME content type,
    *    cannot be <code>null</code>
    *    and cannot be an empty string.
    *
    * @throws IllegalArgumentException
    *    if <code>name                 == null
    *          || name.length()        == 0</code>.
    *          || data                 == null
    *          || contentType          == null
    *          || contentType.length() == 0</code>.
    */
   public ByteArrayDataSource(String name, byte[] data, String contentType)
   throws IllegalArgumentException {

      // Check preconditions
      MandatoryArgumentChecker.check("name",        name,
                                     "data",        data,
                                     "contentType", contentType);
      if (name.length() < 1) {
         throw new IllegalArgumentException("name.length() == 0");
      } else if (contentType.length() < 1) {
         throw new IllegalArgumentException("contentType.length() == 0");
      }

      // Populate fields
      _name         = name;
      _data         = new byte[data.length];
      _contentType  = contentType;
      _lastModified = System.currentTimeMillis();

      System.arraycopy(data, 0, _data, 0, data.length);
   }


   //-------------------------------------------------------------------------
   // Fields
   //-------------------------------------------------------------------------

   /**
    * The name.
    * Never <code>null</code> and never an empty string.
    */
   private final String _name;

   /**
    * The contained data, as a byte array. Never <code>null</code> and never
    * writable by another class.
    */
   private final byte[] _data;

   /**
    * The MIME content type.
    * Never <code>null</code> and never an empty string.
    */
   private final String _contentType;

   /**
    * Timestamp indicating when the encapsulated data was last modified.
    */
   private final long _lastModified;


   //-------------------------------------------------------------------------
   // Methods
   //-------------------------------------------------------------------------

   public InputStream getInputStream() {
      return new ByteArrayInputStream(_data);
   }

   public OutputStream getOutputStream()
   throws IOException {
      throw new IOException("Operation 'getOutputStream' not supported.");
   }

   public String getContentType() {
      return _contentType;
   }

   public String getName() {
      return _name;
   }

   public long lastModified() {
      return _lastModified;
   }

   public int length() {
      return _data.length;
   }

   public File getFile() {
      return null;
   }
}
