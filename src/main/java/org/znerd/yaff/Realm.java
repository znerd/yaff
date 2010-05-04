// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff;

import org.znerd.yaff.activation.ByteArrayDataSource;
import org.znerd.yaff.activation.XDataSource;
import static org.znerd.yaff.Assertions.*;
import static org.znerd.yaff.DatabaseType.*;
import org.znerd.yaff.form.FormDefinition;
import org.znerd.yaff.security.Key;
import org.znerd.yaff.types.Dialect;
import org.znerd.yaff.types.Type;
import org.znerd.yaff.types.TypeCreateException;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.Utils;
import org.xins.common.collections.BasicPropertyReader;
import org.xins.common.collections.InvalidPropertyValueException;
import org.xins.common.collections.MissingRequiredPropertyException;
import org.xins.common.collections.PropertyException;
import org.xins.common.collections.PropertyReader;
import org.xins.common.collections.PropertyReaderUtils;
import org.xins.common.text.HexConverter;
import org.xins.common.text.ParseException;
import org.xins.common.text.TextUtils;
import org.xins.common.xml.Element;
import org.xins.common.xml.ElementParser;
import org.xins.common.xml.ElementUtils;

/**
 * Login realm, within a site.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 *
 * @see DataHub
 * @see Site
 * @see AccountIndex
 * @see Account
 */
public abstract class Realm extends SubDataContext {

   //-------------------------------------------------------------------------
   // Class functions
   //-------------------------------------------------------------------------

   /**
    * Creates an appropriate <code>Realm</code> instance for the specified
    * site.
    *
    * @param site
    *    the {@link Site}, cannot be <code>null</code>.
    *
    * @param xml
    *    the XML {@link Element} to parse, cannot be <code>null</code>.
    *
    * @return
    *    an appropriate <code>Realm</code> instance, never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>site == null || xml == null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   static final Realm createRealm(Site site, Element xml)
   throws IllegalArgumentException, ContentAccessException {
      boolean secure;
      try {
         secure = ElementUtils.parseBooleanAttribute(xml, "secure", false);
      } catch (ParseException cause) {
         throw new TechnicalContentAccessException("Failed to parse \"secure\" attribute on <Realm/> element.", cause);
      }

      return secure ? new   SecureRealm(site, xml)
                    : new InsecureRealm(site, xml);
   }


   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>Realm</code>.
    *
    * @param site
    *    the containing {@link Site}, cannot be <code>null</code>.
    *
    * @param xml
    *    the XML {@link Element} to parse, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>site == null || xml == null || authenticator == null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   Realm(Site site, Element xml)
   throws IllegalArgumentException,
          ContentAccessException {

      // Check preconditions
      MandatoryArgumentChecker.check("site", site, "xml", xml);

      // Parse the name
      String name = xml.getAttribute("name");
      try {
         assertValidRealmName(name);
      } catch (IllegalArgumentException cause) {
         throw new TechnicalContentAccessException("Invalid \"name\" attribute on <Realm/> element: " + TextUtils.quote(name) + '.');
      }

      // Initialize instance fields
      _site                     = site;
      _xml                      = xml;
      _name                     = name;
      _random                   = new Random();
      _indexesByName            = initAccountIndexes();
      _accountDataDefs          = initAccountDataDefs();
      _accountPropertyDefs      = initAccountPropertyDefs();
      _accountPropertySources   = initAccountPropertySources();
      _userNameType             = initUserNameType();
      _passwordType             = initPasswordType();
      _defaultAccountProperties = initDefaultAccountProperties();
      _disabledAccountIDs       = initDisabledAccountIDs();
      _accountSource            = initAccountSource();
      _loginRegistration        = (getAccountIndex("combo") != null && getAccountIndex("authtoken") != null)
                                ? new LoginRegistration(this)
                                : null;
      _authUserNameProperty     = initAuthUserNameProperty();
   }


   //-------------------------------------------------------------------------
   // Fields
   //-------------------------------------------------------------------------

   /**
    * The containing <code>Site</code>. Never <code>null</code>.
    */
   private final Site _site;

   /**
    * The XML <code>Element</code> that defines this realm.
    * Never <code>null</code>.
    */
   private final Element _xml;

   /**
    * The realm name. Never <code>null</code> and always valid.
    */
   private final String _name;

   /**
    * Random number generator. Never <code>null</code>.
    */
   private final Random _random;

   /**
    * Unmodifiable and immutable collection of account indexes,
    * indexed by name. Never <code>null</code>.
    */
   private final Map<String,AccountIndex> _indexesByName;

   /**
    * The <code>AccountDataDef</code> instances, indexed by database type.
    * Never <code>null</code>.
    */
   private final Map<DatabaseType,AccountDataDef> _accountDataDefs;

   /**
    * The <code>AccountPropertyDef</code> instances, indexed by name.
    * Never <code>null</code>.
    */
   private final Map<String,AccountPropertyDef> _accountPropertyDefs;

   /**
    * Mapping from property name to the source database type. A property can
    * only come from one database type, this {@link Map} stores the
    * association. Never <code>null</code>.
    */
   private final Map<String,DatabaseType> _accountPropertySources;

   /**
    * The <code>Type</code> for login user names. Never <code>null</code>.
    */
   private final Type _userNameType;

   /**
    * The <code>Type</code> for login passwords. Never <code>null</code>.
    */
   private final Type _passwordType;

   /**
    * Default properties for new accounts. Never <code>null</code>.
    */
   private final PropertyReader _defaultAccountProperties;

   /**
    * The IDs of all disabled accounts. Never <code>null</code>.
    */
   protected final Collection<String> _disabledAccountIDs; // TODO: Make private

   /**
    * The <code>LoginRegistration</code> for this realm.
    * Can be <code>null</code>.
    */
   private final LoginRegistration _loginRegistration;

   /**
    * The primary database for account data. Accounts are created in this
    * database. Never <code>null</code>; either {@link DatabaseType#CONTENTDB}
    * or {@link DatabaseType#STATICDB}.
    */
   private final DatabaseType _accountSource;

   /**
    * The user name for authentication. Is <code>null</code> if this realm
    * does not support authentication.
    */
   private final AccountPropertyDef _authUserNameProperty;


   //-------------------------------------------------------------------------
   // Methods
   //-------------------------------------------------------------------------

