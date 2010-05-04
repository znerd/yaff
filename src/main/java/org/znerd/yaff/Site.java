// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff;

import org.znerd.yaff.activation.XDataSource;
import org.znerd.yaff.form.FormDefinition;
import org.znerd.yaff.form.FormEnvironment;
import org.znerd.yaff.form.FormState;
import org.znerd.yaff.form.PageFormDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.Utils;
import org.xins.common.collections.PropertyReader;
import org.xins.common.collections.PropertyReaderUtils;
import org.xins.common.text.ParseException;
import org.xins.common.text.TextUtils;
import org.xins.common.xml.Element;

/**
 * Concrete site.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 *
 * @see DataHub
 * @see Realm
 * @see AccountIndex
 * @see Account
 */
public final class Site extends SubDataContext {

   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>Site</code>.
    *
    * @param dataHub
    *    the {@link DataHub}, cannot be <code>null</code>.
    *
    * @param name
    *    the site name, cannot be <code>null</code>
    *    and must be a valid site name.
    *
    * @throws IllegalArgumentException
    *    if <code>dataHub == null || name == null</code>
    *    or if the name is considered invalid.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   Site(DataHub dataHub, String name)
   throws IllegalArgumentException, ContentAccessException {

      // Check preconditions
      MandatoryArgumentChecker.check("dataHub", dataHub, "name", name);
      Assertions.assertValidSiteName(name);

      // Initialize instance fields
      _dataHub      = dataHub;
      _name         = name;
      _xml          = initSiteXML();
      _properties   = initProperties();
      _vhosts       = initVirtualHosts();
      _pageNames    = initPageNames();
      _realmsByName = initRealms();
      _formsByName  = initFormDefinitions();
      _formStorage  = initFormStorage();
      _rngsByName   = initRandomNumberGenerators();
      _pageDefXML   = initPageDefXML();
      _structure    = new SiteStructure(this);
   }


   //-------------------------------------------------------------------------
   // Fields
   //-------------------------------------------------------------------------

   /**
    * The <code>DataHub</code>. Never <code>null</code>.
    */
   private final DataHub _dataHub;

   /**
    * The site name. Never <code>null</code> and always valid.
    */
   private final String _name;

   /**
    * Definition of this site, as XML. Never <code>null</code>.
    */
   private final Element _xml;

   /**
    * The site properties. Never <code>null</code>.
    */
   private final PropertyReader _properties;

   /**
    * All virtual hosts for this site. Never <code>null</code>.
    */
   private final Collection<SiteVirtualHost> _vhosts;

   /**
    * Unmodifiable and immutable collection of all page names.
    * Never <code>null</code> and contains at least one element.
    */
   private final Collection<String> _pageNames;

   /**
    * Unmodifiable and immutable collection of realms,
    * indexed by name. Never <code>null</code>.
    */
   private final Map<String,Realm> _realmsByName;

   /**
    * Unmodifiable and immutable collection of form definitions,
    * indexed by name. Never <code>null</code>.
    */
   private final Map<String,FormDefinition> _formsByName;

   /**
    * Form storage. Never <code>null</code>.
    */
   private final SiteFormStorage _formStorage;

   /**
    * Unmodifiable and immutable collection of random number generators for
    * this site, indexed by name. Never <code>null</code>.
    */
   private final Map<String,RandomNumberGenerator> _rngsByName;

   /**
    * The structure for this site. Never <code>null</code>.
    */
   private final SiteStructure _structure;

   /**
    * Page definitions, as XML. Never <code>null</code>.
    */
   private final Map<String,Element> _pageDefXML;


   //-------------------------------------------------------------------------
   // Methods
   //-------------------------------------------------------------------------

