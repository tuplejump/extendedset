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

package it.uniroma3.mat.extendedset;


import it.uniroma3.mat.extendedset.IntSet.ExtendedIntIterator;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * An {@link ExtendedSet} implementation that maps each element of the universe
 * (i.e., the collection of all possible elements) to an integer referred to as
 * its "index".
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @param <T>
 *            the type of elements maintained by this set
 * 
 * @see ExtendedSet
 * @see AbstractExtendedSet
 * @see ConciseSet
 * @see FastSet
 */
//TODO: usare IntSet invece di IntSet
public class IndexedSet<T> extends AbstractExtendedSet<T> implements java.io.Serializable {
	/** generated serial ID */
	private static final long serialVersionUID = -2386771695765773453L;

	// indices
	private final IntSet indices;

	// mapping to translate items to indices and vice-versa
	private final Map<T, Integer> itemToIndex;
//	private final Map<Integer, T> indexToItem;
	private final T[] indexToItem;

//	/**
//	 * Used when the universe is a sequence of integral numbers
//	 */
//	private class CheckedFakeMap implements Map<Integer, Integer>, java.io.Serializable {
//		/** generated serial ID */
//		private static final long serialVersionUID = 9179931456581081163L;
//		
//		private final int size;
//		private final int shift;
//		private final boolean inverse;
//		
//		/**
//		 * Specifies the first and the last element of the map
//		 * 
//		 * @param size
//		 *            sequence length
//		 * @param shift
//		 *            first element of the sequence
//		 * @param inverse
//		 *            <code>false</code> if it is used as a index-to-item map,
//		 *            <code>false</code> if it is used as a item-to-index map
//		 */
//		public CheckedFakeMap(int size, int shift, boolean inverse) {
//			this.size = size;
//			this.shift = shift;
//			this.inverse = inverse;
//		}
//
//		@Override public Integer get(Object key) {
//			Integer value = (Integer) key - (inverse ? shift : 0);
//			if (value.compareTo(0) < 0 || value.compareTo(size) >= 0)
//				throw new IndexOutOfBoundsException(key.toString());
//			return value + (inverse ? 0 : shift);
//		}
//		
//		@Override public int size() {return size;}
//
//		@Override public void clear() {throw new UnsupportedOperationException();}
//		@Override public boolean containsKey(Object key) {throw new UnsupportedOperationException();}
//		@Override public boolean containsValue(Object value) {throw new UnsupportedOperationException();}
//		@Override public Set<Entry<Integer, Integer>> entrySet() {throw new UnsupportedOperationException();}
//		@Override public boolean isEmpty() {throw new UnsupportedOperationException();}
//		@Override public Set<Integer> keySet() {throw new UnsupportedOperationException();}
//		@Override public Integer put(Integer key, Integer value) {throw new UnsupportedOperationException();}
//		@Override public void putAll(Map<? extends Integer, ? extends Integer> m) {throw new UnsupportedOperationException();}
//		@Override public Integer remove(Object key) {throw new UnsupportedOperationException();}
//		@Override public Collection<Integer> values() {throw new UnsupportedOperationException();}
//	}
//	
//	/**
//	 * Used when the universe is a sequence of integral numbers
//	 */
//	private class UncheckedFakeMap implements Map<Integer, Integer>, java.io.Serializable {
//		/** generated serial ID */
//		private static final long serialVersionUID = 4383471467074220611L;
//
//		private final int shift;
//		
//		/**
//		 * Specifies the first element of the map
//		 * 
//		 * @param shift
//		 *            first element of the sequence
//		 */
//		public UncheckedFakeMap(int shift) {
//			this.shift = shift;
//		}
//
//		/**
//		 * {@inheritDoc}
//		 * <p>
//		 * There is no bound check, thus {@link IndexedSet#absoluteGet(int)} and
//		 * {@link IndexedSet#absoluteIndexOf(Object)} methods does not throw exceptions
//		 * when using indices below the lower bound
//		 */
//		@Override public Integer get(Object key) {return (Integer) key + shift;}
//		
//		/**
//		 * {@inheritDoc}
//		 * <p>
//		 * By not supporting this method we make the method
//		 * {@link IndexedSet#universe()} not working
//		 */
//		@Override public int size() {throw new UnsupportedOperationException();}
//
//		@Override public void clear() {throw new UnsupportedOperationException();}
//		@Override public boolean containsKey(Object key) {throw new UnsupportedOperationException();}
//		@Override public boolean containsValue(Object value) {throw new UnsupportedOperationException();}
//		@Override public Set<Entry<Integer, Integer>> entrySet() {throw new UnsupportedOperationException();}
//		@Override public boolean isEmpty() {throw new UnsupportedOperationException();}
//		@Override public Set<Integer> keySet() {throw new UnsupportedOperationException();}
//		@Override public Integer put(Integer key, Integer value) {throw new UnsupportedOperationException();}
//		@Override public void putAll(Map<? extends Integer, ? extends Integer> m) {throw new UnsupportedOperationException();}
//		@Override public Integer remove(Object key) {throw new UnsupportedOperationException();}
//		@Override public Collection<Integer> values() {throw new UnsupportedOperationException();}
//	}