   /**
    * Initializes the collection of account indexes for this realm by querying
    * the database accessors.
    *
    * @return
    *    an unmodifiable and immutable {@link Collection} of
    *    {@link AccountIndex}es, indexed by name, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private Map<String,AccountIndex> initAccountIndexes()
   throws ContentAccessException {

      // Prepare a map to contain the indexes, by name
      Map<String,AccountIndex> indexes = new HashMap<String,AccountIndex>();

      try {
         // Find the (optional) <AccountIndexes/> element
         Element indexesElem = _xml.getOptionalChildElement("AccountIndexes");
         if (indexesElem != null) {

            // Parse each of the (optional) <AccountIndex/> elements
            for (Element indexElem : indexesElem.getChildElements("AccountIndex")) {

               // Determine the database type
               String dbTypeName = indexElem.getAttribute("db");
               if (TextUtils.isEmpty(dbTypeName)) {
                  throw new ParseException("Found <AccountIndex/> element without \"db\" attribute, for site \"" + _site.getName() + "\", realm \"" + _name + "\".");
               }
               DatabaseType dbType;
               try {
                  dbType = DatabaseType.valueOf(dbTypeName);
               } catch (IllegalArgumentException cause) {
                  throw new ParseException("Found <AccountIndex/> element with invalid \"db\" attribute value \"" + dbTypeName + "\", for site \"" + _site.getName() + "\", realm \"" + _name + "\".", cause);
               }

               // Determine the name
               String indexName = indexElem.getAttribute("name");
               if (TextUtils.isEmpty(indexName)) {
                  throw new ParseException("Found <AccountIndex/> element without \"name\" attribute, for site \"" + _site.getName() + "\", realm \"" + _name + "\".");
               }
               try {
                  assertValidAccountIndexName(indexName);
               } catch (IllegalArgumentException cause) {
                  throw new ParseException("Found <AccountIndex/> element with invalid \"name\" attribute value \"" + indexName + "\", for site \"" + _site.getName() + "\", realm \"" + _name + "\".");
               }

               // Construct the AccountIndex object and add it to the map
               indexes.put(indexName, new AccountIndex(this, dbType, indexName));
            }
         }

      } catch (ParseException cause) {
         throw new TechnicalContentAccessException(toString() + ": Failed to parse <Realm/>.", cause);
      }

      return indexes;
   }

   /**
    * Initializes the <code>AccountDataDef</code> objects for this realm by
    * querying the database accessors.
    *
    * @return
    *    the appropriate {@link AccountDataDef} instances,
    *    indexed by {@link DatabaseType}, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private Map<DatabaseType,AccountDataDef> initAccountDataDefs()
   throws ContentAccessException {

      Map<DatabaseType,AccountDataDef> defs = new HashMap<DatabaseType,AccountDataDef>();

      for (DatabaseType dbType : DatabaseType.values()) {

         // Determine the file name (e.g. "FLEXDB.AccountDataDef.xml")
         String path = dbType.name() + ".AccountDataDef.xml";

         // Load the file, if it exists
         XDataSource file;
         try {
            file = getFile(CONTENTDB, path);
         } catch (NoSuchFileException e) {
            file = null;
         }

         // If the file does not exist, then just assume no account data for
         // this realm
         AccountDataDef def;
         if (file == null) {
            Collection<AccountPropertyDef> properties = Collections.emptyList();
            Collection<AccountSnippetDef> snippetDefs = new ArrayList<AccountSnippetDef>();
            def = new AccountDataDef(properties, snippetDefs);

         // File does exist, parse it
         } else {
            try {
               def = AccountDataDef.parseXML(new ElementParser().parse(file.getInputStream()));
            } catch (Exception cause) {
               throw new TechnicalContentAccessException(toString() + ": Failed to parse file \"" + path + "\" from the Content Database.", cause);
            }
         }

         // Store the association
         defs.put(dbType, def);
      }

      return defs;
   }

   /**
    * Initializes the <code>AccountPropertyDef</code> objects for this realm.
    *
    * @return
    *    the appropriate {@link AccountPropertyDef} instances,
    *    indexed by name, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private Map<String,AccountPropertyDef> initAccountPropertyDefs()
   throws ContentAccessException {

      Map<String,AccountPropertyDef> map = new HashMap<String,AccountPropertyDef>();

      for (AccountDataDef dataDef : _accountDataDefs.values()) {
         for (String propertyName : dataDef.getPropertyNames()) {
            map.put(propertyName, dataDef.getProperty(propertyName));
         }
      }

      return map;
   }

   /**
    * Creates a <code>Map</code> that maps from a property name to the source
    * database the property is defined in.
    *
    * @return
    *    the mapping, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private final Map<String,DatabaseType> initAccountPropertySources()
   throws ContentAccessException {

      // Loop over de database types
      Map<String,DatabaseType> map = new HashMap<String,DatabaseType>();
      for (DatabaseType dbType : DatabaseType.values()) {

         // Loop over all properties
         AccountDataDef def = getAccountDataDef(dbType);
         for (String propertyName : def.getPropertyNames()) {

            // Check for duplicates
            DatabaseType existing = map.get(propertyName);
            if (existing != null) {
               throw new TechnicalContentAccessException("Found property \"" + propertyName + "\" both in \"" + existing.name() + " database and in " + dbType + " database.");
            }

            // Store the association
            map.put(propertyName, dbType);
         }
      }

      return map;
   }

   /**
    * Determines the <code>Type</code> for login user names.
    *
    * @return
    *    the login user name {@link Type}, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private final Type initUserNameType()
   throws ContentAccessException {
      try {
         return findLoginFieldType("User");
      } catch (Exception cause) {
         throw new TechnicalContentAccessException(toString() + ": Failed to determine user name type.", cause);
      }
   }

   /**
    * Determines the <code>Type</code> for login passwords
    *
    * @return
    *    the login password {@link Type}, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private final Type initPasswordType()
   throws ContentAccessException {
      try {
         return findLoginFieldType("Pass");
      } catch (Exception cause) {
         throw new TechnicalContentAccessException(toString() + ": Failed to determine password type.", cause);
      }
   }

   /**
    * Determines the default properties for new accounts. These properties
    * will be used if <code>null</code> is passed for the
    * <code>accountProperties</code> argument to
    * {@link #createAccount(String,PropertyReader,Collection)}.
    *
    * @return
    *    the default properties to use, as an unmodifiable
    *    {@link PropertyReader}, not guaranteed to contain all required
    *    properties and not required to contain valid values for the
    *    respective types; never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private PropertyReader initDefaultAccountProperties()
   throws ContentAccessException {

      // Load the file
      Element xml = null;
      try {
         xml = getXMLFile(CONTENTDB, "DefaultAccountProperties.xml").getXML();

      // If the file does not exist, then return an empty property set
      } catch (NoSuchFileException e) {
         return PropertyReaderUtils.EMPTY_PROPERTY_READER;

      // File could not be loaded, fail
      } catch (Exception cause) {
         throw new TechnicalContentAccessException(toString() + ": Failed to load \"DefaultAccountProperties.xml\" file.", cause);
      }

      // Parse the file as a PropertyReader and return an unmodifiable copy
      PropertyReader properties = PropertyReaderUtils.parsePropertyReader(xml);
      return PropertyReaderUtils.copyUnmodifiable(properties);
   }

   /**
    * Determines the IDs of all disabled accounts.
    *
    * @return
    *    the IDs of all disabled accounts,
    *    never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private Collection<String> initDisabledAccountIDs()
   throws ContentAccessException {

      HashSet<String> ids = new HashSet<String>();

      // Load the file
      String fileName = "DisabledAccounts.xml";
      Element xml;
      try {
         xml = getXMLFile(CONTENTDB, fileName).getXML();

      // If the file does not exist, then that is OK
      } catch (NoSuchFileException e) {
         xml = null;

      // File could not be loaded, fail
      } catch (Exception cause) {
         throw new TechnicalContentAccessException(toString() + ": Failed to load \"" + fileName + "\" file.", cause);
      }

      // Parse the file
      if (xml != null) {
         for (Element accountElem : xml.getChildElements("Account")) {
            String id = accountElem.getAttribute("id");
            try {
               assertValidAccountID(id);
            } catch (IllegalArgumentException cause) {
               throw new TechnicalContentAccessException(toString() + ": Failed to parse \"" + fileName + "\". Found <Account/> without a valid ID (" + TextUtils.quote(id) + ").");
            }
            ids.add(id);
         }
      }

      return ids;
   }

   /**
    * Determines the <code>Type</code> for a field in the login form.
    *
    * @param fieldName
    *    the name of the field, cannot be <code>null</code>.
    *
    * @return
    *    the {@link Type}, never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>fieldName == null</code>.
    *
    * @throws ParseException
    *    in case of a parsing error.
    *
    * @throws TypeCreateException
    *    if the defined type could not be created.
    */
   private final Type findLoginFieldType(String fieldName)
   throws ParseException, TypeCreateException {

      // Check preconditions
      MandatoryArgumentChecker.check("fieldName", fieldName);

      // Find the LoginPage and the Field element
      Element loginPageElem = _xml.getUniqueChildElement("LoginPage");
      Element     fieldElem = findElement(loginPageElem, "Field", "name", fieldName);

      return Type.createType(fieldElem.getAttribute("type"));
   }

