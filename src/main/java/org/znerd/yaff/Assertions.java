// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff;

import java.util.regex.Pattern;

import org.xins.common.MandatoryArgumentChecker;
import org.xins.common.text.TextUtils;

/**
 * Various text string check functions.
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public final class Assertions extends Object {

   //-------------------------------------------------------------------------
   // Class fields
   //-------------------------------------------------------------------------

   /**
    * Pattern for account IDs.
    */
   static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^[0-9a-f]{16}$");


   //-------------------------------------------------------------------------
   // Class functions
   //-------------------------------------------------------------------------

   /**
    * Executes an assertion in a generic manner.
    *
    * @param description
    *    a short description of what is checked,
    *    e.g. <code>"site name"</code>, <code>"file path"</code>, etc.;
    *    cannot be <code>null</code>.
    *
    * @param pattern
    *    the regular expression that the character string should match,
    *    cannot be <code>null</code>.
    *
    * @param s
    *    the character string to validate, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>description == null
    *          || pattern     == null
    *          || s           == null
    *          || ! s.matches(pattern)</code>.
    */
   public static final void executeAssertion(String description,
                                             String pattern,
                                             String s) {
      MandatoryArgumentChecker.check("description", description,
                                     "pattern",     pattern,
                                     "s",           s);
      if (! s.matches(pattern)) {
         throw new IllegalArgumentException(TextUtils.quote(s) + " is not considered a valid " + description + '.');
      }
   }

   /**
    * Checks if the specified string is considered a valid name for a concrete
    * site.
    *
    * @param s
    *    the candidate concrete site name, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid concrete site name.
    */
   public static final void assertValidSiteName(String s)
   throws IllegalArgumentException {
      executeAssertion("site name", "^[a-z][a-z0-9]*([_-][a-z0-9]+)*$", s);
   }

   /**
    * Checks if the specified string is considered a valid page name.
    *
    * <p>Examples of valid page names:
    * <ul>
    * <li><code>"Register"</code>
    * <li><code>"NewsItem_John_20091153"</code>
    * <li><code>"RegI"</code>
    * <li><code>"X"</code>
    * </ul>
    *
    * @param s
    *    the candidate page name, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid page name.
    */
   public static final void assertValidPageName(String s)
   throws IllegalArgumentException {
      executeAssertion("page name", "^[A-Z]\\w*$", s);
   }

   /**
    * Checks if the specified string is considered a valid page path.
    *
    * <p>Examples of valid page names:
    * <ul>
    * <li><code>"/register/"</code>
    * <li><code>"/register/re-ve_r12/"</code>
    * </ul>
    *
    * @param s
    *    the candidate page path, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid page path.
    */
   public static final void assertValidPagePath(String s)
   throws IllegalArgumentException {
      executeAssertion("page path", "^\\/([\\w-]+\\/)*$", s);
   }

   /**
    * Checks if the specified string is considered a valid form name.
    *
    * @param s
    *    the candidate form name, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid form name.
    */
   public static final void assertValidFormName(String s)
   throws IllegalArgumentException {
      executeAssertion("form name", "^([A-Z][a-z0-9]*)+$", s);
   }

   /**
    * Checks if the specified string is considered a valid realm name.
    *
    * @param s
    *    the candidate realm name, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid realm name.
    */
   public static final void assertValidRealmName(String s)
   throws IllegalArgumentException {
      executeAssertion("realm name", "^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$", s);
   }

   /**
    * Checks if the specified string is considered a valid account index name.
    *
    * @param s
    *    the candidate name, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid account index name.
    */
   public static final void assertValidAccountIndexName(String s)
   throws IllegalArgumentException {
      executeAssertion("realm name", "^[a-z]+$", s);
   }

   /**
    * Checks if the specified string is considered a valid reference within an
    * account index.
    *
    * @param s
    *    the candidate reference, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid account reference.
    */
   public static final void assertValidAccountRefString(String s)
   throws IllegalArgumentException {
      executeAssertion("account reference", "^[0-9a-fA-F]{40}$", s);
   }

   /**
    * Checks if the specified string is considered a valid account ID.
    *
    * @param s
    *    the candidate account ID, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid account ID.
    */
   public static final void assertValidAccountID(String s)
   throws IllegalArgumentException {
      executeAssertion("account ID", "^[0-9a-f]{16}$", s);
   }

   /**
    * Checks if the specified string is considered a valid account property
    * name.
    *
    * @param s
    *    the candidate account property name, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid account property name.
    */
   public static final void assertValidAccountPropertyName(String s)
   throws IllegalArgumentException {
      executeAssertion("account property name", "^[A-Z][A-Za-z0-9_]*$", s);
   }

   /**
    * Checks if the specified string is considered a valid file name.
    *
    * @param s
    *    the candidate file name, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid file name.
    */
   public static final void assertValidFileName(String s)
   throws IllegalArgumentException {
      executeAssertion("file name", "^[\\w-]+\\.[a-z]{2,4}$", s);
   }

   /**
    * Checks if the specified string is considered a valid file path.
    *
    * <p>Examples of valid file paths:
    * <ul>
    * <li><code>"abc.gif"</code>
    * <li><code>"abc-012-x_y/def.png"</code>
    * </ul>
    *
    * @param s
    *    the candidate path, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid file path.
    */
   public static final void assertValidFilePath(String s)
   throws IllegalArgumentException {
      executeAssertion("file path", "^[\\w@-]+(/[\\w@-]+)*(\\.[\\w]*)+$", s);
   }

   /**
    * Checks if the specified string is considered a valid path for a file or
    * directory.
    *
    * <p>Examples of valid file paths:
    * <ul>
    * <li><code>"abc.gif"</code>
    * <li><code>"abc-012-x_y/def.png"</code>
    * <li><code>"abc"</code>
    * <li><code>"abc/def"</code>
    * </ul>
    *
    * @param s
    *    the candidate path, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid path.
    */
   public static final void assertValidPath(String s)
   throws IllegalArgumentException {
      executeAssertion("path", "^[\\w@-]+(/[\\w@-]+)*(\\.[\\w]*)*$", s);
   }

   /**
    * Checks if the specified string is considered a valid path for a 
    * directory.
    *
    * <p>Examples of valid directory paths:
    * <ul>
    * <li><code>"abc"</code>
    * <li><code>"abc/def"</code>
    * </ul>
    *
    * @param s
    *    the candidate path, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid path.
    */
   public static final void assertValidDirectoryPath(String s)
   throws IllegalArgumentException {
      executeAssertion("directory path", "^[\\w-]+(/[\\w-]+)*\\/?$", s);
   }

   /**
    * Checks if the specified string is considered a valid host name.
    *
    * <p>Examples of valid host names:
    * <ul>
    * <li><code>"rtr88.nl"</code>
    * <li><code>"www.rtr-88.com"</code>
    * <li><code>"bb.fr"</code>
    * </ul>
    *
    * @param s
    *    the candidate host name, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid host name.
    */
   public static final void assertValidHostName(String s)
   throws IllegalArgumentException {
      executeAssertion("host name", "^[\\w-]+(\\.[\\w-]+)+$", s);
   }

   /**
    * Checks if the specified string is considered a web URL.
    *
    * <p>Examples of valid web URLs:
    * <ul>
    * <li><code>"http://www.something.com/"</code>
    * <li><code>"https://bla.tv/bla-gg/bla/bla"</code>
    * </ul>
    *
    * @param s
    *    the candidate web URL, cannot be <code>null</code>.
    *
    * @throws IllegalArgumentException
    *    if <code>s == null</code>
    *    or if it is not a valid web URL.
    */
   public static final void assertValidWebURL(String s)
   throws IllegalArgumentException {
      executeAssertion("web URL", "^http(s)?:\\/\\/[\\w-]+(\\.[\\w-]+)+(:\\d{1,5})?\\/.*$", s);
   }


   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>Assertions</code> instance.
    */
   private Assertions() {
      // empty
   }
}
