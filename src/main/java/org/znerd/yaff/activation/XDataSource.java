// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff.activation;

import java.io.File;

import javax.activation.DataSource;

/**
 * Extended <code>DataSource</code> that provides some additional
 * functionality.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public interface XDataSource extends DataSource {

   /**
    * Returns the time that the encapsulated data was last modified.
    *
    * @return
    *    the time the encapsulated data was last modified, measured in
    *    milliseconds since the epoch (midnight, start of January 1, 1970);
    *    always &gt;= <code>0L</code>.
    *
    * @throws DataAccessException
    *    if the last modified date cannot be determined, for example because
    *    the data does not actually exist (anymore).
    */
   public long lastModified()
   throws DataAccessException;

   /**
    * Returns the length of the content.
    *
    * @return
    *    the lenth of the data in number of bytes for this data source. Always &gt;= 0.
    *
    * @throws DataAccessException
    *    if the length of the data cannot be determined, for example because
    *    the data does not actually exist (anymore).
    */
   public int length()
   throws DataAccessException;

   /**
    * Returns the (optional) <code>File</code> that is represented by the data
    * source.
    *
    * @return
    *    the {@link File} represented by this data source,
    *    or <code>null</code>.
    *
    * @throws DataAccessException
    *    if there was an error while retrieving the associated {@link File}.
    */
   public File getFile()
   throws DataAccessException;
}
