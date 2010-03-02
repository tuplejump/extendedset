package it.uniroma3.mat.extendedset.transactions;

import it.uniroma3.mat.extendedset.ConciseSet;
import it.uniroma3.mat.extendedset.ExtendedSet;
import it.uniroma3.mat.extendedset.FastSet;
import it.uniroma3.mat.extendedset.IndexedSet;
import it.uniroma3.mat.extendedset.ExtendedSet.ExtendedIterator;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class can be used to represent a set of transactions.
 * <p>
 * It can be seen as a binary matrix, where rows are transactions, columns are
 * the items involved with each transaction.
 * <p>
 * The matrix is internally represented by a bitmap defined by putting rows
 * (transactions) in sequence. All the provided constructors allows to specify
 * if the bitmap should be compressed (via {@link ConciseSet}) or plain (via
 * {@link FastSet}).
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @param <T>
 *            transaction type
 * @param <I>
 *            item type
 * 
 * @see Pair
 * @see ConciseSet
 * @see FastSet
 * 
 */
public class PairSet<T, I> extends AbstractSet<Pair<T, I>> {
	/** transaction-item pair indices */
	private /*final*/ ExtendedSet<Integer> indices;
	
	/** all possible transactions */
	private final IndexedSet<T> allTransactions;
	
	/** all possible items */
	private final IndexedSet<I> allItems;

	/** cached number of all transactions */
	private final int maxTransactionCount;

	/** cached number of all items */
	private final int maxItemCount;

	/**
	 * Initializes the set by specifying all possible transactions and items.
	 * 
	 * @param transactions
	 *            collection of <i>all</i> possible transactions. The specified
	 *            order will be preserved within when iterating over the
	 *            {@link PairSet} instance.
	 * @param items
	 *            collection of <i>all</i> possible items. The specified order
	 *            will be preserved within each transaction
	 *            {@link PairSet}.
	 * @param compressed
	 *            <code>true</code> if a compressed internal representation
	 *            should be used
	 */
	public PairSet(Collection<T> transactions, Collection<I> items, boolean compressed) {
		this(newEmptyTransactionSet(transactions, items, compressed));
	}

	/**
	 * Avoids to directly call the class constructor #PairSet(Collection,
	 * Collection, boolean), thus allowing for a more readable code
	 * 
	 * @param <XT>
	 *            transaction type
	 * @param <XI>
	 *            item type
	 * @param transactions
	 *            collection of <i>all</i> possible transactions. The specified
	 *            order will be preserved within when iterating over the
	 *            {@link PairSet} instance.
	 * @param items
	 *            collection of <i>all</i> possible items. The specified order
	 *            will be preserved within each transaction
	 *            {@link PairSet}.
	 * @param compressed
	 *            <code>true</code> if a compressed internal representation
	 *            should be used
	 * @return a new class instance           
	 */
	public static <XT, XI> PairSet<XT, XI> newPairSet(Collection<XT> transactions, Collection<XI> items, boolean compressed) {
		return new PairSet<XT, XI>(transactions, items, compressed);
	}

	/**
	 * Initializes the set by specifying the <i>number</i> of all possible
	 * transactions and items
	 * <p>
	 * In this case, transactions and items are {@link Integer} ranging from 0
	 * to the given sizes <code>- 1</code>
	 * 
	 * @param maxTransactionCount
	 *            maximum number of transactions
	 * @param maxItemCount
	 *            maximum number of items
	 * @param compressed
	 *            <code>true</code> if a compressed internal representation
	 *            should be used
	 * @throws ClassCastException
	 *             when <code>T</code> and <code>I</code> do <i>not</i> equal to
	 *             {@link Integer}.
	 */
	@SuppressWarnings("unchecked")
	public PairSet(int maxTransactionCount, int maxItemCount, boolean compressed) {
		this((PairSet<T, I>) newEmptyTransactionSet(maxTransactionCount, maxItemCount, compressed));
	}

