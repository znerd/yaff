// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff.activation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.text.TextUtils;

/**
 * Implementation of a <code>DataSource</code> that provides access to a file
 * on the file system.
 *
 * <p>The {@link #getInputStream()} returns a {@link FileInputStream} for the 
 * encapsulated file.
 *
 * <p>If this data source is in read-only mode, then the
 * {@link #getOutputStream()} method, however, will throw an
 * {@link IOException}, because modification of the file is not permitted.
 *
 * <p>The {@link #getContentType()} method in this class uses the
 * {@link StandardFileTypeMap} to determine the content type for a file.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public final class FileDataSource
extends Object
implements XDataSource {

   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>FileDataSource</code> for the specified 
    * <code>File</code>.
    *
    * <p>The file will not be opened until {@link #getInputStream()} is 
    * called.
    *
    * @param file
    *    the file to create a <code>FileDataSource</code> for,
    *    cannot be <code>null</code>.
    *
    * @param mode
    *    the mode for opening the file, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>file == null || mode == null</code>.
    */
   public FileDataSource(File file, Mode mode)
   throws IllegalArgumentException {

      // Check preconditions
      MandatoryArgumentChecker.check("file", file, "mode", mode);

      // Populate fields
      _file = file;
      _mode = mode;
   }


   //-------------------------------------------------------------------------
   // Fields
   //-------------------------------------------------------------------------

   /**
    * The encapsulated abstract pathname. Never <code>null</code>.
    */
   private final File _file;

   /**
    * The file access mode. Never <code>null</code>.
    */
   private final Mode _mode;


   //-------------------------------------------------------------------------
   // Methods
   //-------------------------------------------------------------------------

   public InputStream getInputStream() throws IOException {
      return new FileInputStream(_file);
   }

   public OutputStream getOutputStream() throws IOException {
      if (_mode == Mode.READ_WRITE) {
         return new FileOutputStream(_file);
      } else {
         throw new IOException("Operation not supported.");
      }
   }

   public String getContentType() {
      return StandardFileTypeMap.SINGLETON.getContentType(_file);
   }

   public String getName() {
      return _file.getName();
   }

   public long lastModified() throws DataAccessException {

      long lastModified;
      try {
         lastModified = _file.lastModified();
      } catch (Exception cause) {
         throw new DataAccessException("Failed to determine last modified time of file " + TextUtils.quote(_file.getPath()) + '.', cause);
      }

      if (lastModified < 1L) {
         throw new DataAccessException("Failed to determine last modified time of file " + TextUtils.quote(_file.getPath()) + '.', null);
      }
      
      return lastModified;
   }

   public int length() throws DataAccessException {

      int length;
      try {
         // NOTE: This doesn't work for files > 2GB
         length = (int) _file.length();
      } catch (Exception cause) {
         throw new DataAccessException("Failed to determine length of file " + TextUtils.quote(_file.getPath()) + '.', cause);
      }

      if (length < 1 && !_file.exists()) {
         throw new DataAccessException("Failed to determine length of file " + TextUtils.quote(_file.getPath()) + '.', null);
      }
      
      return length;
   }

   public File getFile() {
      return _file;
   }


   //-------------------------------------------------------------------------
   // Inner classes
   //-------------------------------------------------------------------------

   /**
    * The file access mode, either read-only or read-write.
    *
    * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
    */
   public enum Mode {

      /**
       * Read-only, no file modifications supported.
       */
      READ_ONLY,

      /**
       * Read-write, file modifications are supported.
       */
      READ_WRITE;
   }
}
