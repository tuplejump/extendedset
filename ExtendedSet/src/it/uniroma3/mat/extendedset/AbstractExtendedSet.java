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
 */
public abstract class AbstractExtendedSet<T> extends AbstractSet<T> implements ExtendedSet<T> {
	/**
	 * {@inheritDoc}
	 */
	public AbstractExtendedSet<T> getIntersection(Collection<? extends T> other) {
		Statistics.increaseIntersectionCount();
		if (other == null)
			return emptySet();
		AbstractExtendedSet<T> clone = clone();
		clone.retainAll(other);
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	public AbstractExtendedSet<T> getUnion(Collection<? extends T> other) {
		Statistics.increaseUnionCount();
		if (other == null)
			return clone();
		AbstractExtendedSet<T> clone = clone();
		clone.addAll(other);
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	public AbstractExtendedSet<T> getDifference(Collection<? extends T> other) {
		Statistics.increaseDifferenceCount();
		if (other == null)
			return clone();
		AbstractExtendedSet<T> clone = clone();
		clone.removeAll(other);
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	public AbstractExtendedSet<T> getSymmetricDifference(Collection<? extends T> other) {
		Statistics.increaseSymmetricDifferenceCount();
		if (other == null)
			return clone();
		AbstractExtendedSet<T> res = this.getUnion(other);
		res.removeAll(this.getIntersection(other));
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	public AbstractExtendedSet<T> getComplement() {
		AbstractExtendedSet<T> clone = clone();
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
		return other == null ? 0 : this.getIntersection(other).size();
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
		return getComplement().size();
	}

	/**
	 * {@inheritDoc}
	 */
	public abstract AbstractExtendedSet<T> emptySet();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AbstractExtendedSet<T> headSet(T toElement) {
		return new ExtendedSubSet(null, toElement);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AbstractExtendedSet<T> subSet(T fromElement, T toElement) {
		return new ExtendedSubSet(fromElement, toElement);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AbstractExtendedSet<T> tailSet(T fromElement) {
		return new ExtendedSubSet(fromElement, null);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public AbstractExtendedSet<T> clone() {
		try {
			return (AbstractExtendedSet<T>) super.clone();
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
	public Iterator<T> descendingIterator() {
		return new Iterator<T>() {
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
	public List<? extends AbstractExtendedSet<T>> powerSet() {
		return powerSet(1, Integer.MAX_VALUE);
	}

	/**
	 * {@inheritDoc}
	 */
	public List<? extends AbstractExtendedSet<T>> powerSet(int min, int max) {
		if (min < 1 || max < min)
			throw new IllegalArgumentException();

		// special cases
		List<AbstractExtendedSet<T>> res = new ArrayList<AbstractExtendedSet<T>>();
		if (size() < min)
			return res;
		if (size() == min) {
			res.add(this.clone());
			return res;
		}
		if (size() == min + 1) {
			for (T item : this.descending()) {
				AbstractExtendedSet<T> set = this.clone();
				set.remove(item);
				res.add(set);
			}
			if (max > min)
				res.add(this.clone());
			return res;
		}

		// the first level contains only one prefix made up of all 1-subsets
		List<List<AbstractExtendedSet<T>>> level = new ArrayList<List<AbstractExtendedSet<T>>>();
		level.add(new ArrayList<AbstractExtendedSet<T>>());
		for (T item : this) {
			AbstractExtendedSet<T> single = this.emptySet();
			single.add(item);
			level.get(0).add(single);
		}
		if (min == 1)
			res.addAll(level.get(0));

		// all combinations
		int l = 2;
		while (!level.isEmpty() && l <= max) {
			List<List<AbstractExtendedSet<T>>> newLevel = new ArrayList<List<AbstractExtendedSet<T>>>();
			for (List<AbstractExtendedSet<T>> prefix : level) {
				for (int i = 0; i < prefix.size() - 1; i++) {
					List<AbstractExtendedSet<T>> newPrefix = new ArrayList<AbstractExtendedSet<T>>();
					for (int j = i + 1; j < prefix.size(); j++) {
						AbstractExtendedSet<T> x = prefix.get(i).clone();
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
		AbstractExtendedSet<T> toAdd = emptySet();
		toAdd.add(to);
		toAdd.complement();
		toAdd.add(to);

		AbstractExtendedSet<T> toRemove = emptySet();
		toRemove.add(from);
		toRemove.complement();
		
		toAdd.removeAll(toRemove);
		
		this.addAll(toAdd);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void clear(T from, T to) {
		AbstractExtendedSet<T> toRemove = emptySet();
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
	 * Used by {@link AbstractExtendedSet#headSet(T)},
	 * {@link AbstractExtendedSet#tailSet(T)} and {@link AbstractExtendedSet#subSet(T, T)}
	 * to offer a restricted view of the entire set
	 */
	private class ExtendedSubSet extends AbstractExtendedSet<T> {
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
		private final AbstractExtendedSet<T> mask;

		/**
		 * Creates the subset
		 * 
		 * @param min
		 *            minimun allowed element (<i>included</i>)
		 * @param max
		 *            maximum allowed element (<i>excluded</i>)
		 */
		private ExtendedSubSet(T min, T max) {
			if (min == null && max == null)
				throw new IllegalArgumentException();

			if (min != null && max != null
					&& localComparator.compare(min, max) > 0)
				throw new IllegalArgumentException("min > max");

			this.min = min;
			this.max = max;

			// add all elements that are strictly less than "max"
			mask = AbstractExtendedSet.this.emptySet();
			if (max != null) {
				mask.add(max);
				mask.complement();

				// remove all elements that are strictly less than "min"
				if (min != null) {
					AbstractExtendedSet<T> tmp = AbstractExtendedSet.this.emptySet();
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
		private boolean completelyContains(AbstractExtendedSet<T> other) {
			return (max == null || localComparator.compare(other.last(), max) < 0)
					&& (min == null || localComparator.compare(other.first(), min) >= 0);
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
		private AbstractExtendedSet<T> filterByMask(AbstractExtendedSet<T> toFilter) {
			if (completelyContains(toFilter))
				return toFilter;
			if (max != null)
				return toFilter.getIntersection(mask);
			return toFilter.getDifference(mask);
		}

		/**
		 * Clears the bits of the given set according to the mask
		 * 
		 * @param other
		 *            set to clear
		 */
		private void clearByMask(AbstractExtendedSet<T> other) {
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
		public AbstractExtendedSet<T> headSet(T toElement) {
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
		public AbstractExtendedSet<T> subSet(T fromElement, T toElement) {
			if (localComparator.compare(fromElement, min) < 0
					|| localComparator.compare(toElement, max) > 0)
				throw new IllegalArgumentException();
			return new ExtendedSubSet(fromElement, toElement);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public AbstractExtendedSet<T> tailSet(T fromElement) {
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
			if (c instanceof AbstractExtendedSet) {
				if (!completelyContains((AbstractExtendedSet) c))
					throw new IllegalArgumentException();
				return AbstractExtendedSet.this.addAll(c);
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
			if (c instanceof AbstractExtendedSet) 
				return completelyContains((AbstractExtendedSet) c) && AbstractExtendedSet.this.containsAll(c);
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
		public Iterator<T> iterator() {
			return filterByMask(AbstractExtendedSet.this).iterator();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator<T> descendingIterator() {
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
			if (c instanceof AbstractExtendedSet) 
				return AbstractExtendedSet.this.removeAll(filterByMask((AbstractExtendedSet) c));
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
			if (c instanceof AbstractExtendedSet) {
				if (completelyContains(AbstractExtendedSet.this)) 
					return AbstractExtendedSet.this.retainAll(c);

				int sizeBefore = AbstractExtendedSet.this.size();
				AbstractExtendedSet<T> res = AbstractExtendedSet.this.getIntersection((Collection<? extends T>) c);
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
			AbstractExtendedSet.this.addAll(getComplement());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int complementSize() {
			return getComplement().size();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public AbstractExtendedSet<T> getComplement() {
			return filterByMask(filterByMask(AbstractExtendedSet.this).getComplement());
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public AbstractExtendedSet<T> getDifference(Collection<? extends T> other) {
			return filterByMask(AbstractExtendedSet.this.getDifference(other));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public AbstractExtendedSet<T> emptySet() {
			return AbstractExtendedSet.this.emptySet();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public AbstractExtendedSet<T> getSymmetricDifference(Collection<? extends T> other) {
			return filterByMask(AbstractExtendedSet.this.getSymmetricDifference(other));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public AbstractExtendedSet<T> getIntersection(Collection<? extends T> other) {
			return filterByMask(AbstractExtendedSet.this.getIntersection(other));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public AbstractExtendedSet<T> getUnion(Collection<? extends T> other) {
			return filterByMask(AbstractExtendedSet.this.getUnion(other));
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public int intersectionSize(Collection<? extends T> other) {
			return filterByMask(AbstractExtendedSet.this.getIntersection(other)).size();
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
		public AbstractExtendedSet<T> unmodifiable() {
			return AbstractExtendedSet.this.unmodifiable().subSet(min, max);
		}
	}
	
	/** 
	 * Exception message when writing operations are performed on {@link #unmodifiable()}
	 */
	protected final static String UNSUPPORTED_MSG = "The class is read-only!";

	/**
	 * {@inheritDoc}
	 */
	public abstract AbstractExtendedSet<T> unmodifiable();
}