   /**
    * Loads the <code>PreprocessedSite.xml</code> file.
    *
    * @return
    *    the parsed XML, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private Element initSiteXML()
   throws ContentAccessException {
      return getXMLFile(DatabaseType.CONTENTDB, "content/PreprocessedSite.xml").getXML();
   }

   /**
    * Initializes the properties for this site.
    *
    * @return
    *    an unmodifiable {@link PropertyReader}, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private PropertyReader initProperties()
   throws ContentAccessException {
      try {
         Element elem = _xml.getOptionalChildElement("Properties");
         return (elem == null)
              ? PropertyReaderUtils.EMPTY_PROPERTY_READER
              : PropertyReaderUtils.copyUnmodifiable(PropertyReaderUtils.parsePropertyReader(elem));
      } catch (ParseException cause) {
         throw new TechnicalContentAccessException("Error while parsing site properties.", cause);
      }
   }

   /**
    * Initializes the virtual hosts associated with this site.
    *
    * @return
    *    an unmodifiable and immutable {@link Collection} of
    *    {@link SiteVirtualHost}s, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private Collection<SiteVirtualHost> initVirtualHosts()
   throws ContentAccessException {

      List<SiteVirtualHost> vhosts = new ArrayList<SiteVirtualHost>();

      try {
         for (Element vhostXML : _xml.getUniqueChildElement("VirtualHosts").getChildElements("VirtualHost")) {
            String hostName = vhostXML.getAttribute("name");
            if (! TextUtils.isEmpty(hostName)) {
               boolean requireSSL = "true".equals(vhostXML.getAttribute("requireSSL"));
               vhosts.add(new SiteVirtualHost(this, hostName, requireSSL));
            }
         }
      } catch (ParseException cause) {
         throw new TechnicalContentAccessException(toString() + ": Failed to parse virtual hosts.", cause);
      }

      return vhosts;
   }

   /**
    * Initializes the list of pages for this site.
    *
    * @return
    *    an unmodifiable and immutable {@link Collection} of page names,
    *    never <code>null</code> and containing at least one name.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private Collection<String> initPageNames()
   throws ContentAccessException {

      List<String> pageNames = new ArrayList<String>();

      try {
         // Loop over all <Page/> elements in the Site.xml file
         for (Element pageElement : _xml.getUniqueChildElement("Pages").getChildElements("Page")) {

            // The "name" attribute must be set (not null) and must be unique
            String name = pageElement.getAttribute("name");
            if (TextUtils.isEmpty(name)) {
               throw new ParseException(toString() + ": Found a <Page/> element without a \"name\" attribute.");
            } else if (pageNames.contains(name)) {
               throw new ParseException(toString() + ": Found two <Page/> elements with the same \"name\" attribute: \"" + name + "\".");
            }

            // Add
            pageNames.add(name);
         }

      // Convert a parsing exception to a ContentAccessException
      } catch (ParseException cause) {
         throw new TechnicalContentAccessException(toString() + ": Failed to parse page names.", cause);
      }

      // Check postconditions
      if (pageNames.size() < 1) {
         throw new TechnicalContentAccessException(toString() + ": No pages found.");
      }

      return Collections.unmodifiableList(pageNames);
   }

   /**
    * Initializes the realms by querying the database accessors.
    *
    * @return
    *    an unmodifiable and immutable {@link Collection} of
    *    {@link Realm}s, indexed by name, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private Map<String,Realm> initRealms()
   throws ContentAccessException {

      Map<String,Realm> realms = new HashMap<String,Realm>();

      try {
         Element realmsElement = _xml.getOptionalChildElement("Realms");
         if (realmsElement != null) {
            for (Element realmElement : realmsElement.getChildElements("Realm")) {
               Realm      realm = Realm.createRealm(this, realmElement);
               String realmName = realm.getName();
               if (realms.get(realmName) != null) {
                  throw new TechnicalContentAccessException(toString() + ": Found duplicate realm name \"" + realmName + "\".");
               }
               realms.put(realmName, realm);
            }
         }

      } catch (ParseException cause) {
         throw new TechnicalContentAccessException(toString() + ": Failed to parse realms for site.", cause);
      }

      return Collections.unmodifiableMap(realms);
   }

   /**
    * Initializes the form definitions by querying the database accessors.
    *
    * @return
    *    an unmodifiable and immutable {@link Collection} of
    *    {@link FormDefinition}s, indexed by name, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private Map<String,FormDefinition> initFormDefinitions()
   throws ContentAccessException {

      Map<String,FormDefinition> forms = new HashMap<String,FormDefinition>();

      try {

         // Get the list of form names
         Element xml = getXMLFile(DatabaseType.CONTENTDB, "content/PreprocessedForms.xml").getXML();
         for (Element formXML : xml.getChildElements("Form")) {

            String formName = formXML.getAttribute("name");

            // Ignore duplicate form definitions
            if (forms.keySet().contains(formName)) {
               Utils.logWarning(toString() + ": Found duplicate definition for form \"" + formName + "\". Using first occurrence.");
               continue; // TODO: Review
            }

            // Associate the form name with its definition object
            forms.put(formName, new PageFormDefinition(this, formXML));
         }
      } catch (Exception cause) {
         throw new TechnicalContentAccessException(toString() + ": Failed to initialize the form definitions.", cause);
      }

      return Collections.unmodifiableMap(forms);
   }

   /**
    * Initializes the form storage for this site.
    *
    * @return
    *    the {@link SiteFormStorage}, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private SiteFormStorage initFormStorage()
   throws ContentAccessException {
      return new SiteFormStorage(this);
   }

   /**
    * Initializes the random number generators by querying the database
    * accessors.
    *
    * @return
    *    an unmodifiable and immutable {@link Collection} of
    *    {@link RandomNumberGenerator}s,
    *    indexed by name, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private Map<String,RandomNumberGenerator> initRandomNumberGenerators()
   throws ContentAccessException {

      Map<String,RandomNumberGenerator> rngs = new HashMap<String,RandomNumberGenerator>();

      try {
         Element randomsElement = _xml.getOptionalChildElement("Randoms");

         if (randomsElement != null) {
            for (Element randomElement : randomsElement.getChildElements("Random")) {
               RandomNumberGenerator generator = RandomNumberGenerator.createFromXML(_name, randomElement);
               rngs.put(generator.getName(), generator);
            }
         }
      } catch (Exception cause) {
         throw new TechnicalContentAccessException(toString() + ": Failed to initialize random number generators.", cause);
      }

      return Collections.unmodifiableMap(rngs);
   }

   /**
    * Initializes the random number generators by querying the database
    * accessors.
    *
    * @return
    *    an unmodifiable and immutable {@link Collection} of
    *    {@link RandomNumberGenerator}s,
    *    indexed by name, never <code>null</code>.
    *
    * @throws ContentAccessException
    *    in case of a content retrieval error.
    */
   private Map<String,Element> initPageDefXML()
   throws ContentAccessException {

      HashMap<String,Element> map = new HashMap<String,Element>();

      // Search through the Page elements to find the match
      try {
         Element pagesElement = _xml.getUniqueChildElement("Pages");
         if (pagesElement != null) {
            for (Element child : pagesElement.getChildElements("Page")) {
               map.put(child.getAttribute("name"), child);
            }
         }
      } catch (Exception cause) {
         throw new TechnicalContentAccessException(toString() + ": Failed to initialize page definitions XML.", cause);
      }

      return map;
   }

