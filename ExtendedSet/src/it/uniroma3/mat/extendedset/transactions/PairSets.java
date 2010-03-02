package it.uniroma3.mat.extendedset.transactions;

import static it.uniroma3.mat.extendedset.transactions.PairSet.newPairSet;
import static it.uniroma3.mat.extendedset.util.CollectionMap.newCollectionMap;
import it.uniroma3.mat.extendedset.IndexedSet;
import it.uniroma3.mat.extendedset.util.CollectionMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * This class consists exclusively of static methods that operate on or return
 * {@link PairSet} instances.
 * <p>
 * The methods of this class all throw a {@link NullPointerException} if the
 * objects provided are <code>null</code>.
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 */
public class PairSets {
    // suppresses default constructor, ensuring non-instantiability.
    private PairSets() {/* empty */}

    /**
	 * Filter pairs by transaction attribute
	 * 
	 * @param <A>
	 *            attribute type
	 * @param <T>
	 *            transaction type
	 * @param <I>
	 *            item type
	 * @param ts
	 *            {@link PairSet} instance
	 * @param index
	 *            attribute index
	 * @param value
	 *            attribute value
	 * @return filtered pairs
	 */
	public static <A, T extends Entity<A>, I> PairSet<T, I> filterByTransactionAttribute(PairSet<T, I> ts, int index, A value) {
		// TODO: improve by directly working on indices
		
		// identify involved transactions
		IndexedSet<T> transactions = ts.allTransactions().empty();
		for (T u : ts.allTransactions()) 
			if (u.attributes().get(index).contains(value))
				transactions.add(u);
		
		// clear the not-involved part of the set
		return ts.subSet(transactions, null);
	}

	/**
	 * Filter pairs by item attribute
	 * 
	 * @param <A>
	 *            attribute type
	 * @param <T>
	 *            transaction type
	 * @param <I>
	 *            item type
	 * @param ts
	 *            {@link PairSet} instance
	 * @param index
	 *            attribute index
	 * @param value
	 *            attribute value
	 * @return filtered pairs
	 */
	public static <A, T, I extends Entity<A>> PairSet<T, I> filterByItemAttribute(PairSet<T, I> ts, int index, A value) {
		// TODO: improve by directly working on indices

		// identify involved items
		IndexedSet<I> items = ts.allItems().empty();
		for (I p : ts.allItems()) 
			if (p.attributes().get(index).contains(value))
				items.add(p);
		
		// clear the not-involved part of the set
		return ts.subSet(null, items);
	}

	/**
	 * Gets transaction attributes that are involved with some pairs. It returns
	 * <code>null</code> if there are no attribute involved.
	 * 
	 * @param <A>
	 *            attribute type
	 * @param <T>
	 *            transaction type
	 * @param <I>
	 *            item type
	 * @param ts
	 *            {@link PairSet} instance
	 * @param index
	 *            attribute index
	 * @return empty collection if not present
	 */
	public static <A, T extends Entity<A>, I> Set<A> involvedTransactionAttributes(PairSet<T, I> ts, int index) {
		Set<A> res = new HashSet<A>();
		for (T u : ts.involvedTransactions())
			res.addAll(u.attributes().get(index));
		return res.isEmpty() ? null : res;
	}

	/**
	 * Gets item attributes that are involved with some pairs. It returns
	 * <code>null</code> if there are no attribute involved.
	 * 
	 * @param <A>
	 *            attribute type
	 * @param <T>
	 *            transaction type
	 * @param <I>
	 *            item type
	 * @param ts
	 *            {@link PairSet} instance
	 * @param index
	 *            attribute index
	 * @return empty collection if not present
	 */
	public static <A, T, I extends Entity<A>> Set<A> involvedItemAttributes(PairSet<T, I> ts, int index) {
		Set<A> res = new HashSet<A>();
		for (I p : ts.involvedItems())
			res.addAll(p.attributes().get(index));
		return res.isEmpty() ? null : res;
	}

	/**
	 * Generates a compact representation of the given {@link PairSet}
	 * instance, where each transaction (item) is a collection of all
	 * transactions (items) that share the same items (transactions).
	 * 
	 * @param <T>
	 *            transaction type
	 * @param <I>
	 *            item type
	 * @param ts
	 *            {@link PairSet} instance
	 * @param compressed
	 *            <code>true</code> if a compressed internal representation
	 *            should be used
	 * @return the compact representation of the given {@link PairSet}
	 *         instance
	 */
	public static <T, I> PairSet<IndexedSet<T>, IndexedSet<I>> compact(PairSet<T, I> ts, boolean compressed) {
		// identify distinct transactions
		CollectionMap<IndexedSet<I>, T, IndexedSet<T>> itemsToTransactions = newCollectionMap(ts.allTransactions().empty());
		for (T t : ts.allTransactions()) { 
			IndexedSet<I> ii = ts.itemsOf(t);
			if (!ii.isEmpty())
				itemsToTransactions.putItem(ii, t);
		}
		List<IndexedSet<T>> transactions = sortedGroups(itemsToTransactions);
			
		// identify distinct items
		CollectionMap<IndexedSet<T>, I, IndexedSet<I>> transactionsToItems = newCollectionMap(ts.allItems().empty());
		for (I i : ts.allItems()) {
			IndexedSet<T> tt = ts.transactionsOf(i);
			if (!tt.isEmpty())
				transactionsToItems.putItem(tt, i);
		}
		List<IndexedSet<I>> items = sortedGroups(transactionsToItems);

		// final result
		PairSet<IndexedSet<T>, IndexedSet<I>> res = newPairSet(transactions, items, compressed);
		for (IndexedSet<T> t : res.allTransactions())
			for (IndexedSet<I> i : res.allItems()) 
				// check for only one representative pair
				if (ts.contains(t.last(), i.last()))
					res.add(t, i);
		return res;
	}

	/**
	 * Used by {@link #compact(PairSet, boolean)}
	 * <p>
	 * It returns all the groups (keys of the given {@link Map}) sorted by
	 * ascending number of corresponding elements (values of the given
	 * {@link Map})
	 * 
	 * @param <X>
	 *            class of type {@link IndexedSet} representing all the elements
	 *            associated to a group
	 * @param groupToElements
	 *            all the elements associated to a group
	 * @return all the groups sorted by ascending number of elements
	 */
	private static <X extends IndexedSet<?>> List<X> sortedGroups(Map<?, X> groupToElements) {
		// sort by descending frequencies
		List<Entry<?, X>> sortedGroupToElements = new ArrayList<Entry<?, X>>(groupToElements.entrySet());
		Collections.sort(sortedGroupToElements, new Comparator<Entry<?, X>>() {
			@Override
			public int compare(Entry<?, X> o1, Entry<?, X> o2) {
				return o2.getValue().size() - o1.getValue().size();
			}
		});
		List<X> groups = new ArrayList<X>(sortedGroupToElements.size());
		for (Entry<?, X> t : sortedGroupToElements) 
			groups.add(t.getValue());
		return groups;
	}
}
