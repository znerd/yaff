// See the COPYRIGHT file for copyright and license information
package org.znerd.yaff;

/**
 * A subcontext for data. Effectively: a <code>DataContext</code> that is not a
 * <code>DataHub</code>
 *
 * @author <a href="mailto:ernst@ernstdehaan.com">Ernst de Haan</a>
 */
public abstract class SubDataContext extends DataContext {

   //-------------------------------------------------------------------------
   // Constructors
   //-------------------------------------------------------------------------

   /**
    * Constructs a new <code>SubDataContext</code>.
    */
   protected SubDataContext() {
      // empty
   }


   //-------------------------------------------------------------------------
   // Methods
   //-------------------------------------------------------------------------

   /**
    * Returns the name of this object.
    *
    * @return
    *    the name, never <code>null</code> and always a valid name for this
    *    kind of object.
    */
   public abstract String getName();
}
