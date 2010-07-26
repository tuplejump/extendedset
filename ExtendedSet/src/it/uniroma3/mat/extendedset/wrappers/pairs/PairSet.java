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


package it.uniroma3.mat.extendedset.wrappers.pairs;

import it.uniroma3.mat.extendedset.AbstractExtendedSet;
import it.uniroma3.mat.extendedset.ExtendedSet;
import it.uniroma3.mat.extendedset.intset.ConciseSet;
import it.uniroma3.mat.extendedset.intset.FastSet;
import it.uniroma3.mat.extendedset.intset.IntSet;
import it.uniroma3.mat.extendedset.wrappers.IndexedSet;
import it.uniroma3.mat.extendedset.wrappers.LongSet;
import it.uniroma3.mat.extendedset.wrappers.LongSet.ExtendedLongIterator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
public class PairSet<T, I> extends AbstractExtendedSet<Pair<T, I>> implements Cloneable, java.io.Serializable {
	/** generated serial ID */
	private static final long serialVersionUID = 7902458899512666217L;
	
	/** binary matrix */
	//TODO: mettere al posto di indices due CollectionMap di tutti gli utenti dei permessi e tutti i permessi degli utenti!!!
	private final LongSet indices;

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
	 * @param indices
	 *            {@link IntSet} instance used to internally represent the set
	 * @param transactions
	 *            collection of <i>all</i> possible transactions. The specified
	 *            order will be preserved within when iterating over the
	 *            {@link PairSet} instance.
	 * @param items
	 *            collection of <i>all</i> possible items. The specified order
	 *            will be preserved within each transaction {@link PairSet}.
	 */
	public PairSet(LongSet indices, Collection<T> transactions, Collection<I> items) {
		this(newEmptyPairSet(indices, transactions, items));
	}

	/**
	 * Avoids to directly call the class constructor #PairSet(Collection,
	 * Collection, boolean), thus allowing for a more readable code
	 * 
	 * @param <XT>
	 *            transaction type
	 * @param <XI>
	 *            item type
	 * @param indices
	 *            {@link IntSet} instance used to internally represent the set
	 * @param transactions
	 *            collection of <i>all</i> possible transactions. The specified
	 *            order will be preserved within when iterating over the
	 *            {@link PairSet} instance.
	 * @param items
	 *            collection of <i>all</i> possible items. The specified order
	 *            will be preserved within each transaction
	 *            {@link PairSet}.
	 * @return a new class instance           
	 */
	public static <XT, XI> PairSet<XT, XI> newPairSet(LongSet indices, Collection<XT> transactions, Collection<XI> items) {
		return new PairSet<XT, XI>(indices, transactions, items);
	}

	/**
	 * Converts a generic collection of transaction-item pairs to a
	 * {@link PairSet} instance.
	 * 
	 * @param indices
	 *            {@link IntSet} instance used to internally represent the set
	 * @param pairs
	 *            collection of {@link Pair} instances
	 */
	public PairSet(LongSet indices, Collection<? extends Pair<T, I>> pairs) {
		this(newPairSet(indices, pairs));
	}

	/**
	 * Converts an array of transaction-item pairs to a {@link PairSet}
	 * instance.
	 * <p>
	 * In this case, the types <code>T</code> and <code>I</code> must be the
	 * same, namely transaction and items must be of the same type
	 * 
	 * @param indices
	 *            {@link IntSet} instance used to internally represent the set
	 * @param pairs
	 *            array of transaction-item pairs
	 * @throws ClassCastException
	 *             when <code>T</code> and <code>I</code> are <i>not</i> of the
	 *             same type
	 */
	@SuppressWarnings("unchecked")
	public PairSet(LongSet indices, T[][] pairs) {
		this((PairSet<T, I>) newPairSet(indices, pairs));
	}

