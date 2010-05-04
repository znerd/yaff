// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff.activation;

/**
 * Exception thrown when a data access operation fails.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public class DataAccessException extends RuntimeException {

   /**
    * Constructs a new <code>DataAccessException</code> with the specified
    * detail message and cause exception. Both are optional.
    *
    * @param detail
    *    the detail message, or <code>null</code>.
    *
    * @param cause
    *    if cause exception, or <code>null</code>.
    */
   public DataAccessException(String detail, Throwable cause) {
      super(detail);
      if (cause != null) {
         initCause(cause);
      }
   }
}
