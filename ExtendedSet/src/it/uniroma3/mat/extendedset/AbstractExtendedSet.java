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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;

/**
 * An implementation of {@link SortedSet} with fast
 * intersection/union/difference and other set operations.
 * <p>
 * It collects all the basic functionalities and the interface of
 * {@link ConciseSet}, {@link FastSet}, and {@link IndexedSet}.
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @param <T>
 *            the type of elements maintained by this set
 * 
 * @see ExtendedSet
 * @see ConciseSet
 * @see FastSet
 * @see IndexedSet
 * @see MatrixSet
 */
public abstract class AbstractExtendedSet<T> extends AbstractSet<T> implements ExtendedSet<T> {
	/**
	 * {@inheritDoc}
	 */
	public ExtendedSet<T> intersection(Collection<? extends T> other) {
		Statistics.increaseIntersectionCount();
		if (other == null)
			return empty();
		ExtendedSet<T> clone = clone();
		clone.retainAll(other);
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	public ExtendedSet<T> union(Collection<? extends T> other) {
		Statistics.increaseUnionCount();
		if (other == null)
			return clone();
		ExtendedSet<T> clone = clone();
		clone.addAll(other);
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	public ExtendedSet<T> difference(Collection<? extends T> other) {
		Statistics.increaseDifferenceCount();
		if (other == null)
			return clone();
		ExtendedSet<T> clone = clone();
		clone.removeAll(other);
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	public ExtendedSet<T> symmetricDifference(Collection<? extends T> other) {
		Statistics.increaseSymmetricDifferenceCount();
		if (other == null)
			return clone();
		ExtendedSet<T> res = this.union(other);
		res.removeAll(this.intersection(other));
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	public ExtendedSet<T> complemented() {
		ExtendedSet<T> clone = clone();
		clone.complement();
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	public abstract void complement();

	/**
	 * {@inheritDoc}
	 */
	public boolean containsAny(Collection<? extends T> other) {
		return intersectionSize(other) > 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean containsAtLeast(Collection<? extends T> other, int minElements) {
		if (minElements < 1)
			throw new IllegalArgumentException();
		return intersectionSize(other) >= minElements;
	}

	/**
	 * {@inheritDoc}
	 */
	public int intersectionSize(Collection<? extends T> other) {
		Statistics.increaseIntersectionCount();
		return other == null ? 0 : this.intersection(other).size();
	}

	/**
	 * {@inheritDoc}
	 */
	public int unionSize(Collection<? extends T> other) {
		return other == null ? size() : size() + other.size() - intersectionSize(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public int symmetricDifferenceSize(Collection<? extends T> other) {
		return other == null ? size() : this.size() + other.size() - 2 * intersectionSize(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public int differenceSize(Collection<? extends T> other) {
		return other == null ? size() : this.size() - intersectionSize(other);
	}

	/**
	 * {@inheritDoc}
	 */
	public int complementSize() {
		return complemented().size();
	}

	/**
	 * {@inheritDoc}
	 */
	public abstract ExtendedSet<T> empty();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<T> headSet(T toElement) {
		return new ExtendedSubSet(null, toElement);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<T> subSet(T fromElement, T toElement) {
		return new ExtendedSubSet(fromElement, toElement);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<T> tailSet(T fromElement) {
		return new ExtendedSubSet(fromElement, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ExtendedSet<T> clone() {
		try {
			return (ExtendedSet<T>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public abstract double bitmapCompressionRatio();

	/**
	 * {@inheritDoc}
	 */
	public abstract double collectionCompressionRatio();

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public ExtendedIterator<T> descendingIterator() {
		// used to compare items
		Comparator<? super T> tmpComp = AbstractExtendedSet.this.comparator();
		if (tmpComp == null)
			tmpComp = new Comparator<T>() {
				@Override
				public int compare(T o1, T o2) {
					return ((Comparable) o1).compareTo(o2);
				}
			};
		final Comparator<? super T> comp = tmpComp;
			
		return new ExtendedIterator<T>() {
			// iterator from last element
			private final ListIterator<T> itr = new ArrayList<T>(AbstractExtendedSet.this)
					.listIterator(AbstractExtendedSet.this.size());
			
			@Override
			public boolean hasNext() {
				return itr.hasPrevious();
			}

			@Override
			public T next() {
				return itr.previous();
			}
			
			@Override
			public void skipAllBefore(T element) {
				// iterate until the element is found
				while (itr.hasPrevious()) {
					int res = comp.compare(itr.previous(), element);
					
					// the element has not been found, thus the next call to
					// itr.previous() will provide the right value
					if (res < 0)
						return;

					// the element has been found. Hence, we have to get back
					// to make itr.previous() provide the right value
					if (res == 0) {
						itr.next();
						return;
					}
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public Iterable<T> descending() {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return descendingIterator();
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends ExtendedSet<T>> powerSet() {
		return powerSet(1, Integer.MAX_VALUE);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends ExtendedSet<T>> powerSet(int min, int max) {
		if (min < 1 || max < min)
			throw new IllegalArgumentException();

		// special cases
		List<ExtendedSet<T>> res = new ArrayList<ExtendedSet<T>>();
		if (size() < min)
			return res;
		if (size() == min) {
			res.add(this.clone());
			return res;
		}
		if (size() == min + 1) {
			for (T item : this.descending()) {
				ExtendedSet<T> set = this.clone();
				set.remove(item);
				res.add(set);
			}
			if (max > min)
				res.add(this.clone());
			return res;
		}

		// the first level contains only one prefix made up of all 1-subsets
		List<List<ExtendedSet<T>>> level = new ArrayList<List<ExtendedSet<T>>>();
		level.add(new ArrayList<ExtendedSet<T>>());
		for (T item : this) {
			ExtendedSet<T> single = this.empty();
			single.add(item);
			level.get(0).add(single);
		}
		if (min == 1)
			res.addAll(level.get(0));

		// all combinations
		int l = 2;
		while (!level.isEmpty() && l <= max) {
			List<List<ExtendedSet<T>>> newLevel = new ArrayList<List<ExtendedSet<T>>>();
			for (List<ExtendedSet<T>> prefix : level) {
				for (int i = 0; i < prefix.size() - 1; i++) {
					List<ExtendedSet<T>> newPrefix = new ArrayList<ExtendedSet<T>>();
					for (int j = i + 1; j < prefix.size(); j++) {
						ExtendedSet<T> x = prefix.get(i).clone();
						x.addLastOf(prefix.get(j));
						newPrefix.add(x);
						if (l >= min)
							res.add(x);
					}
					if (newPrefix.size() > 1)
						newLevel.add(newPrefix);
				}
			}
			level = newLevel;
			l++;
		}

		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	public int powerSetSize() {
		return isEmpty() ? 0 : (int) Math.pow(2, size()) - 1;
	}

	/**
	 * {@inheritDoc}
	 */
	public int powerSetSize(int min, int max) {
		if (min < 1 || max < min)
			throw new IllegalArgumentException();
		final int size = size();

		// special cases
		if (size < min)
			return 0;
		if (size == min)
			return 1;

		// sum all combinations of "size" elements from "min" to "max"
		return binomialSum(size, Math.min(size, max), min);
	}

	/**
	 * Computes the sum of binomial coefficients ranging from
	 * <code>(n choose kMax)</code> to <code>(n choose kMin)</code> using
	 * <i>dynamic programming</i>
	 * 
	 * @param n
	 * @param kMax
	 * @param kMin
	 * @return the sum of binomial coefficients
	 */
	private static int binomialSum(int n, int kMax, int kMin) {
		// illegal parameters
		if (kMin < 0 || kMin > kMax || kMax > n)
			throw new IllegalArgumentException();
		
		// trivial cases
		if (kMax == kMin && (kMax == 0 || kMax == n))
			return 1;

		// compute all binomial coefficients for "n"
		int[] b = new int[n + 1];    
		for (int i = 0; i <= n; i++)
			b[i] = 1;
		for (int i = 1; i <= n; i++)   
			for (int j = i - 1; j > 0; j--)             
				b[j] += b[j - 1];        
		
		// sum binomial coefficients
		int res = 0;
		for (int i = kMin; i <= kMax; i++)
			res += b[i];
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean addLastOf(SortedSet<T> set) {
		return add(set.last());
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean addFirstOf(SortedSet<T> set) {
		return add(set.first());
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean removeLastOf(SortedSet<T> set) {
		return remove(set.last());
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean removeFirstOf(SortedSet<T> set) {
		return remove(set.first());
	}

	/**
	 * {@inheritDoc}
	 */
	public abstract String debugInfo();

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(ExtendedSet<T> o) {
		Iterator<T> thisIterator = this.descendingIterator();
		Iterator<T> otherIterator = o.descendingIterator();
		while (thisIterator.hasNext() && otherIterator.hasNext()) {
			T thisItem = thisIterator.next();
			T otherItem = otherIterator.next();
			int res = ((Comparable) thisItem).compareTo(otherItem);
			if (res != 0)
				return res;
		}
		return thisIterator.hasNext() ? 1 : (otherIterator.hasNext() ? -1 : 0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void fill(T from, T to) {
		ExtendedSet<T> toAdd = empty();
		toAdd.add(to);
		toAdd.complement();
		toAdd.add(to);

		ExtendedSet<T> toRemove = empty();
		toRemove.add(from);
		toRemove.complement();
		
		toAdd.removeAll(toRemove);
		
		this.addAll(toAdd);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void clear(T from, T to) {
		ExtendedSet<T> toRemove = empty();
		toRemove.fill(from, to);
		this.removeAll(toRemove);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flip(T e) {
		if (!add(e))
			remove(e);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public T position(int i) {
		int size = size();
		if (i < 0 || i >= size)
			throw new IndexOutOfBoundsException();
		
		Iterator<T> itr;
		if (i < (size / 2)) {
			itr = iterator();
			for (int j = 0; j <= i - 1; j++) 
				itr.next();
		} else {
			itr = descendingIterator();
			for (int j = size - 1; j >= i + 1; j--) 
				itr.next();
		}
		return itr.next();
	}
	
	/**
	 * Used by {@link AbstractExtendedSet#headSet(T)},
	 * {@link AbstractExtendedSet#tailSet(T)} and {@link AbstractExtendedSet#subSet(T, T)}
	 * to offer a restricted view of the entire set
	 */
	protected class ExtendedSubSet extends AbstractExtendedSet<T> {
		/**
		 * Minimun allowed element (included) and maximum allowed element
		 * (excluded)
		 */
		private final T min, max;

		/**
		 * When <code>max != null</code>, it contains all elements from
		 * {@link #min} to {@link #max} - 1. Otherwise, it contains all the
		 * elements <i>strictly</i> below {@link #min}
		 */
		private final ExtendedSet<T> mask;

		/**
		 * Creates the subset
		 * 
		 * @param min
		 *            minimun allowed element (<i>included</i>)
		 * @param max
		 *            maximum allowed element (<i>excluded</i>)
		 */
		public ExtendedSubSet(T min, T max) {
			if (min == null && max == null)
				throw new IllegalArgumentException();

			if (min != null && max != null
					&& localComparator.compare(min, max) > 0)
				throw new IllegalArgumentException("min > max");

			this.min = min;
			this.max = max;

			// add all elements that are strictly less than "max"
			mask = AbstractExtendedSet.this.empty();
			if (max != null) {
				mask.add(max);
				mask.complement();

				// remove all elements that are strictly less than "min"
				if (min != null) {
					ExtendedSet<T> tmp = AbstractExtendedSet.this.empty();
					tmp.add(min);
					tmp.complement();
					mask.removeAll(tmp);
				}
			} else {
				mask.add(min);
				mask.complement();
			}
		}

		
		
		/*
		 * PRIVATE UTILITY METHODS
		 */
		
		/**
		 * Comparator for elements of type <code>T</code>
		 */
		private final Comparator<? super T> localComparator;

		// initialize the comparator
		{
			final Comparator<? super T> c = AbstractExtendedSet.this.comparator();
			if (c != null) {
				localComparator = c;
			} else {
				localComparator = new Comparator<T>() {
					@SuppressWarnings("unchecked")
					@Override
					public int compare(T o1, T o2) {
						return ((Comparable) o1).compareTo(o2);
					}
				};
			}
		}

		/**
		 * Checks if a given set is completely contained within {@link #min} and
		 * {@link #max}
		 * 
		 * @param other
		 *            given set
		 * @return <code>true</code> if the given set is completely contained
		 *         within {@link #min} and {@link #max}
		 */
		private boolean completelyContains(ExtendedSet<T> other) {
			return other.isEmpty() || 
					  ((max == null || localComparator.compare(other.last(), max) < 0)
					&& (min == null || localComparator.compare(other.first(), min) >= 0));
		}

		/**
		 * Checks if a given element is completely contained within {@link #min}
		 * and {@link #max}
		 * 
		 * @param e
		 *            given element
		 * @return <code>true</code> if the given element is completely
		 *         contained within {@link #min} and {@link #max}
		 */
		private boolean completelyContains(T e) {
			return (max == null || localComparator.compare(e, max) < 0)
					&& (min == null || localComparator.compare(e, min) >= 0);
		}

		/**
		 * Generates a set that represent a subview of the given set, namely
		 * elements from {@link #min} (included) to {@link #max} (excluded)
		 * 
		 * @param toFilter
		 *            given set
		 * @return the subview
		 */
		private ExtendedSet<T> filterByMask(ExtendedSet<T> toFilter) {
			if (completelyContains(toFilter))
				return toFilter;
			if (max != null)
				return toFilter.intersection(mask);
			return toFilter.difference(mask);
		}

		/**
		 * Clears the bits of the given set according to the mask
		 * 
		 * @param other
		 *            set to clear
		 */
		private void clearByMask(ExtendedSet<T> other) {
			if (completelyContains(other)) {
				other.clear();
			} else if (max != null) {
				other.removeAll(mask);
			} else {
				other.retainAll(mask);
			}
		}

		
		
		/*
		 * PUBLIC METHODS
		 */
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Comparator<? super T> comparator() {
			return AbstractExtendedSet.this.comparator();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public T first() {
			return filterByMask(AbstractExtendedSet.this).first();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ExtendedSet<T> headSet(T toElement) {
			if (localComparator.compare(toElement, max) > 0)
				throw new IllegalArgumentException();
			return new ExtendedSubSet(min, toElement);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public T last() {
			return filterByMask(AbstractExtendedSet.this).last();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ExtendedSet<T> subSet(T fromElement, T toElement) {
			if (localComparator.compare(fromElement, min) < 0
					|| localComparator.compare(toElement, max) > 0)
				throw new IllegalArgumentException();
			return new ExtendedSubSet(fromElement, toElement);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ExtendedSet<T> tailSet(T fromElement) {
			if (localComparator.compare(fromElement, min) < 0)
				throw new IllegalArgumentException();
			return new ExtendedSubSet(fromElement, max);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean add(T e) {
			if (!completelyContains(e))
				throw new IllegalArgumentException();
			return AbstractExtendedSet.this.add(e);
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean addAll(Collection<? extends T> c) {
			if (c == null)
				return false;
			if (c instanceof ExtendedSet) {
				if (!completelyContains((ExtendedSet) c))
					throw new IllegalArgumentException();
				return AbstractExtendedSet.this.addAll(convert(c));
			} 
			// make calls to add() and, consequently, to completelyContains()
			return super.addAll(c);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void clear() {
			clearByMask(AbstractExtendedSet.this);
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean contains(Object o) {
			return o != null && completelyContains((T) o) && AbstractExtendedSet.this.contains(o);
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean containsAll(Collection<?> c) {
			if (c == null)
				return false;
			if (c instanceof ExtendedSet) 
				return completelyContains((ExtendedSet) c) && AbstractExtendedSet.this.containsAll(convert(c));
			// make calls to contains() and, consequently, to completelyContains()
			return super.containsAll(c);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isEmpty() {
			return filterByMask(AbstractExtendedSet.this).isEmpty();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ExtendedIterator<T> iterator() {
			return filterByMask(AbstractExtendedSet.this).iterator();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ExtendedIterator<T> descendingIterator() {
			return filterByMask(AbstractExtendedSet.this).descendingIterator();
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean remove(Object o) {
			return o != null && completelyContains((T) o) && AbstractExtendedSet.this.remove(o);
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean removeAll(Collection<?> c) {
			if (c == null)
				return false;
			if (c instanceof ExtendedSet) 
				return AbstractExtendedSet.this.removeAll(filterByMask(convert(c)));
			// make calls to remove() and, consequently, to completelyContains()
			return super.removeAll(c);
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean retainAll(Collection<?> c) {
			if (c == null)
				return false;
			if (c instanceof ExtendedSet) {
				if (completelyContains(AbstractExtendedSet.this)) 
					return AbstractExtendedSet.this.retainAll(convert(c));

				int sizeBefore = AbstractExtendedSet.this.size();
				ExtendedSet<T> res = AbstractExtendedSet.this.intersection(convert(c));
				clearByMask(AbstractExtendedSet.this);
				AbstractExtendedSet.this.addAll(res);
				return AbstractExtendedSet.this.size() != sizeBefore;
			} 
			// make calls to remove() and, consequently, to completelyContains()
			return super.retainAll(c);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int size() {
			if (completelyContains(AbstractExtendedSet.this))
				return AbstractExtendedSet.this.size();
			if (max != null)
				return AbstractExtendedSet.this.intersectionSize(mask);
			return AbstractExtendedSet.this.differenceSize(mask);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object o) {
			return filterByMask(AbstractExtendedSet.this).equals(o);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			return filterByMask(AbstractExtendedSet.this).hashCode();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compareTo(ExtendedSet<T> o) {
			return filterByMask(AbstractExtendedSet.this).compareTo(o);
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void complement() {
			clearByMask(AbstractExtendedSet.this);
			AbstractExtendedSet.this.addAll(complemented());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int complementSize() {
			return complemented().size();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public ExtendedSet<T> complemented() {
			return filterByMask(filterByMask(AbstractExtendedSet.this).complemented());
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public ExtendedSet<T> difference(Collection<? extends T> other) {
			return filterByMask(AbstractExtendedSet.this.difference(convert(other)));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public ExtendedSet<T> empty() {
			return AbstractExtendedSet.this.empty();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public ExtendedSet<T> symmetricDifference(Collection<? extends T> other) {
			return filterByMask(AbstractExtendedSet.this.symmetricDifference(convert(other)));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public ExtendedSet<T> intersection(Collection<? extends T> other) {
			return filterByMask(AbstractExtendedSet.this.intersection(convert(other)));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public ExtendedSet<T> union(Collection<? extends T> other) {
			return filterByMask(AbstractExtendedSet.this.union(convert(other)));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int intersectionSize(Collection<? extends T> other) {
			return filterByMask(AbstractExtendedSet.this.intersection(convert(other))).size();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public double bitmapCompressionRatio() {
			return filterByMask(AbstractExtendedSet.this).bitmapCompressionRatio();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public double collectionCompressionRatio() {
			return filterByMask(AbstractExtendedSet.this).collectionCompressionRatio();
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String debugInfo() {
			return String.format("min = %s, max = %s\nmask = %s\nelements = %s", 
					min.toString(), max.toString(), mask.debugInfo(), AbstractExtendedSet.this.toString());
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public ExtendedSet<T> unmodifiable() {
			return AbstractExtendedSet.this.unmodifiable().subSet(min, max);
		}

		/**
		 * Gets the container instance
		 * 
		 * @return the container instance
		 */
		protected ExtendedSet<T> container() {
			if (AbstractExtendedSet.this instanceof AbstractExtendedSet<?>.UnmodifiableExtendedSet)
				return ((UnmodifiableExtendedSet) AbstractExtendedSet.this).container();
			if (AbstractExtendedSet.this instanceof AbstractExtendedSet<?>.ExtendedSubSet)
				return ((UnmodifiableExtendedSet) AbstractExtendedSet.this).container();
			return AbstractExtendedSet.this;
		}

		/** {@inheritDoc} */
		@SuppressWarnings("unchecked")
		@Override
		public ExtendedSet<T> convert(Collection<?> c) {
			Collection<?> other;
			if (c instanceof AbstractExtendedSet.UnmodifiableExtendedSet)
				other = ((AbstractExtendedSet.UnmodifiableExtendedSet) c).container();
			else if (c instanceof AbstractExtendedSet.ExtendedSubSet)
				other = ((AbstractExtendedSet.ExtendedSubSet) c).container();
			else
				other = c;
			return filterByMask(((AbstractExtendedSet) container()).convert(other));
		}

		/** {@inheritDoc} */
		@SuppressWarnings("unchecked")
		@Override
		public ExtendedSet<T> convert(Object... e) {
			return filterByMask(((AbstractExtendedSet) container()).convert(e));
		}
	}
	
	/** 
	 * Exception message when writing operations are performed on {@link #unmodifiable()}
	 */
	private final static String UNSUPPORTED_MSG = "The class is read-only!";

	/**
	 * Read-only view of the set.
	 * <p>
	 * Note that it extends {@link AbstractExtendedSet} instead of implementing
	 * {@link ExtendedSet} because of the methods {@link #tailSet(Object)},
	 * {@link #headSet(Object)}, and {@link #subSet(Object, Object)}.
	 */
	protected class UnmodifiableExtendedSet extends AbstractExtendedSet<T> {
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
		/** {@inheritDoc} */ @Override public void flip(T e) {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
		
		/*
		 * Read-only methods
		 */
		/** {@inheritDoc} */ @Override public ExtendedSet<T> intersection(Collection<? extends T> other) {return AbstractExtendedSet.this.intersection(convert(other));}
		/** {@inheritDoc} */ @Override public ExtendedSet<T> difference(Collection<? extends T> other) {return AbstractExtendedSet.this.difference(convert(other));}
		/** {@inheritDoc} */ @Override public ExtendedSet<T> union(Collection<? extends T> other) {return AbstractExtendedSet.this.union(convert(other));}
		/** {@inheritDoc} */ @Override public ExtendedSet<T> symmetricDifference(Collection<? extends T> other) {return AbstractExtendedSet.this.symmetricDifference(convert(other));}
		/** {@inheritDoc} */ @Override public ExtendedSet<T> complemented() {return AbstractExtendedSet.this.complemented();}
		/** {@inheritDoc} */ @Override public ExtendedSet<T> empty() {return AbstractExtendedSet.this.empty();}
		/** {@inheritDoc} */ @Override public int intersectionSize(Collection<? extends T> other) {return AbstractExtendedSet.this.intersectionSize(convert(other));}
		/** {@inheritDoc} */ @Override public int differenceSize(Collection<? extends T> other) {return AbstractExtendedSet.this.differenceSize(convert(other));}
		/** {@inheritDoc} */ @Override public int unionSize(Collection<? extends T> other) {return AbstractExtendedSet.this.unionSize(convert(other));}
		/** {@inheritDoc} */ @Override public int symmetricDifferenceSize(Collection<? extends T> other) {return AbstractExtendedSet.this.symmetricDifferenceSize(convert(other));}
		/** {@inheritDoc} */ @Override public int complementSize() {return AbstractExtendedSet.this.complementSize();}
		/** {@inheritDoc} */ @Override public int powerSetSize() {return AbstractExtendedSet.this.powerSetSize();}
		/** {@inheritDoc} */ @Override public int powerSetSize(int min, int max) {return AbstractExtendedSet.this.powerSetSize(min, max);}
		/** {@inheritDoc} */ @Override public int size() {return AbstractExtendedSet.this.size();}
		/** {@inheritDoc} */ @Override public boolean isEmpty() {return AbstractExtendedSet.this.isEmpty();}
		/** {@inheritDoc} */ @Override public boolean contains(Object o) {return AbstractExtendedSet.this.contains(o);}
		/** {@inheritDoc} */ @Override public boolean containsAll(Collection<?> c) {return AbstractExtendedSet.this.containsAll(convert(c));}
		/** {@inheritDoc} */ @Override public boolean containsAny(Collection<? extends T> other) {return AbstractExtendedSet.this.containsAny(convert(other));}
		/** {@inheritDoc} */ @Override public boolean containsAtLeast(Collection<? extends T> other, int minElements) {return AbstractExtendedSet.this.containsAtLeast(convert(other), minElements);}
		/** {@inheritDoc} */ @Override public T first() {return AbstractExtendedSet.this.first();}
		/** {@inheritDoc} */ @Override public T last() {return AbstractExtendedSet.this.last();}
		/** {@inheritDoc} */ @Override public Comparator<? super T> comparator() {return AbstractExtendedSet.this.comparator();}
		/** {@inheritDoc} */ @Override public int compareTo(ExtendedSet<T> o) {return AbstractExtendedSet.this.compareTo(o);}
		/** {@inheritDoc} */ @Override public boolean equals(Object o) {return AbstractExtendedSet.this.equals(o);}
		/** {@inheritDoc} */ @Override public int hashCode() {return AbstractExtendedSet.this.hashCode();}
		/** {@inheritDoc} */ @Override public Iterable<T> descending() {return AbstractExtendedSet.this.descending();}
		/** {@inheritDoc} */ @Override public ExtendedIterator<T> descendingIterator() {return AbstractExtendedSet.this.descendingIterator();}
		/** {@inheritDoc} */ @Override public List<? extends ExtendedSet<T>> powerSet() {return AbstractExtendedSet.this.powerSet();}
		/** {@inheritDoc} */ @Override public List<? extends ExtendedSet<T>> powerSet(int min, int max) {return AbstractExtendedSet.this.powerSet(min, max);}
		/** {@inheritDoc} */ @Override public double bitmapCompressionRatio() {return AbstractExtendedSet.this.bitmapCompressionRatio();}
		/** {@inheritDoc} */ @Override public double collectionCompressionRatio() {return AbstractExtendedSet.this.collectionCompressionRatio();}
		/** {@inheritDoc} */ @Override public String debugInfo() {return AbstractExtendedSet.this.debugInfo();}
		/** {@inheritDoc} */ @Override public Object[] toArray() {return AbstractExtendedSet.this.toArray();}
		/** {@inheritDoc} */ @Override public <X> X[] toArray(X[] a) {return AbstractExtendedSet.this.toArray(a);}
		/** {@inheritDoc} */ @Override public String toString() {return AbstractExtendedSet.this.toString();}
		/** {@inheritDoc} */ @Override public T position(int i) {return AbstractExtendedSet.this.position(i);}

		/*
		 * Special purpose methods
		 */
		
		/** {@inheritDoc} */ 
		@Override
		public ExtendedIterator<T> iterator() {
			final ExtendedIterator<T> itr = AbstractExtendedSet.this.iterator();
			return new ExtendedIterator<T>() {
				@Override public boolean hasNext() {return itr.hasNext();}
				@Override public T next() {return itr.next();}
				@Override public void skipAllBefore(T element) {itr.skipAllBefore(element);}
				@Override public void remove() {throw new UnsupportedOperationException(UNSUPPORTED_MSG);}
			};
		}

		/** {@inheritDoc} */
		@Override
		public ExtendedSet<T> headSet(T toElement) {
			return UnmodifiableExtendedSet.this.new ExtendedSubSet(null, toElement);
		}

		/** {@inheritDoc} */
		@Override
		public ExtendedSet<T> subSet(T fromElement, T toElement) {
			return UnmodifiableExtendedSet.this.new ExtendedSubSet(fromElement, toElement);
		}

		/** {@inheritDoc} */
		@Override
		public ExtendedSet<T> tailSet(T fromElement) {
			return UnmodifiableExtendedSet.this.new ExtendedSubSet(fromElement, null);
		}
		
		/** {@inheritDoc} */ 
		@Override 
		public ExtendedSet<T> clone() {
			return AbstractExtendedSet.this.clone(); 
		}
		
		/** {@inheritDoc} */ 
		@Override 
		public ExtendedSet<T> unmodifiable() {
			// useless to create another instance
			return this;
		}

		/**
		 * Gets the container instance
		 * 
		 * @return the container instance
		 */
		protected ExtendedSet<T> container() {
			if (AbstractExtendedSet.this instanceof AbstractExtendedSet<?>.UnmodifiableExtendedSet)
				return ((UnmodifiableExtendedSet) AbstractExtendedSet.this).container();
			if (AbstractExtendedSet.this instanceof AbstractExtendedSet<?>.ExtendedSubSet)
				return ((UnmodifiableExtendedSet) AbstractExtendedSet.this).container();
			return AbstractExtendedSet.this;
		}

		/** {@inheritDoc} */
		@SuppressWarnings("unchecked")
		@Override
		public ExtendedSet<T> convert(Collection<?> c) {
			Collection<?> other;
			if (c instanceof AbstractExtendedSet.UnmodifiableExtendedSet)
				other = ((AbstractExtendedSet.UnmodifiableExtendedSet) c).container();
			else if (c instanceof AbstractExtendedSet.ExtendedSubSet)
				other = ((AbstractExtendedSet.ExtendedSubSet) c).container();
			else
				other = c;
			return ((AbstractExtendedSet) container()).convert(other);
		}

		/** {@inheritDoc} */
		@SuppressWarnings("unchecked")
		@Override
		public ExtendedSet<T> convert(Object... e) {
			return ((AbstractExtendedSet) container()).convert(e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<T> unmodifiable() {
		return new UnmodifiableExtendedSet();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract ExtendedIterator<T> iterator();
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double jaccardSimilarity(ExtendedSet<T> other) {
		int inters = intersectionSize(other);
		return (double) inters / (size() + other.size() - inters);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double jaccardDistance(ExtendedSet<T> other) {
		return 1D - jaccardSimilarity(other);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double weightedJaccardSimilarity(ExtendedSet<T> other) {
		ExtendedSet<T> inters = intersection(other);
		double intersSum = 0D;
		for (T t : inters) 
			if (t instanceof Integer) 
				intersSum += (Integer) t;
			else if (t instanceof Double) 
				intersSum += (Double) t;
			else if (t instanceof Float) 
				intersSum += (Float) t;
			else if (t instanceof Byte) 
				intersSum += (Byte) t;
			else if (t instanceof Long) 
				intersSum += (Long) t;
			else if (t instanceof Short) 
				intersSum += (Short) t;
			else
				throw new IllegalArgumentException("A collection of numbers is required");

		ExtendedSet<T> symmetricDiff = symmetricDifference(other);
		double symmetricDiffSum = 0D;
		for (T t : symmetricDiff) 
			if (t instanceof Integer) 
				symmetricDiffSum += (Integer) t;
			else if (t instanceof Double) 
				symmetricDiffSum += (Double) t;
			else if (t instanceof Float) 
				symmetricDiffSum += (Float) t;
			else if (t instanceof Byte) 
				symmetricDiffSum += (Byte) t;
			else if (t instanceof Long) 
				symmetricDiffSum += (Long) t;
			else if (t instanceof Short) 
				symmetricDiffSum += (Short) t;
			else
				throw new IllegalArgumentException("A collection of numbers is required");
		
		return intersSum / (intersSum + symmetricDiffSum);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double weightedJaccardDistance(ExtendedSet<T> other) {
		return 1D - weightedJaccardSimilarity(other);
	}
}