   /**
    * Finds an element within another element, given an element name and a
    * combination of attribute name and value.
    *
    * @param xml
    *    the XML {@link Element} to find a matching element inside (or the
    *    element self may match), cannot be <code>null</code>.
    *
    * @param elementName
    *    the name for the element to find, cannot be <code>null</code>.
    *
    * @param attributeName
    *    the name of the attribute to match, cannot be <code>null</code>.
    *
    * @param attributeValue
    *    the value of the attribute to match, cannot be <code>null</code>.
    *
    * @return
    *    the first matching {@link Element},
    *    or <code>null</code> if no match was found.
    *
    * @throws IllegalArgumentException
    *    if <code>xml            == null
    *          || elementName    == null
    *          || attributeName  == null
    *          || attributeValue == null</code>.
    */
   private final Element findElement(Element xml,
                                     String  elementName,
                                     String  attributeName,
                                     String  attributeValue)
   throws IllegalArgumentException {

      // Check preconditions
      MandatoryArgumentChecker.check("xml",            xml,
                                     "elementName",    elementName,
                                     "attributeName",  attributeName,
                                     "attributeValue", attributeValue);

      // This one may be a match
      if (   elementName.equals(xml.getLocalName())
          && attributeValue.equals(xml.getAttribute(attributeName))) {
         return xml;
      }

      // Loop over all children until a match is found
      for (Element child : xml.getChildElements()) {
         Element found = findElement(child, elementName, attributeName, attributeValue);
         if (found != null) {
            return found;
         }
      }

      // If nothing else, return null
      return null;
   }

   /**
    * Determines the account source database type.
    *
    * @return
    *    the account source {@link DatabaseType},
    *    never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private DatabaseType initAccountSource() throws ContentAccessException {

      // Get the appropriate attribute from the XML
      String s = _xml.getAttribute("accountSource");
      if (TextUtils.isEmpty(s)) {
         throw new TechnicalContentAccessException(toString() + ": Failed to retrieve accountSource value.");
      }

      // Get the DatabaseType instance
      try {
         return DatabaseType.valueOf(s);
      } catch (IllegalArgumentException cause) {
         throw new TechnicalContentAccessException(toString() + ": Invalid accountSource value " + TextUtils.quote(s) + '.');
      }
   }

   /**
    * Determines the account property that contains the user name for
    * authentication.
    *
    * @return
    *    the user name for authentication, or <code>null</code> if this realm
    *    does not support authentication.
    *
    * @return
    *    the {@link AccountPropertyDef} for the user name property,
    *    or <code>null</code> if no authentication is supported.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private AccountPropertyDef initAuthUserNameProperty() throws ContentAccessException {

      // Require "AuthUserName" field if "authtoken" index exists
      String            indexName = "authtoken";
      AccountIndex authtokenIndex = getAccountIndex(indexName);

      // That account index does not exist
      if (authtokenIndex == null) {
         return null;
      }

      // Find the account property
      final String propertyName = "AuthUserName";
      DatabaseType dbType = getAccountPropertySource(propertyName);

      // Error: property does not exist
      if (dbType == null) {
         throw new TechnicalContentAccessException(toString() + ": Expected account property \"" + propertyName + "\" because account index \"" + indexName + "\" exists.");

      // Error: property database does not match index database
      } else if (! dbType.equals(authtokenIndex.getDatabaseType())) {
         throw new TechnicalContentAccessException(toString() + ": Account property \"" + propertyName + "\" database type (" + dbType.name() + ") does not match \"" + indexName + "\" index database type (" + authtokenIndex.getDatabaseType().name() + ").");
      }

      return getAccountPropertyDef(propertyName);
   }

   // Specified by interface DataHubSub
   public DataHub getDataHub() {
      return _site.getDataHub();
   }

   /**
    * Retrieves the containing <code>Site</code>.
    *
    * @return
    *    the {@link Site}, never <code>null</code>.
    */
   public Site getSite() {
      return _site;
   }