   public DataHub getDataHub() {
      return _dataHub;
   }

   /**
    * Retrieves the name of this site.
    *
    * @return
    *    the site name, never <code>null</code> and always valid.
    */
   @Override
   public String getName() {
      return _name;
   }

   /**
    * Retrieves the definition of this site, as XML.
    *
    * @return
    *    the definition of this site, as XML,
    *    never <code>null</code>.
    */
   public Element getDefinitionXML() {
      return _xml;
   }

   /**
    * Retrieves the definition of a specific page, as XML.
    *
    * @param pageName
    *    the name of the page, cannot be <code>null</code>.
    *
    * @return
    *    the definition of the page, part of the site definition XML,
    *    or <code>null</code> if no definition was found for a page with the
    *    specified name (which indicates there is no page by that name in this
    *    site).
    *
    * @throws IllegalArgumentException
    *    if <code>pageName == null</code>.
    */
   public Element getPageDefinitionXML(String pageName)
   throws IllegalArgumentException {

      // Check preconditions
      MandatoryArgumentChecker.check("pageName", pageName);

      return _pageDefXML.get(pageName);
   }

   /**
    * Returns all properties defined for this site.
    *
    * @return
    *    a {@link PropertyReader} that will never change,
    *    never <code>null</code>.
    */
   public PropertyReader getProperties() {
      return _properties;
   }

   /**
    * Gets the value of a property.
    *
    * @param name
    *    the property name, cannot be <code>null</code>.
    *
    * @return
    *    the property value, or <code>null</code> if it is unset.
    *
    * @throws IllegalArgumentException
    *    if <code>name == null</code>.
    */
   public String getProperty(String name) throws IllegalArgumentException {
      MandatoryArgumentChecker.check("name", name);
      return _properties.get(name);
   }

   /**
    * Retrieves all associated virtual hosts.
    *
    * @return
    *    a {@link Collection} containing all {@link SiteVirtualHost}s for this
    *    site, never <code>null</code> (although the collection can be empty).
    */
   public Collection<SiteVirtualHost> getVirtualHosts() {
      return _vhosts;
   }

   /**
    * Retrieves the names of all pages inside this site.
    *
    * @return
    *    the {@link Collection} of names, never <code>null</code> and never
    *    empty (size is at least 1).
    *
    * @see #getPreprocessedPageXML(String)
    */
   public Collection<String> getPageNames() {
      return _pageNames;
   }

