package it.uniroma3.mat.extendedset.transactions;

import it.uniroma3.mat.extendedset.IndexedSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Extended version of {@link TransactionSet} for extracting subset of
 * assignments filtered by transaction or item attributes
 * 
 * @author Alessandro Colantonio
 * @version $Id$

 * @param <A> transaction and item attribute type
 * @param <T> transaction type
 * @param <I> item type
 */
public class ExtendedTransactionSet<A, T extends Entity<A>, I extends Entity<A>> extends TransactionSet<T, I> {
	/**
	 * @see TransactionSet#TransactionSet(Collection)
	 */
	public ExtendedTransactionSet(Collection<Pair<T, I>> as) {
		super(as);
	}

	/**
	 * @see TransactionSet#TransactionSet(Collection, Collection, boolean)
	 */
	public ExtendedTransactionSet(Collection<T> allTransactions, Collection<I> allItems, boolean compressed) {
		super(allTransactions, allItems, compressed);
	}

	/**
	 * @see TransactionSet#TransactionSet(int, int, boolean)
	 */
	public ExtendedTransactionSet(int maxTransactionCount, int maxItemCount, boolean compressed) {
		super(maxTransactionCount, maxItemCount, compressed);
	}

	/**
	 * @see TransactionSet#TransactionSet(Object[][])
	 */
	public ExtendedTransactionSet(T[][] as) {
		super(as);
	}

	/**
	 * Filter pairs by transaction attribute
	 * 
	 * @param index
	 *            attribute index
	 * @param value
	 *            attribute value
	 * @return filtered pairs
	 */
	public TransactionSet<T, I> filterByTransacionAttribute(int index, A value) {
		// identify involved transactions
		IndexedSet<T> transactions = allTransactions().empty();
		for (T u : allTransactions()) 
			if (u.attributes().get(index).contains(value))
				transactions.add(u);
		
		// clear the not-involved part of the set
		return subSet(transactions, null);
	}

	/**
	 * Filter pairs by item attribute
	 * 
	 * @param index
	 *            attribute index
	 * @param value
	 *            attribute value
	 * @return filtered pairs
	 */
	public TransactionSet<T, I> filterByItemAttribute(int index, A value) {
		// identify involved items
		IndexedSet<I> items = allItems().empty();
		for (I p : allItems()) 
			if (p.attributes().get(index).contains(value))
				items.add(p);
		
		// clear the not-involved part of the set
		return subSet(null, items);
	}

	/**
	 * Gets transaction attributes that are involved with some pairs. It returns
	 * <code>null</code> if there are no attribute involved.
	 * 
	 * @param index
	 *            attribute index
	 * @return empty collection if not present
	 */
	public Set<A> involvedTransactionAttributes(int index) {
		Set<A> res = new HashSet<A>();
		for (T u : involvedTransactions())
			res.addAll(u.attributes().get(index));
		return res.isEmpty() ? null : res;
	}

	/**
	 * Gets item attributes that are involved with some pairs. It returns
	 * <code>null</code> if there are no attribute involved.
	 * 
	 * @param index
	 *            attribute index
	 * @return empty collection if not present
	 */
	public Set<A> involvedItemAttributes(int index) {
		Set<A> res = new HashSet<A>();
		for (I p : involvedItems())
			res.addAll(p.attributes().get(index));
		return res.isEmpty() ? null : res;
	}
}