   /**
    * Retrieves the name of this realm.
    *
    * @return
    *    the realm name, never <code>null</code> and always valid.
    */
   @Override
   public String getName() {
      return _name;
   }

   /**
    * Determines if this realm is secure.
    *
    * @return
    *    <code>true</code> if this realm is secure,
    *    <code>false</code> if not.
    */
   public abstract boolean isSecure();

   /**
    * Retrieves all account indexes.
    *
    * @return
    *    a {@link Collection} containing all {@link AccountIndex}es for this
    *    realm, never <code>null</code> (although the collection can be empty).
    */
   public Collection<AccountIndex> getAccountIndexes() {
      return _indexesByName.values();
   }

   /**
    * Retrieves an account index by name.
    *
    * @param name
    *    the name of the {@link AccountIndex}, cannot be <code>null</code>
    *    and must be a valid account index name
    *    (see {@link Assertions#assertValidAccountIndexName(String)}).
    *
    * @return
    *    the matching {@link AccountIndex},
    *    or <code>null</code> if no match is found.
    *
    * @throws IllegalArgumentException
    *    if <code>name == null</code> or if the name is considered invalid
    *    (see {@link Assertions#assertValidAccountIndexName(String)}).
    */
   public AccountIndex getAccountIndex(String name)
   throws IllegalArgumentException {
      assertValidAccountIndexName(name);
      return _indexesByName.get(name);
   }

   /**
    * Retrieves a required account index by name.
    *
    * @param indexName
    *    the name of the {@link AccountIndex}, cannot be <code>null</code>
    *    and must be a valid account index name
    *    (see {@link Assertions#assertValidAccountIndexName(String)}).
    *
    * @return
    *    the matching {@link AccountIndex},
    *    never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>name == null</code> or if the name is considered invalid
    *    (see {@link Assertions#assertValidAccountIndexName(String)}).
    *
    * @throws NoSuchAccountIndexException
    *    if the account index does not exist.
    */
   public AccountIndex getRequiredAccountIndex(String indexName)
   throws IllegalArgumentException, NoSuchAccountIndexException {

      // Call getAccountIndex(String) - this method will check the
      // preconditions
      AccountIndex index = getAccountIndex(indexName);

      // Make sure the index is not null
      if (index == null) {
         throw new NoSuchAccountIndexException(this, indexName);
      }

      return index;
   }

   /**
    * Returns the type of database in which accounts are created primarily.
    * Either {@link DatabaseType#CONTENTDB} or {@link DatabaseType#STATICDB}.
    *
    * @return
    *    the account source {@link DatabaseType}, never <code>null</code>,
    *    either {@link DatabaseType#CONTENTDB}
    *    or     {@link DatabaseType#STATICDB}.
    */
   public DatabaseType getAccountSource() {
      return _accountSource;
   }

   /**
    * Retrieves the <code>LoginRegistration</code> associated with this
    * realm, if any.
    *
    * @return
    *    the {@link LoginRegistration} for this realm, or <code>null</code> if
    *    login registration is not supported for this realm.
    */
   public LoginRegistration getLoginRegistration() {
      return _loginRegistration;
   }

   /**
    * Returns the account data definition for the specified source database.
    *
    * @param dbType
    *    the {@link DatabaseType} to determine the account data definition
    *    for, cannot be <code>null</code>.
    *
    * @return
    *    the {@link AccountDataDef} for the specified source,
    *    never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>dbType == null</code>.
    */
   public AccountDataDef getAccountDataDef(DatabaseType dbType)
   throws IllegalArgumentException {

      // Check preconditions
      MandatoryArgumentChecker.check("dbType", dbType);

      // Lookup
      AccountDataDef def = _accountDataDefs.get(dbType);

      // Check postconditions
      if (def == null) {
         throw Utils.logProgrammingError(Realm.class.getName(), "getAccountDataDef(DatabaseType)",
                                         Realm.class.getName(), null,
                                         "No AccountDataDef object found for database type " + dbType.name() + '.');
      }

      return def;
   }

   /**
    * Returns an account property definition by name.
    *
    * @param name
    *    the name of the account property, cannot be <code>null</code>.
    *
    * @return
    *    the {@link AccountPropertyDef},
    *    or <code>null</code> if the property definition is not found.
    *
    * @throws IllegalArgumentException
    *    if <code>name == null</code>.
    */
   public AccountPropertyDef getAccountPropertyDef(String name)
   throws IllegalArgumentException {
      MandatoryArgumentChecker.check("name", name);
      return _accountPropertyDefs.get(name);
   }

   /**
    * Returns all account property definitions.
    *
    * @return
    *    a {@link Collection} containing all account property definitions for
    *    this realm; never <code>null</code> (although it may be empty).
    */
   public Collection<AccountPropertyDef> getAccountPropertyDefs() {
      return _accountPropertyDefs.values();
   }

   /**
    * Determines the source database for the specified account property.
    *
    * @param propertyName
    *    the property for which to determine where it comes from,
    *    cannot be <code>null</code>.
    *
    * @return
    *    the source {@link DatabaseType},
    *    or <code>null</code> if no property with that name is defined.
    *
    * @throws IllegalArgumentException
    *    if <code>propertyName == null</code>.
    */
   public DatabaseType getAccountPropertySource(String propertyName)
   throws IllegalArgumentException {
      MandatoryArgumentChecker.check("propertyName", propertyName);
      return _accountPropertySources.get(propertyName);
   }

