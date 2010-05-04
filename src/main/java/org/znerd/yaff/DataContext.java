// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff;

import org.znerd.yaff.activation.XDataSource;
import org.znerd.yaff.activation.XMLDataSource;
import org.znerd.yaff.security.Key;
import org.znerd.yaff.security.Vault;
import org.znerd.yaff.security.VaultIO;
import org.znerd.yaff.security.WrongKeyException;

import java.io.File;
import java.io.IOException;

import javax.activation.DataSource;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.Utils;
import org.xins.common.text.ParseException;
import org.xins.common.text.TextUtils;
import org.xins.common.xml.Element;

/**
 * Object that allows the retrieval of files, relative to itself.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public abstract class DataContext extends Object implements DataHubSub {

   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>DataContext</code>.
    */
   protected DataContext() {
      // empty
   }


   //-------------------------------------------------------------------------
   // Methods
   //-------------------------------------------------------------------------

   /**
    * Retrieves the default encryption key for this object. This key is
    * optional.
    *
    * <p>The implementation of this method in class <code>DataContext</code>
    * always returns <code>null</code>.
    *
    * @return
    *    the default encryption {@link Key} for this object,
    *    or <code>null</code> if there is none.
    */
   public Key getDefaultKey() {
      return null;
   }

   /**
    * Translates the specified path to an path that is relative to the
    * <code>DataHub</code>.
    *
    * @param path
    *    the path that is relative to this {@link DataContext} and should be
    *    translated so it becomes relative to the {@link DataHub}, cannot be
    *    <code>null</code> and must be a valid path,
    *    see {@link Assertions#assertValidPath(String)}.
    *
    * @return
    *    the translated path, relative to the {@link DataHub},
    *    never <code>null</code> and always valid.
    *
    * @throws IllegalArgumentException
    *    if <code>path == null</code> or if the path is not a valid path,
    *    see {@link Assertions#assertValidPath(String)}.
    */
   public abstract String translatePath(String path)
   throws IllegalArgumentException;

   /**
    * Retrieves a database by type.
    *
    * <p>The default implementation in class <code>DataContext</code>
    * basically implements this method as:
    *
    * <blockquote><pre>return {@linkplain #getDataHub()}.{@linkplain DataHub#getDatabase(DatabaseType) getDatabase}(dbType)}</pre></blockquote>
    *
    * @param dbType
    *    the {@link DatabaseType}, cannot be <code>null</code>.
    *
    * @return
    *    the appropriate {@link Database} instance,
    *    never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>dbType == null</code>.
    */
   protected Database getDatabase(DatabaseType dbType)
   throws IllegalArgumentException {

      // Check preconditions
      MandatoryArgumentChecker.check("dbType", dbType);

      // Get the DataHub
      DataHub dataHub = getDataHub();
      if (dataHub == null) {
         throw Utils.logProgrammingError(DataContext.class.getName(), "getDatabase(DatabaseType)", getClass().getName(), "getDataHub()", "Method getDataHub() returned null.", (Throwable) null);
      }

      // Get the Database from the DataHub instance
      return dataHub.getDatabase(dbType);
   }

   /**
    * Retrieves a file as an <code>XDataSource</code>, using the default
    * encryption key.
    * If the file is not found, then a {@link NoSuchFileException} is thrown.
    *
    * @param dbType
    *    the type of database to get the file from,
    *    cannot be <code>null</code>.
    *
    * @param path
    *    the path to the file, cannot be <code>null</code>
    *    and must be a valid file path
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @return
    *    the file, as an {@link XDataSource} instance,
    *    never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>dbType == null || path == null</code>
    *    or if the <code>path</code> is not valid,
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @throws NoSuchDatabaseException
    *    if the specified database is currently unavailable.
    *
    * @throws NoSuchFileException
    *    if the file cannot be found.
    *
    * @throws TechnicalContentAccessException
    *    if the content access failed.
    */
   public final XDataSource getFile(DatabaseType dbType, String path)
   throws IllegalArgumentException,
          NoSuchDatabaseException,
          NoSuchFileException,
          TechnicalContentAccessException {
      return getFile(dbType, path, getDefaultKey());
   }

   /**
    * Retrieves a file as an <code>XDataSource</code>, using the specified
    * encryption key.
    * If the file is not found, then a {@link NoSuchFileException} is thrown.
    *
    * @param dbType
    *    the type of database to get the file from,
    *    cannot be <code>null</code>.
    *
    * @param path
    *    the path to the file, cannot be <code>null</code>
    *    and must be a valid file path
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @param key
    *    the encryption {@link Key} to use to decrypt the file,
    *    or <code>null</code> if an unencrypted file is expected.
    *
    * @return
    *    the file, as an {@link XDataSource} instance,
    *    never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>dbType == null || path == null</code>
    *    or if the <code>path</code> is not valid,
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @throws NoSuchDatabaseException
    *    if the specified database is currently unavailable.
    *
    * @throws NoSuchFileException
    *    if the file cannot be found.
    *
    * @throws TechnicalContentAccessException
    *    if the content access failed.
    */
   public final XDataSource getFile(DatabaseType dbType, String path, Key key)
   throws IllegalArgumentException,
          NoSuchDatabaseException,
          NoSuchFileException,
          TechnicalContentAccessException {

      // Check preconditions
      MandatoryArgumentChecker.check("dbType", dbType, "path", path);
      Assertions.assertValidFilePath(path);

      // Get the appropriate Database
      Database database = getDatabase(dbType);
      if (database == null) {
         throw new NoSuchDatabaseException(dbType);
      }

      // Translate the path and check the result
      String translatedPath = translatePath(path);
      try {
         Assertions.assertValidFilePath(translatedPath);
      } catch (IllegalArgumentException cause) {
         throw Utils.logProgrammingError(DataContext.class.getName(), "getFile(DatabaseType,String)", // detecting class/method
                                         getClass().getName(),        "translatePath(String)",        // subject   class/method
                                         "Invalid path " + TextUtils.quote(translatedPath) + '.', cause);
      }

      // No encryption
      if (key == null) {
         return database.getFile(translatedPath);

      // Decrypt the file
      } else {
         String encryptedPath = translatedPath + ".Ciphered.xml";

         try {
            XMLDataSource encryptedData = database.getXMLFile(encryptedPath);
            Vault                 vault = VaultIO.deserialize(encryptedData.getXML());
            return vault.getContentAsDataSource(key, path);

         // XML parsing error
         } catch (ParseException cause) {
            throw new TechnicalContentAccessException(toString() + ": Failed to parse encrypted file " + TextUtils.quote(encryptedPath) + " in " + dbType.name() + '.', cause);

         // Decryption failed
         } catch (WrongKeyException cause) {
            throw new TechnicalContentAccessException(toString() + ": Failed to decrypt file " + TextUtils.quote(encryptedPath) + " in " + dbType.name() + '.', cause);
         }
      }
   }

   /**
    * Retrieves a file, parses it as XML and returns it as an
    * <code>XMLDataSource</code>, using the default encryption key.
    * If the file is not found,            then a {@link NoSuchFileException}             is thrown.
    * If the file cannot be parsed as XML, then a {@link TechnicalContentAccessException} is thrown.
    *
    * @param dbType
    *    the type of database to get the file from,
    *    cannot be <code>null</code>.
    *
    * @param path
    *    the path to the file, cannot be <code>null</code>
    *    and must be a valid file path
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @return
    *    the file, as an {@link XMLDataSource} instance,
    *    never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>dbType == null || path == null</code>
    *    or if the <code>path</code> is not valid,
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @throws NoSuchDatabaseException
    *    if the database of the specified type is not available in this
    *    environment.
    *
    * @throws NoSuchFileException
    *    if the file cannot be found.
    *
    * @throws TechnicalContentAccessException
    *    if the content access failed, for a technical reason.
    */
   public final XMLDataSource getXMLFile(DatabaseType dbType, String path)
   throws IllegalArgumentException,
          NoSuchDatabaseException,
          NoSuchFileException,
          TechnicalContentAccessException {
      return getXMLFile(dbType, path, getDefaultKey());
   }

   /**
    * Retrieves a file, parses it as XML and returns it as an
    * <code>XMLDataSource</code>, using the specified encryption key.
    * If the file is not found,            then a {@link NoSuchFileException}             is thrown.
    * If the file cannot be parsed as XML, then a {@link TechnicalContentAccessException} is thrown.
    *
    * @param dbType
    *    the type of database to get the file from,
    *    cannot be <code>null</code>.
    *
    * @param path
    *    the path to the file, cannot be <code>null</code>
    *    and must be a valid file path
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @param key
    *    the encryption {@link Key} to use to decrypt the file,
    *    or <code>null</code> if an unencrypted file is expected.
    *
    * @return
    *    the file, as an {@link XMLDataSource} instance,
    *    never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>dbType == null || path == null</code>
    *    or if the <code>path</code> is not valid,
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @throws NoSuchDatabaseException
    *    if the database of the specified type is not available in this
    *    environment.
    *
    * @throws NoSuchFileException
    *    if the file cannot be found.
    *
    * @throws TechnicalContentAccessException
    *    if the content access failed, for a technical reason.
    */
   public final XMLDataSource getXMLFile(DatabaseType dbType, String path, Key key)
   throws IllegalArgumentException,
          NoSuchDatabaseException,
          NoSuchFileException,
          TechnicalContentAccessException {

      // Delegate to regular getFile method
      try {
         return new XMLDataSource(getFile(dbType, path, key));

      // I/O error
      } catch (IOException cause) {
         throw new TechnicalContentAccessException(toString() + ": I/O error while opening/reading file \"" + path + "\" from " + dbType.name() + '.', cause);

      // Parsing error
      } catch (ParseException cause) {
         throw new TechnicalContentAccessException(toString() + ": Error while parsing file \"" + path + "\" from " + dbType.name() + " as XML.", cause);
      }
   }

   /**
    * Creates a new file with a unique name, using the default encryption key.
    * The name is made unique by replacing a token in the file name.
    *
    * <p>The path to the actual created file is returned.
    *
    * @param dbType
    *    the type of database to store the file in,
    *    cannot be <code>null</code>.
    *
    * @param path
    *    the file name path, containing a token to be replaced,
    *    cannot be <code>null</code> and must be valid.
    *
    * @param replaceToken
    *    the token to be replaced in the path,
    *    cannot be <code>null</code>,
    *    the length must be at least 1 and
    *    the string must be found exactly once in <code>path</code>
    *    and that position must be in the last component
    *    (after the last path separator <code>'/'</code>).
    *
    * @return
    *    the path to the created file, never <code>null</code> and always
    *    matching the pattern specified in the <code>path</code> argument
    *    (with the token replaced with a string that makes the path name
    *    unique).
    *
    * @param data
    *    the data to store in the new file, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>dbType       == null
    *          || path         == null
    *          || replaceToken == null
    *          || data         == null
    *          || replaceToken.length() &lt; 1
    *          || !path.contains(replaceToken)
    *          || path.indexOf(replaceToken) != path.lastIndexOf(replaceToken)
    *          || path.contains("/") &amp;&amp; path.indexOf(replaceToken) &lt; path.lastIndexOf('/')</code>
    *    or if the path is not valid according to
    *    {@link Assertions#assertValidFilePath(String)}.
    *
    * @throws NoSuchDatabaseException
    *    if the database of the specified type is not available in this
    *    environment.
    *
    * @throws ReadOnlyDatabaseException
    *    if the <code>Database</code> is read-only,
    *    see {@link Database#isWritable()}.
    *
    * @throws TechnicalContentAccessException
    *    if there was a technical error.
    */
   public final String createUniqueFile(DatabaseType dbType,
                                        String       path,
                                        String       replaceToken,
                                        DataSource   data)
   throws IllegalArgumentException,
          NoSuchDatabaseException,
          ReadOnlyDatabaseException,
          TechnicalContentAccessException {
      return createUniqueFile(dbType, path, replaceToken, data, getDefaultKey());
   }

   /**
    * Creates a new file with a unique name, using the specified encryption
    * key.
    * The name is made unique by replacing a token in the file name.
    *
    * <p>The path to the actual created file is returned.
    *
    * @param dbType
    *    the type of database to store the file in,
    *    cannot be <code>null</code>.
    *
    * @param path
    *    the file name path, containing a token to be replaced,
    *    cannot be <code>null</code> and must be valid.
    *
    * @param replaceToken
    *    the token to be replaced in the path,
    *    cannot be <code>null</code>,
    *    the length must be at least 1 and
    *    the string must be found exactly once in <code>path</code>
    *    and that position must be in the last component
    *    (after the last path separator <code>'/'</code>).
    *
    * @param key
    *    the encryption {@link Key} to use to encrypt the file,
    *    or <code>null</code> if an unencrypted file should be produced.
    *
    * @return
    *    the path to the created file, never <code>null</code> and always
    *    matching the pattern specified in the <code>path</code> argument
    *    (with the token replaced with a string that makes the path name
    *    unique).
    *
    * @param data
    *    the data to store in the new file, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>dbType       == null
    *          || path         == null
    *          || replaceToken == null
    *          || data         == null
    *          || replaceToken.length() &lt; 1
    *          || !path.contains(replaceToken)
    *          || path.indexOf(replaceToken) != path.lastIndexOf(replaceToken)
    *          || path.contains("/") &amp;&amp; path.indexOf(replaceToken) &lt; path.lastIndexOf('/')</code>
    *    or if the path is not valid according to
    *    {@link Assertions#assertValidFilePath(String)}.
    *
    * @throws NoSuchDatabaseException
    *    if the database of the specified type is not available in this
    *    environment.
    *
    * @throws ReadOnlyDatabaseException
    *    if this <code>Database</code> is read-only,
    *    see {@link Database#isWritable()}.
    *
    * @throws NoSuchDatabaseException
    *    if the database of the specified type is not available in this
    *    environment.
    *
    * @throws TechnicalContentAccessException
    *    if there was a technical error.
    */
   public final String createUniqueFile(DatabaseType dbType,
                                        String       path,
                                        String       replaceToken,
                                        DataSource   data,
                                        Key          key)
   throws IllegalArgumentException,
          NoSuchDatabaseException,
          ReadOnlyDatabaseException,
          TechnicalContentAccessException {

      // Check preconditions
      MandatoryArgumentChecker.check("dbType",       dbType,
                                     "path",         path,
                                     "replaceToken", replaceToken,
                                     "data",         data);

      // Get the appropriate Database
      Database database = getDatabase(dbType);
      if (database == null) {
         throw new NoSuchDatabaseException(dbType);
      }

      // Translate the path and check the result
      String translatedPath = translatePath(path);
      try {
         Assertions.assertValidFilePath(translatedPath);
      } catch (IllegalArgumentException cause) {
         throw Utils.logProgrammingError(DataContext.class.getName(), "getFile(DatabaseType,String)", // detecting class/method
                                         getClass().getName(),        "translatePath(String)",        // subject   class/method
                                         "Invalid path " + TextUtils.quote(translatedPath) + '.', cause);
      }

      // Without encryption
      String actualName;
      if (key == null) {
         actualName = database.createUniqueFile(translatedPath, replaceToken, data);

      // With encryption
      } else {
         String      encryptedPath = translatedPath + ".Ciphered.xml";
         XDataSource encryptedData = encrypt(dbType, translatedPath, data, key);
         actualName                = database.createUniqueFile(encryptedPath, replaceToken, encryptedData);
      }

      return actualName;
   }

   // TODO: Document
   private XDataSource encrypt(DatabaseType dbType, String encryptedPath, DataSource data, Key key)
   throws IllegalArgumentException, TechnicalContentAccessException {

      // Check preconditions
      MandatoryArgumentChecker.check("dbType", dbType, "encryptedPath", encryptedPath, "data", data, "key", key);
      Assertions.assertValidFilePath(encryptedPath);

      try {
         Vault               vault = new Vault(key, data);
         Element  encryptedDataXML = VaultIO.serialize(vault);

         return new XMLDataSource(encryptedDataXML, (File) null, encryptedPath, System.currentTimeMillis());

      // I/O error
      } catch (IOException cause) {
         throw new TechnicalContentAccessException(toString() + ": I/O error while producing encrypted file " + TextUtils.quote(encryptedPath) + " in " + dbType.name() + '.', cause);

      // Encryption error
      } catch (WrongKeyException cause) {
         throw new TechnicalContentAccessException(toString() + ": Failed to encrypt file " + TextUtils.quote(encryptedPath) + " in " + dbType.name() + '.', cause);
      }
   }

   /**
    * Stores a file as a <code>DataSource</code>, by path, using the default
    * encryption key.
    *
    * @param dbType
    *    the type of database to store the file in,
    *    cannot be <code>null</code>.
    *
    * @param path
    *    the path to the file, cannot be <code>null</code>
    *    and must be a valid file path
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @param data
    *    the data, as a {@link DataSource} instance,
    *    canoot be <code>null</code>.
    *
    * @param mode
    *    the {@link FileStoreMode} that indicates how to behave, for example
    *    if the file already exists; cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>dbType == null
    *          || path   == null
    *          || data   == null
    *          || mode   == null</code>
    *    or if the <code>path</code> is not valid
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @throws ReadOnlyDatabaseException
    *    if the {@link Database} is read-only,
    *    see {@link Database#isWritable()}.
    *
    * @throws FileExistsException
    *    if the {@link FileStoreMode} does not allow the file to exist,
    *    but still it does.
    *
    * @throws NoSuchFileException
    *    if the {@link FileStoreMode} requires the file to exist,
    *    but still it does not.
    *
    * @throws TechnicalContentAccessException
    *    if the content access failed for a technical reason.
    */
   public void storeFile(DatabaseType  dbType,
                         String        path,
                         DataSource    data,
                         FileStoreMode mode)
   throws IllegalArgumentException,
          ReadOnlyDatabaseException,
          FileExistsException,
          NoSuchFileException,
          TechnicalContentAccessException {
      storeFile(dbType, path, data, mode, getDefaultKey());
   }

   /**
    * Stores a file as a <code>DataSource</code>, by path, using the specified
    * encryption key.
    *
    * @param dbType
    *    the type of database to store the file in,
    *    cannot be <code>null</code>.
    *
    * @param path
    *    the path to the file, cannot be <code>null</code>
    *    and must be a valid file path
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @param data
    *    the data, as a {@link DataSource} instance,
    *    canoot be <code>null</code>.
    *
    * @param mode
    *    the {@link FileStoreMode} that indicates how to behave, for example
    *    if the file already exists; cannot be <code>null</code>.
    *
    * @param key
    *    the encryption {@link Key} to use to encrypt the file,
    *    or <code>null</code> if an unencrypted file should be produced.
    *
    * @throws IllegalArgumentException
    *    if <code>dbType == null
    *          || path   == null
    *          || data   == null
    *          || mode   == null</code>
    *    or if the <code>path</code> is not valid
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @throws ReadOnlyDatabaseException
    *    if the {@link Database} is read-only,
    *    see {@link Database#isWritable()}.
    *
    * @throws FileExistsException
    *    if the {@link FileStoreMode} does not allow the file to exist,
    *    but still it does.
    *
    * @throws NoSuchFileException
    *    if the {@link FileStoreMode} requires the file to exist,
    *    but still it does not.
    *
    * @throws TechnicalContentAccessException
    *    if the content access failed for a technical reason.
    */
   public void storeFile(DatabaseType  dbType,
                         String        path,
                         DataSource    data,
                         FileStoreMode mode,
                         Key           key)
   throws IllegalArgumentException,
          ReadOnlyDatabaseException,
          FileExistsException,
          NoSuchFileException,
          TechnicalContentAccessException {

      // Check preconditions
      MandatoryArgumentChecker.check("dbType", dbType,
                                     "path",   path,
                                     "data",   data,
                                     "mode",   mode);
      Assertions.assertValidFilePath(path);


      // Get the appropriate Database and translate the path
      Database     database = getDatabase(dbType);
      String translatedPath = translatePath(path);

      // Check the result from translatedPath
      try {
         Assertions.assertValidFilePath(translatedPath);
      } catch (IllegalArgumentException cause) {
         throw Utils.logProgrammingError(DataContext.class.getName(), "storeFile(DatabaseType,String,DataSource,FileStoreMode)", // detecting class/method
                                         getClass().getName(),        "translatePath(String)",                                   // subject   class/method
                                         "Invalid path " + TextUtils.quote(translatedPath) + '.', cause);
      }


      // Without encryption
      if (key == null) {
         database.storeFile(translatedPath, data, mode);

      // With encryption
      } else {
         String      encryptedPath = translatedPath + ".Ciphered.xml";
         XDataSource encryptedData = encrypt(dbType, translatedPath, data, key);
         database.storeFile(encryptedPath, encryptedData, mode);
      }
   }

   /**
    * Deletes a file, by path. Whether an encrypted file is expected depends
    * on whether this object has a default encryption key; if it has one, then
    * an encrypted file is expected.
    *
    * @param dbType
    *    the type of database from which to delete a file,
    *    cannot be <code>null</code>.
    *
    * @param path
    *    the path to the file, cannot be <code>null</code>
    *    and must be a valid file path
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @throws IllegalArgumentException
    *    if <code>dbType == null || path == null</code>
    *    or if the <code>path</code> is not valid,
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @throws ContentAccessException
    *    if the content access failed.
    */
   public void deleteFile(DatabaseType dbType, String path)
   throws IllegalArgumentException, ContentAccessException {
      deleteFile(dbType, path, (getDefaultKey() != null));
   }

   /**
    * Deletes a file, by path, specifying whether an encrypted file is
    * expected.
    *
    * @param dbType
    *    the type of database from which to delete a file,
    *    cannot be <code>null</code>.
    *
    * @param path
    *    the path to the file, cannot be <code>null</code>
    *    and must be a valid file path
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @param expectedEncrypted
    *    flag that indicates whether an encrypted file is expected.
    *
    * @throws IllegalArgumentException
    *    if <code>dbType == null || path == null</code>
    *    or if the <code>path</code> is not valid,
    *    (see {@link Assertions#assertValidFilePath(String)}).
    *
    * @throws ContentAccessException
    *    if the content access failed.
    */
   public void deleteFile(DatabaseType dbType, String path, boolean expectEncrypted)
   throws IllegalArgumentException, ContentAccessException {

      // Check preconditions
      MandatoryArgumentChecker.check("dbType", dbType, "path", path);
      Assertions.assertValidFilePath(path);

      // Get the appropriate Database and translate the path
      Database     database = getDatabase(dbType);
      String translatedPath = translatePath(path);

      // Check the result from translatedPath
      try {
         Assertions.assertValidFilePath(translatedPath);
      } catch (IllegalArgumentException cause) {
         throw Utils.logProgrammingError(DataContext.class.getName(), "deleteFile(DatabaseType,String,DataSource)", // detecting class/method
                                         getClass().getName(),        "translatePath(String)",                      // subject   class/method
                                         "Invalid path " + TextUtils.quote(translatedPath) + '.', cause);
      }

      // Not an encrypted file
      if (! expectEncrypted) {
         database.deleteFile(translatedPath);

      // An encrypted file
      } else {
         database.deleteFile(translatedPath + ".Ciphered.xml");
      }
   }
}
