// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff.io;

import static org.znerd.yaff.io.IOHelper.newIOException;

import java.io.InputStream;
import java.io.IOException;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.text.HexConverter;

/**
 * Input stream that reads from a character string containing hex characters
 * only. Two hex characters represent one byte.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public final class HexStringInputStream extends InputStream {

   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>HexStringInputStream</code>.
    *
    * @param s
    *    the character string to read from, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null || s.length() % 2 != 0</code>.
    */
   public HexStringInputStream(String s) {

      // Check preconditions
      MandatoryArgumentChecker.check("s", s);
      if (s.length() % 2 != 0) {
         throw new IllegalArgumentException("s.length() (" + s.length() + ") is not divisible by 2.");
      }

      // Initialize fields
      _string    = s;
      _byteIndex = 0;
   }


   //-------------------------------------------------------------------------
   // Fields
   //-------------------------------------------------------------------------

   /**
    * The character string to read from. Never <code>null</code>.
    */
   private final String _string;

   /**
    * The index, as a number of bytes. Double this number to get the index
    * into the string. Initially <code>0</code>.
    */
   private int _byteIndex;


   //-------------------------------------------------------------------------
   // Methods
   //-------------------------------------------------------------------------

   @Override
   public int read() throws IOException {
      int charIndex = _byteIndex * 2;

      int retValue;
      if (charIndex >= _string.length()) {
         retValue = -1;
      } else try {
         retValue = HexConverter.parseHexByte(_string, charIndex);
         _byteIndex++;
      } catch (NumberFormatException cause) {
         throw newIOException("Substring \"" + _string.charAt(_byteIndex) + _string.charAt(_byteIndex + 1) + "\" (at index " + charIndex + ") to a hex string.", cause);
      }

      return retValue;
   }
}
