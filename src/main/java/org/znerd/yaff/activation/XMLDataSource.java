// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff.activation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.apache.commons.io.IOUtils;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.text.TextUtils;
import org.xins.common.text.ParseException;
import org.xins.common.xml.Element;
import org.xins.common.xml.ElementParser;

/**
 * Implementation of a <code>DataSource</code> for parsed XML content.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public final class XMLDataSource
extends Object
implements XDataSource {

   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>XMLDataSource</code> from an existing
    * <code>DataSource</code> object.
    *
    * @param dataSource
    *    the {@link DataSource}, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>dataSource == null</code>.
    *
    * @throws IOException
    *    if there was an I/O error while reading from the input stream of the
    *    data source.
    *
    * @throws ParseException
    *    if there was a parsing error while parsing the content as XML.
    */
   public XMLDataSource(DataSource dataSource)
   throws IllegalArgumentException, IOException, ParseException {

      // Check preconditions
      MandatoryArgumentChecker.check("dataSource", dataSource);

      // The DataSource could be an XDataSource instance
      XDataSource xds = (dataSource instanceof XDataSource)
                      ? (XDataSource) dataSource
                      : null;

      // Populate fields
      _file         = (xds != null) ? xds.getFile() : null;
      _name         = dataSource.getName();
      _lastModified = (xds != null) ? xds.lastModified() : System.currentTimeMillis();
      _xml          = new ElementParser().parse(dataSource.getInputStream());
   }

   /**
    * Constructs a new <code>XMLDataSource</code> using the specified
    * parameters.
    *
    * <p>If <code>name == null &amp;&amp; file != null</code>, then
    * <code>file.{@linkplain File#getName() getName()}</code> is used as the
    * name.
    *
    * @param xml
    *    the XML {@link Element}, cannot be <code>null</code>.
    *
    * @param file
    *    the associated {@link File}, or <code>null</code> if none.
    *
    * @param name
    *    the file name, or <code>null</code>.
    *
    * @param lastModified
    *    the last modified time stamp, must be &gt;= <code>0L</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>xml == null || lastModified &lt;= 0L</code>.
    */
   public XMLDataSource(Element xml, File file, String name, long lastModified)
   throws IllegalArgumentException {

      // Check preconditions
      MandatoryArgumentChecker.check("xml", xml);
      if (lastModified <= 0L) {
         throw new IllegalArgumentException("lastModified (" + lastModified + "L) <= 0L");
      }

      // Populate fields
      _file         = file;
      _name         = (name == null && file != null) ? file.getName() : name;
      _lastModified = lastModified;
      _xml          = (Element) xml.clone();
   }

   /**
    * Constructs a new <code>XMLDataSource</code> using only an XML
    * <code>Element</code>.
    *
    * @param xml
    *    the XML {@link Element}, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>xml == null</code>.
    */
   public XMLDataSource(Element xml)
   throws IllegalArgumentException {

      // Check preconditions
      MandatoryArgumentChecker.check("xml", xml);

      // Populate fields
      _file         = null;
      _name         = null;
      _lastModified = System.currentTimeMillis();
      _xml          = (Element) xml.clone();
   }


   //-------------------------------------------------------------------------
   // Fields
   //-------------------------------------------------------------------------

   /**
    * The size of the data. Initially <code>-1</code>, meaning undetermined.
    */
   private int _size = -1;

   /**
    * The name. Can be <code>null</code>.
    */
   private final String _name;

   /**
    * The last modified time stamp.
    */
   private final long _lastModified;

   /**
    * The encapsulated abstract pathname. Can be <code>null</code>.
    */
   private final File _file;

   /**
    * The XML <code>Element</code>. Can be <code>null</code>.
    */
   private final Element _xml;


   //-------------------------------------------------------------------------
   // Methods
   //-------------------------------------------------------------------------

   private byte[] getBytes() {
      return TextUtils.toUTF8(_xml.toString());
   }

   public InputStream getInputStream() throws IOException {
      byte[] bytes = getBytes();
      if (_size < 0) {
         _size = bytes.length;
      }
      return new ByteArrayInputStream(bytes);
   }

   public OutputStream getOutputStream() throws IOException {
      throw new IOException("Operation not supported.");
   }

   public String getContentType() {
      return "text/xml";
   }

   public String getName() {
      return _name;
   }

   public long lastModified() throws DataAccessException {
      return _lastModified;
   }

   public int length() throws DataAccessException {
      if (_size < 0) {
         _size = getBytes().length;
      }
      return _size;
   }

   public File getFile() {
      return _file;
   }

   // TODO: Document
   public Element getXML() {
      return (Element) _xml.clone();
   }
}
