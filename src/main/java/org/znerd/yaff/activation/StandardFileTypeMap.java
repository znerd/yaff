// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff.activation;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.activation.FileTypeMap;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.Utils;

/**
 * Implementation of a <code>FileTypeMap</code> that determines the content
 * type based on an extension.
 *
 * <p>This implementation is specific for Albizia and recognizes a limited set
 * of file extensions:
 *
 * <table>
 *    <thead>
 *       <tr><th>File extension</th><th>MIME content type</th></tr>
 *    </thead>
 *    <tbody>
 *       <tr><td>bin</td><td>application/octet-stream</td></tr>
 *       <tr><td>css</td><td>text/css</td></tr>
 *       <tr><td>csv</td><td>text/csv</td></tr>
 *       <tr><td>gif</td><td>image/gif</td></tr>
 *       <tr><td>htc</td><td>text/x-component</td></tr>
 *       <tr><td>html</td><td>text/html</td></tr>
 *       <tr><td>ico</td><td>image/x-icon</td></tr>
 *       <tr><td>jpeg</td><td>image/jpeg</td></tr>
 *       <tr><td>jpg</td><td>image/jpeg</td></tr>
 *       <tr><td>js</td><td>text/javascript</td></tr>
 *       <tr><td>less</td><td>text/x-lesscss</td></tr>
 *       <tr><td>pdf</td><td>application/pdf</td></tr>
 *       <tr><td>png</td><td>image/png</td></tr>
 *       <tr><td>txt</td><td>text/plain</td></tr>
 *       <tr><td>xml</td><td>text/xml</td></tr>
 *    </tbody>
 * </table>
 *
 * <p>If the MIME type of a file cannot be determined, then a message is
 * logged and <code>"application/octet-stream"</code> is returned.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public final class StandardFileTypeMap extends FileTypeMap {

   //-------------------------------------------------------------------------
   // Class fields
   //-------------------------------------------------------------------------

   /**
    * A singleton <code>StandardFileTypeMap</code> instance.
    * Never <code>null</code>.
    */
   public static final StandardFileTypeMap SINGLETON = new StandardFileTypeMap();


   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>StandardFileTypeMap</code>.
    */
   private StandardFileTypeMap() {
      _mappings = new HashMap<String,String>();
      _mappings.put("bin",  "application/octet-stream");
      _mappings.put("css",  "text/css"                );
      _mappings.put("csv",  "text/csv"                );
      _mappings.put("gif",  "image/gif"               );
      _mappings.put("htc",  "text/x-component"        );
      _mappings.put("html", "text/html"               );
      _mappings.put("ico",  "image/x-icon"            );
      _mappings.put("jpeg", "image/jpeg"              );
      _mappings.put("jpg",  "image/jpeg"              );
      _mappings.put("js",   "text/javascript"         );
      _mappings.put("less", "text/x-lesscss"          );
      _mappings.put("pdf",  "application/pdf"         );
      _mappings.put("png",  "image/png"               );
      _mappings.put("txt",  "text/plain"              );
      _mappings.put("xml",  "text/xml"                );
   }


   //-------------------------------------------------------------------------
   // Fields
   //-------------------------------------------------------------------------

   /**
    * All known mappings from file extension to content type.
    */
   private final Map<String,String> _mappings;


   //-------------------------------------------------------------------------
   // Methods
   //-------------------------------------------------------------------------

   /**
    * Returns the type of the specified <code>File</code> object.
    *
    * <p>This method always returns a valid MIME type.
    *
    * @param file
    *    the file to be typed, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>file == null</code>.
    */
   public String getContentType(File file)
   throws IllegalArgumentException {

      // Check preconditions
      MandatoryArgumentChecker.check("file", file);

      return getContentType(file.getName());
   }

   /**
    * Returns the type of the specified file name.
    *
    * <p>This method always returns a valid MIME type.
    *
    * @param fileName
    *    the name of the file to be typed,
    *    cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>fileName == null</code>.
    */
   public String getContentType(String fileName)
   throws IllegalArgumentException {

      // Check preconditions
      MandatoryArgumentChecker.check("fileName", fileName);

      // Determine the extension
      int index = fileName.lastIndexOf('.');
      if (index > 0) {
         String suffix = fileName.substring(index + 1);

         // If a mapping was found, return the content type found
         String type = _mappings.get(suffix);
         if (type != null) {
            return type;
         }
      }

      // Fallback default
      Utils.logWarning("StandardFileTypeMap: No MIME type found for file name \"" + fileName + "\".");
      return "application/octet-stream";
   }
}