   /**
    * Counts the number of accounts in this realm, both enabled and disabled.
    *
    * @return
    *    the number of accounts in this realm, always &gt;= 0.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   public int getAccountCount() throws ContentAccessException {
      return getAccountIDs().size();
   }

   /**
    * Returns the collection of the IDs of all accounts in this realm. Both
    * enabled and disabled accounts may be returned.
    *
    * @return
    *    a {@link Collection} containing all account IDs for this realm,
    *    never <code>null</code> (although it may be empty if there are no
    *    accounts in this domain).
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error, for example because this realm
    *    does not support account set retrieval.
    */
   public Collection<String> getAccountIDs() throws ContentAccessException {

      // Prepare
      HashSet<String> accountIDs = new HashSet<String>();
      String                path = translatePath("accounts");
      Database                db = getDataHub().getDatabase(_accountSource);
      File              writeDir = db.getWriteDir();
      File               readDir = db.getReadDir();

      // Find accounts in the read directory
      findAccountIDs(accountIDs, new File(readDir, path), _accountSource.name() + " read directory");

      // Also find accounts in the write directory
      // (if there is a distinct write directory)
      if (! readDir.equals(writeDir)) {
         findAccountIDs(accountIDs, new File(writeDir, path), _accountSource.name() + " write directory");
      }

      return accountIDs;
   }

   /**
    * Finds account IDs in the specified directory and adds them to an
    * existing collection.
    *
    * @param accountIDs
    *    the existing {@link Collection} of account IDs,
    *    cannot be <code>null</code>.
    *
    * @param dir
    *    the directory to find accounts in, cannot be <code>null</code>.
    *
    * @param description
    *    description of the directory, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>accountIDs == null || dir == null || description == null</code>.
    *
    * @throws ContentAccessException
    *    in case of an error.
    */
   private final void findAccountIDs(Collection<String> accountIDs, File dir, String description)
   throws IllegalArgumentException, ContentAccessException {

      // Check preconditions
      MandatoryArgumentChecker.check("accountIDs", accountIDs, "dir", dir, "description", description);

      int originalAccountCount = accountIDs.size();

      // Only add items if the directory exists
      if (dir.exists()) {
         if (! dir.isDirectory()) {
            throw new TechnicalContentAccessException(toString() + ": Path for accounts directory (" + dir.getAbsolutePath() + ") exists, but is not a directory.");
         } else if (! dir.canRead()) {
            throw new TechnicalContentAccessException(toString() + ": Accounts directory (" + dir.getAbsolutePath() + ") exists, but is not readable.");
         }

         // Loop over all files in the directory
         for (File file : dir.listFiles(new AccountDirectoryFilenameFilter())) {
            accountIDs.add(file.getName());
         }
      }

      int newAccountsFound = accountIDs.size() - originalAccountCount;
      Utils.logDebug("Found " + newAccountsFound + " (additional) accounts in: " + dir.getAbsolutePath() + " (" + description + ").");
   }

   /**
    * Retrieves the appropriate account encryption <code>Key</code> using the
    * specified combo settings.
    *
    * @param accountID
    *    the account ID, cannot be <code>null</code> and must be valid.
    *
    * @param settings
    *    the combo settings, cannot be <code>null</code>.
    *
    * @return
    *    the encryption {@link Key}, or <code>null</code> if none is needed.
    *
    * @throws IllegalArgumentException
    *    if <code>accountID == null || settings == null</code>
    *    or if <code>accountID</code> is invalid
    *    (see {@link Assertions#assertValidAccountID(String)}).
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   protected abstract Key getKeyByCombo(String accountID, PropertyReader settings)
   throws IllegalArgumentException, ContentAccessException;

   /**
    * Retrieves an account by account identifier.
    *
    * <p>If the account cannot be found, then a {@link NoSuchAccountException}
    * is thrown.
    *
    * <p>A <code>Key</code> can be passed, but it is only used (and required)
    * by secure realms, see {@link SecureRealm}.
    *
    * @param id
    *    the unique account ID, cannot be <code>null</code> and must be valid
    *    (see {@link Assertions#assertValidAccountID(String)}),
    *
    * @param key
    *    the encryption {@link Key} for the account,
    *    can perhaps be <code>null</code>, depending on the type of realm
    *    (secure or insecure).
    *
    * @return
    *    the appropriate {@link Account} object, never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>id == null
    *          || ! {@linkplain Assertions}.{@linkplain Assertions#assertValidAccountID(String) assertValidAccountID}(id)
    *          || (key == null &amp;&amp;  isSecure())
    *          || (key != null &amp;&amp; !isSecure())</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   public abstract Account getAccount(String id, Key key)
   throws IllegalArgumentException, ContentAccessException;

   /**
    * Returns the default properties for new accounts. These properties
    * will be used if <code>null</code> is passed for the
    * <code>accountProperties</code> argument to
    * {@link #createAccount(String,PropertyReader,Collection)}.
    *
    * @return
    *    the default properties to use, as an unmodifiable
    *    {@link PropertyReader}, not guaranteed to contain all required
    *    properties and not required to contain valid values for the
    *    respective types; never <code>null</code>.
    */
   public PropertyReader getDefaultAccountProperties() {
      return _defaultAccountProperties;
   }

   /**
    * Takes properties intended to be used to initialize a new account
    * instance, and translates them to a map with <code>AccountData</code>
    * instances. This method can be used to check that all properties are set
    * correctly.
    *
    * @param properties
    *    the new account properties, cannot be <code>null</code>.
    *
    * @param snippets
    *    the account snippets, or <code>null</code>.
    *
    * @param stylesheets
    *    the account stylesheets, or <code>null</code>.
    *
    * @return
    *    the {@link Map}, never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>properties == null</code>.
    *
    * @throws MissingRequiredPropertyException
    *    if a mandatory property was found missing.
    *
    * @throws InvalidPropertyValueException
    *    if a property was found to have an invalid value.
    */
   public Map<DatabaseType,AccountData> newAccountDataMap(PropertyReader                   properties,
                                                          Collection<AccountSnippet>       snippets,
                                                          Collection<AccountStylesheetRef> stylesheets)
   throws IllegalArgumentException,
          MissingRequiredPropertyException,
          InvalidPropertyValueException {

      // Check preconditions
      MandatoryArgumentChecker.check("properties", properties);

      // Loop over all database types
      Map<DatabaseType,AccountData> dataMap = new HashMap<DatabaseType,AccountData>();
      for (DatabaseType dbType : DatabaseType.values()) {
         AccountDataDef def = getAccountDataDef(dbType);
         dataMap.put(dbType, new AccountData(def, properties, snippets, (dbType == DatabaseType.CONTENTDB ? stylesheets : null))); // TODO: Review 'stylesheets' parameter
      }

      return dataMap;
   }