	/**
	 * Avoids to directly call the class constructor #PairSet(Collection,
	 * Collection, boolean), thus allowing for a more readable code
	 * 
	 * @param maxTransactionCount
	 *            maximum number of transactions
	 * @param maxItemCount
	 *            maximum number of items
	 * @param compressed
	 *            <code>true</code> if a compressed internal representation
	 *            should be used
	 * @return a new class instance           
	 */
	public static PairSet<Integer, Integer> newPairSet(int maxTransactionCount, int maxItemCount, boolean compressed) {
		return new PairSet<Integer, Integer>(maxTransactionCount, maxItemCount, compressed);
	}

	/**
	 * Converts a generic collection of transaction-item pairs to a
	 * {@link PairSet} instance.
	 * 
	 * @param pairs
	 *            collection of {@link Pair} instances
	 */
	public PairSet(Collection<? extends Pair<T, I>> pairs) {
		this(newPairSet(pairs));
	}

	/**
	 * Converts an array of transaction-item pairs to a {@link PairSet}
	 * instance.
	 * <p>
	 * In this case, the types <code>T</code> and <code>I</code> must be the
	 * same, namely transaction and items must be of the same type
	 * 
	 * @param pairs
	 *            array of transaction-item pairs
	 * @throws ClassCastException
	 *             when <code>T</code> and <code>I</code> are <i>not</i> of the
	 *             same type
	 */
	@SuppressWarnings("unchecked")
	public PairSet(T[][] pairs) {
		this((PairSet<T, I>) newPairSet(pairs));
	}

	/**
	 * Converts a generic collection of {@link Pair} to an
	 * {@link PairSet} instance
	 * 
	 * @param <XT>
	 *            type of transactions
	 * @param <XI>
	 *            type of items
	 * @param as
	 *            collection of {@link Pair} instances
	 * @return the new {@link PairSet} instance, or the parameter if it is
	 *         an instance of {@link PairSet}
	 */
	@SuppressWarnings("unchecked")
	public static <XT, XI> PairSet<XT, XI> newPairSet(Collection<? extends Pair<XT, XI>> as) {
		if (as instanceof PairSet)
			return (PairSet<XT, XI>) as;

		/*
		// compute transactions and items, in the same order of the collection
		HashMap<XT, Integer> transactionToFreq = new HashMap<XT, Integer>();
		HashMap<XI, Integer> itemToFreq = new HashMap<XI, Integer>();
		Integer frequency;
		for (Pair<XT, XI> a : as) {
			frequency = transactionToFreq.get(a.transaction);
			transactionToFreq.put(a.transaction, frequency == null ? 1 : frequency + 1);
			
			frequency = itemToFreq.get(a.item);
			itemToFreq.put(a.item, frequency == null ? 1 : frequency + 1);
		}
		
		// sort transactions and items by descending frequencies
		List<Entry<XT, Integer>> sortedTransactionFreqs = new ArrayList<Entry<XT, Integer>>(transactionToFreq.entrySet());
		List<Entry<XI, Integer>> sortedItemFreqs = new ArrayList<Entry<XI, Integer>>(itemToFreq.entrySet());
		Collections.sort(sortedTransactionFreqs, new Comparator<Entry<XT, Integer>>() {
			@Override
			public int compare(Entry<XT, Integer> o1, Entry<XT, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		Collections.sort(sortedItemFreqs, new Comparator<Entry<XI, Integer>>() {
			@Override
			public int compare(Entry<XI, Integer> o1, Entry<XI, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		
		// final list of transactions and items
		List<XT> sortedTransactions = new ArrayList<XT>(transactionToFreq.size());
		List<XI> sortedItems = new ArrayList<XI>(itemToFreq.size());
		for (Entry<XT, Integer> e : sortedTransactionFreqs) 
			sortedTransactions.add(e.getKey());
		for (Entry<XI, Integer> e : sortedItemFreqs) 
			sortedItems.add(e.getKey());

		// add pairs to the final result, in the same order of the collection
		PairSet<XT, XI> res = new PairSet<XT, XI>(sortedTransactions, sortedItems, true);
		for (Pair<XT, XI> a : as) 
			res.add(a);
		*/

		Set<XT> ts = new LinkedHashSet<XT>();
		Set<XI> is = new LinkedHashSet<XI>();
		for (Pair<XT, XI> a : as) {
			ts.add(a.transaction);
			is.add(a.item);
		}
		
		// add pairs to the final result, in the same order of the collection
		PairSet<XT, XI> res = new PairSet<XT, XI>(ts, is, true);
		for (Pair<XT, XI> a : as) 
			res.add(a);
		
		return res;
	}