	/**
	 * Creates an empty {@link IndexedSet} based on a given collection that
	 * represents the set of <i>all</i> possible items that can be added to the
	 * {@link IndexedSet} instance.
	 * <p>
	 * <b>VERY IMPORTANT!</b> to correctly work and effectively reduce the
	 * memory allocation, new instances of {@link IndexedSet} <i>must</i>
	 * be created through the {@link #clone()} or {@link #empty()}
	 * methods and <i>not</i> by calling many times this constructor with the
	 * same collection for <code>universe</code>!
	 * 
	 * @param universe
	 *            collection of <i>all</i> possible items. Order will be
	 *            preserved.
	 * @param compressed
	 *            <code>true</code> if a compressed internal representation
	 *            should be used
	 */
	@SuppressWarnings("unchecked")
	public IndexedSet(final Collection<T> universe, boolean compressed) {
		// check if indices are sequential
//		boolean isSequence = true;
//		int shift = 0;
//		try {
//			SortedSet<Integer> ss = (SortedSet) universe;
//			shift = ss.first();
//			isSequence = ss.comparator() == null && ss.last().equals(shift + ss.size() - 1);
//		} catch (ClassCastException e) {
//			isSequence = false;
//		}
//
//		if (isSequence) {
//			indexToItem = (Map<Integer, T>) new CheckedFakeMap(universe.size(), shift, false);
//			itemToIndex = (Map<T, Integer>) new CheckedFakeMap(universe.size(), shift, true);
//		} else {
			// NOTE: it removes duplicates and keeps the order
//			indexToItem = new ArrayMap<T>(universe instanceof Set ? 
//					(T[]) universe.toArray() :
//					(T[]) (new LinkedHashSet<T>(universe)).toArray());
//
//			itemToIndex = new HashMap<T, Integer>(Math.max((int) (indexToItem.size() / .75f) + 1, 16));
//			for (int i = 0; i < indexToItem.size(); i++)
//				itemToIndex.put(indexToItem.get(i), i);

			indexToItem = (T[]) universe.toArray();
			itemToIndex = new HashMap<T, Integer>(Math.max((int) (indexToItem.length / .75f) + 1, 16));
			for (int i = 0; i < indexToItem.length; i++)
				itemToIndex.put(indexToItem[i], Integer.valueOf(i));
//		}

		indices = compressed ? new ConciseSet() : new FastSet();
	}

//	/**
//	 * Creates an empty {@link IndexedSet} instance that can contain all
//	 * integral numbers ranging from the given first number to "infinity"
//	 * <p>
//	 * Note that <code>T</code> must be {@link Integer}.
//	 * <p>
//	 * Since there is not an upper bound, the method {@link #universe()} does
//	 * not work when using this constructor.
//	 * <p>
//	 * <b>IMPORTANT:</b> in this case there is no bound check, thus
//	 * {@link #absoluteGet(int)} and {@link #absoluteIndexOf(Object)} methods does not throw
//	 * exceptions when using indices below the lower bound
//	 * <p>
//	 * <b>VERY IMPORTANT!</b> to correctly work and effectively reduce the
//	 * memory allocation, new instances of {@link IndexedSet} <i>must</i> be
//	 * created through the {@link #clone()} or {@link #empty()} methods and
//	 * <i>not</i> by calling many times this constructor with the same
//	 * collection for <code>universe</code>!
//	 * 
//	 * @param first
//	 *            lowest representable integral.
//	 * @param compressed
//	 *            <code>true</code> if a compressed internal representation
//	 *            should be used
//	 */
//	@SuppressWarnings("unchecked")
//	public IndexedSet(int first, boolean compressed) {
//		indexToItem = (Map<Integer, T>) new UncheckedFakeMap(first);
//		itemToIndex = (Map<T, Integer>) new UncheckedFakeMap(-first);
//		indices = compressed ? new ConciseSet() : new FastSet();
//	}

//	/**
//	 * Creates an empty {@link IndexedSet} instance that can contain all
//	 * integral number ranging from the given first number to the given last
//	 * number
//	 * <p>
//	 * Note that <code>T</code> must be {@link Integer}.
//	 * <p>
//	 * <b>VERY IMPORTANT!</b> to correctly work and effectively reduce the
//	 * memory allocation, new instances of {@link IndexedSet} <i>must</i> be
//	 * created through the {@link #clone()} or {@link #empty()} methods and
//	 * <i>not</i> by calling many times this constructor with the same
//	 * collection for <code>universe</code>!
//	 * 
//	 * @param first
//	 *            lowest representable integral.
//	 * @param last
//	 *            highest representable integral.
//	 * @param compressed
//	 *            <code>true</code> if a compressed internal representation
//	 *            should be used
//	 */
//	@SuppressWarnings("unchecked")
//	public IndexedSet(int first, int last, boolean compressed) {
//		if (first > last)
//			throw new IllegalArgumentException("first > last");
//		indexToItem = (Map<Integer, T>) new CheckedFakeMap(last - first + 1, first, false);
//		itemToIndex = (Map<T, Integer>) new CheckedFakeMap(last - first + 1, first, true);
//		indices = compressed ? new ConciseSet() : new FastSet();
//	}

