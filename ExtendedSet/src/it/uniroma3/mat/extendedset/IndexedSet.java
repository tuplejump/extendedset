/* (c) 2010 Alessandro Colantonio
 * <mailto:colanton@mat.uniroma3.it>
 * <http://ricerca.mat.uniroma3.it/users/colanton>
 *  
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */ 

package it.uniroma3.mat.extendedset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * An {@link AbstractExtendedSet} implementation that maps elements to an integer set
 * referred to as "indices".
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
public class IndexedSet<T> extends AbstractExtendedSet<T> {
	// indices
	private final ExtendedSet<Integer> indices;

	// mapping to translate items to indices and vice-versa
	private final Map<T, Integer> itemToIndex;
	private final Map<Integer, T> indexToItem;

	/**
	 * Used when the universe is a sequence of integral numbers
	 */
	private class CheckedFakeMap implements Map<Integer, Integer> {
		private final int size;
		private final int shift;
		private final boolean inverse;
		
		/**
		 * Specifies the first and the last element of the map
		 * 
		 * @param size
		 *            sequence length
		 * @param shift
		 *            first element of the sequence
		 * @param inverse
		 *            <code>false</code> if it is used as a index-to-item map,
		 *            <code>false</code> if it is used as a item-to-index map
		 */
		public CheckedFakeMap(int size, int shift, boolean inverse) {
			this.size = size;
			this.shift = shift;
			this.inverse = inverse;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Integer get(Object key) {
			Integer value = (Integer) key - (inverse ? shift : 0);
			if (value.compareTo(0) < 0 || value.compareTo(size) >= 0)
				throw new IndexOutOfBoundsException(key.toString());
			return value + (inverse ? 0 : shift);
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public int size() {
			return size;
		}

		/** {@inheritDoc} */ @Override public void clear() {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public boolean containsKey(Object key) {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public boolean containsValue(Object value) {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public Set<Entry<Integer, Integer>> entrySet() {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public boolean isEmpty() {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public Set<Integer> keySet() {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public Integer put(Integer key, Integer value) {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public void putAll(Map<? extends Integer, ? extends Integer> m) {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public Integer remove(Object key) {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public Collection<Integer> values() {throw new UnsupportedOperationException();}
	}
	
	/**
	 * Used when the universe is a sequence of integral numbers
	 */
	private class UncheckedFakeMap implements Map<Integer, Integer> {
		private final int shift;
		
		/**
		 * Specifies the first element of the map
		 * 
		 * @param shift
		 *            first element of the sequence
		 */
		public UncheckedFakeMap(int shift) {
			this.shift = shift;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * There is no bound check, thus {@link IndexedSet#absoluteGet(int)} and
		 * {@link IndexedSet#absoluteIndexOf(Object)} methods does not throw exceptions
		 * when using indices below the lower bound
		 */
		@Override
		public Integer get(Object key) {
			return (Integer) key + shift;
		}
		
		/**
		 * {@inheritDoc}
		 * <p>
		 * By not supporting this method we make the method
		 * {@link IndexedSet#universe()} not working
		 */
		@Override
		public int size() {
			throw new UnsupportedOperationException();
		}

		/** {@inheritDoc} */ @Override public void clear() {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public boolean containsKey(Object key) {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public boolean containsValue(Object value) {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public Set<Entry<Integer, Integer>> entrySet() {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public boolean isEmpty() {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public Set<Integer> keySet() {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public Integer put(Integer key, Integer value) {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public void putAll(Map<? extends Integer, ? extends Integer> m) {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public Integer remove(Object key) {throw new UnsupportedOperationException();}
		/** {@inheritDoc} */ @Override public Collection<Integer> values() {throw new UnsupportedOperationException();}
	}

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
		boolean isSequence = true;
		int shift = 0;
		try {
			SortedSet<Integer> ss = (SortedSet) universe;
			shift = ss.first();
			isSequence = ss.comparator() == null && ss.last().equals(shift + ss.size() - 1);
		} catch (ClassCastException e) {
			isSequence = false;
		}

		if (isSequence) {
			indexToItem = (Map<Integer, T>) new CheckedFakeMap(universe.size(), shift, false);
			itemToIndex = (Map<T, Integer>) new CheckedFakeMap(universe.size(), shift, true);
		} else {
			// NOTE: it removes duplicates and keeps the order
			indexToItem = new ArrayMap<T>(universe instanceof Set ? 
					(T[]) universe.toArray() :
					(T[]) (new LinkedHashSet<T>(universe)).toArray());

			itemToIndex = new HashMap<T, Integer>(Math.max((int) (indexToItem.size() / .75f) + 1, 16));
			for (int i = 0; i < indexToItem.size(); i++)
				itemToIndex.put(indexToItem.get(i), i);
		}

		// indices
		if (compressed)
			indices = new ConciseSet();
		else
			indices = new FastSet();
	}

	/**
	 * Creates an empty {@link IndexedSet} instance that can contain all
	 * integral number ranging from the given first number to "infinity"
	 * <p>
	 * Note that <code>T</code> must be {@link Integer}.
	 * <p>
	 * Since there is not an upper bound, the method {@link #universe()} does
	 * not work when using this constructor.
	 * <p>
	 * <b>IMPORTANT:</b> in this case there is no bound check, thus
	 * {@link #absoluteGet(int)} and {@link #absoluteIndexOf(Object)} methods does not throw
	 * exceptions when using indices below the lower bound
	 * <p>
	 * <b>VERY IMPORTANT!</b> to correctly work and effectively reduce the
	 * memory allocation, new instances of {@link IndexedSet} <i>must</i> be
	 * created through the {@link #clone()} or {@link #empty()} methods and
	 * <i>not</i> by calling many times this constructor with the same
	 * collection for <code>universe</code>!
	 * 
	 * @param first
	 *            lowest representable integral.
	 * @param compressed
	 *            <code>true</code> if a compressed internal representation
	 *            should be used
	 */
	@SuppressWarnings("unchecked")
	public IndexedSet(int first, boolean compressed) {
		// maps
		indexToItem = (Map<Integer, T>) new UncheckedFakeMap(first);
		itemToIndex = (Map<T, Integer>) new UncheckedFakeMap(-first);

		// indices
		if (compressed)
			indices = new ConciseSet();
		else
			indices = new FastSet();
	}

	/**
	 * Creates an empty {@link IndexedSet} instance that can contain all
	 * integral number ranging from the given first number to the given last
	 * number
	 * <p>
	 * Note that <code>T</code> must be {@link Integer}.
	 * <p>
	 * <b>VERY IMPORTANT!</b> to correctly work and effectively reduce the
	 * memory allocation, new instances of {@link IndexedSet} <i>must</i> be
	 * created through the {@link #clone()} or {@link #empty()} methods and
	 * <i>not</i> by calling many times this constructor with the same
	 * collection for <code>universe</code>!
	 * 
	 * @param first
	 *            lowest representable integral.
	 * @param last
	 *            highest representable integral.
	 * @param compressed
	 *            <code>true</code> if a compressed internal representation
	 *            should be used
	 */
	@SuppressWarnings("unchecked")
	public IndexedSet(int first, int last, boolean compressed) {
		// maps
		if (first > last)
			throw new IllegalArgumentException("first > last");
		indexToItem = (Map<Integer, T>) new CheckedFakeMap(last - first + 1, first, false);
		itemToIndex = (Map<T, Integer>) new CheckedFakeMap(last - first + 1, first, true);

		// indices
		if (compressed)
			indices = new ConciseSet();
		else
			indices = new FastSet();
	}

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
	private IndexedSet(Map<T, Integer> itemToIndex, Map<Integer, T> indexToItem, ExtendedSet<Integer> indices) {
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
		return new IndexedSet<T>(this.itemToIndex, this.indexToItem, this.indices.clone());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
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
		if (itemToIndex instanceof IndexedSet<?>.UncheckedFakeMap 
				|| itemToIndex instanceof IndexedSet<?>.CheckedFakeMap)
			return null;
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
		return indexToItem.get(indices.first());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T last() {
		return indexToItem.get(indices.last());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(T e) {
		Integer index = itemToIndex.get(e);
		if (index == null)
			throw new IllegalArgumentException("element not in the current universe");
		return indices.add(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends T> c) {
		return c != null && !c.isEmpty() && this.indices.addAll(convert(c).indices);
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
		indices.flip(itemToIndex.get(e));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Object o) {
		if (o == null)
			return false;
		Integer index = itemToIndex.get(o);
		return index != null && indices.contains(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		return c != null && !c.isEmpty() && this.indices.containsAll(convert(c).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(Collection<? extends T> other) {
		return other != null && !other.isEmpty() && this.indices.containsAny(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAtLeast(Collection<? extends T> other, int minElements) {
		return other != null && !other.isEmpty() && this.indices.containsAtLeast(convert(other).indices, minElements);
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
			private final ExtendedIterator<Integer> indexIterator = indices.iterator();

			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean hasNext() {
				return indexIterator.hasNext();
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public T next() {
				return indexToItem.get(indexIterator.next());
			}
			
			/**
			 * {@inheritDoc}
			 */
			@Override
			public void skipAllBefore(T element) {
				indexIterator.skipAllBefore(itemToIndex.get(element));
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void remove() {
				indexIterator.remove();
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<T> descendingIterator() {
		return new ExtendedIterator<T>() {
			private final ExtendedIterator<Integer> indexIterator = indices.descendingIterator();

			/**
			 * {@inheritDoc}
			 */
			@Override
			public boolean hasNext() {
				return indexIterator.hasNext();
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public T next() {
				return indexToItem.get(indexIterator.next());
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void skipAllBefore(T element) {
				indexIterator.skipAllBefore(itemToIndex.get(element));
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void remove() {
				indexIterator.remove();
			}
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
		return index != null && indices.remove(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		return c != null && !c.isEmpty() && this.indices.removeAll(convert(c).indices);
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
		return this.indices.retainAll(convert(c).indices);
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
				this.indices.intersection(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> union(Collection<? extends T> other) {
		return other == null ? clone() : new IndexedSet<T>(itemToIndex, indexToItem, 
				this.indices.union(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> difference(Collection<? extends T> other) {
		return other == null ? clone() : new IndexedSet<T>(itemToIndex, indexToItem, 
				this.indices.difference(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> symmetricDifference(Collection<? extends T> other) {
		return other == null ? clone() : new IndexedSet<T>(itemToIndex, indexToItem, 
				this.indices.symmetricDifference(convert(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> complemented() {
		return new IndexedSet<T>(itemToIndex, indexToItem, 
				this.indices.complemented());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complement() {
		this.indices.complement();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int intersectionSize(Collection<? extends T> other) {
		return other == null ? 0 : this.indices.intersectionSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int unionSize(Collection<? extends T> other) {
		return other == null ? size() : this.indices.unionSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int symmetricDifferenceSize(Collection<? extends T> other) {
		return other == null ? size() : this.indices.symmetricDifferenceSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int differenceSize(Collection<? extends T> other) {
		return other == null ? size() : this.indices.differenceSize(convert(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int complementSize() {
		return this.indices.complementSize();
	}

	/**
	 * Returns the collection of all possible elements
	 * 
	 * @return the collection of all possible elements
	 */
	public IndexedSet<T> universe() {
		ExtendedSet<Integer> allItems = indices.empty();
		allItems.add(indexToItem.size() - 1);
		allItems.complement();
		allItems.add(indexToItem.size() - 1);
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
		return indexToItem.get(i);
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
	public ExtendedSet<Integer> indices() {
		if (indexToItem instanceof IndexedSet<?>.UncheckedFakeMap)
			return indices; 
		return indices.headSet(indexToItem.size());
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
			return new IndexedSet<T>(itemToIndex, indexToItem, indices.empty());

		// useless to convert...
		if (hasSameIndices(c))
			return (IndexedSet<T>) c;
		if (c instanceof AbstractExtendedSet.ExtendedSubSet) {
			ExtendedSet<?> x = ((AbstractExtendedSet.ExtendedSubSet) c).container();
			if (hasSameIndices(x)) 
				return (IndexedSet<T>) ((AbstractExtendedSet.ExtendedSubSet) c).convert(c);
		}

		// convert the collection
		IndexedSet<T> res = empty();
		Collection<Integer> is = new ArrayList<Integer>();
		for (Object o : c) 
			is.add(itemToIndex.get(o));
		res.indices.addAll(is);
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public IndexedSet<T> convert(Object... e) {
		if (e == null)
			return new IndexedSet<T>(itemToIndex, indexToItem, indices.empty());
		if (e.length == 1) {
			IndexedSet<T> res = empty();
			res.add((T) e[0]);
			return res;
		} 
			
		return convert(Arrays.asList(e));
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

	/**
	 * Add specific methods to {@link AbstractExtendedSet#UnmodifiableExtendedSet} 
	 */
	protected class UnmodifiableIndexedSet extends IndexedSet<T>  {
		/**
		 * Create an instance with unmodifiable indices
		 */
		public UnmodifiableIndexedSet() {
			super(itemToIndex, indexToItem, indices.unmodifiable());
		}
	}
	
	/**
	 * @return the read-only version of the current set
	 */
	@Override
	public IndexedSet<T> unmodifiable() {
		return new UnmodifiableIndexedSet();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public T get(int i) {
		return indexToItem.get(indices.get(i));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int indexOf(T e) {
		return indices.indexOf(itemToIndex.get(e));
	}
}
