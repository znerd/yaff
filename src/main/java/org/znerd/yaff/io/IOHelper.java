// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff.io;

import java.io.File;
import java.io.IOException;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.text.TextUtils;

/**
 * I/O-related utility functions.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public final class IOHelper extends Object {

   //-------------------------------------------------------------------------
   // Class functions
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>IOException</code> with the specified cause
    * exception.
    *
    * @param message
    *    the detail message, can be <code>null</code>.
    *
    * @param cause
    *    the cause exception, can be <code>null</code>.
    *
    * @return
    *    a correctly initialized {@link IOException}, never <code>null</code>.
    */
   public static final IOException newIOException(String message, Throwable cause) {
      IOException e = new IOException(message);
      if (cause != null) {
         e.initCause(cause);
      }
      return e;
   }

   /**
    * Checks if the specified directory exists and creates it if not,
    * including all ancestor directory. This is equivalent to the
    * <code>mkdir -p</code> command on UNIX systems.
    *
    * <p>If the specified path exists, then this method checks to make sure
    * that the path actually points to a directory; if not an exception is
    * thrown.
    *
    * <p>The directory is checked to make sure it is readable and/or writable
    * if that is required; this is configured using the
    * <code>mustBeReadable</code> and <code>mustBeWritable</code> arguments.
    * If any of these conditions fails, then an exception is thrown.
    *
    * @param dir
    *    the abstract path name for directory that should exist,
    *    cannot be <code>null</code>.
    *
    * @param mustBeReadable
    *    flag that indicates if the directory must be readable.
    *
    * @param mustBeWritable
    *    flag that indicates if the directory must be writeable.
    *
    * @throws IllegalArgumentException
    *    if <code>dir == null</code>.
    *
    * @throws IOException
    *    if the directory cannot be created, is not readable although it
    *    should be or is not writable while it should be.
    */
   public static final void mkdirs(File    dir,
                                   boolean mustBeReadable,
                                   boolean mustBeWritable)
   throws IllegalArgumentException,
          IOException {

      // Check preconditions
      MandatoryArgumentChecker.check("dir", dir);

      String absPath = TextUtils.quote(dir.getAbsolutePath());

      // Directory does not exist; create the directory and all ancestors
      if (! dir.exists()) {
         boolean ok = dir.mkdirs();
         if (! ok) {
            throw new IOException("Failed to create directory " + absPath + '.');
         }
      }

      // Fail if it exists but is not a directory
      if (! dir.isDirectory()) {
         throw new IOException("Path " + absPath + " is not a directory.");

      // Fail if it is not readable, while it must be
      } else if (mustBeReadable && dir.canRead() == false) {
         throw new IOException("Directory " + absPath + " is not readable.");

      // Fail if it is not writable, while it must be
      } else if (mustBeWritable && dir.canWrite() == false) {
         throw new IOException("Directory " + absPath + " is not writable.");
      }
   }


   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>IOHelper</code>.
    */
   private IOHelper() {
      // empty
   }
}