	/**
	 * Creates a {@link IndexedSet} instance from a given universe
	 * mapping
	 * 
	 * @param itemToIndex
	 *            universe item-to-index mapping
	 * @param indexToItem
	 *            universe index-to-item mapping
	 * @param indices
	 *            initial item set
	 */
//	private IndexedSet(Map<T, Integer> itemToIndex, Map<Integer, T> indexToItem, IntSet indices) {
	private IndexedSet(Map<T, Integer> itemToIndex, T[] indexToItem, IntSet indices) {
		this.itemToIndex = itemToIndex;
		this.indexToItem = indexToItem;
		this.indices = indices;
	}
	
	/**
	 * Checks if the given collection is a instance of {@link IndexedSet} with
	 * the same index mappings
	 * 
	 * @param c
	 *            collection to check
	 * @return <code>true</code> if the given collection is a instance of
	 *         {@link IndexedSet} with the same index mappings
	 */
	@SuppressWarnings("unchecked")
	private boolean hasSameIndices(Collection<?> c) {
		// since indices are always re-created through constructor and
		// referenced through clone(), it is sufficient to check just only one
		// mapping table
		return (c instanceof IndexedSet) && (indexToItem == ((IndexedSet) c).indexToItem);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> clone() {
		return new IndexedSet<T>(itemToIndex, indexToItem, indices.clone());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || !(obj instanceof Collection<?>))
			return false;
		IndexedSet<?> other = convert((Collection<?>) obj);
		return this.indexToItem == other.indexToItem
			&& this.itemToIndex == other.itemToIndex
			&& this.indices.equals(other.indices);
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
	public int compareTo(ExtendedSet<T> o) {
		return indices.compareTo(convert(o).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Comparator<? super T> comparator() {
//		if (itemToIndex instanceof IndexedSet<?>.UncheckedFakeMap 
//				|| itemToIndex instanceof IndexedSet<?>.CheckedFakeMap)
//			return null;
		return new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				// compare elements according to the universe ordering
				return itemToIndex.get(o1).compareTo(itemToIndex.get(o2));
			}
		};
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public T first() {
//		return indexToItem.get(indices.first());
		return indexToItem[indices.first()];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T last() {
//		return indexToItem.get(indices.last());
		return indexToItem[indices.last()];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(T e) {
		Integer index = itemToIndex.get(e);
		if (index == null)
			throw new IllegalArgumentException("element not in the current universe");
		return indices.add(index.intValue());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends T> c) {
		return c != null && !c.isEmpty() && indices.addAll(convert(c).indices);
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
	@Override
	public void flip(T e) {
		indices.flip(itemToIndex.get(e).intValue());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Object o) {
		if (o == null)
			return false;
		Integer index = itemToIndex.get(o);
		return index != null && indices.contains(index.intValue());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		return c == null || indices.containsAll(convert(c).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(Collection<? extends T> other) {
		return other == null || indices.containsAny(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAtLeast(Collection<? extends T> other, int minElements) {
		return other != null && !other.isEmpty() && indices.containsAtLeast(convert(other).indices, minElements);
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
	public ExtendedIterator<T> iterator() {
		return new ExtendedIterator<T>() {
			final ExtendedIntIterator itr = indices.intIterator();
			@Override public boolean hasNext() {return itr.hasNext();}
//			@Override public T next() {return indexToItem.get(itr.next());}
			@Override public T next() {return indexToItem[itr.next()];}
			@Override public void skipAllBefore(T element) {itr.skipAllBefore(itemToIndex.get(element).intValue());}
			@Override public void remove() {itr.remove();}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<T> descendingIterator() {
		return new ExtendedIterator<T>() {
			final ExtendedIntIterator itr = indices.descendingIntIterator();
			@Override public boolean hasNext() {return itr.hasNext();}
//			@Override public T next() {return indexToItem.get(itr.next());}
			@Override public T next() {return indexToItem[itr.next()];}
			@Override public void skipAllBefore(T element) {itr.skipAllBefore(itemToIndex.get(element).intValue());}
			@Override public void remove() {itr.remove();}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(Object o) {
		if (o == null)
			return false;
		Integer index = itemToIndex.get(o);
		return index != null && indices.remove(index.intValue());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		return c != null && !c.isEmpty() && indices.removeAll(convert(c).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		if (isEmpty())
			return false;
		if (c == null || c.isEmpty()) {
			indices.clear();
			return true;
		}
		return indices.retainAll(convert(c).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return indices.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> intersection(Collection<? extends T> other) {
		return other == null ? empty() : new IndexedSet<T>(itemToIndex, indexToItem, 
				indices.intersection(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> union(Collection<? extends T> other) {
		return other == null ? clone() : new IndexedSet<T>(itemToIndex, indexToItem, 
				indices.union(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> difference(Collection<? extends T> other) {
		return other == null ? clone() : new IndexedSet<T>(itemToIndex, indexToItem, 
				indices.difference(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> symmetricDifference(Collection<? extends T> other) {
		return other == null ? clone() : new IndexedSet<T>(itemToIndex, indexToItem, 
				indices.symmetricDifference(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> complemented() {
		return new IndexedSet<T>(itemToIndex, indexToItem, indices.complemented());
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
	public int intersectionSize(Collection<? extends T> other) {
		return other == null ? 0 : indices.intersectionSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int unionSize(Collection<? extends T> other) {
		return other == null ? size() : indices.unionSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int symmetricDifferenceSize(Collection<? extends T> other) {
		return other == null ? size() : indices.symmetricDifferenceSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int differenceSize(Collection<? extends T> other) {
		return other == null ? size() : indices.differenceSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int complementSize() {
		return indices.complementSize();
	}

	/**
	 * Returns the collection of all possible elements
	 * 
	 * @return the collection of all possible elements
	 */
	public IndexedSet<T> universe() {
		IntSet allItems = indices.empty();
//		allItems.fill(0, indexToItem.size() - 1);
		allItems.fill(0, indexToItem.length - 1);
		return new IndexedSet<T>(itemToIndex, indexToItem, allItems);
	}

	/**
	 * Returns the index of the given item
	 * 
	 * @param item
	 * @return the index of the given item
	 */
	public Integer absoluteIndexOf(T item) {
		return itemToIndex.get(item);
	}

	/**
	 * Returns the item corresponding to the given index
	 * 
	 * @param i index
	 * @return the item 
	 */
	public T absoluteGet(int i) {
//		return indexToItem.get(i);
		return indexToItem[i];
	}

	/**
	 * Returns the set of indices. Modifications to this set are reflected to
	 * this {@link IndexedSet} instance. Trying to perform operation on
	 * out-of-bound indices will throw an {@link IllegalArgumentException}
	 * exception.
	 * 
	 * @return the index set
	 * @see #absoluteGet(int)
	 * @see #absoluteIndexOf(Object)
	 */
	public IntSet indices() {
		//TODO: optimize indices.headSet
//		if (indexToItem instanceof IndexedSet<?>.UncheckedFakeMap)
//			return indices; 
//		return indices.headSet(indexToItem.size());
		return indices; 
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addLastOf(SortedSet<T> set) {
		if (hasSameIndices(set)) 
			return indices.add(((IndexedSet<?>) set).indices.last());
		return super.addLastOf(set);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addFirstOf(SortedSet<T> set) {
		if (hasSameIndices(set)) 
			return indices.add(((IndexedSet<?>) set).indices.first());
		return super.addFirstOf(set);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeLastOf(SortedSet<T> set) {
		if (hasSameIndices(set)) 
			return indices.remove(((IndexedSet<?>) set).indices.last());
		return super.removeLastOf(set);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeFirstOf(SortedSet<T> set) {
		if (hasSameIndices(set)) 
			return indices.remove(((IndexedSet<?>) set).indices.first());
		return super.removeFirstOf(set);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> empty() {
		return new IndexedSet<T>(itemToIndex, indexToItem, indices.empty());
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
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public IndexedSet<T> convert(Collection<?> c) {
		if (c == null)
			return empty();

		// useless to convert...
		if (hasSameIndices(c))
			return (IndexedSet<T>) c;
		
		// NOTE: cannot call super.convert(c) because of loop
		IndexedSet<T> res = empty();
		for (T t : (Collection<T>) c) 
			res.add(t);
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> convert(Object... e) {
		return (IndexedSet<T>) super.convert(e);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends IndexedSet<T>> powerSet() {
		return (List<? extends IndexedSet<T>>) super.powerSet();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends IndexedSet<T>> powerSet(int min, int max) {
		return (List<? extends IndexedSet<T>>) super.powerSet(min, max);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String debugInfo() {
		return String.format("items = %s\nitemToIndex = %s\nindexToItem = %s\n", 
				indices.debugInfo(), itemToIndex.toString(), indexToItem.toString());
	}

	//TODO
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public IndexedSet<T> unmodifiable() {
//		return new IndexedSet<T>(itemToIndex, indexToItem, indices.unmodifiable());
//	}
//	
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public IndexedSet<T> subSet(T fromElement, T toElement) {
//		return new IndexedSet<T>(itemToIndex, indexToItem, 
//				indices.subSet(itemToIndex.get(fromElement), itemToIndex.get(toElement)));
//	}
//	
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public IndexedSet<T> headSet(T toElement) {
//		return new IndexedSet<T>(itemToIndex, indexToItem, 
//				indices.headSet(itemToIndex.get(toElement)));
//	}
//	
//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	public IndexedSet<T> tailSet(T fromElement) {
//		return new IndexedSet<T>(itemToIndex, indexToItem, 
//				indices.tailSet(itemToIndex.get(fromElement)));
//	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T get(int i) {
//		return indexToItem.get(indices.get(i));
		return indexToItem[indices.get(i)];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int indexOf(T e) {
		return indices.indexOf(itemToIndex.get(e).intValue());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear(T from, T to) {
		indices.clear(itemToIndex.get(from).intValue(), itemToIndex.get(to).intValue());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fill(T from, T to) {
		indices.fill(itemToIndex.get(from).intValue(), itemToIndex.get(to).intValue());
	}
}
