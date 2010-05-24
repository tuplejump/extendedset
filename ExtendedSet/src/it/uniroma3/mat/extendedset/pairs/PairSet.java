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
import it.uniroma3.mat.extendedset.ExtendedSet;
import it.uniroma3.mat.extendedset.FastSet;
import it.uniroma3.mat.extendedset.IndexedSet;
import it.uniroma3.mat.extendedset.IntSet;
import it.uniroma3.mat.extendedset.IntSet.ExtendedIntIterator;
import it.uniroma3.mat.extendedset.utilities.MersenneTwister;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

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

	/* maximum cardinality of each subset */
	private static int SUBSET_SIZE = ConciseSet.MAX_ALLOWED_INTEGER + 1;
	
	/** transaction-item pair indices (from 0 to {@link #SUBSET_SIZE} - 1) */
	private final IntSet firstIndices;

	/** transaction-item pair indices (from {@link #SUBSET_SIZE}) */
	private final NavigableMap<Long, IntSet> otherIndices;
	
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
//			allTransactions = new IndexedSet<XT>(transactions, compressed).universe(); //.unmodifiable();
			allTransactions = new IndexedSet<XT>(transactions, false).universe(); //.unmodifiable();

		// all items
		IndexedSet<XI> allItems; 
		if (items instanceof IndexedSet<?>)
			allItems = ((IndexedSet<XI>) items); //.unmodifiable();
		else
