/* 
 * (c) 2010 Alessandro Colantonio
 * <mailto:colanton@mat.uniroma3.it>
 * <http://ricerca.mat.uniroma3.it/users/colanton>
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 


package it.uniroma3.mat.extendedset.pairs;

import it.uniroma3.mat.extendedset.AbstractExtendedSet;
import it.uniroma3.mat.extendedset.ConciseSet;
import it.uniroma3.mat.extendedset.FastSet;
import it.uniroma3.mat.extendedset.IntegerSet;
import it.uniroma3.mat.extendedset.ExtendedSet;
import it.uniroma3.mat.extendedset.IndexedSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A binary matrix internally represented by a bitmap.
 * <p>
 * This class can be used to represent a set of transactions, where each
 * transaction is a set of items. Rows are transactions, columns are the items
 * involved with each transaction.
 * <p>
 * The matrix is internally represented by a bitmap defined by putting rows
 * (transactions) in sequence. All the provided constructors allows to specify
 * if the bitmap should be compressed (via {@link ConciseSet}) or plain (via
 * {@link FastSet}).
 * <p>
 * <b>NOTE:</b> this class provides two important methods:
 * {@link #transactionsOf(Object)} to get a column of the matrix, and
 * {@link #itemsOf(Object)} to get a row of the matrix. Since the matrix is
 * internally organized as a bitmap where rows (transactions) are put in
 * sequence, {@link #itemsOf(Object)} is much faster than
 * {@link #transactionsOf(Object)}.
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
//TODO: complete, make it compliant with ExtendedSet, and override methods of AbstractExtendedSet
public class PairSet<T, I> extends AbstractExtendedSet<Pair<T, I>> implements Cloneable {
	/** transaction-item pair indices */
	private final ExtendedSet<Integer> indices;
	
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
		this(newEmptyPairSet(transactions, items, compressed));
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
		this((PairSet<T, I>) newEmptyPairSet(maxTransactionCount, maxItemCount, compressed));
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
	 * @param compressed
	 *            <code>true</code> if a compressed internal representation
	 *            should be used
	 */
	public PairSet(Collection<? extends Pair<T, I>> pairs, boolean compressed) {
		this(newPairSet(pairs, compressed));
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
	 * @param compressed
	 *            <code>true</code> if a compressed internal representation
	 *            should be used
	 * @throws ClassCastException
	 *             when <code>T</code> and <code>I</code> are <i>not</i> of the
	 *             same type
	 */
	@SuppressWarnings("unchecked")
	public PairSet(T[][] pairs, boolean compressed) {
		this((PairSet<T, I>) newPairSet(pairs, compressed));
	}

	/**
	 * Converts a generic collection of {@link Pair} to an
	 * {@link PairSet} instance
	 * 
	 * @param <XT>
	 *            type of transactions
	 * @param <XI>
	 *            type of items
	 * @param ps
	 *            collection of {@link Pair} instances
	 * @param compressed
	 *            <code>true</code> if a compressed internal representation
	 *            should be used
	 * @return the new {@link PairSet} instance, or the parameter if it is
	 *         an instance of {@link PairSet}
	 */
	@SuppressWarnings("unchecked")
	public static <XT, XI> PairSet<XT, XI> newPairSet(Collection<? extends Pair<XT, XI>> ps, boolean compressed) {
		if (ps instanceof PairSet)
			return (PairSet<XT, XI>) ps;

		PairSet<XT, XI> res;
		
		if (!compressed) {
			// identify all possible transactions and items
			Set<XT> ts = new LinkedHashSet<XT>();
			Set<XI> is = new LinkedHashSet<XI>();
			for (Pair<XT, XI> a : ps) {
				ts.add(a.transaction);
				is.add(a.item);
			}
			
			// add pairs to the final result, in the same order of the collection
			res = new PairSet<XT, XI>(ts, is, compressed);
			for (Pair<XT, XI> a : ps) 
				res.add(a);
		} else {
			// identify all possible transactions and items
			final Map<XT, Integer> ts = new LinkedHashMap<XT, Integer>();
			final Map<XI, Integer> is = new LinkedHashMap<XI, Integer>();
			int ti = 0, ii = 0;
			for (Pair<XT, XI> p : ps) {
				if (!ts.containsKey(p.transaction))
					ts.put(p.transaction, ti++);
				if (!is.containsKey(p.item))
					is.put(p.item, ii++);
			}

			// sort the collection according to the sets of transactions and items
			List<Pair<XT, XI>> sorted = new ArrayList<Pair<XT, XI>>(ps);
			Collections.sort(sorted, new Comparator<Pair<XT, XI>>() {
				@Override
				public int compare(Pair<XT, XI> o1, Pair<XT, XI> o2) {
					int r = ts.get(o1.transaction) - ts.get(o2.transaction);
					if (r == 0)
						r = is.get(o1.item) - is.get(o2.item);
					return r;
				}
			});

			// add pairs to the final result, according to the identified order 
			res = new PairSet<XT, XI>(ts.keySet(), is.keySet(), compressed);
			for (Pair<XT, XI> p : sorted) 
				res.add(p);
		}
		
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
	 * @param compressed
	 *            <code>true</code> if a compressed internal representation
	 *            should be used
	 * @return the new {@link PairSet} instance
	 */
	public static <X> PairSet<X, X> newPairSet(X[][] pairs, boolean compressed) {
		if (pairs == null || pairs[0].length != 2)
			throw new IllegalArgumentException();
		
		List<Pair<X, X>> as = new ArrayList<Pair<X, X>>(pairs.length);
		for (int i = 0; i < pairs.length; i++) 
			as.add(new Pair<X, X>(pairs[i][0], pairs[i][1]));
		
		return newPairSet(as, compressed);
	}

	/**
	 * Creates an empty instance of {@link PairSet}
	 */
	private static <XT, XI> PairSet<XT, XI> newEmptyPairSet(
			Collection<XT> transactions, Collection<XI> items, boolean compressed) {
		if (transactions == null || items == null)
			throw new NullPointerException();
		
		// all transactions
		IndexedSet<XT> allTransactions;
		if (transactions instanceof IndexedSet<?>)
			allTransactions = ((IndexedSet<XT>) transactions); //.unmodifiable();
		else
			allTransactions = new IndexedSet<XT>(transactions, compressed).universe(); //.unmodifiable();

		// all items
		IndexedSet<XI> allItems; 
		if (items instanceof IndexedSet<?>)
			allItems = ((IndexedSet<XI>) items); //.unmodifiable();
		else
			allItems = new IndexedSet<XI>(items, compressed).universe(); //.unmodifiable();
		
		// empty index set
		ExtendedSet<Integer> indices = new IntegerSet(compressed ? new ConciseSet() : new FastSet());
		
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
	private static PairSet<Integer, Integer> newEmptyPairSet(
			int maxTransactionCount, int maxItemCount, boolean compressed) {
		return newEmptyPairSet(
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
	@Override
	public PairSet<T, I> clone() {
		// NOTE: do not use super.clone() since it is 10 times slower!
		return new PairSet<T, I>(
				allTransactions, allItems, maxTransactionCount, maxItemCount, 
				indices.clone());
	}

	/**
	 * Checks if the given collection is a instance of {@link PairSet} with
	 * the same index mappings
	 * 
	 * @param c
	 *            collection to check
	 * @return <code>true</code> if the given collection is a instance of
	 *         {@link PairSet} with the same index mappings
	 */
	@SuppressWarnings("unchecked")
	private boolean hasSameIndices(Collection<?> c) {
		return c != null 
				&& (c instanceof PairSet) 
				&& (allTransactions == ((PairSet) c).allTransactions)
				&& (allItems == ((PairSet) c).allItems);
	}

	/**
	 * Returns the pair corresponding to the given index
	 * 
	 * @param index
	 *            index calculated as <code>transaction * maxItemCount + item</code>
	 * @return the pair corresponding to the given index
	 */
	//TODO rename, since it is conflicting with ExtendedSet methods
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
	//TODO rename, since it is conflicting with ExtendedSet methods
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
	//TODO rename, since it is conflicting with ExtendedSet methods
	@Override
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
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(Collection<? extends Pair<T, I>> c) {
		if (hasSameIndices(c))
			return indices.addAll(((PairSet) c).indices);
		return super.addAll(c);
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
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean containsAll(Collection<?> c) {
		if (hasSameIndices(c))
			return indices.containsAll(((PairSet) c).indices);
		return super.containsAll(c);
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
	public ExtendedIterator<Pair<T, I>> iterator() {
		return new ExtendedIterator<Pair<T, I>>() {
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

			@Override
			public void skipAllBefore(Pair<T, I> element) {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("TODO");
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
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean removeAll(Collection<?> c) {
		if (hasSameIndices(c))
			return indices.removeAll(((PairSet) c).indices);
		return super.removeAll(c);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean retainAll(Collection<?> c) {
		if (hasSameIndices(c))
			return indices.retainAll(((PairSet) c).indices);
		return super.retainAll(c);
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
	//TODO do the same for retainAll...
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
		while (itr.hasNext() && ((next = itr.next()) <= indexOfLast)) 
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
		ExtendedIterator<Integer> itr = indices.iterator();
		while (itr.hasNext()) {
			int t = itr.next() / maxItemCount;
			inv.indices().add(t);
			itr.skipAllBefore((t + 1) * maxItemCount);
		}
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
		ExtendedIterator<Integer> itr = indices.iterator();
		while (itr.hasNext() && (inv.size() < allItems.size()))
			inv.indices().add(itr.next() % maxItemCount);
		return inv;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pair<T, I> get(int index) {
		return getPair(indices.get(index));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
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
	 * {@inheritDoc}
	 */
	@Override
	public double bitmapCompressionRatio() {
		return indices.bitmapCompressionRatio();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
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
//		return indices.headSet(maxTransactionCount * maxItemCount);
		return indices;
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
			// identify all potential indices
			Collection<Integer> pis = new ArrayList<Integer>(involvedItems.size());
			for (I p : involvedItems) 
				pis.add(allItems.absoluteIndexOf(p));
			for (int ui = 0; ui < maxTransactionCount; ui++) 
				for (Integer pi : pis)
					res.indices.add(ui * maxItemCount + pi);
		} else if (involvedItems == null) {
			// identify all potential indices
			for (T u : involvedTransactions) {
				int first = allTransactions.absoluteIndexOf(u) * maxItemCount;
				res.indices.fill(first, first + maxItemCount - 1);
			}
		} else {
			// identify all potential indices
			Collection<Integer> pis = new ArrayList<Integer>(involvedItems.size());
			for (I p : involvedItems) 
				pis.add(allItems.absoluteIndexOf(p));
			for (T u : involvedTransactions) {
				int ui = allTransactions.absoluteIndexOf(u);
				for (Integer pi : pis)
					res.indices.add(ui * maxItemCount + pi);
			}
		}

		// remove out-of-range pairs
		res.indices.retainAll(indices);
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> empty() {
		return new PairSet<T, I>(
				allTransactions, 
				allItems, 
				maxTransactionCount,
				maxItemCount,
				indices.empty());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complement() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Comparator<? super Pair<T, I>> comparator() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO");
	}

	/**
	 * Test procedure
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PairSet<String, Integer> m = new PairSet<String, Integer>(
				Arrays.asList("T1", "T2", "T3", "T4", "T5", "T6"), 
				Arrays.asList(100, 200, 300, 400, 500, 600), 
				true);
		m.add("T3", 200);
		m.add("T6", 100);
		m.add("T2", 100);
		m.add("T2", 200);
		m.add("T4", 300);
		m.add("T4", 600);
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
