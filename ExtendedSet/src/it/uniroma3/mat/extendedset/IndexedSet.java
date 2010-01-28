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
 * @see IndexedSet
 */
public class IndexedSet<T> extends AbstractExtendedSet<T> {
	// indices
	private final AbstractExtendedSet<Integer> items;

	// mapping to translate items to indices and vice-versa
	private final Map<T, Integer> itemToIndex;
	private final T[] indexToItem;

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
	public IndexedSet(Collection<T> universe, boolean compressed) {
		// index-to-item map
		if (universe instanceof Set)
			indexToItem = (T[]) universe.toArray();
		else
			// remove duplicates
			indexToItem = (T[]) (new LinkedHashSet<T>(universe)).toArray();

		// item-to-index map
		itemToIndex = new HashMap<T, Integer>(Math.max((int) (indexToItem.length / .75f) + 1, 16));
		for (int i = 0; i < indexToItem.length; i++)
			itemToIndex.put(indexToItem[i], i);

		// items
		if (compressed)
			items = new ConciseSet();
		else
			items = new FastSet();
	}

	/**
	 * Creates a {@link IndexedSet} instance from a given universe
	 * mapping
	 * 
	 * @param itemToIndex
	 *            universe item-to-index mapping
	 * @param indexToItem
	 *            universe index-to-item mapping
	 * @param items
	 *            initial item set
	 */
	private IndexedSet(Map<T, Integer> itemToIndex, T[] indexToItem, AbstractExtendedSet<Integer> items) {
		this.itemToIndex = itemToIndex;
		this.indexToItem = indexToItem;
		this.items = items;
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
		return new IndexedSet<T>(this.itemToIndex, this.indexToItem, this.items.clone());
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
		if (this.getClass() != obj.getClass())
			return false;
		IndexedSet<?> other = (IndexedSet<?>) obj;
		return this.indexToItem == other.indexToItem
				&& this.itemToIndex == other.itemToIndex
				&& this.items.equals(other.items);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return items.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(ExtendedSet<T> o) {
		return items.compareTo(asIndexedSet(o).items);
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
		return indexToItem[items.first()];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T last() {
		return indexToItem[items.last()];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean add(T e) {
		Integer index = itemToIndex.get(e);
		if (index == null)
			throw new IllegalArgumentException("element not in the current universe");
		return items.add(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends T> c) {
		return c != null && !c.isEmpty() && this.items.addAll(asIndexedSet(c).items);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		items.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Object o) {
		if (o == null)
			return false;
		Integer index = itemToIndex.get(o);
		return index != null && items.contains(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		return c != null && !c.isEmpty() && this.items.containsAll(asIndexedSet(c).items);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(Collection<? extends T> other) {
		return other != null && !other.isEmpty() && this.items.containsAny(asIndexedSet(other).items);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAtLeast(Collection<? extends T> other, int minElements) {
		return other != null && !other.isEmpty() && this.items.containsAtLeast(asIndexedSet(other).items, minElements);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return items.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private final Iterator<Integer> indexIterator = items.iterator();

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
				return indexToItem[indexIterator.next()];
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
			private final Iterator<Integer> indexIterator = items.descendingIterator();

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
				return indexToItem[indexIterator.next()];
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
		return index != null && items.remove(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		return c != null && !c.isEmpty() && this.items.removeAll(asIndexedSet(c).items);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		return c != null && !c.isEmpty() && this.items.retainAll(asIndexedSet(c).items);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return items.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> getIntersection(Collection<? extends T> other) {
		return new IndexedSet<T>(itemToIndex, indexToItem, 
				this.items.getIntersection(asIndexedSet(other).items));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> getUnion(Collection<? extends T> other) {
		return new IndexedSet<T>(itemToIndex, indexToItem, 
				this.items.getUnion(asIndexedSet(other).items));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> getDifference(Collection<? extends T> other) {
		return new IndexedSet<T>(itemToIndex, indexToItem, 
				this.items.getDifference(asIndexedSet(other).items));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> getSymmetricDifference(Collection<? extends T> other) {
		return new IndexedSet<T>(itemToIndex, indexToItem, 
				this.items.getSymmetricDifference(asIndexedSet(other).items));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> getComplement() {
		return new IndexedSet<T>(itemToIndex, indexToItem, 
				this.items.getComplement());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complement() {
		this.items.complement();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int intersectionSize(Collection<? extends T> other) {
		return this.items.intersectionSize(asIndexedSet(other).items);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int unionSize(Collection<? extends T> other) {
		return this.items.unionSize(asIndexedSet(other).items);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int symmetricDifferenceSize(Collection<? extends T> other) {
		return this.items.symmetricDifferenceSize(asIndexedSet(other).items);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int differenceSize(Collection<? extends T> other) {
		return this.items.differenceSize(asIndexedSet(other).items);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int complementSize() {
		return this.items.complementSize();
	}

	/**
	 * Returns the collection of all possible elements
	 * 
	 * @return the collection of all possible elements
	 */
	public IndexedSet<T> universe() {
		AbstractExtendedSet<Integer> allItems = items.emptySet();
		allItems.add(indexToItem.length - 1);
		allItems.complement();
		allItems.add(indexToItem.length - 1);
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
		return indexToItem[i];
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
	public AbstractExtendedSet<Integer> indices() {
		return items.subSet(0, indexToItem.length);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addLastOf(SortedSet<T> set) {
		if (hasSameIndices(set)) 
			return items.add(((IndexedSet<?>) set).items.last());
		return super.addLastOf(set);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addFirstOf(SortedSet<T> set) {
		if (hasSameIndices(set)) 
			return items.add(((IndexedSet<?>) set).items.first());
		return super.addFirstOf(set);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeLastOf(SortedSet<T> set) {
		if (hasSameIndices(set)) 
			return items.remove(((IndexedSet<?>) set).items.last());
		return super.removeLastOf(set);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeFirstOf(SortedSet<T> set) {
		if (hasSameIndices(set)) 
			return items.remove(((IndexedSet<?>) set).items.first());
		return super.removeFirstOf(set);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public IndexedSet<T> emptySet() {
		return new IndexedSet<T>(itemToIndex, indexToItem, items.emptySet());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double bitmapCompressionRatio() {
		return items.bitmapCompressionRatio();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double collectionCompressionRatio() {
		return items.collectionCompressionRatio();
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
		// useless to convert...
		if (hasSameIndices(c))
			return (IndexedSet<T>) c;

		// convert the collection
		IndexedSet<T> res = emptySet();
		Collection<Integer> indices = new ArrayList<Integer>();
		for (Object o : c) 
			indices.add(itemToIndex.get(o));
		res.items.addAll(indices);
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
				items.debugInfo(), itemToIndex.toString(), indexToItem.toString());
	}
	
	/**
	 * Read-only view of the set
	 * <p>
	 * This class override <i>all</i> public and protected methods of the
	 * parent class {@link IndexedSet} so that any subclass will be correctly
	 * handled.
	 */
	protected class UnmodifiableIndexedSet extends IndexedSet<T> {
		private UnmodifiableIndexedSet(Map<T, Integer> itemToIndex, T[] indexToItem, AbstractExtendedSet<Integer> items) {
			super(itemToIndex, indexToItem, items);
		}

		/*
		 * Writing methods
		 */
		/** {@inheritDoc} */ @Override public boolean add(T e) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean addAll(Collection<? extends T> c) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean addFirstOf(SortedSet<T> set) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean addLastOf(SortedSet<T> set) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean remove(Object o) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean removeAll(Collection<?> c) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean removeFirstOf(SortedSet<T> set) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean removeLastOf(SortedSet<T> set) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean retainAll(Collection<?> c) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public void clear() {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public void clear(T from, T to) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public void fill(T from, T to) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public void complement() {throw UNSUPPORTED;}
		
		/** {@inheritDoc} */ @Override
		public Iterator<T> iterator() {
			final Iterator<T> itr = IndexedSet.this.iterator();
			return new Iterator<T>() {
				@Override public boolean hasNext() {return itr.hasNext();}
				@Override public T next() {return itr.next();}
				@Override public void remove() {throw UNSUPPORTED;}
			};
		}

		/*
		 * Read-only methods
		 */
		/** {@inheritDoc} */ @Override public IndexedSet<T> getIntersection(Collection<? extends T> other) {return IndexedSet.this.getIntersection(other);}
		/** {@inheritDoc} */ @Override public IndexedSet<T> getDifference(Collection<? extends T> other) {return IndexedSet.this.getDifference(other);}
		/** {@inheritDoc} */ @Override public IndexedSet<T> getUnion(Collection<? extends T> other) {return IndexedSet.this.getUnion(other);}
		/** {@inheritDoc} */ @Override public IndexedSet<T> getSymmetricDifference(Collection<? extends T> other) {return IndexedSet.this.getSymmetricDifference(other);}
		/** {@inheritDoc} */ @Override public IndexedSet<T> getComplement() {return IndexedSet.this.getComplement();}
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
			// useless to clone
			return this; 
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
		public AbstractExtendedSet<Integer> indices() {
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
		return new UnmodifiableIndexedSet(itemToIndex, indexToItem, items);
	}
}
