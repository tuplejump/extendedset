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
	 * 
	 * @return the list of properties, or <code>null</code> when the object does
	 *         not have any property
	 * @throws IndexOutOfBoundsException
	 *             if the provided index is not valid.
	 */
	public List<Set<A>> attributes();
}