	/**
	 * Convert a generic array of transaction-item pairs to an
	 * {@link PairSet} instance
	 * 
	 * @param <X>
	 *            type of transactions and items
	 * @param pairs
	 *            array of transaction-item pairs
	 * @return the new {@link PairSet} instance
	 */
	public static <X> PairSet<X, X> newPairSet(X[][] pairs) {
		if (pairs == null || pairs[0].length != 2)
			throw new IllegalArgumentException();
		
		Set<Pair<X, X>> as = new HashSet<Pair<X, X>>(pairs.length);
		for (int i = 0; i < pairs.length; i++) 
			as.add(new Pair<X, X>(pairs[i][0], pairs[i][1]));
		
		return newPairSet(as);
	}

	/**
	 * Creates an empty instance of {@link PairSet}
	 */
	private static <XT, XI> PairSet<XT, XI> newEmptyTransactionSet(
			Collection<XT> transactions, Collection<XI> items, boolean compressed) {
		if (transactions == null || items == null)
			throw new NullPointerException();
		
		// all transactions
		IndexedSet<XT> allTransactions;
		if (transactions instanceof IndexedSet<?>)
			allTransactions = ((IndexedSet<XT>) transactions).unmodifiable();
		else
			allTransactions = new IndexedSet<XT>(transactions, compressed).universe().unmodifiable();

		// all items
		IndexedSet<XI> allItems; 
		if (items instanceof IndexedSet<?>)
			allItems = ((IndexedSet<XI>) items).unmodifiable();
		else
			allItems = new IndexedSet<XI>(items, compressed).universe().unmodifiable();
		
		// empty index set
		ExtendedSet<Integer> indices = compressed ? new ConciseSet() : new FastSet();
		
		// final pair set
		return new PairSet<XT, XI>(
				allTransactions, 
				allItems, 
				allTransactions.size(), 
				allItems.size(), 
				indices);
	}
	
	/**
	 * Creates an empty instance of {@link PairSet}
	 */
	private static PairSet<Integer, Integer> newEmptyTransactionSet(
			int maxTransactionCount, int maxItemCount, boolean compressed) {
		return newEmptyTransactionSet(
				new IndexedSet<Integer>(0, maxTransactionCount - 1, compressed).universe(),
				new IndexedSet<Integer>(0, maxItemCount - 1, compressed).universe(),
				compressed);
	}

	/**
	 * Shallow-copy constructor
	 */
	private PairSet(PairSet<T, I> as) {
		allTransactions = as.allTransactions;
		allItems = as.allItems;
		maxTransactionCount = as.maxTransactionCount;
		maxItemCount = as.maxItemCount;
		indices = as.indices;
	}

