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
import java.util.Iterator;
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
 * @see MatrixSet
 */
public class IndexedSet<T> extends AbstractExtendedSet<T> {
	// indices
	private final AbstractExtendedSet<Integer> indices;

	// mapping to translate items to indices and vice-versa
	private final Map<T, Integer> itemToIndex;
	private final Map<Integer, T> indexToItem;

	/**
	 * Used when the universe is a sequence of integral numbers
	 */
	private class FakeMap implements Map<Integer, Integer> {
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
		public FakeMap(int size, int shift, boolean inverse) {
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
				throw new IllegalArgumentException(key.toString());
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
	 * Creates an empty {@link IndexedSet} based on a given collection that
	 * represents the set of <i>all</i> possible items that can be added to the
	 * {@link IndexedSet} instance.
	 * <p>
	 * <b>VERY IMPORTANT!</b> to correctly work and effectively reduce the
	 * memory allocation, new instances of {@link IndexedSet} <i>must</i>
	 * be created through the {@link #clone()} or {@link #emptySet()}
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
			indexToItem = (Map<Integer, T>) new FakeMap(universe.size(), shift, false);
			itemToIndex = (Map<T, Integer>) new FakeMap(universe.size(), shift, true);
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
	private IndexedSet(Map<T, Integer> itemToIndex, Map<Integer, T> indexToItem, AbstractExtendedSet<Integer> indices) {
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
		if (!(obj instanceof IndexedSet))
			return false;
		IndexedSet<?> other = (IndexedSet<?>) obj;
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
		return indices.compareTo(asIndexedSet(o).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Comparator<? super T> comparator() {
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
		return c != null && !c.isEmpty() && this.indices.addAll(asIndexedSet(c).indices);
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
		return c != null && !c.isEmpty() && this.indices.containsAll(asIndexedSet(c).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(Collection<? extends T> other) {
		return other != null && !other.isEmpty() && this.indices.containsAny(asIndexedSet(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAtLeast(Collection<? extends T> other, int minElements) {
		return other != null && !other.isEmpty() && this.indices.containsAtLeast(asIndexedSet(other).indices, minElements);
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
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private final Iterator<Integer> indexIterator = indices.iterator();

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
			public void remove() {
				indexIterator.remove();
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<T> descendingIterator() {
		return new Iterator<T>() {
			private final Iterator<Integer> indexIterator = indices.descendingIterator();

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
		return c != null && !c.isEmpty() && this.indices.removeAll(asIndexedSet(c).indices);
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
		return this.indices.retainAll(asIndexedSet(c).indices);
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
	public IndexedSet<T> intersectionSet(Collection<? extends T> other) {
		return other == null ? emptySet() : new IndexedSet<T>(itemToIndex, indexToItem, 
				this.indices.intersectionSet(asIndexedSet(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> unionSet(Collection<? extends T> other) {
		return other == null ? clone() : new IndexedSet<T>(itemToIndex, indexToItem, 
				this.indices.unionSet(asIndexedSet(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> differenceSet(Collection<? extends T> other) {
		return other == null ? clone() : new IndexedSet<T>(itemToIndex, indexToItem, 
				this.indices.differenceSet(asIndexedSet(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> symmetricDifferenceSet(Collection<? extends T> other) {
		return other == null ? clone() : new IndexedSet<T>(itemToIndex, indexToItem, 
				this.indices.symmetricDifferenceSet(asIndexedSet(other).indices));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> complementSet() {
		return new IndexedSet<T>(itemToIndex, indexToItem, 
				this.indices.complementSet());
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
		return other == null ? 0 : this.indices.intersectionSize(asIndexedSet(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int unionSize(Collection<? extends T> other) {
		return other == null ? size() : this.indices.unionSize(asIndexedSet(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int symmetricDifferenceSize(Collection<? extends T> other) {
		return other == null ? size() : this.indices.symmetricDifferenceSize(asIndexedSet(other).indices);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int differenceSize(Collection<? extends T> other) {
		return other == null ? size() : this.indices.differenceSize(asIndexedSet(other).indices);
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
		AbstractExtendedSet<Integer> allItems = indices.emptySet();
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
	public Integer indexOf(T item) {
		return itemToIndex.get(item);
	}

	/**
	 * Returns the item corresponding to the given index
	 * 
	 * @param i index
	 * @return the item 
	 */
	public T get(int i) {
		return indexToItem.get(i);
	}

	/**
	 * Returns the set of indices. Modifications to this set are reflected to
	 * this {@link IndexedSet} instance. Trying to perform operation on
	 * out-of-bound indices will throw an {@link IllegalArgumentException}
	 * exception.
	 * 
	 * @return the index set
	 * @see #get(int)
	 * @see #indexOf(Object)
	 */
	public ExtendedSet<Integer> indices() {
		return indices.subSet(0, indexToItem.size());
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
	public IndexedSet<T> emptySet() {
		return new IndexedSet<T>(itemToIndex, indexToItem, indices.emptySet());
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
	 * Converts a given {@link Collection} instance to a {@link IndexedSet}
	 * instance
	 * 
	 * @param c
	 *            collection to use to generate the {@link IndexedSet} instance
	 * @return the generated {@link IndexedSet} instance. <b>NOTE:</b> if the
	 *         parameter is an instance of {@link IndexedSet} with the same
	 *         index mapping, the method returns this instance.
	 */
	@SuppressWarnings("unchecked")
	public IndexedSet<T> asIndexedSet(Collection<?> c) {
		if (c == null)
			return new IndexedSet<T>(itemToIndex, indexToItem, indices.emptySet());

		// useless to convert...
		if (hasSameIndices(c))
			return (IndexedSet<T>) c;

		// convert the collection
		IndexedSet<T> res = emptySet();
		Collection<Integer> is = new ArrayList<Integer>();
		for (Object o : c) 
			is.add(itemToIndex.get(o));
		res.indices.addAll(is);
		return res;
	}

	/**
	 * Converts the given integers to a {@link ConciseSet} instance
	 * 
	 * @param e
	 *            integers to put within the new instance of
	 *            {@link ConciseSet}
	 * @return new instance of {@link ConciseSet}
	 */
	public IndexedSet<T> asIndexedSet(T... e) {
		if (e == null)
			return new IndexedSet<T>(itemToIndex, indexToItem, indices.emptySet());
		if (e.length == 1) {
			IndexedSet<T> res = emptySet();
			res.add(e[0]);
			return res;
		} 
			
		return asIndexedSet(Arrays.asList(e));
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
	 * Read-only view of the set
	 * <p>
	 * This class override <i>all</i> public and protected methods of the
	 * parent class {@link IndexedSet} so that any subclass will be correctly
	 * handled.
	 */
	private class UnmodifiableIndexedSet extends IndexedSet<T> implements Unmodifiable {
		private UnmodifiableIndexedSet() {
			super(itemToIndex, indexToItem, indices);
		}

		/*
		 * Writing methods
		 */
		/** {@inheritDoc} */ @Override public boolean add(T e) {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		/** {@inheritDoc} */ @Override public boolean addAll(Collection<? extends T> c) {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		/** {@inheritDoc} */ @Override public boolean addFirstOf(SortedSet<T> set) {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		/** {@inheritDoc} */ @Override public boolean addLastOf(SortedSet<T> set) {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		/** {@inheritDoc} */ @Override public boolean remove(Object o) {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		/** {@inheritDoc} */ @Override public boolean removeAll(Collection<?> c) {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		/** {@inheritDoc} */ @Override public boolean removeFirstOf(SortedSet<T> set) {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		/** {@inheritDoc} */ @Override public boolean removeLastOf(SortedSet<T> set) {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		/** {@inheritDoc} */ @Override public boolean retainAll(Collection<?> c) {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		/** {@inheritDoc} */ @Override public void clear() {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		/** {@inheritDoc} */ @Override public void clear(T from, T to) {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		/** {@inheritDoc} */ @Override public void fill(T from, T to) {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		/** {@inheritDoc} */ @Override public void complement() {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		
		/** {@inheritDoc} */ @Override
		public Iterator<T> iterator() {
			final Iterator<T> itr = IndexedSet.this.iterator();
			return new Iterator<T>() {
				@Override public boolean hasNext() {return itr.hasNext();}
				@Override public T next() {return itr.next();}
				@Override public void remove() {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
			};
		}

		/*
		 * Read-only methods
		 */
		/** {@inheritDoc} */ @Override public IndexedSet<T> intersectionSet(Collection<? extends T> other) {return IndexedSet.this.intersectionSet(other);}
		/** {@inheritDoc} */ @Override public IndexedSet<T> differenceSet(Collection<? extends T> other) {return IndexedSet.this.differenceSet(other);}
		/** {@inheritDoc} */ @Override public IndexedSet<T> unionSet(Collection<? extends T> other) {return IndexedSet.this.unionSet(other);}
		/** {@inheritDoc} */ @Override public IndexedSet<T> symmetricDifferenceSet(Collection<? extends T> other) {return IndexedSet.this.symmetricDifferenceSet(other);}
		/** {@inheritDoc} */ @Override public IndexedSet<T> complementSet() {return IndexedSet.this.complementSet();}
		/** {@inheritDoc} */ @Override public IndexedSet<T> emptySet() {return IndexedSet.this.emptySet();}
		/** {@inheritDoc} */ @Override public int intersectionSize(Collection<? extends T> other) {return IndexedSet.this.intersectionSize(other);}
		/** {@inheritDoc} */ @Override public int differenceSize(Collection<? extends T> other) {return IndexedSet.this.differenceSize(other);}
		/** {@inheritDoc} */ @Override public int unionSize(Collection<? extends T> other) {return IndexedSet.this.unionSize(other);}
		/** {@inheritDoc} */ @Override public int symmetricDifferenceSize(Collection<? extends T> other) {return IndexedSet.this.symmetricDifferenceSize(other);}
		/** {@inheritDoc} */ @Override public int complementSize() {return IndexedSet.this.complementSize();}
		/** {@inheritDoc} */ @Override public int powerSetSize() {return IndexedSet.this.powerSetSize();}
		/** {@inheritDoc} */ @Override public int powerSetSize(int min, int max) {return IndexedSet.this.powerSetSize(min, max);}
		/** {@inheritDoc} */ @Override public int size() {return IndexedSet.this.size();}
		/** {@inheritDoc} */ @Override public boolean isEmpty() {return IndexedSet.this.isEmpty();}
		/** {@inheritDoc} */ @Override public boolean contains(Object o) {return IndexedSet.this.contains(o);}
		/** {@inheritDoc} */ @Override public boolean containsAll(Collection<?> c) {return IndexedSet.this.containsAll(c);}
		/** {@inheritDoc} */ @Override public boolean containsAny(Collection<? extends T> other) {return IndexedSet.this.containsAny(other);}
		/** {@inheritDoc} */ @Override public boolean containsAtLeast(Collection<? extends T> other, int minElements) {return IndexedSet.this.containsAtLeast(other, minElements);}
		/** {@inheritDoc} */ @Override public T first() {return IndexedSet.this.first();}
		/** {@inheritDoc} */ @Override public T last() {return IndexedSet.this.last();}
		/** {@inheritDoc} */ @Override public Comparator<? super T> comparator() {return IndexedSet.this.comparator();}
		/** {@inheritDoc} */ @Override public int compareTo(ExtendedSet<T> o) {return IndexedSet.this.compareTo(o);}
		/** {@inheritDoc} */ @Override public boolean equals(Object o) {return IndexedSet.this.equals(o);}
		/** {@inheritDoc} */ @Override public int hashCode() {return IndexedSet.this.hashCode();}
		/** {@inheritDoc} */ @Override public Iterable<T> descending() {return IndexedSet.this.descending();}
		/** {@inheritDoc} */ @Override public Iterator<T> descendingIterator() {return IndexedSet.this.descendingIterator();}
		/** {@inheritDoc} */ @Override public List<? extends IndexedSet<T>> powerSet() {return IndexedSet.this.powerSet();}
		/** {@inheritDoc} */ @Override public List<? extends IndexedSet<T>> powerSet(int min, int max) {return IndexedSet.this.powerSet(min, max);}
		/** {@inheritDoc} */ @Override public double bitmapCompressionRatio() {return IndexedSet.this.bitmapCompressionRatio();}
		/** {@inheritDoc} */ @Override public double collectionCompressionRatio() {return IndexedSet.this.collectionCompressionRatio();}
		/** {@inheritDoc} */ @Override public String debugInfo() {return IndexedSet.this.debugInfo();}
		/** {@inheritDoc} */ @Override public Object[] toArray() {return IndexedSet.this.toArray();}
		/** {@inheritDoc} */ @Override public <X> X[] toArray(X[] a) {return IndexedSet.this.toArray(a);}
		/** {@inheritDoc} */ @Override public String toString() {return IndexedSet.this.toString();}
		/** {@inheritDoc} */ @Override public T position(int i) {return IndexedSet.this.position(i);}

		/*
		 * Special purpose methods
		 */
		/* NOTE: the following methods do not have to be overridden:
		 * - public IndexedSet<T> headSet(T toElement) {}
		 * - public IndexedSet<T> subSet(T fromElement, T toElement) {}
		 * - public IndexedSet<T> tailSet(T fromElement) {
		 * In this way, modification to the subview will not be permitted
		 */
		/** {@inheritDoc} */ @Override 
		public IndexedSet<T> clone() {
			return IndexedSet.this.clone(); 
		}
		/** {@inheritDoc} */ @Override 
		public IndexedSet<T> unmodifiable() {
			// useless to create another instance
			return this;
		}
		
		/*
		 * Additional methods with respect to ExtendedSet
		 */
		
		/** {@inheritDoc} */
		@Override
		public ExtendedSet<Integer> indices() {
			return IndexedSet.this.indices().unmodifiable();
		}

		/** {@inheritDoc} */
		@Override
		public T get(int i) {
			return IndexedSet.this.get(i);
		}

		/** {@inheritDoc} */
		@Override
		public Integer indexOf(T item) {
			return IndexedSet.this.indexOf(item);
		}

		/** {@inheritDoc} */
		@Override
		public IndexedSet<T> universe() {
			return IndexedSet.this.universe();
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
	public T position(int i) {
		return indexToItem.get(indices.position(i));
	}
}