   /**
    * Creates a new enabled account with a random account ID, taking the data
    * from the specified <code>AccountInfo</code> object.
    *
    * <p>Note: disabled accounts cannot be created directly. For this, the
    * <code>DisabledAccounts.xml</code> file must be modified manually.
    *
    * @param accountInfo
    *    the {@link AccountInfo} containing the data, cannot be
    *    <code>null</code>.
    *
    * @return
    *    the created {@link Account}, never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>accountInfo == null</code>.
    *
    * @throws AccountCreationException
    *    if the account could not be created.
    */
   public Account createAccount(final AccountInfo accountInfo)
   throws AccountCreationException {

      // Check preconditions
      MandatoryArgumentChecker.check("accountInfo", accountInfo);

      // Get the data
      String                             accountID = accountInfo.getID();
      PropertyReader                    properties = accountInfo.getProperties(Dialect.ORIGINAL);
      Collection<AccountSnippet>          snippets = accountInfo.getSnippets();
      Collection<AccountStylesheetRef> stylesheets = accountInfo.getStylesheets();

      // Delegate to a sibling createAccount method
      return accountID == null ? createAccount(           properties, snippets, stylesheets)
                               : createAccount(accountID, properties, snippets, stylesheets);
   }

   /**
    * Creates a new enabled account with a random account ID
    * and the specified data.
    *
    * <p>Note: disabled accounts cannot be created directly. For this, the
    * <code>DisabledAccounts.xml</code> file must be modified manually.
    *
    * @param accountProperties
    *    the properties for the account, or <code>null</code> if the default
    *    account properties should be used
    *    (see {@link #getDefaultAccountProperties()}).
    *
    * @param accountSnippets
    *    the snippets for the account, or <code>null</code> if none.
    *
    * @param accountStylesheets
    *    the stylesheets for the account, or <code>null</code> if none.
    *
    * @return
    *    the created {@link Account}, never <code>null</code>.
    *
    * @throws AccountCreationException
    *    if the account could not be created.
    */
   public Account createAccount(final PropertyReader                   accountProperties,
                                final Collection<AccountSnippet>       accountSnippets,
                                final Collection<AccountStylesheetRef> accountStylesheets)
   throws AccountCreationException {
      String accountID = HexConverter.toHexString(_random.nextLong());
      return createAccount(accountID, accountProperties, accountSnippets, accountStylesheets);
   }

   public final Account createAccount(final PropertyReader             accountProperties,
                                      final Collection<AccountSnippet> accountSnippets)
   throws IllegalArgumentException,
          AccountCreationException {
      return createAccount(accountProperties, accountSnippets, (Collection<AccountStylesheetRef>) null);
   }

   /**
    * Creates a new enabled account with the specified data and the specified
    * account ID (wrapper method).
    *
    * <p>Note: disabled accounts cannot be created directly. For this, the
    * <code>DisabledAccounts.xml</code> file must be modified manually.
    *
    * <p>This method delegates to
    * {@link #createAccountImpl(String,PropertyReader)}.
    *
    * @param accountID
    *    the unique account identifier, cannot be <code>null</code>
    *    and must be valid
    *    (see {@link Assertions#assertValidAccountID(String)}).
    *
    * @param accountProperties
    *    the properties for the account, or <code>null</code> if the default
    *    account properties should be used
    *    (see {@link #getDefaultAccountProperties()}).
    *
    * @param accountSnippets
    *    the account snippets, or <code>null</code> if none are defined.
    *
    * @throws IllegalArgumentException
    *    if <code>accountID == null</code>
    *    or if <code>accountID</code> is invalid
    *    (see {@link Assertions#assertValidAccountID(String)}).
    *
    * @throws AccountCreationException
    *    if the account could not be created.
    */
   public final Account createAccount(final String                     accountID,
                                      final PropertyReader             accountProperties,
                                      final Collection<AccountSnippet> accountSnippets)
   throws IllegalArgumentException,
          AccountCreationException {
      return createAccount(accountID, accountProperties, accountSnippets, (Collection<AccountStylesheetRef>) null);
   }

   /**
    * Creates a new enabled account with the specified data and the specified
    * account ID (wrapper method).
    *
    * <p>Note: disabled accounts cannot be created directly. For this, the
    * <code>DisabledAccounts.xml</code> file must be modified manually.
    *
    * <p>This method delegates to
    * {@link #createAccountImpl(String,PropertyReader)}.
    *
    * @param accountID
    *    the unique account identifier, cannot be <code>null</code>
    *    and must be valid
    *    (see {@link Assertions#assertValidAccountID(String)}).
    *
    * @param accountProperties
    *    the properties for the account, or <code>null</code> if the default
    *    account properties should be used
    *    (see {@link #getDefaultAccountProperties()}).
    *
    * @param accountSnippets
    *    the account snippets, or <code>null</code> if none are defined.
    *
    * @param accountStylesheets
    *    the account stylesheets, or <code>null</code> if none are defined.
    *
    * @throws IllegalArgumentException
    *    if <code>accountID == null</code>
    *    or if <code>accountID</code> is invalid
    *    (see {@link Assertions#assertValidAccountID(String)}).
    *
    * @throws AccountCreationException
    *    if the account could not be created.
    */
   public final Account createAccount(final String                           accountID,
                                      final PropertyReader                   accountProperties,
                                      final Collection<AccountSnippet>       accountSnippets,
                                      final Collection<AccountStylesheetRef> accountStylesheets)
   throws IllegalArgumentException,
          AccountCreationException {

      // Check preconditions
      assertValidAccountID(accountID);

      // Use fallback default properties, when appropriate
      PropertyReader accountPropsToUse = (accountProperties == null)
                                       ? getDefaultAccountProperties()
                                       : accountProperties;

      String logPrefix = toString() + ", account \"" + accountID + "\": ";

      // Log before account creation
      Utils.logDebug(logPrefix + "About to create.");

      // TODO: Make sure an account with the specified ID does not exist yet

      // Delegate to the implementation method
      Account account = null;
      try {
         account = createAccountImpl(accountID, accountPropsToUse, accountSnippets, accountStylesheets);

      // Property missing or has an invalid value
      } catch (PropertyException cause) {
         Utils.logError(logPrefix + "Failed to create due to property-related error.", cause);
         throw new AccountCreationException(this, accountID, accountPropsToUse, cause);

      // Missing required snippet
      } catch (MissingRequiredAccountSnippetException cause) {
         Utils.logError(logPrefix + "Failed to create due to a missing account snippet.", cause);
         throw new AccountCreationException(this, accountID, accountPropsToUse, cause);

      // Content access exception
      } catch (ContentAccessException cause) {
         Utils.logError(logPrefix + "Failed to create due to data access-related error.", cause);
         throw new AccountCreationException(this, accountID, accountPropsToUse, cause);

      // If anything failed at all, then delete all created files
      } finally {
         if (account == null) {
            Utils.logError(logPrefix + "Failed to create, removing account-related files.");
            deleteAccountFiles(accountID);
         }
      }

      Utils.logInfo(logPrefix + "Created.");

      return account;
   }

