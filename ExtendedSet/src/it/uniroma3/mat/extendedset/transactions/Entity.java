package it.uniroma3.mat.extendedset.transactions;

import java.util.List;
import java.util.Set;

/**
 * Interface to get transaction or item attributes
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @param <A>
 *            attribute type
 */
public interface Entity<A> {
	/**
	 * Gets the list of properties. For each property, the entity may have many
	 * values. If the object does not have any property, it returns
	 * <code>null</code>. The method {@link List#get(int)} returns an empty set
	 * if the provided index is valid but the entity has no value for the
	 * corresponding attribute, or raises {@link IndexOutOfBoundsException} if
	 * the provided index is not valid.
	 * <p>
	 * When attributes are represented by several data members of the current
	 * class, s possible way to implement the method is:
	 * <p>
	 * <pre>
	 *  public List&lt;Set&lt;A&gt;&gt; attributes() {
	 *      return attributes;
	 *  }
	 * 
	 *  private final List&lt;Set&lt;A&gt;&gt; attributes = new AbstractList&lt;Set&lt;A&gt;&gt;() {
	 *      public Set&lt;String&gt; get(int index) {
	 *          switch (index) {
	 *          case 0:
	 *              return attr1 == null ? Collections.&lt;A&gt;emptySet() : Collections.singleton(attr1);
	 *          case 1:
	 *              return attr2 == null ? Collections.&lt;A&gt;emptySet() : Collections.singleton(attr2);
	 *          ...
	 *          ...
	 *          ...	
	 *          case 9:
	 *              return attr9 == null ? Collections.&lt;A&gt;emptySet() : Collections.singleton(attr9);
	 *          default:
	 *              throw new IndexOutOfBoundsException(String.valueOf(index));
	 *          }
	 *      }
	 *      
	 *      public int size() {
	 *          return 10;
	 *      }
	 *  };
	 * </pre>
	 * 
	 * @return the list of properties, or <code>null</code> when the object does
	 *         not have any property
	 * @throws IndexOutOfBoundsException
	 *             if the provided index is not valid.
	 */
	public List<Set<A>> attributes();
}