	/**
	 * Shallow-copy constructor
	 */
	private PairSet(
			IndexedSet<T> allTransactions, 
			IndexedSet<I> allItems,
			int maxTransactionCount,
			int maxItemCount,
			ExtendedSet<Integer> indices) {
		this.allTransactions = allTransactions;
		this.allItems = allItems;
		this.indices = indices;
		this.maxTransactionCount = maxTransactionCount;
		this.maxItemCount = maxItemCount;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PairSet<T, I> clone() {
//		return new TransactionSet<T, I>(
//				allTransactions, 
//				allItems, 
//				maxTransactionCount, 
//				maxItemCount, 
//				indices.clone());
		try {
			PairSet<T, I> cloned = (PairSet<T, I>) super.clone();
			cloned.indices = indices.clone();
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	/**
	 * Returns the pair corresponding to the given index
	 * 
	 * @param index
	 *            index calculated as <code>transaction * maxItemCount + item</code>
	 * @return the pair corresponding to the given index
	 */
	public final Pair<T, I> getPair(int index) {
		return new Pair<T, I>(allTransactions.absoluteGet(index / maxItemCount), allItems.absoluteGet(index % maxItemCount));
	}

	/**
	 * Returns the index corresponding to the given transaction-item pair
	 * 
	 * @param transaction
	 *            the transaction of the pair
	 * @param item
	 *            the item of the pair
	 * @return the index corresponding to the given pair
	 * @see #indexOf(Pair) 
	 */
	public final int indexOf(T transaction, I item) {
		return allTransactions.absoluteIndexOf(transaction) * maxItemCount + allItems.absoluteIndexOf(item);
	}
	
	/**
	 * Returns the index corresponding to the given transaction-item pair
	 * 
	 * @param p
	 *            the transaction-item pair
	 * @return the index corresponding to the given pair 
	 * @see #indexOf(Object, Object) 
	 */
	public final int indexOf(Pair<T, I> p) {
		return indexOf(p.transaction, p.item);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(Pair<T, I> e) {
		return add(e.transaction, e.item);
	}

	/**
	 * Adds a single transaction-item pair
	 * 
	 * @param transaction
	 *            the transaction of the pair
	 * @param item
	 *            the item of the pair
	 * @return <code>true</code> if the set has been changed
	 */
	public boolean add(T transaction, I item) {
		return indices.add(indexOf(transaction, item));
	}

	/**
	 * Add the pairs obtained from the Cartesian product of transactions
	 * and items
	 * 
	 * @param transactionSet
	 *            collection of transactions
	 * @param itemSet
	 *            collection of items
	 * @return <code>true</code> if the set set has been changed
	 */
	public boolean addAll(Collection<T> transactionSet, Collection<I> itemSet) {
		if (transactionSet == null || transactionSet.isEmpty())
			return false;
		if (itemSet == null || itemSet.isEmpty())
			return false;

		boolean m = false;
		for (T u : transactionSet)
			for (I p : itemSet)
				m |= add(u, p);
		return m;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		indices.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(Object o) {
		return o instanceof Pair<?, ?>
				&& contains(((Pair<T, I>) o).transaction, ((Pair<T, I>) o).item);
	}

	/**
	 * Checks if the given transaction-item pair is contained within the set
	 * 
	 * @param transaction
	 *            the transaction of the pair
	 * @param item
	 *            the item of the pair
	 * @return <code>true</code> if the given transaction-item pair is contained
	 *         within the set
	 */
	public boolean contains(T transaction, I item) {
		return indices.contains(indexOf(transaction, item));
	}
	
	/**
	 * Checks if the pairs obtained from the Cartesian product of
	 * transactions and items are contained
	 * 
	 * @param transactionSet
	 *            collection of transactions
	 * @param itemSet
	 *            collection of items
	 * @return <code>true</code> if the pairs set set has been changed
	 */
	public boolean containsAll(Collection<T> transactionSet, Collection<I> itemSet) {
		if (transactionSet == null || transactionSet.isEmpty())
			return false;
		if (itemSet == null || itemSet.isEmpty())
			return false;

		for (T u : transactionSet)
			for (I p : itemSet) 
				if (!contains(u, p)) 
					return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return indices.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<Pair<T, I>> iterator() {
		return new Iterator<Pair<T, I>>() {
			private final Iterator<Integer> itr = indices.iterator();

			/** {@inheritDoc} */
			@Override
			public boolean hasNext() {
				return itr.hasNext();
			}

			/** {@inheritDoc} */
			@Override
			public Pair<T, I> next() {
				return getPair(itr.next());
			}

			/** {@inheritDoc} */
			@Override
			public void remove() {
				itr.remove();
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object o) {
		return o instanceof Pair<?, ?> 
				&& remove(((Pair<T, I>) o).transaction, ((Pair<T, I>) o).item);
	}

	/**
	 * Removes a single transaction-item pair
	 * 
	 * @param transacion
	 *            the transaction of the pair
	 * @param item
	 *            the item of the pair
	 * @return <code>true</code> if the pair set has been changed
	 */
	public boolean remove(T transacion, I item) {
		return indices.remove(indexOf(transacion, item));
	}

	/**
	 * Removes the pairs obtained from the Cartesian product of transactions and
	 * items
	 * 
	 * @param transactionSet
	 *            collection of transactions
	 * @param itemSet
	 *            collection of items
	 * @return <code>true</code> if the set set has been changed
	 */
	public boolean removeAll(Collection<T> transactionSet, Collection<I> itemSet) {
		if (transactionSet == null || transactionSet.isEmpty())
			return false;
		if (itemSet == null || itemSet.isEmpty())
			return false;

		boolean m = false;
		for (T u : transactionSet)
			for (I p : itemSet)
				m |= remove(u, p);
		return m;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return indices.size();
	}

	/**
	 * Gets the set of all possible transactions that can be contained within
	 * the set
	 * 
	 * @return the set of all possible transactions that can be contained within
	 *         the set
	 */
	public IndexedSet<T> allTransactions() {
		return allTransactions;
	}

	/**
	 * Gets the set of all possible items that can be contained within each
	 * transaction
	 * 
	 * @return the set of all possible items that can be contained within each
	 *         transaction
	 */ 
	public IndexedSet<I> allItems() {
		return allItems;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return indices.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof PairSet<?, ?>))
			return false;
		PairSet<?, ?> other = (PairSet<?, ?>) obj;
		return allTransactions == other.allTransactions && allItems == other.allItems
				&& indices.equals(other.indices);
	}

	/**
	 * Lists all items contained within a given transaction
	 * 
	 * @param transaction
	 *            the given transaction
	 * @return items contained within the given transaction
	 */
	public IndexedSet<I> itemsOf(T transaction) {
		// index range to scan
		int indexOfFirst = allTransactions.absoluteIndexOf(transaction) * maxItemCount;
		int indexOfLast = indexOfFirst + maxItemCount - 1;
		
		// final result
		IndexedSet<I> res = allItems.empty();

		// index iterator
		ExtendedIterator<Integer> itr = indices.iterator();
		itr.skipAllBefore(indexOfFirst);
		Integer next;
		while (itr.hasNext() && (next = itr.next()) <= indexOfLast) 
			res.indices().add(next - indexOfFirst);
		return res;
	}

	/**
	 * Lists all transactions involved with a specified item
	 * 
	 * @param item
	 *            the given item
	 * @return transactions involved with a specified item
	 */
	public IndexedSet<T> transactionsOf(I item) {
		// index range to scan
		int indexOfFirst = allItems.absoluteIndexOf(item);
		int indexOfLast = indexOfFirst + (maxTransactionCount - 1) * maxItemCount;
		
		// final result
		IndexedSet<T> res = allTransactions.empty();

		// index iterator
		ExtendedIterator<Integer> itr = indices.iterator();
		itr.skipAllBefore(indexOfFirst);
		int next;
		while (itr.hasNext() && ((next = itr.next()) <= indexOfLast)) {
			int shift = (next - indexOfFirst) % maxItemCount;
			if (shift == 0)
				res.indices().add(next / maxItemCount);
			itr.skipAllBefore(next + maxItemCount - shift);
		}
		return res;
	}

	/**
	 * Gets the set of transactions in {@link #allTransactions()} that contains
	 * at least one item
	 * 
	 * @return the set of transactions in {@link #allTransactions()} that
	 *         contains at least one item
	 */
	public IndexedSet<T> involvedTransactions() {
		IndexedSet<T> inv = allTransactions.empty();
		for (Integer ai : indices)
			inv.indices().add(ai / maxItemCount);
		return inv;
	}

	/**
	 * Gets the set of items in {@link #allItems()} that are contained in at
	 * least one transaction
	 * 
	 * @return the set of items in {@link #allItems()} that are contained in at
	 *         least one transaction
	 */
	public IndexedSet<I> involvedItems() {
		IndexedSet<I> inv = allItems.empty();
		for (Integer ai : indices) 
			inv.indices().add(ai % maxItemCount);
		return inv;
	}

	/**
	 * Get the i-th transaction-item pair of the set
	 * 
	 * @param index
	 *            position of the transaction-item pair
	 * @return the i-th pair of the set
	 */
	public Pair<T, I> get(int index) {
		return getPair(indices.get(index));
	}

	/**
	 * Gets the matrix representation of the transaction set
	 * 
	 * @return the matrix representation of the transaction set
	 */
	public String debugInfo() {
		StringBuilder s = new StringBuilder();
		
		s.append("possible transactions: ");
		s.append(allTransactions);
		s.append('\n');
		s.append("possible items: ");
		s.append(allItems);
		s.append('\n');

		s.append("pairs:\n");
		s.append('+');
		for (int p = 0; p < maxItemCount; p++) 
			s.append('-');
		s.append("+\n");

		for (int u = 0; u < maxTransactionCount; u++) {
			s.append('|');
			for (int p = 0; p < maxItemCount; p++) {
				if (indices.contains(u * maxItemCount + p))
					s.append('*');
				else
					s.append(' ');
			}
			s.append("|\n");
		}
		
		s.append('+');
		for (int p = 0; p < maxItemCount; p++) 
			s.append('-');
		s.append('+');
		
		s.append('\n');
		s.append(indices.debugInfo());

		return s.toString();
	}

	/**
	 * Computes the compression factor of the bitmap-based representation of the
	 * pair collection (1 means not compressed)
	 * 
	 * @return the compression factor
	 */
	public double bitmapCompressionRatio() {
		return indices.bitmapCompressionRatio();
	}

	/**
	 * Computes the compression factor of the array-based representation of the
	 * pair collection (1 means not compressed)
	 * 
	 * @return the compression factor
	 */	
	public double collectionCompressionRatio() {
		return indices.collectionCompressionRatio();
	}

	/**
	 * Returns the set of indices. Modifications to this set are reflected to
	 * this {@link PairSet} instance. Trying to perform operation on
	 * out-of-bound indices will throw an {@link IllegalArgumentException}
	 * exception.
	 * 
	 * @return the index set
	 * @see #getPair(int)
	 * @see #indexOf(Pair)
	 * @see #indexOf(Object, Object)
	 */
	public ExtendedSet<Integer> indices() {
		return indices.headSet(maxTransactionCount * maxItemCount);
	}

	/**
	 * Extracts a subset represented by a certain range of transactions and
	 * items, according to the ordering provided by {@link #allTransactions()}
	 * and {@link #allItems()}.
	 * 
	 * @param fromTransaction
	 *            the first transaction of the range (if <code>null</code> it
	 *            represents the first one)
	 * @param toTransaction
	 *            the last transaction of the range (if <code>null</code> it
	 *            represents the last one)
	 * @param fromItem
	 *            the first item of the range (if <code>null</code> it
	 *            represents the first one)
	 * @param toItem
	 *            the last item of the range (if <code>null</code> it represents
	 *            the last one)
	 * @return the specified subset
	 */
	public PairSet<T, I> subSet(T fromTransaction, T toTransaction, I fromItem, I toItem) {
		// final set
		PairSet<T, I> res = empty();
		
		// trivial case
		if (fromTransaction == null && toTransaction == null && fromItem == null && toItem == null)
			return clone();
		
		// transaction range
		int fromTransactionIndex = fromTransaction == null ? 0 : allTransactions.absoluteIndexOf(fromTransaction);
		int toTransactionIndex = toTransaction == null ? maxTransactionCount - 1 : allTransactions.absoluteIndexOf(toTransaction);
		
		// item range
		if (fromItem == null && toItem == null) {
			// all items are involved
			res.indices.fill(fromTransactionIndex * maxItemCount, (toTransactionIndex + 1) * maxItemCount - 1);
		} {
			// identify the item subset
			int fromItemIndex = fromItem == null ? 0 : allItems.absoluteIndexOf(fromItem);
			int toItemIndex = toItem == null ? maxItemCount - 1 : allItems.absoluteIndexOf(toItem);
			
			// identify indices
			for (int ui = fromTransactionIndex; ui <= toTransactionIndex; ui++) {
				int firstItemOfTransaction = ui * maxItemCount;
				res.indices.fill(firstItemOfTransaction + fromItemIndex, firstItemOfTransaction + toItemIndex);
			}
		}

		// remove out-of-range pairs
		res.indices.retainAll(indices);
		return res;
	}

	/**
	 * Extracts a subset represented by a collection of transactions and items
	 * 
	 * @param involvedTransactions
	 *            involved transactions (if <code>null</code>, it represents all
	 *            transactions in {@link #allTransactions()})
	 * @param involvedItems
	 *            involved items (if <code>null</code>, it represents all items
	 *            in {@link #allItems()})
	 * @return all the transaction-item pairs that represent the specified
	 *         subset
	 */
	public PairSet<T, I> subSet(Collection<T> involvedTransactions, Collection<I> involvedItems) {
		// final set of pairs
		PairSet<T, I> res = empty();

		// trivial case
		if (involvedTransactions == null && involvedItems == null) 
			return clone();
		
		// identify indices
		if (involvedTransactions == null) {
			for (int ui = 0; ui < maxTransactionCount; ui++) 
				for (I p : involvedItems) 
					res.indices.add(ui * maxItemCount + allItems.absoluteIndexOf(p));
		} else if (involvedItems == null) {
			for (T u : involvedTransactions) {
				int first = allTransactions.absoluteIndexOf(u) * maxItemCount;
				res.indices.fill(first, first + maxItemCount - 1);
			}
		} else {
			for (T u : involvedTransactions) 
				for (I p : involvedItems) 
					res.add(u, p);
		}

		// remove out-of-range pairs
		res.indices.retainAll(indices);
		return res;
	}

	/**
	 * Creates an empty set with the same set of possible transactions and items
	 * 
	 * @return the empty set
	 */
	public PairSet<T, I> empty() {
		return new PairSet<T, I>(
				allTransactions, 
				allItems, 
				maxTransactionCount,
				maxItemCount,
				indices.empty());
	}
	
	/**
	 * Test procedure
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PairSet<String, Integer> m = new PairSet<String, Integer>(
				Arrays.asList("T1", "T2", "T3", "T4"), 
				Arrays.asList(100, 200, 300), 
				true);
		m.add("T3", 200);
		m.add("T1", 100);
		m.add("T2", 100);
		m.add("T2", 200);
		m.add("T4", 300);
		m.add("T4", 200);
		System.out.println(m);
		System.out.println(m.debugInfo());
		
		System.out.println("Position 3: " + m.get(2));
		System.out.println("Transaction T2: " + m.itemsOf("T2"));
		System.out.println("Item 200: " + m.transactionsOf(200));
		
		System.out.println(m.subSet("T1", "T2", 100, 200).debugInfo());
		System.out.println(m.subSet(Arrays.asList("T1", "T3"), Arrays.asList(100, 200)).debugInfo());
		
		System.out.println(m.involvedItems());
		System.out.println(m.involvedTransactions());
	}

}