   /**
    * Creates a new enabled account with the specified data and the specified
    * account ID (implementation method).
    *
    * <p>This method is and should only be called from
    * {@link #createAccount(String,PropertyReader)}.
    *
    * <p>While creating the account, this method delegates:
    * <ol>
    * <li>the creation of the {@link Account} object to
    *     {@link #newAccount(String,boolean,Key)};
    * <li>the persistence of the actual account data to
    *     {@link #persistAccountData(String,DatabaseType,AccountData,FileStoreMode)}.
    * </ol>
    *
    * @param accountID
    *    the unique account identifier, cannot be <code>null</code>.
    *
    * @param accountProperties
    *    the properties for the account, cannot be <code>null</code> if the default
    *    account properties should be used
    *    (see {@link #getDefaultAccountProperties()}).
    *
    * @param accountSnippets
    *    the account snippets, or <code>null</code> if none are defined.
    *
    * @param accountStylesheets
    *    the account stylesheet, or <code>null</code> if none are defined.
    *
    * @throws PropertyException
    *    if a mandatory property is missing,
    *    or if a found property has an invalid value.
    *
    * @throws MissingRequiredAccountSnippetException
    *    if a mandatory account snippet is unset.
    *
    * @throws ContentAccessException
    *    in case of a content access error.
    */
   protected final Account createAccountImpl(final String                           accountID,
                                             final PropertyReader                   accountProperties,
                                             final Collection<AccountSnippet>       accountSnippets,
                                             final Collection<AccountStylesheetRef> accountStylesheets)
   throws PropertyException,
          MissingRequiredAccountSnippetException,
          ContentAccessException {

      // Initialize all AccountData instances - note that this may fail due to
      // a missing property or an invalid property value
      Map<DatabaseType,AccountData> dataMap = newAccountDataMap(accountProperties, accountSnippets, accountStylesheets);

      // Create a new Key, or null (depends on the type of Realm)
      Key key = newKey();

      // Write all AccountData.xml files that have at least one property
      for (DatabaseType dbType : DatabaseType.values()) {
         persistAccountData(accountID, dbType, dataMap.get(dbType), FileStoreMode.MUST_NOT_EXIST, key);
      }

      // Construct an enabled Account object
      Account account = newAccount(accountID, true, key);

      // Store the "combo" reference (if any)
      persistComboRef(account);

      // Store the "id" reference (if any)
      persistIDRef(account);

      return account;
   }

   /**
    * Creates a new encryption <code>Key</code>, or none, depending on the
    * type of realm.
    *
    * <p>This method is and should only be called from
    * {@link #createAccountImpl(String,PropertyReader)}.
    *
    * @return
    *    a new encryption {@link Key}, or <code>null</code>.
    */
   protected abstract Key newKey();

   /**
    * Persists the specified account data.
    *
    * @param accountID
    *    the account ID, cannot be <code>null</code> and must be a valid
    *    account ID (see {@link Assertions#assertValidAccountID(String)}).
    *
    * @param dbType
    *    the {@link DatabaseType}, cannot be <code>null</code>.
    *
    * @param data
    *    the data to be persisted, cannot be <code>null</code>.
    *
    * @param fileStoreMode
    *    the {@link FileStoreMode} to use, cannot be <code>null</code>.
    *
    * @param key
    *    the encryption {@link Key} to use, or <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content access error.
    */
   final void persistAccountData(String        accountID,
                                 DatabaseType  dbType,
                                 AccountData   data,
                                 FileStoreMode fileStoreMode,
                                 Key           key)
   throws ContentAccessException {

      String fileName = "AccountData.xml";
      String     path = "accounts/" + accountID + '/' + fileName;

      // If there are no property values, then just delete the file
      if (! data.containsValidValue()) {
         // TODO: Do something with FileStoreMode
         try {
            deleteFile(dbType, path, (key != null));
         } catch (NoSuchFileException cause) {
            // ignore
         }

      // If there are property values, then store the file
      } else {

         // Create a DataSource with the file contents
         ByteArrayDataSource ds = ByteArrayDataSource.fromString(fileName, data.toXML().toString(), "text/xml");

         // Store the file
         storeFile(dbType, path, ds, fileStoreMode, key);
      }
   }

   // TODO: Document
   protected void persistComboRef(Account account)
   throws IllegalArgumentException, ContentAccessException {
      AccountIndex index = getAccountIndex("combo");
      if (index != null) {
         index.storeRef(account);
      }
   }

   // TODO: Document
   protected void persistIDRef(Account account)
   throws IllegalArgumentException, ContentAccessException {
      AccountIndex index = getAccountIndex("id");
      if (index != null) {
         index.storeRef(account);
      }
   }

