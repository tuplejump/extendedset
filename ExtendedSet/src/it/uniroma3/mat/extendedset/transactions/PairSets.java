package it.uniroma3.mat.extendedset.transactions;

import static it.uniroma3.mat.extendedset.transactions.PairSet.newPairSet;
import static it.uniroma3.mat.extendedset.util.CollectionMap.newCollectionMap;
import it.uniroma3.mat.extendedset.IndexedSet;
import it.uniroma3.mat.extendedset.util.CollectionMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
	 * Generates a new {@link GroupAndFirst} instance from the given
	 * transaction/item set
	 * 
	 * @param <X>
	 *            <code>T</code> if transaction, <code>I</code> if item
	 * @param group
	 *            transaction/item set
	 * @return the new {@link GroupAndFirst} instance
	 */
	private static <XT, XI> Group<XT, XI> group(IndexedSet<XT> group, IndexedSet<XI> related) {
		return new Group<XT, XI>(group, related);
	}
	
	/**
	 * @param <XT>
	 * @param <XI>
	 */
	public static class Group<XT, XI> {
		/** transaction/item grouping */
		public final IndexedSet<XT> group;
		
		/** first element of the grouping */
		private final XT first;

		/** all items/transactions associated to the transaction/item grouping */
		public final IndexedSet<XI> related;
		
		// cannot instantiate outside this class...
		private Group(IndexedSet<XT> group, IndexedSet<XI> related) {
			this.group = group;
			this.related = related;
			this.first = group.first();
		}
		
		/** {@inheritDoc} */  
		@Override public int hashCode() {
			return first.hashCode();
		}
		
		/** {@inheritDoc} */
		@Override public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			return first.equals(((Group<?, ?>) obj).first);
		}
		
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
	public static <T, I> PairSet<Group<T, I>, Group<I, T>> compact(PairSet<T, I> ts, boolean compressed) {
		// identify all transactions associated to an item, and items associated to a transaction
		CollectionMap<T, I, IndexedSet<I>> transactionToItems = newCollectionMap(ts.allItems().empty());
		CollectionMap<I, T, IndexedSet<T>> itemToTransactions = newCollectionMap(ts.allTransactions().empty());
		for (Pair<T, I> p : ts) {
			transactionToItems.putItem(p.transaction, p.item);
			itemToTransactions.putItem(p.item, p.transaction);
		}
		
		// identify distinct transactions and items
		CollectionMap<IndexedSet<T>, I, IndexedSet<I>> transactionsToItemGroup = newCollectionMap(ts.allItems().empty());
		CollectionMap<IndexedSet<I>, T, IndexedSet<T>> itemsToTransactionGroup = newCollectionMap(ts.allTransactions().empty());
		transactionsToItemGroup.mapValueToKeys(itemToTransactions);
		itemsToTransactionGroup.mapValueToKeys(transactionToItems);

		// identify representative transactions and items
		Map<T, Group<T, I>> transactionToGroup = new HashMap<T, Group<T, I>>();
		for (T t: transactionToItems.keySet()) {
			IndexedSet<I> items = transactionToItems.get(t);
			transactionToGroup.put(t, group(itemsToTransactionGroup.get(items), items));
		}
		Map<I, Group<I, T>> itemToGroup = new HashMap<I, Group<I, T>>(); 
		for (I i: itemToTransactions.keySet()) {
			IndexedSet<T> transactions = itemToTransactions.get(i);
			itemToGroup.put(i, group(transactionsToItemGroup.get(transactions), transactions));
		}
		// final result
		// NOTE: first transactions and first items within each group are in the
		// same order of the original matrix
		PairSet<Group<T, I>, Group<I, T>> res = newPairSet(transactionToGroup.values(), itemToGroup.values(), compressed);
		for (Pair<T, I> pair : ts) {
			// add the pair, only once for each group-pair
			Group<T, I> gt = transactionToGroup.get(pair.transaction);
			Group<I, T> gi = itemToGroup.get(pair.item);
			if (pair.transaction.equals(gt.first) && pair.item.equals(gi.first))
				res.add(gt, gi);
		}
		
		return res;
	}
}