	/**
	 * Converts a generic collection of {@link Pair} to an
	 * {@link PairSet} instance
	 * 
	 * @param <XT>
	 *            type of transactions
	 * @param <XI>
	 *            type of items
	 * @param indices
	 *            {@link IntSet} instance used to internally represent the set
	 * @param ps
	 *            collection of {@link Pair} instances
	 * @return the new {@link PairSet} instance, or the parameter if it is
	 *         an instance of {@link PairSet}
	 */
	@SuppressWarnings("unchecked")
	public static <XT, XI> PairSet<XT, XI> newPairSet(LongSet indices, Collection<? extends Pair<XT, XI>> ps) {
		if (ps == null)
			throw new RuntimeException("null pair set");
		if (ps.isEmpty())
			throw new RuntimeException("empty pair set");
		
		if (ps instanceof PairSet)
			return (PairSet<XT, XI>) ps;

		PairSet<XT, XI> res;
		
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
				int r = ts.get(o1.transaction).compareTo(ts.get(o2.transaction));
				if (r == 0)
					r = is.get(o1.item).compareTo(is.get(o2.item));
				return r;
			}
		});

		// add pairs to the final result, according to the identified order 
		res = new PairSet<XT, XI>(indices, ts.keySet(), is.keySet());
		for (Pair<XT, XI> p : sorted) 
			res.add(p);
		
		return res;
	}

	/**
	 * Convert a generic array of transaction-item pairs to an
	 * {@link PairSet} instance
	 * 
	 * @param <X>
	 *            type of transactions and items
	 * @param indices
	 *            {@link IntSet} instance used to internally represent the set
	 * @param pairs
	 *            array of transaction-item pairs
	 * @return the new {@link PairSet} instance
	 */
	public static <X> PairSet<X, X> newPairSet(LongSet indices, X[][] pairs) {
		if (pairs == null || pairs[0].length != 2)
			throw new IllegalArgumentException();
		
		List<Pair<X, X>> as = new ArrayList<Pair<X, X>>(pairs.length);
		for (int i = 0; i < pairs.length; i++) 
			as.add(new Pair<X, X>(pairs[i][0], pairs[i][1]));
		
		return newPairSet(indices, as);
	}

	/**
	 * Creates an empty instance of {@link PairSet}
	 */
	private static <XT, XI> PairSet<XT, XI> newEmptyPairSet(
			LongSet indices,
			Collection<XT> transactions, 
			Collection<XI> items) {
		if (transactions == null || items == null)
			throw new NullPointerException();
		
		// all transactions
		IndexedSet<XT> allTransactions;
		//TODO unmodifiable
		if (transactions instanceof IndexedSet<?>) {
			allTransactions = ((IndexedSet<XT>) transactions); //.unmodifiable();
		} else {
			allTransactions = new IndexedSet<XT>(indices.emptyBlock(), transactions).universe(); //.unmodifiable();
		}

		// all items
		IndexedSet<XI> allItems; 
		//TODO unmodifiable
		if (items instanceof IndexedSet<?>) {
			allItems = ((IndexedSet<XI>) items); //.unmodifiable();
		} else {
			allItems = new IndexedSet<XI>(indices.emptyBlock(), items).universe(); //.unmodifiable();
		}
			
		// final pair set
		return new PairSet<XT, XI>(
				allTransactions, 
				allItems, 
				allTransactions.size(), 
				allItems.size(), 
				indices);
	}
	
	/**
	 * Shallow-copy constructor
	 */
	protected PairSet(PairSet<T, I> as) {
		allTransactions = as.allTransactions;
		allItems = as.allItems;
		maxTransactionCount = as.maxTransactionCount;
		maxItemCount = as.maxItemCount;
		
		indices = as.indices;
	}

	/**
	 * Shallow-copy constructor
	 */
	protected PairSet(
			IndexedSet<T> allTransactions, 
			IndexedSet<I> allItems,
			int maxTransactionCount,
			int maxItemCount,
			LongSet indices) {
		this.allTransactions = allTransactions;
		this.allItems = allItems;
		this.maxTransactionCount = maxTransactionCount;
		this.maxItemCount = maxItemCount;

		this.indices = indices;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> clone() {
		// NOTE: do not use super.clone() since it is 10 times slower!
		return new PairSet<T, I>(
				allTransactions, 
				allItems, 
				maxTransactionCount, 
				maxItemCount, 
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
	public final Pair<T, I> indexToPair(long index) {
		return new Pair<T, I>(
				allTransactions.absoluteGet((int) (index / maxItemCount)), 
				allItems.absoluteGet((int) (index % maxItemCount)));
	}

	/**
	 * Returns the index corresponding to the given transaction-item pair
	 * 
	 * @param transaction
	 *            the transaction of the pair
	 * @param item
	 *            the item of the pair
	 * @return the index corresponding to the given pair
	 * @see #pairToIndex(Pair) 
	 */
	public final long pairToIndex(T transaction, I item) {
		return allTransactions.absoluteIndexOf(transaction).longValue() * maxItemCount + allItems.absoluteIndexOf(item).longValue();
	}
	
	/**
	 * Returns the index corresponding to the given transaction-item pair
	 * 
	 * @param p
	 *            the transaction-item pair
	 * @return the index corresponding to the given pair 
	 * @see #pairToIndex(Object, Object) 
	 */
	public final long pairToIndex(Pair<T, I> p) {
		return pairToIndex(p.transaction, p.item);
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
		return indices.add(pairToIndex(transaction, item));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends Pair<T, I>> c) {
		return indices.addAll(convert(c).indices);
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
		return o != null 
				&& o instanceof Pair<?, ?>
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
		Integer t = allTransactions.absoluteIndexOf(transaction);
		if (t == null)
			return false;
		Integer i = allItems.absoluteIndexOf(item);
		if (i == null)
			return false;
		long index = t.longValue() * maxItemCount + i.longValue();
		return indices.contains(index);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		return indices.containsAll(convert(c).indices);
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
			ExtendedLongIterator itr = indices.longIterator();
			@Override public boolean hasNext() {return itr.hasNext();}
			@Override public Pair<T, I> next() {return indexToPair(itr.next());}
			@Override public void skipAllBefore(Pair<T, I> element) {itr.skipAllBefore(pairToIndex(element));}
			@Override public void remove() {itr.remove();}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<Pair<T, I>> descendingIterator() {
		return new ExtendedIterator<Pair<T, I>>() {
			ExtendedLongIterator itr = indices.descendingLongIterator();
			@Override public boolean hasNext() {return itr.hasNext();}
			@Override public Pair<T, I> next() {return indexToPair(itr.next());}
			@Override public void skipAllBefore(Pair<T, I> element) {itr.skipAllBefore(pairToIndex(element));}
			@Override public void remove() {itr.remove();}
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
	 * @param transaction
	 *            the transaction of the pair
	 * @param item
	 *            the item of the pair
	 * @return <code>true</code> if the pair set has been changed
	 */
	public boolean remove(T transaction, I item) {
		return indices.remove(pairToIndex(transaction, item));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		return indices.removeAll(convert(c).indices);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		return indices.retainAll(convert(c).indices);
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
	 * Retains the pairs obtained from the Cartesian product of transactions and
	 * items
	 * 
	 * @param transactionSet
	 *            collection of transactions
	 * @param itemSet
	 *            collection of items
	 * @return <code>true</code> if the set set has been changed
	 */
	public boolean retainAll(Collection<T> transactionSet, Collection<I> itemSet) {
		if (transactionSet == null || transactionSet.isEmpty()) {
			clear();
			return false;
		}
		if (itemSet == null || itemSet.isEmpty()) {
			clear();
			return false;
		}

		boolean m = false;
		for (Pair<T, I> p : this)
			if (!transactionSet.contains(p.transaction) || !itemSet.contains(p.item)) {
				remove(p);
				m = true;
			}
		return m;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return (int) indices.size();
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
		final PairSet<?, ?> other = (PairSet<?, ?>) obj;
		return hasSameIndices(other) && indices.equals(other.indices);
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
		long indexOfFirst = allTransactions.absoluteIndexOf(transaction).longValue() * maxItemCount;
		long indexOfLast = indexOfFirst + maxItemCount - 1;

		// final result
		IndexedSet<I> res = allItems.empty();
		
		// index iterator
		ExtendedLongIterator itr = indices.longIterator();
		itr.skipAllBefore(indexOfFirst);
		Long next;
		while (itr.hasNext() && ((next = Long.valueOf(itr.next())).longValue() <= indexOfLast)) 
			res.indices().add((int) (next.longValue() - indexOfFirst));

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
		long indexOfFirst = allItems.absoluteIndexOf(item).longValue();
		long indexOfLast = indexOfFirst + (long) (maxTransactionCount - 1) * maxItemCount;
		
		// final result
		IndexedSet<T> res = allTransactions.empty();

		// index iterator
		ExtendedLongIterator itr = indices.longIterator();
		itr.skipAllBefore(indexOfFirst);
//		Set<Integer> is = new HashSet<Integer>();
		IntSet is = res.indices();
		long next;
		while (itr.hasNext() && ((next = itr.next()) <= indexOfLast)) {
			long shift = (next - indexOfFirst) % maxItemCount;
			if (shift == 0L)
//				is.add(Integer.valueOf((int) (next / maxItemCount)));
				is.add((int) (next / maxItemCount));
			itr.skipAllBefore(next + maxItemCount - shift);
		}
//		res.indices().addAll(res.indices().convert(is));
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
		ExtendedLongIterator itr = indices.longIterator();
//		Set<Integer> is = new HashSet<Integer>();
		IntSet is = inv.indices();
		while (itr.hasNext()) {
			int t = (int) (itr.next() / maxItemCount);
//			is.add(Integer.valueOf(t));
			is.add(t);
			itr.skipAllBefore((long) (t + 1) * maxItemCount);
		}
//		inv.indices().addAll(inv.indices().convert(is));
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
		ExtendedLongIterator itr = indices.longIterator();
		while (itr.hasNext() && (inv.size() < allItems.size()))
			inv.indices().add((int) (itr.next() % maxItemCount));
		return inv;
	}

	/**
	 * Gets the <code>i</code><sup>th</sup> element of the set
	 * 
	 * @param index
	 *            position of the element in the sorted set
	 * @return the <code>i</code><sup>th</sup> element of the set
	 * @throws IndexOutOfBoundsException
	 *             if <code>i</code> is less than zero, or greater or equal to
	 *             {@link #size()}
	 */
	public Pair<T, I> get(long index) {
		return indexToPair(indices.get(index));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pair<T, I> get(int index) {
		return get((long) index);
	}

	/**
	 * Provides position of element within the set.
	 * <p>
	 * It returns -1 if the element does not exist within the set.
	 * 
	 * @param el
	 *            element of the set
	 * @return the element position
	 */
	public long longIndexOf(Pair<T, I> el) {
		return indices.indexOf(pairToIndex(el));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int indexOf(Pair<T, I> e) {
		return (int) longIndexOf(e);
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
				if (contains(allTransactions.absoluteGet(u), allItems.absoluteGet(p)))
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
		s.append("indices: " + indices.debugInfo());
		
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
	 * @see #indexToPair(long)
	 * @see #pairToIndex(Pair)
	 * @see #pairToIndex(Object, Object)
	 */
	public LongSet indices() {
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
		int fromTransactionIndex = fromTransaction == null ? 0 : allTransactions.absoluteIndexOf(fromTransaction).intValue();
		int toTransactionIndex = toTransaction == null ? maxTransactionCount - 1 : allTransactions.absoluteIndexOf(toTransaction).intValue();
		
		// item range
		if (fromItem == null && toItem == null) {
			// all items are involved
			long start = (long) fromTransactionIndex * maxItemCount;
			long end = (long) (toTransactionIndex + 1) * maxItemCount - 1L;
			res.indices.fill(start, end);
		} {
			// identify the item subset
			int fromItemIndex = fromItem == null ? 0 : allItems.absoluteIndexOf(fromItem).intValue();
			int toItemIndex = toItem == null ? maxItemCount - 1 : allItems.absoluteIndexOf(toItem).intValue();
			
			// identify indices
			for (int ui = fromTransactionIndex; ui <= toTransactionIndex; ui++) {
				long firstItemOfTransaction = (long) ui * maxItemCount;
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

		// trivial cases
		if ((involvedTransactions == null || involvedTransactions == allTransactions)
				&& (involvedItems == null || involvedItems == allItems)) 
			return clone();
		
		// identify indices
		if (involvedTransactions == null) {
			// identify all potential indices
			Collection<Integer> pis = new ArrayList<Integer>(involvedItems.size());
			for (I p : involvedItems) 
				pis.add(allItems.absoluteIndexOf(p));
			for (int ui = 0; ui < maxTransactionCount; ui++) 
				for (Integer pi : pis)
					res.indices().add((long) ui * maxItemCount + pi.longValue());
		} else if (involvedItems == null) {
			// identify all potential indices
			for (T u : involvedTransactions) {
				long first = allTransactions.absoluteIndexOf(u).longValue() * maxItemCount;
				res.indices().fill(first, first + maxItemCount - 1);
			}
		} else {
			// identify all potential indices
			Collection<Integer> pis = new ArrayList<Integer>(involvedItems.size());
			for (I p : involvedItems) 
				pis.add(allItems.absoluteIndexOf(p));
			for (T u : involvedTransactions) {
				long ui = allTransactions.absoluteIndexOf(u).longValue();
				for (Integer pi : pis)
					res.indices().add(ui * maxItemCount + pi.longValue());
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
		indices.complement();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Comparator<? super Pair<T, I>> comparator() {
		return new Comparator<Pair<T, I>>() {
			@Override
			public int compare(Pair<T, I> o1, Pair<T, I> o2) {
				return Long.valueOf(pairToIndex(o1)).compareTo(Long.valueOf(pairToIndex(o2)));
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PairSet<T, I> convert(Collection<?> c) {
		if (c == null)
			return empty();

		// useless to convert...
		if (hasSameIndices(c))
			return (PairSet<T, I>) c;
		
		long[] indxs = new long[c.size()];
		int i = 0;
		for (Pair<T, I> p : (Collection<Pair<T,I>>) c)
			indxs[i++] = pairToIndex(p);
		Arrays.sort(indxs);	

		PairSet<T, I> res = empty();
		for (long l : indxs) 
			res.indices.add(l);
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PairSet<T, I> convert(Object... e) {
		return (PairSet<T, I>) super.convert(e);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear(Pair<T, I> from, Pair<T, I> to) {
		indices.clear(pairToIndex(from), pairToIndex(to));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int complementSize() {
		return (int) indices.complementSize();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> complemented() {
		return new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount, indices.complemented());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> difference(Collection<? extends Pair<T, I>> other) {
		return other == null ? clone() : new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount, 
				indices.difference(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(Collection<? extends Pair<T, I>> other) {
		return other == null || indices.containsAny(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAtLeast(Collection<? extends Pair<T, I>> other, int minElements) {
		return other != null && !other.isEmpty() && indices.containsAtLeast(convert(other).indices, minElements);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int differenceSize(Collection<? extends Pair<T, I>> other) {
		return other == null ? (int) size() : (int) indices.differenceSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fill(Pair<T, I> from, Pair<T, I> to) {
		indices.fill(pairToIndex(from), pairToIndex(to));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flip(Pair<T, I> e) {
		indices.flip(pairToIndex(e));
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PairSet<T, I> subSet(Pair<T, I> fromElement, Pair<T, I> toElement) {
		return (PairSet<T, I>) super.subSet(fromElement, toElement);
//		return new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount,  
//				indices.subSet(pairToIndex(fromElement), pairToIndex(toElement)));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PairSet<T, I> headSet(Pair<T, I> toElement) {
		return (PairSet<T, I>) super.headSet(toElement);
//		return new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount, 
//				indices.headSet(pairToIndex(toElement)));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PairSet<T, I> tailSet(Pair<T, I> fromElement) {
		return (PairSet<T, I>) super.tailSet(fromElement);
//		return new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount,  
//				indices.tailSet(pairToIndex(fromElement)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> intersection(Collection<? extends Pair<T, I>> c) {
		return c == null ? empty() : new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount,  
				indices.intersection(convert(c).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends PairSet<T, I>> powerSet() {
		return (List<? extends PairSet<T, I>>) super.powerSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends PairSet<T, I>> powerSet(int min, int max) {
		return (List<? extends PairSet<T, I>>) super.powerSet(min, max);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> symmetricDifference(Collection<? extends Pair<T, I>> other) {
		return other == null ? clone() : new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount,  
				indices.symmetricDifference(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int symmetricDifferenceSize(Collection<? extends Pair<T, I>> other) {
		return other == null ? (int) size() : (int) indices.symmetricDifferenceSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> union(Collection<? extends Pair<T, I>> other) {
		return other == null ? clone() : new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount, 
				indices.union(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int unionSize(Collection<? extends Pair<T, I>> other) {
		return other == null ? (int) size() : (int) indices.unionSize(convert(other).indices);
	}

//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public PairSet<T, I> unmodifiable() {
//		return new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount, indices.unmodifiable());
//	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pair<T, I> first() {
		return indexToPair(indices.first());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pair<T, I> last() {
		return indexToPair(indices.last());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(ExtendedSet<Pair<T, I>> o) {
		return indices.compareTo(convert(o).indices);
	}

	
	
	//
	// COMPRESSED OBJECT SERIALIZATION
	//
	
	private static class ZipObjectOutputStream extends ObjectOutputStream {
		private GZIPOutputStream out;
		ZipObjectOutputStream(ObjectOutputStream out) throws IOException {this(new GZIPOutputStream(out));}
		ZipObjectOutputStream(GZIPOutputStream out) throws IOException {super(out); this.out = out;}
		@Override public void close() throws IOException {out.flush(); out.finish();}
	}
	
	private static class ZipObjectInputStream extends ObjectInputStream {
		ZipObjectInputStream(ObjectInputStream in) throws IOException {super(new GZIPInputStream(in));}
	}
	
    private void writeObject(ObjectOutputStream out) throws IOException {
		if (out instanceof ZipObjectOutputStream) {
			out.defaultWriteObject();
		} else {
			ObjectOutputStream oos = new ZipObjectOutputStream(out);
			oos.writeObject(this);
			oos.close();
		}
    }

    private transient Object serialize;

	@SuppressWarnings("unused")
	private Object readResolve() throws ObjectStreamException {
		if (serialize == null) 
			serialize = this;
		return serialize;
	}
	
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		if (in instanceof ZipObjectInputStream) {
			in.defaultReadObject();
		} else {
			ObjectInputStream ois = new ZipObjectInputStream(in);
			serialize = ois.readObject();
		}
	}
}