   /**
    * Constructs an appropriate <code>Account</code> instance.
    *
    * <p>This method is and should only be called from
    * {@link #createAccountImpl(String,PropertyReader)}.
    *
    * @param accountID
    *    the account ID, cannot be <code>null</code> and must be a valid
    *    account ID (see {@link Assertions#assertValidAccountID(String)}).
    *
    * @param enabled
    *    flag that indicates if the new {@link Account} object should
    *    consider itself enabled.
    *
    * @param key
    *    the encryption {@link Key} to use, or <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   protected abstract Account newAccount(String accountID, boolean enabled, Key key)
   throws ContentAccessException;

   /**
    * Deletes all files associated with the specified account.
    *
    * @param accountID
    *    the account ID, cannot be <code>null</code> and must be a valid
    *    account ID (see {@link Assertions#assertValidAccountID(String)}).
    *
    * @throws IllegalArgumentException
    *    if <code>accountID == null</code>
    *    or if <code>accountID</code> is invalid
    *    (see {@link Assertions#assertValidAccountID(String)}).
    */
   public void deleteAccountFiles(String accountID) throws IllegalArgumentException {

      // Check preconditions
      Assertions.assertValidAccountID(accountID);

      // Delete all account data directories
      for (DatabaseType dbType : DatabaseType.values()) {
         Database db = getDataHub().getDatabase(dbType);
         try {
            db.emptyDirectory(translatePath("accounts/" + accountID));
         } catch (Exception e) {
            if (! (e instanceof NoSuchFileException)) {
               Utils.logIgnoredException(e);
            }
         }
      }

      // Remove all references to the account
      for (AccountIndex index : getAccountIndexes()) {
         try {
            for (String ref : getAccountReferences(index, accountID)) {
               try {
                  index.removeRef(ref);
               } catch (Exception cause) {
                  Utils.logError(index.toString() + ": While deleting account: failed to remove reference \"" + ref + "\" to account \"" + accountID + "\". Ignoring and proceeding.", cause);
               }
            }
         } catch (ContentAccessException cause) {
            Utils.logIgnoredException(cause);
         }
      }
   }

   /**
    * Retrieves all references to the specified account in the specified index.
    *
    * @param index
    *    the {@link AccountIndex}, cannot be <code>null</code> and must be in
    *    the same realm.
    *
    * @param accountID
    *    the account ID, cannot be <code>null</code> and must be a valid
    *    account ID (see {@link Assertions#assertValidAccountID(String)}).
    *
    * @return
    *    a {@link Collection} of all references to the account in the
    *    specified account index; never <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>index == null
    *          || ! equals(index.{@linkplain AccountIndex#getRealm() getRealm()}</code>
    *    or if the specified account ID is invalid.
    *
    * @throws ContentAccessException
    *    if there was an error while accessing the database(s).
    */
   Collection<String> getAccountReferences(AccountIndex index, String accountID)
   throws IllegalArgumentException, ContentAccessException {

      // Check preconditions
      MandatoryArgumentChecker.check("index", index, "accountID", accountID);
      Assertions.assertValidAccountID(accountID);
      if (! equals(index.getRealm())) {
         throw new IllegalArgumentException(toString() + ": index is an AccountIndex in a different realm (" + index + ").");
      }

      // TODO: Make this implementation scalable, using backreferences

      // Find all references in the account index
      Collection<String> found = new ArrayList<String>();
      for (String ref: index.getRefs()) {
         if (accountID.equals(index.lookupAccountID(ref))) {
            found.add(ref);
         }
      }

      return found;
   }

   /**
    * Retrieves the <code>Type</code> for login user names.
    *
    * @return
    *    the {@link Type} for login user names, never <code>null</code>.
    */
   public Type getUserNameType() {
      return _userNameType;
   }

   /**
    * Retrieves the <code>Type</code> for login passwords.
    *
    * @return
    *    the {@link Type} for login passwords, never <code>null</code>.
    */
   public Type getPasswordType() {
      return _passwordType;
   }

   /**
    * Retrieves the <code>Authenticator</code> for this realm.
    *
    * @return
    *    the {@link Authenticator} for this realm, never <code>null</code>.
    */
   public abstract Authenticator getAuthenticator();

   /**
    * Authenticates with the specified user name and password.
    *
    * @param userName
    *    the user name, or <code>null</code>.
    *
    * @param password
    *    the password, or <code>null</code>.
    *
    * @return
    *    the authentication result, as an instance of class
    *    {@link Authenticator.Result}, never <code>null</code>.
    */
   public Authenticator.Result authenticate(String userName, String password) {
      return getAuthenticator().authenticate(new Authenticator.Request(userName, password));
   }

   /**
    * Returns the account property that stores the user name used for
    * authentication.
    *
    * @return
    *    the {@link AccountPropertyDef} that stores the user name used for
    *    authentication, or <code>null</code> if this realm does not support
    *    authentication.
    */
   public AccountPropertyDef getAuthUserNameProperty() {
      return _authUserNameProperty;
   }

   @Override
   public String translatePath(String path)
   throws IllegalArgumentException {
      assertValidPath(path);
      return _site.translatePath("realms/" + _name + '/' + path);
   }

   @Override
   public boolean equals(Object obj) {
      if (! (obj instanceof Realm)) {
         return false;
      }

      Realm that = (Realm) obj;

      return _site.equals(that._site) && _name.equals(that._name);
   }

   @Override
   public int hashCode() {
      return _site.hashCode() ^ _name.hashCode();
   }

   @Override
   public String toString() {
      return _site.toString() + ", realm \"" + _name + '"';
   }

   //-------------------------------------------------------------------------
   // Inner classes
   //-------------------------------------------------------------------------

   /**
    * File name filter that only matches account directories.
    *
    * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
    */
   private final static class AccountDirectoryFilenameFilter
   extends Object
   implements FilenameFilter {

      //----------------------------------------------------------------------
      // Constructors
      //----------------------------------------------------------------------

      /**
       * Constructs a new <code>AccountDirectoryFilenameFilter</code>.
       */
      private AccountDirectoryFilenameFilter() {
         // empty
      }


      //----------------------------------------------------------------------
      // Methods
      //----------------------------------------------------------------------

      /**
       * Determines if the specified file matches.
       *
       * @param dir
       *    the parent directory for the file,
       *    should not be <code>null</code>.
       *
       * @param name
       *    the file name, should not be <code>null</code>.
       */
      public boolean accept(File dir, String name) {
         return Assertions.ACCOUNT_ID_PATTERN.matcher(name).matches();
      }
   }
}
