// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff.activation;

import org.znerd.yaff.io.HexStringInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.text.TextUtils;

/**
 * Implementation of an <code>XDataSource</code>, based on a string containing
 * only hex characters.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public final class HexStringDataSource implements XDataSource {

   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>HexStringDataSource</code>.
    *
    * @param name
    *    the name, cannot be <code>null</code>
    *    and cannot be an empty string.
    *
    * @param data
    *    the data, as a character string, cannot be <code>null</code>.
    *
    * @param contentType
    *    the MIME content type,
    *    cannot be <code>null</code> and cannot be an empty string.
    *
    * @throws IllegalArgumentException
    *    if <code>name                 == null
    *          || name.length()        == 0</code>.
    *          || data                 == null
    *          || contentType          == null
    *          || contentType.length() == 0</code>.
    */
   public HexStringDataSource(String name, String data, String contentType)
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
      _data         = data;
      _contentType  = contentType;
      _lastModified = System.currentTimeMillis();
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
   private final String _data;

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
      return new HexStringInputStream(_data);
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
      return _data.length() / 2;
   }

   public File getFile() {
      return null;
   }
}