//			allItems = new IndexedSet<XI>(items, compressed).universe(); //.unmodifiable();
			allItems = new IndexedSet<XI>(items, false).universe(); //.unmodifiable();
		
		// empty index set
		IntSet firstIndices = compressed ? new ConciseSet() : new FastSet();
		NavigableMap<Long, IntSet> otherIndices = new TreeMap<Long, IntSet>();
		
		// final pair set
		return new PairSet<XT, XI>(
				allTransactions, 
				allItems, 
				allTransactions.size(), 
				allItems.size(), 
				firstIndices,
				otherIndices);
	}
	
	/**
	 * Creates an empty instance of {@link PairSet}
	 */
	private static PairSet<Integer, Integer> newEmptyPairSet(
			int maxTransactionCount, int maxItemCount, boolean compressed) {
		return newEmptyPairSet(
//				new IndexedSet<Integer>(0, maxTransactionCount - 1, compressed).universe(),
//				new IndexedSet<Integer>(0, maxItemCount - 1, compressed).universe(),
				new IndexedSet<Integer>(0, maxTransactionCount - 1, false).universe(),
				new IndexedSet<Integer>(0, maxItemCount - 1, false).universe(),
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
		firstIndices = as.firstIndices;
		otherIndices = as.otherIndices;
	}

	/**
	 * Shallow-copy constructor
	 */
	private PairSet(
			IndexedSet<T> allTransactions, 
			IndexedSet<I> allItems,
			int maxTransactionCount,
			int maxItemCount,
			IntSet firstIndices,
			NavigableMap<Long, IntSet> otherIndices) {
		this.allTransactions = allTransactions;
		this.allItems = allItems;
		this.firstIndices = firstIndices;
		this.otherIndices = otherIndices;
		this.maxTransactionCount = maxTransactionCount;
		this.maxItemCount = maxItemCount;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> clone() {
		// NOTE: do not use super.clone() since it is 10 times slower!
		NavigableMap<Long, IntSet> otherIndicesClone = new TreeMap<Long, IntSet>();
		for (Entry<Long, IntSet> e : otherIndices.entrySet()) 
			otherIndicesClone.put(e.getKey(), e.getValue().clone());
		return new PairSet<T, I>(
				allTransactions, 
				allItems, 
				maxTransactionCount, 
				maxItemCount, 
				firstIndices.clone(),
				otherIndicesClone);
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
		return (long) allTransactions.absoluteIndexOf(transaction) * maxItemCount + allItems.absoluteIndexOf(item);
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
		long i = pairToIndex(transaction, item);
		if (i < SUBSET_SIZE)
			return firstIndices.add((int) i);
		Long first = Long.valueOf((i / SUBSET_SIZE) * SUBSET_SIZE);
		IntSet subset = otherIndices.get(first);
		if (subset == null)
			otherIndices.put(first, subset = firstIndices.empty());
		return subset.add((int) (i - first.longValue()));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends Pair<T, I>> c) {
		if (c == null || c.isEmpty())
			return false;
		final PairSet<T, I> other = convert(c);
		boolean res = firstIndices.addAll(other.firstIndices);
		for (Entry<Long, IntSet> e : other.otherIndices.entrySet()) {
			IntSet subset = otherIndices.get(e.getKey());
			if (subset == null) {
				otherIndices.put(e.getKey(), e.getValue().clone());
				res = true;
			} else {
				res |= subset.addAll(e.getValue());
			}
		}
		return res;
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
		firstIndices.clear();
		otherIndices.clear();
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
		long i = pairToIndex(transaction, item);
		if (i < SUBSET_SIZE)
			return firstIndices.contains((int) i);
		Long first = Long.valueOf((i / SUBSET_SIZE) * SUBSET_SIZE);
		IntSet subset = otherIndices.get(first);
		if (subset == null)
			return false;
		return subset.contains((int) (i - first.longValue()));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		if (c == null || c.isEmpty() || c == this)
			return true;
		final PairSet<T, I> other = convert(c);
		if (!firstIndices.containsAll(other.firstIndices))
			return false;
		for (Entry<Long, IntSet> e : other.otherIndices.entrySet()) {
			IntSet subset = otherIndices.get(e.getKey());
			if (subset == null || !subset.containsAll(e.getValue()))
				return false;
		}
		return true;
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
		return firstIndices.isEmpty() && otherIndices.isEmpty();
	}
	
	/**
	 * Iteration over the union of all indices
	 */
	private class LongIterator {
		ExtendedIntIterator itr;
		Iterator<Entry<Long, IntSet>> otherItrs;
		long first = 0;
		
		LongIterator() {
			itr = firstIndices.intIterator();
			otherItrs = otherIndices.entrySet().iterator();
			first = 0;
		}
		
		void nextItr() {
			Entry<Long, IntSet> e = otherItrs.next();
			itr = e.getValue().intIterator();
			first = e.getKey().longValue();
		}

		boolean hasNext() {
			return otherItrs.hasNext() || itr.hasNext();
		}
		
		long next() {
			if (!itr.hasNext())
				nextItr();
			return first + itr.next();
		}
		
		void skipAllBefore(long element) {
			while (element >= first + SUBSET_SIZE)
				nextItr();
			if (element < first)
				return;
			itr.skipAllBefore((int) (element - first));
		}
		
		void remove() {
			itr.remove();
			//TODO controllare che otherIndices non abbia insiemi vuoti!!!
		}
	}
	
	/**
	 * Iteration over the union of all indices, reverse order
	 */
	private class ReverseLongIterator extends LongIterator {
		ReverseLongIterator() {
			nextItr();
		}
		
		@Override
		void nextItr() {
			if (otherItrs.hasNext()) {
				Entry<Long, IntSet> e = otherItrs.next();
				itr = e.getValue().descendingIntIterator();
				first = e.getKey().longValue();
			} else {
				itr = firstIndices.descendingIntIterator();
				first = 0;
			}
		}
		
		@Override
		void skipAllBefore(long element) {
			while (element <= first)
				nextItr();
			if (element > first + SUBSET_SIZE)
				return;
			itr.skipAllBefore((int) (element - first));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<Pair<T, I>> iterator() {
		return new ExtendedIterator<Pair<T, I>>() {
			LongIterator itr = new LongIterator();
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
			LongIterator itr = new ReverseLongIterator();
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
		long i = pairToIndex(transaction, item);
		if (i < SUBSET_SIZE)
			return firstIndices.remove((int) i);
		Long first = Long.valueOf((i / SUBSET_SIZE) * SUBSET_SIZE);
		IntSet subset = otherIndices.get(first);
		if (subset == null)
			return false;
		boolean res = subset.remove((int) (i - first.longValue()));
		if (res && subset.isEmpty())
			otherIndices.remove(first);
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		if (c == null || c.isEmpty())
			return false;
		final PairSet<T, I> other = convert(c);
		boolean res = firstIndices.removeAll(other.firstIndices);
		for (Entry<Long, IntSet> e : other.otherIndices.entrySet()) {
			IntSet subset = otherIndices.get(e.getKey());
			if (subset != null) {
				res |= subset.removeAll(e.getValue());
				if (subset.isEmpty())
					otherIndices.remove(e.getKey());
			}
		}
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		if (isEmpty())
			return false;
		if (c == null || c.isEmpty()) {
			clear();
			return true;
		}
		
		final PairSet<T, I> other = convert(c);
		boolean res = firstIndices.retainAll(other.firstIndices);
		for (Entry<Long, IntSet> e : other.otherIndices.entrySet()) {
			IntSet subset = otherIndices.get(e.getKey());
			if (subset != null) {
				res |= subset.retainAll(e.getValue());
				if (subset.isEmpty())
					otherIndices.remove(e.getKey());
			}
		}
		if (!other.otherIndices.isEmpty()) {
			Iterator<Entry<Long, IntSet>> itr = otherIndices.entrySet().iterator();
			while (itr.hasNext()) {
				Entry<Long, IntSet> e = itr.next();
				if (!other.otherIndices.containsKey(e.getKey())) {
					itr.remove();
					res = true;
				}
			}
		}
		return res;
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
		int res = firstIndices.size();
		for (Entry<Long, IntSet> e : otherIndices.entrySet())
			res += e.getValue().size();
		return res;
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
		return 31 * firstIndices.hashCode() + otherIndices.hashCode();
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
		return hasSameIndices(other) 
			&& firstIndices.equals(other.firstIndices)
			&& otherIndices.equals(other.otherIndices);
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
		long indexOfFirst = (long) allTransactions.absoluteIndexOf(transaction) * maxItemCount;
		long indexOfLast = indexOfFirst + maxItemCount - 1;

		// final result
		IndexedSet<I> res = allItems.empty();
		
		// index iterator
		LongIterator itr = new LongIterator();
		itr.skipAllBefore(indexOfFirst);
		Long next;
		while (itr.hasNext() && ((next = itr.next()) <= indexOfLast)) 
			res.indices().add((int) (next - indexOfFirst));

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
		long indexOfFirst = allItems.absoluteIndexOf(item);
		long indexOfLast = indexOfFirst + (maxTransactionCount - 1) * maxItemCount;
		
		// final result
		IndexedSet<T> res = allTransactions.empty();

		// index iterator
		LongIterator itr = new LongIterator();
		itr.skipAllBefore(indexOfFirst);
		long next;
		while (itr.hasNext() && ((next = itr.next()) <= indexOfLast)) {
			int shift = (int) ((next - indexOfFirst) % maxItemCount);
			if (shift == 0)
				res.indices().add((int) (next / maxItemCount));
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
		LongIterator itr = new LongIterator();
		while (itr.hasNext()) {
			int t = (int) (itr.next() / maxItemCount);
			inv.indices().add(t);
			itr.skipAllBefore((long) (t + 1) * maxItemCount);
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
		LongIterator itr = new LongIterator();
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
		if (index < firstIndices.size())
			return indexToPair(firstIndices.get((int) index));
		
		index -= firstIndices.size();
		for (Entry<Long, IntSet> e : otherIndices.entrySet()) {
			if (index < e.getValue().size())
				return indexToPair(e.getValue().get((int) index));
			index -= e.getValue().size();
		}
		throw new IndexOutOfBoundsException(Long.toString(index));
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
		long i = pairToIndex(el);
		if (i < SUBSET_SIZE)
			return firstIndices.indexOf((int) i);
		for (Entry<Long, IntSet> e : otherIndices.entrySet())
			if (i < e.getKey().longValue() + SUBSET_SIZE)
				return e.getValue().indexOf((int) i);
		return -1L;
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
		s.append("firstIndices: " + firstIndices.debugInfo());
		s.append("otherIndices: " + otherIndices.size());
		for (Entry<Long, IntSet> e : otherIndices.entrySet())
			s.append("first: " + e.getKey() + ", " + e.getValue().debugInfo());

		return s.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double bitmapCompressionRatio() {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return firstIndices.bitmapCompressionRatio();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double collectionCompressionRatio() {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return firstIndices.collectionCompressionRatio();
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
	public IntSet indices() {
//TODO
//		return indices.headSet(maxTransactionCount * maxItemCount);
		return new IntSet() {
			@Override
			public ExtendedIntIterator intIterator() {
				return new ExtendedIntIterator() {
					final LongIterator itr = new LongIterator();
					@Override public boolean hasNext() {return itr.hasNext();}
					@Override public int next() {return (int) itr.next();}
					@Override public void remove() {itr.remove();}
					@Override public void skipAllBefore(int element) {itr.skipAllBefore(element);}
				};
			}
			
			@Override
			public ExtendedIntIterator descendingIntIterator() {
				return new ExtendedIntIterator() {
					final ReverseLongIterator itr = new ReverseLongIterator();
					@Override public boolean hasNext() {return itr.hasNext();}
					@Override public int next() {return (int) itr.next();}
					@Override public void remove() {itr.remove();}
					@Override public void skipAllBefore(int element) {itr.skipAllBefore(element);}
				};
			}
			
			@Override
			public int size() {
				return PairSet.this.size();
			}
			
			@Override
			public boolean isEmpty() {
				return PairSet.this.isEmpty();
			}
			
			@Override
			public boolean add(int i) {
				//TODO
				throw new UnsupportedOperationException("TODO");
//				if (i < SUBSET_SIZE)
//					return firstIndices.add(i);
//				Long first = Long.valueOf((i / SUBSET_SIZE) * SUBSET_SIZE);
//				IntSet subset = otherIndices.get(first);
//				if (subset == null)
//					otherIndices.put(first, subset = firstIndices.empty());
//				return subset.add(i - first.intValue());
			}
			
			@Override
			public void fill(int from, int to) {
				//TODO
				throw new UnsupportedOperationException("TODO");
//				if (from < SUBSET_SIZE) {
//					if (to < SUBSET_SIZE) {
//						firstIndices.fill(from, to);
//						return;
//					}
//					firstIndices.fill(from, SUBSET_SIZE - 1);
//					from = SUBSET_SIZE;
//				}
//				for (Long first = Long.valueOf(from); first < to; first += SUBSET_SIZE) {
//					IntSet subset = otherIndices.get(first);
//					if (subset == null)
//						otherIndices.put(first, subset = firstIndices.empty());
//					if (to < first + SUBSET_SIZE) 
//						subset.fill(0, SUBSET_SIZE - 1);
//					else
//						subset.fill(0, to % SUBSET_SIZE);
//				}
			}
			
			@Override
			public boolean retainAll(IntSet c) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			//FINIREEEEEEE
			
			@Override
			public int compareTo(IntSet o) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public IntSet union(IntSet other) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public IntSet symmetricDifference(IntSet other) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public boolean removeAll(IntSet c) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public boolean remove(int i) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public int last() {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public int intersectionSize(IntSet other) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public IntSet intersection(IntSet other) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public int indexOf(int e) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public int get(int i) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public void flip(int e) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public IntSet empty() {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public IntSet difference(IntSet other) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public String debugInfo() {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public IntSet convert(int... a) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public boolean containsAtLeast(IntSet other, int minElements) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public boolean containsAny(IntSet other) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public boolean containsAll(IntSet c) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public boolean contains(int i) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public IntSet complemented() {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public void complement() {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public double collectionCompressionRatio() {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public IntSet clone() {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public void clear() {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public void clear(int from, int to) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public double bitmapCompressionRatio() {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
			
			@Override
			public boolean addAll(IntSet c) {
				//TODO
				throw new UnsupportedOperationException("TODO");
			}
		};
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
		//TODO
		throw new UnsupportedOperationException("TODO");
//		// final set
//		PairSet<T, I> res = empty();
//		
//		// trivial case
//		if (fromTransaction == null && toTransaction == null && fromItem == null && toItem == null)
//			return clone();
//		
//		// transaction range
//		int fromTransactionIndex = fromTransaction == null ? 0 : allTransactions.absoluteIndexOf(fromTransaction);
//		int toTransactionIndex = toTransaction == null ? maxTransactionCount - 1 : allTransactions.absoluteIndexOf(toTransaction);
//		
//		// item range
//		if (fromItem == null && toItem == null) {
//			// all items are involved
//			long start = (long) fromTransactionIndex * maxItemCount;
//			long end = (toTransactionIndex + 1) * maxItemCount - 1;
//			for (long x = start; x <= end; x += SUBSET_SIZE)
//				if (x + SUBSET_SIZE >= end)
//					res.indices.fill(x, end);
//				else
//					res.indices.fill(x, x + SUBSET_SIZE - 1);
//		} {
//			// identify the item subset
//			int fromItemIndex = fromItem == null ? 0 : allItems.absoluteIndexOf(fromItem);
//			int toItemIndex = toItem == null ? maxItemCount - 1 : allItems.absoluteIndexOf(toItem);
//			
//			// identify indices
//			for (int ui = fromTransactionIndex; ui <= toTransactionIndex; ui++) {
//				int firstItemOfTransaction = ui * maxItemCount;
//				res.indices.fill(firstItemOfTransaction + fromItemIndex, firstItemOfTransaction + toItemIndex);
//			}
//		}
//
//		// remove out-of-range pairs
//		res.indices.retainAll(indices);
//		return res;
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
		//TODO
		throw new UnsupportedOperationException("TODO");
//		// final set of pairs
//		PairSet<T, I> res = empty();
//
//		// trivial case
//		if (involvedTransactions == null && involvedItems == null) 
//			return clone();
//		
//		// identify indices
//		if (involvedTransactions == null) {
//			// identify all potential indices
//			Collection<Integer> pis = new ArrayList<Integer>(involvedItems.size());
//			for (I p : involvedItems) 
//				pis.add(allItems.absoluteIndexOf(p));
//			for (int ui = 0; ui < maxTransactionCount; ui++) 
//				for (Integer pi : pis)
//					res.indices().add(ui * maxItemCount + pi);
//		} else if (involvedItems == null) {
//			// identify all potential indices
//			for (T u : involvedTransactions) {
//				int first = allTransactions.absoluteIndexOf(u) * maxItemCount;
//				res.indices().fill(first, first + maxItemCount - 1);
//			}
//		} else {
//			// identify all potential indices
//			Collection<Integer> pis = new ArrayList<Integer>(involvedItems.size());
//			for (I p : involvedItems) 
//				pis.add(allItems.absoluteIndexOf(p));
//			for (T u : involvedTransactions) {
//				int ui = allTransactions.absoluteIndexOf(u);
//				for (Integer pi : pis)
//					res.indices().add(ui * maxItemCount + pi);
//			}
//		}
//
//		// remove out-of-range pairs
//		res.indices().retainAll(indices());
//		return res;
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
				firstIndices.empty(),
				new TreeMap<Long, IntSet>());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complement() {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		indices.complement();
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
		
		// NOTE: cannot call super.convert(c) because of loop
		PairSet<T, I> res = empty();
		for (Pair<T, I> t : (Collection<Pair<T, I>>) c) 
			res.add(t);
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
		//TODO
		throw new UnsupportedOperationException("TODO");
//		indices.clear(pairToIndex(from), pairToIndex(to));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int complementSize() {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return indices.complementSize();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> complemented() {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount, indices.complemented());
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PairSet<T, I> difference(Collection<? extends Pair<T, I>> other) {
		//TODO
		return (PairSet<T, I>) super.difference(other);
//		return other == null ? clone() : new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount, 
//				indices.difference(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(Collection<? extends Pair<T, I>> other) {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return other == null || indices.containsAny(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAtLeast(Collection<? extends Pair<T, I>> other, int minElements) {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return other != null && !other.isEmpty() && indices.containsAtLeast(convert(other).indices, minElements);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int differenceSize(Collection<? extends Pair<T, I>> other) {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return other == null ? size() : indices.differenceSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fill(Pair<T, I> from, Pair<T, I> to) {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		indices.fill(pairToIndex(from), pairToIndex(to));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flip(Pair<T, I> e) {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		indices.flip(pairToIndex(e));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> subSet(Pair<T, I> fromElement, Pair<T, I> toElement) {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount,  
//				indices.subSet(pairToIndex(fromElement), pairToIndex(toElement)));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> headSet(Pair<T, I> toElement) {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount, 
//				indices.headSet(pairToIndex(toElement)));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> tailSet(Pair<T, I> fromElement) {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount,  
//				indices.tailSet(pairToIndex(fromElement)));
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PairSet<T, I> intersection(Collection<? extends Pair<T, I>> c) {
		//TODO
		return (PairSet<T, I>) super.intersection(c);
//		return other == null ? empty() : new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount,  
//				indices.intersection(convert(other).indices));
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
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return other == null ? clone() : new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount,  
//				indices.symmetricDifference(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int symmetricDifferenceSize(Collection<? extends Pair<T, I>> other) {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return other == null ? size() : indices.symmetricDifferenceSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public PairSet<T, I> union(Collection<? extends Pair<T, I>> other) {
		//TODO
		return (PairSet<T, I>) super.union(other);
//		return other == null ? clone() : new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount, 
//				indices.union(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int unionSize(Collection<? extends Pair<T, I>> other) {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return other == null ? size() : indices.unionSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PairSet<T, I> unmodifiable() {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return new PairSet<T, I>(allTransactions, allItems, maxTransactionCount, maxItemCount, indices.unmodifiable());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pair<T, I> first() {
		if (!firstIndices.isEmpty())
			return indexToPair(firstIndices.first());
		if (otherIndices.isEmpty())
			throw new NoSuchElementException();
		return indexToPair(otherIndices.firstEntry().getValue().first());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Pair<T, I> last() {
		if (otherIndices.isEmpty() && firstIndices.isEmpty())
			throw new NoSuchElementException();
		if (!otherIndices.isEmpty())
			return indexToPair(otherIndices.lastEntry().getValue().last());
		return indexToPair(firstIndices.last());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(ExtendedSet<Pair<T, I>> o) {
		//TODO
		throw new UnsupportedOperationException("TODO");
//		return indices.compareTo(convert(o).indices);
	}

	/**
	 * TODO: TO REMOVE
	 * @param <T>
	 * @param bits
	 * @param items
	 * @return
	 */
	private static <T> boolean checkContent(ExtendedSet<T> bits, SortedSet<T> items) {
		if (bits.size() != items.size())
			return false;
		if (bits.isEmpty() && items.isEmpty())
			return true;
		for (T i : bits) 
			if (!items.contains(i)) 
				return false;
		for (T i : items) 
			if (!bits.contains(i)) 
				return false;
		if (!bits.last().equals(items.last()))
			return false;
		if (!bits.first().equals(items.first()))
			return false;
		return true;
	}
	
	/**
	 * TODO: TO REMOVE
	 * @param args
	 */
	public static void main(String[] args) {
		Random r = new MersenneTwister();

		final int tranCount = (SUBSET_SIZE - 32) / 10000;
		final int itemCount = 1000000;
		ExtendedSet<Pair<Integer, Integer>> bitsLeft = new PairSet<Integer, Integer>(tranCount, itemCount, true);
		ExtendedSet<Pair<Integer, Integer>> bitsRight = bitsLeft.empty();
		SortedSet<Pair<Integer, Integer>> itemsLeft = new TreeSet<Pair<Integer, Integer>>(new Comparator<Pair<Integer, Integer>>() {
			@Override
			public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
				int res = o1.transaction.compareTo(o2.transaction);
				if (res == 0)
					res = o1.item.compareTo(o2.item);
				return res;
			}
		});
		@SuppressWarnings("unchecked")
		SortedSet<Pair<Integer, Integer>> itemsRight = (TreeSet) ((TreeSet) itemsLeft).clone();
		
//		for (int h = 0; h < 100; h++) {
//			Pair<Integer, Integer> p = new Pair<Integer, Integer>(r.nextInt(x), r.nextInt(xxx));
//			bitsLeft.add(p);
//			itemsLeft.add(p);
//		}
//		if (!bitsLeft.toString().equals(itemsLeft.toString()))
//			throw new RuntimeException("errore");
//		System.out.println(bitsLeft);
//		for (int h = 0; h < 100; h++) {
//			Pair<Integer, Integer> p = new Pair<Integer, Integer>(r.nextInt(x), r.nextInt(xxx));
//			bitsRight.add(p);
//			itemsRight.add(p);
//		}
//		if (!bitsRight.toString().equals(itemsRight.toString()))
//			throw new RuntimeException("errore");
//		System.out.println(bitsRight);
//		
//		bitsLeft.addAll(bitsRight);
//		itemsLeft.addAll(itemsRight);
//		if (!bitsLeft.toString().equals(itemsLeft.toString()))
//			throw new RuntimeException("errore");
//		System.out.println(bitsLeft);
//
//		
//		System.exit(0);
		
		final int maxCardinality = 1000;
		
		// random operation loop
		for (int i = 0; i < 1000000; i++) {
			System.out.print("Test " + i + ": ");
			
			/*
			 * contains(), add(), and remove()
			 */
			bitsRight.clear();
			itemsRight.clear();
			ExtendedSet<Pair<Integer, Integer>> clone; 
			for (int j = 0; j < maxCardinality; j++) {
				Pair<Integer, Integer> p = new Pair<Integer, Integer>(r.nextInt(tranCount/1000), r.nextInt(itemCount/1000));
				
				if (itemsRight.contains(p) ^ bitsRight.contains(p)) {
					System.out.println("CONTAINS ERROR!");
					System.out.println("itemsRight.contains(" + p + "): " + itemsRight.contains(p));
					System.out.println("bitsRight.contains(" + p + "): " + bitsRight.contains(p));
					System.out.println("itemsRight:");
					System.out.println(itemsRight);
					System.out.println("bitsRight:");
					System.out.println(bitsRight.debugInfo());
					return;
				}
				clone = bitsRight.clone();
				boolean resItems = itemsRight.add(p);
				boolean resBits = bitsRight.add(p);
				ExtendedSet<Pair<Integer, Integer>> app = bitsLeft.empty();
				app.addAll(itemsRight);
				if (bitsRight.hashCode() != app.hashCode()) {
					System.out.println("ADD ERROR!");
					System.out.println("itemsRight.contains(" + p + "): " + itemsRight.contains(p));
					System.out.println("bitsRight.contains(" + p + "): " + bitsRight.contains(p));
					System.out.println("itemsRight:");
					System.out.println(itemsRight);
					System.out.println("bitsRight:");
					System.out.println(bitsRight.debugInfo());
					System.out.println("Append:");
					System.out.println(app.debugInfo());
					System.out.println("Clone:");
					System.out.println(clone.debugInfo());
					return;
				}
				if (resItems != resBits) {
					System.out.println("ADD BOOLEAN ERROR!");
					System.out.println("itemsRight.add(" + p + "): " + resItems);
					System.out.println("bitsRight.add(" + p + "): " + resBits);
					System.out.println("itemsRight:");
					System.out.println(itemsRight);
					System.out.println("bitsRight:");
					System.out.println(bitsRight.debugInfo());
					return;
				}
			}
			for (int j = 0; j < maxCardinality; j++) {
				Pair<Integer, Integer> p = new Pair<Integer, Integer>(r.nextInt(tranCount/1000), r.nextInt(itemCount/1000));

				clone = bitsRight.clone();
				boolean resItems = itemsRight.remove(p);
				boolean resBits = bitsRight.remove(p);
				ExtendedSet<Pair<Integer, Integer>> app = bitsLeft.empty();
				app.addAll(itemsRight);
				if (bitsRight.hashCode() != app.hashCode()) {
					System.out.println("REMOVE ERROR!");
					System.out.println("itemsRight.contains(" + p + "): " + itemsRight.contains(p));
					System.out.println("bitsRight.contains(" + p + "): " + bitsRight.contains(p));
					System.out.println("itemsRight:");
					System.out.println(itemsRight);
					System.out.println("bitsRight:");
					System.out.println(bitsRight.debugInfo());
					System.out.println("Append:");
					System.out.println(app.debugInfo());
					System.out.println("Clone:");
					System.out.println(clone.debugInfo());
					return;
				}
				if (resItems != resBits) {
					System.out.println("REMOVE BOOLEAN ERROR!");
					System.out.println("itemsRight.remove(" + p + "): " + resItems);
					System.out.println("bitsRight.remove(" + p + "): " + resBits);
					System.out.println("itemsRight:");
					System.out.println(itemsRight);
					System.out.println("bitsRight:");
					System.out.println(bitsRight.debugInfo());
					System.out.println("Clone:");
					System.out.println(clone.debugInfo());
					return;
				}
			}
			
			// new right operand
			itemsRight.clear();
			bitsRight.clear();
			for (int j = 0; j < maxCardinality; j++) {
				Pair<Integer, Integer> p = new Pair<Integer, Integer>(r.nextInt(tranCount/1000), r.nextInt(itemCount/1000));
				itemsRight.add(p);
			}
			bitsRight.addAll(itemsRight);

			/*
			 * check for content correctness, first(), and last() 
			 */
			if (!checkContent(bitsRight, itemsRight)) {
				System.out.println("RIGHT OPERAND ERROR!");
				System.out.println("Same elements: " + (itemsRight.toString().equals(bitsRight.toString())));
				System.out.println("itemsRight:");
				System.out.println(itemsRight);
				System.out.println("bitsRight:");
				System.out.println(bitsRight.debugInfo());

				System.out.println("itemsRight.size(): "  + itemsRight.size() + " ?= bitsRight.size(): " + bitsRight.size());
				for (Pair<Integer, Integer> y : bitsRight) 
					if (!itemsRight.contains(y)) 
						System.out.println("itemsRight does not contain " + y);
				for (Pair<Integer, Integer> y : itemsRight) 
					if (!bitsRight.contains(y)) 
						System.out.println("itemsRight does not contain " + y);
				System.out.println("bitsRight.last(): " + bitsRight.last() + " ?= itemsRight.last(): " + itemsRight.last());
				System.out.println("bitsRight.first(): " + bitsRight.first() + " ?= itemsRight.first(): " + itemsRight.first());

				return;
			}
			
			/*
			 * containsAll()
			 */
			boolean bitsRes = bitsLeft.containsAll(bitsRight);
			boolean itemsRes = itemsLeft.containsAll(itemsRight);
			if (bitsRes != itemsRes) {
				System.out.println("CONTAINS_ALL ERROR!");
				System.out.println("bitsLeft.containsAll(bitsRight): " + bitsRes);
				System.out.println("itemsLeft.containsAll(itemsRight): " + itemsRes);
				System.out.println("bitsLeft:");
				System.out.println(bitsLeft.debugInfo());				
				System.out.println("bitsRight:");
				System.out.println(bitsRight.debugInfo());
				System.out.println("bitsLeft.intersection(bitsRight)");
				System.out.println(bitsLeft.intersection(bitsRight));
				System.out.println("itemsLeft.retainAll(itemsRight)");
				itemsLeft.retainAll(itemsRight);
				System.out.println(itemsLeft);
				return;
			}

//			/*
//			 * containsAny()
//			 */
//			bitsRes = bitsLeft.containsAny(bitsRight);
//			itemsRes = true;
//			for (Pair<Integer, Integer> y : itemsRight) {
//				itemsRes = itemsLeft.contains(y);
//				if (itemsRes)
//					break;
//			}
//			if (bitsRes != itemsRes) {
//				System.out.println("bitsLeft.containsAny(bitsRight): " + bitsRes);
//				System.out.println("itemsLeft.containsAny(itemsRight): " + itemsRes);
//				System.out.println("bitsLeft:");
//				System.out.println(bitsLeft.debugInfo());				
//				System.out.println("bitsRight:");
//				System.out.println(bitsRight.debugInfo());
//				System.out.println("bitsLeft.intersection(bitsRight)");
//				System.out.println(bitsLeft.intersection(bitsRight));
//				System.out.println("itemsLeft.retainAll(itemsRight)");
//				itemsLeft.retainAll(itemsRight);
//				System.out.println(itemsLeft);
//				return;
//			}
//			
//			/*
//			 * containsAtLeast()
//			 */
//			int l = 1 + r.nextInt(bitsRight.size() + 1);
//			bitsRes = bitsLeft.containsAtLeast(bitsRight, l);
//			int itemsResCnt = 0;
//			for (Pair<Integer, Integer> y : itemsRight) {
//				if (itemsLeft.contains(y))
//					itemsResCnt++;
//				if (itemsResCnt >= l)
//					break;
//			}
//			if (bitsRes != (itemsResCnt >= l)) {
//				System.out.println("bitsLeft.containsAtLeast(bitsRight, " + l + "): " + bitsRes);
//				System.out.println("itemsLeft.containsAtLeast(itemsRight, " + l + "): " + (itemsResCnt >= l));
//				System.out.println("bitsLeft:");
//				System.out.println(bitsLeft.debugInfo());				
//				System.out.println("bitsRight:");
//				System.out.println(bitsRight.debugInfo());
//				System.out.println("bitsLeft.intersection(bitsRight)");
//				System.out.println(bitsLeft.intersection(bitsRight));
//				System.out.println("itemsLeft.retainAll(itemsRight)");
//				itemsLeft.retainAll(itemsRight);
//				System.out.println(itemsLeft);
//				return;
//			}

			/*
			 * Perform a random operation with the previous set:
			 * addAll() and unionSize()
			 * removeAll() and differenceSize()
			 * retainAll() and intersectionSize()
			 * symmetricDifference() and symmetricDifferenceSize()
			 * complement() and complementSize()
			 */
//			int operationSize = 0;
			boolean resItems = true, resBits = true;
			switch (1 + r.nextInt(5)) {
			case 1:
				System.out.format(" union of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
//				operationSize = bitsLeft.unionSize(bitsRight);
				resItems = itemsLeft.addAll(itemsRight);
				resBits = bitsLeft.addAll(bitsRight);
				break;

			case 2:
				System.out.format(" difference of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
//				operationSize = bitsLeft.differenceSize(bitsRight);
				resItems = itemsLeft.removeAll(itemsRight);
				resBits = bitsLeft.removeAll(bitsRight);
				break;

			case 3:
				System.out.format(" intersection of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
//				operationSize = bitsLeft.intersectionSize(bitsRight);
				resItems = itemsLeft.retainAll(itemsRight);
				resBits = bitsLeft.retainAll(bitsRight);
				break;

			case 4:
				System.out.format(" symmetric difference of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
//				operationSize = bitsLeft.symmetricDifferenceSize(bitsRight);
//				TreeSet<Integer> temp = new TreeSet<Integer>(itemsRight);
//				temp.removeAll(itemsLeft);
//				itemsLeft.removeAll(itemsRight);
//				itemsLeft.addAll(temp);
//				bitsLeft = bitsLeft.symmetricDifference(bitsRight);
				break;

			case 5:
				System.out.format(" complement of %d elements... ", itemsLeft.size());
//				operationSize = bitsLeft.complementSize();
//				if (!itemsLeft.isEmpty())
//					for (int j = itemsLeft.last(); j >= 0; j--)
//						if (!itemsLeft.add(j))
//							itemsLeft.remove(j);
//				bitsLeft.complement();
				break;
			}
			
			// check the list of elements
			if (!checkContent(bitsLeft, itemsLeft)) {
				System.out.println("OPERATION ERROR!");
				System.out.println("Same elements: " + (itemsLeft.toString().equals(bitsLeft.toString())));
				System.out.println("itemsLeft:");
				System.out.println(itemsLeft);
				System.out.println("bitsLeft:");
				System.out.println(bitsLeft.debugInfo());

				System.out.println("itemsLeft.size(): "  + itemsLeft.size() + " ?= bitsLeft.size(): " + bitsLeft.size());
				for (Pair<Integer, Integer> y : bitsLeft) 
					if (!itemsLeft.contains(y)) 
						System.out.println("itemsLeft does not contain " + tranCount);
				for (Pair<Integer, Integer> y : itemsLeft) 
					if (!bitsLeft.contains(y)) 
						System.out.println("itemsLeft does not contain " + tranCount);
				System.out.println("bitsLeft.last(): " + bitsLeft.last() + " ?= itemsLeft.last(): " + itemsLeft.last());
				System.out.println("bitsLeft.first(): " + bitsLeft.first() + " ?= itemsLeft.first(): " + itemsLeft.first());
				
				return;
			}

//			// check the size
//			if (itemsLeft.size() != operationSize) {
//				System.out.println("OPERATION SIZE ERROR");
//				System.out.println("Wrong size: " + operationSize);
//				System.out.println("Correct size: " + itemsLeft.size());
//				System.out.println("bitsLeft:");
//				System.out.println(bitsLeft.debugInfo());
//				return;
//			}

			// check the boolean result
			if (resItems != resBits) {
				System.out.println("OPERATION BOOLEAN ERROR!");
				System.out.println("resItems: " + resItems);
				System.out.println("resBits: " + resBits);
				System.out.println("bitsLeft:");
				System.out.println(bitsLeft.debugInfo());
				return;
			}

			// check the internal representation of the result
			ExtendedSet<Pair<Integer, Integer>> y = bitsLeft.empty();
			y.addAll(itemsLeft);
			if (y.hashCode() != bitsLeft.hashCode()) {
				System.out.println("Internal representation error!");
				System.out.println("FROM APPEND:");
				System.out.println(y.debugInfo());
				System.out.println("FROM OPERATION:");
				System.out.println(bitsLeft.debugInfo());
				return;
			}

			System.out.println("done.");
		}
	}
}