   /**
    * Retrieves the preprocessed XML for the specified page.
    *
    * @param pageName
    *    the page name (see {@link #getPageNames()}),
    *    cannot be <code>null</code>.
    *
    * @return
    *    the preprocessed page XML {@link Element},
    *    or <code>null</code> if there is no page by that name in this site.
    *
    * @throws IllegalArgumentException
    *    if <code>pageName == null</code>.
    */
   public Element getPreprocessedPageXML(String pageName)
   throws IllegalArgumentException {
      MandatoryArgumentChecker.check("pageName", pageName);
      try {
         return _pageNames.contains(pageName)
              ? getXMLFile(DatabaseType.CONTENTDB, "content/" + pageName + ".PreprocessedPage.xml").getXML()
              : null;
      } catch (ContentAccessException cause) {
         throw Utils.logProgrammingError(Site.class.getName(), "getPreprocessedPageXML(String)", Site.class.getName(), null, "Preprocessed XML for page \"" + pageName + "\" cannot be loaded and/or parsed.", cause);
      }
   }

   /**
    * Retrieves all realms.
    *
    * @return
    *    a {@link Collection} containing all {@link Realm}s for this
    *    site, never <code>null</code> (although the collection can be empty).
    */
   public Collection<Realm> getRealms() {
      return _realmsByName.values();
   }

   /**
    * Retrieves a realm by name.
    *
    * @param name
    *    the name of the {@link Realm}, cannot be <code>null</code>.
    *
    * @return
    *    the matching {@link Realm},
    *    or <code>null</code> if no match is found.
    *
    * @throws IllegalArgumentException
    *    if <code>name == null</code>.
    */
   public Realm getRealm(String name)
   throws IllegalArgumentException {
      return _realmsByName.get(name);
   }

   /**
    * Retrieves all form definitions.
    *
    * @return
    *    a {@link Collection} containing all {@link FormDefinition}s for this
    *    site, never <code>null</code> (although the collection can be empty).
    */
   public Collection<FormDefinition> getFormDefinitions() {
      return _formsByName.values();
   }

   /**
    * Retrieves all form names.
    *
    * @return
    *    a {@link Collection} containing all form names, as character
    *    {@link String} objects,
    *    never <code>null</code> (although the collection can be empty).
    */
   public Collection<String> getFormNames() {
      return _formsByName.keySet();
   }

   /**
    * Retrieves a form definition by name.
    *
    * @param name
    *    the name of the {@link FormDefinition}, cannot be <code>null</code>.
    *
    * @return
    *    the matching {@link FormDefinition},
    *    or <code>null</code> if no match is found.
    *
    * @throws IllegalArgumentException
    *    if <code>name == null</code>.
    */
   public FormDefinition getFormDefinition(String name)
   throws IllegalArgumentException {
      return _formsByName.get(name);
   }

   /**
    * Retrieves the form storage for this site.
    *
    * @return
    *    the {@link SiteFormStorage} for this site,
    *    never <code>null</code>.
    */
   public SiteFormStorage getFormStorage() {
      return _formStorage;
   }

   /**
    * Retrieves all random number generators
    *
    * @return
    *    a {@link Collection} containing all {@link RandomNumberGenerator}s
    *    for this site, never <code>null</code>
    *    (although the collection can be empty).
    */
   public Collection<RandomNumberGenerator> getRandomNumberGenerators() {
      return _rngsByName.values();
   }

   /**
    * Retrieves a random number generator by name.
    *
    * @param name
    *    the name of the {@link RandomNumberGenerator},
    *    cannot be <code>null</code>.
    *
    * @return
    *    the matching {@link RandomNumberGenerator},
    *    or <code>null</code> if no match is found.
    *
    * @throws IllegalArgumentException
    *    if <code>name == null</code>.
    */
   public RandomNumberGenerator getRandomNumberGenerator(String name)
   throws IllegalArgumentException {
      return _rngsByName.get(name);
   }

   /**
    * Gets the structure of this site.
    *
    * @return
    *    the {@link SiteStructure}, never <code>null</code>.
    */
   public SiteStructure getStructure() {
      return _structure;
   }

   @Override
   public String translatePath(String path)
   throws IllegalArgumentException {
      Assertions.assertValidPath(path);
      return _dataHub.translatePath(_name + '/' + path);
   }

   @Override
   public boolean equals(Object obj) {
      if (! (obj instanceof Site)) {
         return false;
      }

      Site that = (Site) obj;

      return _dataHub.equals(that._dataHub)
          &&    _name.equals(that._name   );
   }

   @Override
   public int hashCode() {
      return _dataHub.hashCode() ^ _name.hashCode();
   }

   @Override
   public String toString() {
      return "Site \"" + _name + '"';
   }
}
