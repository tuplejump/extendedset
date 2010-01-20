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
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Formatter;
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
 * @see ConciseSet
 * @see FastSet
 * @see IndexedSet
 */
public abstract class ExtendedSet<T> extends AbstractSet<T> implements
		SortedSet<T>, Cloneable, Comparable<ExtendedSet<T>> {
	/**
	 * Statistical data.
	 * <p>
	 * In particular:
	 * <ul>
	 * <li> {@link #getIntersectionCount()} counts the calls to
	 * {@link ExtendedSet#getIntersection(Collection)}
	 * <li> {@link #getUnionCount()} counts the calls to
	 * {@link ExtendedSet#getUnion(Collection)}
	 * <li> {@link #getSymmetricDifferenceCount()} counts the calls to
	 * {@link ExtendedSet#getSymmetricDifference(Collection)}
	 * <li> {@link #getDifferenceCount()} counts the calls to
	 * {@link ExtendedSet#getDifference(Collection)}
	 * <li> {@link #getSizeCheckCount()} counts the calls to
	 * {@link ExtendedSet#intersectionSize(Collection)},
	 * {@link ExtendedSet#unionSize(Collection)},
	 * {@link ExtendedSet#symmetricDifferenceSize(Collection)},
	 * {@link ExtendedSet#differenceSize(Collection)}
	 * <li> {@link #getEqualsCount()} counts the calls to
	 * {@link ExtendedSet#equals(Object)}
	 * </ul>
	 * <b>NOTE:</b> no counting is done for
	 * {@link ExtendedSet#getComplement()} and
	 * {@link ExtendedSet#complementSize()} since they are very fast.
	 */
	public static class Statistics {
		private static long intersectionCount = 0;
		private static long unionCount = 0;
		private static long symmetricDifferenceCount = 0;
		private static long differenceCount = 0;
		private static long sizeCheckCount = 0;
		private static long equalsCount = 0;

		/**
		 * Resets all counters
		 */
		public static void resetAll() {
			intersectionCount = 0;
			unionCount = 0;
			symmetricDifferenceCount = 0;
			differenceCount = 0;
			sizeCheckCount = 0;
			equalsCount = 0;
		}

		/**
		 * Resets the counter of intersections
		 */
		public static void resetIntersectionCount() {
			intersectionCount = 0;
		}

		/**
		 * Resets the counter of unions
		 */
		public static void resetUnionCount() {
			unionCount = 0;
		}

		/**
		 * Resets the counter of symmetric differences
		 */
		public static void resetSymmetricDifferenceCount() {
			symmetricDifferenceCount = 0;
		}

		/**
		 * Resets the counter of differences
		 */
		public static void resetDifferenceCount() {
			differenceCount = 0;
		}

		/**
		 * Resets the counter of size checks
		 */
		public static void resetSizeCheckCount() {
			sizeCheckCount = 0;
		}

		/**
		 * Resets the counter of equals
		 */
		public static void resetEqualsCount() {
			equalsCount = 0;
		}

		/**
		 * Increases the counter of intersections
		 */
		public static void increaseIntersectionCount() {
			intersectionCount++;
		}

		/**
		 * Increases the counter of unions
		 */
		public static void increaseUnionCount() {
			unionCount++;
		}

		/**
		 * Increases the counter of symmetric differences
		 */
		public static void increaseSymmetricDifferenceCount() {
			symmetricDifferenceCount++;
		}

		/**
		 * Increases the counter of differences
		 */
		public static void increaseDifferenceCount() {
			differenceCount++;
		}

		/**
		 * Increases the counter of size checks
		 */
		public static void increaseSizeCheckCount() {
			sizeCheckCount++;
		}

		/**
		 * Increases the counter of equals
		 */
		public static void increaseEqualsCount() {
			equalsCount++;
		}

		/**
		 * Gets the counter of intersections
		 * 
		 * @return the counter
		 */
		public static long getIntersectionCount() {
			return intersectionCount;
		}

		/**
		 * Gets the counter of unions
		 * 
		 * @return the counter
		 */
		public static long getUnionCount() {
			return unionCount;
		}

		/**
		 * Gets the counter of symmetric differences
		 * 
		 * @return the counter
		 */
		public static long getSymmetricDifferenceCount() {
			return symmetricDifferenceCount;
		}

		/**
		 * Gets the counter of differences
		 * 
		 * @return the counter
		 */
		public static long getDifferenceCount() {
			return differenceCount;
		}

		/**
		 * Gets the counter of size checks
		 * 
		 * @return the counter
		 */
		public static long getSizeCheckCount() {
			return sizeCheckCount;
		}

		/**
		 * Gets the counter of equals
		 * 
		 * @return the counter
		 */
		public static long getEqualsCount() {
			return equalsCount;
		}

		/**
		 * Gets summary information
		 * 
		 * @return the summary information string
		 */
		public static String getSummary() {
			final StringBuilder s = new StringBuilder();
			final Formatter f = new Formatter(s);

			f.format("intersectionCount: %d\n", intersectionCount);
			f.format("unionCount: %d\n", unionCount);
			f.format("symmetricDifferenceCount: %d\n", symmetricDifferenceCount);
			f.format("differenceCount: %d\n", differenceCount);
			f.format("sizeCheckCount: %d\n", sizeCheckCount);
			f.format("equalsCount: %d\n", equalsCount);

			return s.toString();
		}
	}

	/**
	 * Generates the intersection set (bitwise <tt>and</tt>)
	 * 
	 * @param other
	 *            {@link ExtendedSet} instance that represents the right
	 *            operand
	 * @return the result of the operation
	 * 
	 * @see #retainAll(java.util.Collection)
	 */
	public ExtendedSet<T> getIntersection(Collection<? extends T> other) {
		Statistics.increaseIntersectionCount();
		ExtendedSet<T> clone = clone();
		clone.retainAll(other);
		return clone;
	}

	/**
	 * Generates the union set (bitwise <tt>or</tt>)
	 * 
	 * @param other
	 *            {@link ExtendedSet} instance that represents the right
	 *            operand
	 * @return the result of the operation
	 * 
	 * @see #addAll(java.util.Collection)
	 */
	public ExtendedSet<T> getUnion(Collection<? extends T> other) {
		Statistics.increaseUnionCount();
		ExtendedSet<T> clone = clone();
		clone.addAll(other);
		return clone;
	}

	/**
	 * Generates the difference set (bitwise <tt>and not</tt>)
	 * 
	 * @param other
	 *            {@link ExtendedSet} instance that represents the right
	 *            operand
	 * @return the result of the operation
	 * 
	 * @see #removeAll(java.util.Collection)
	 */
	public ExtendedSet<T> getDifference(Collection<? extends T> other) {
		Statistics.increaseDifferenceCount();
		ExtendedSet<T> clone = clone();
		clone.removeAll(other);
		return clone;
	}

	/**
	 * Generates the symmetric difference set (bitwise <tt>xor</tt>)
	 * 
	 * @param other
	 *            {@link ExtendedSet} instance that represents the right
	 *            operand
	 * @return the result of the operation
	 */
	public ExtendedSet<T> getSymmetricDifference(Collection<? extends T> other) {
		Statistics.increaseSymmetricDifferenceCount();
		ExtendedSet<T> res = this.getUnion(other);
		res.removeAll(this.getIntersection(other));
		return res;
	}

	/**
	 * Generates the complement set (bitwise <tt>not</tt>). The returned set
	 * is represented by all the elements strictly less than {@link #last()}
	 * that do not exist in the current set.
	 * 
	 * @return the complement set
	 * 
	 * @see ExtendedSet#complement()
	 */
	public ExtendedSet<T> getComplement() {
		ExtendedSet<T> clone = clone();
		clone.complement();
		return clone;
	}

	/**
	 * Complements the current set (bitwise <tt>not</tt>). The modified set
	 * is represented by all the elements strictly less than {@link #last()}
	 * that do not exist in the current set.
	 * 
	 * @see ExtendedSet#getComplement()
	 */
	public abstract void complement();

	/**
	 * Returns <code>true</code> if the specified {@link ExtendedSet} instance
	 * contains any elements that are also contained within this
	 * {@link ExtendedSet} instance
	 * 
	 * @param other
	 *            {@link ExtendedSet} to intersect with
	 * @return a boolean indicating whether this {@link ExtendedSet} intersects
	 *         the specified {@link ExtendedSet}.
	 */
	public boolean containsAny(Collection<? extends T> other) {
		return intersectionSize(other) > 0;
	}

	/**
	 * Returns <code>true</code> if the specified {@link ExtendedSet} instance
	 * contains at least <code>minElements</code> elements that are also
	 * contained within this {@link ExtendedSet} instance
	 * 
	 * @param other
	 *            {@link ExtendedSet} to intersect with
	 * @param minElements
	 *            minimum number of elements to be contained within this
	 *            {@link ExtendedSet} instance
	 * @return a boolean indicating whether this {@link ExtendedSet} intersects
	 *         the specified {@link ExtendedSet}.
	 * @throws IllegalArgumentException
	 *             if <code>minElements &lt; 1</code>
	 */
	public boolean containsAtLeast(Collection<? extends T> other, int minElements) {
		if (minElements < 1)
			throw new IllegalArgumentException();
		return intersectionSize(other) >= minElements;
	}

	/**
	 * Computes the intersection set size.
	 * <p>
	 * This is faster than calling {@link #getIntersection(Collection)} and
	 * then {@link #size()}
	 * 
	 * @param other
	 *            {@link ExtendedSet} instance that represent the right
	 *            operand
	 * @return the size
	 */
	public int intersectionSize(Collection<? extends T> other) {
		Statistics.increaseIntersectionCount();
		return this.getIntersection(other).size();
	}

	/**
	 * Computes the union set size.
	 * <p>
	 * This is faster than calling {@link #getUnion(Collection)} and then
	 * {@link #size()}
	 * 
	 * @param other
	 *            {@link ExtendedSet} instance that represent the right
	 *            operand
	 * @return the size
	 */
	public int unionSize(Collection<? extends T> other) {
		return this.size() + other.size() - intersectionSize(other);
	}

	/**
	 * Computes the symmetric difference set size.
	 * <p>
	 * This is faster than calling {@link #getSymmetricDifference(Collection)} and
	 * then {@link #size()}
	 * 
	 * @param other
	 *            {@link ExtendedSet} instance that represent the right
	 *            operand
	 * @return the size
	 */
	public int symmetricDifferenceSize(Collection<? extends T> other) {
		return this.size() + other.size() - 2 * intersectionSize(other);
	}

	/**
	 * Computes the difference set size.
	 * <p>
	 * This is faster than calling {@link #getDifference(Collection)} and
	 * then {@link #size()}
	 * 
	 * @param other
	 *            {@link ExtendedSet} instance that represent the right
	 *            operand
	 * @return the size
	 */
	public int differenceSize(Collection<? extends T> other) {
		return this.size() - intersectionSize(other);
	}

	/**
	 * Computes the complement set size.
	 * <p>
	 * This is faster than calling {@link #getComplement()} and then
	 * {@link #size()}
	 * 
	 * @return the size
	 */
	public int complementSize() {
		return getComplement().size();
	}

	/**
	 * Generates an empty set
	 * 
	 * @return the empty set
	 */
	public abstract ExtendedSet<T> emptySet();

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
	 * Computes the compression factor of the bitmap representation (1 means not
	 * compressed, namely similar to {@link BitSet})
	 * 
	 * @return the compression factor 
	 */ 
	public abstract double bitmapCompressionRatio();

	/**
	 * Computes the compression factor of the integer collection (1 means not
	 * compressed, namely similar to {@link ArrayList})
	 * 
	 * @return the compression factor 
	 */
	public abstract double collectionCompressionRatio();

	/**
	 * Gets the descending order iterator over the elements of type <code>T</code>
	 * 
	 * @return descending iterator
	 */
	// TODO: override this method in ConciseSet and FastSet (not in IndexedSet)
	// to improve performances
	@SuppressWarnings("unchecked")
	public Iterator<T> descendingIterator() {
		return new Iterator<T>() {
			// iterator from last element
			private final ListIterator<T> itr = new ArrayList<T>(ExtendedSet.this)
					.listIterator(ExtendedSet.this.size());

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
	 * Allows to use the Java "for-each" statement in descending order
	 * 
	 * @return {@link Iterable} instance to iterate items in descending order
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
	 * Computes the power-set of the current set.
	 * <p>
	 * It is a particular implementation of the algorithm <i>Apriori</i>. The
	 * power-set does <i>not</i> contains the empty set. The list is sorted
	 * according to the lexicographical order provided by the sorted set.
	 * 
	 * @return the power-set
	 */
	public List<? extends ExtendedSet<T>> powerSet() {
		return powerSet(1, Integer.MAX_VALUE);
	}

	/**
	 * Computes a subset of the power-set of the current set, composed by those
	 * subsets that have cardinality between <code>min</code> and
	 * <code>max</code>.
	 * <p>
	 * It is a particular implementation of the algorithm <i>Apriori</i>. The
	 * power-set does <i>not</i> contains the empty set. The list is sorted
	 * according to the lexicographical order provided by the sorted set.
	 * 
	 * @param min
	 *            minimum subset size (greater than zero)
	 * @param max
	 *            maximum subset size
	 * @return the power-set
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
			ExtendedSet<T> single = this.emptySet();
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
						x.addLast(prefix.get(j));
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
	 * Computes the power-set size of the current set.
	 * <p>
	 * The power-set does <i>not</i> contains the empty set.
	 * 
	 * @return the power-set size
	 */
	public int powerSetSize() {
		return isEmpty() ? 0 : (int) Math.pow(2, size()) - 1;
	}

	/**
	 * Computes the power-set size of the current set, composed by those subsets
	 * that have cardinality between <code>min</code> and <code>max</code>.
	 * <p>
	 * The power-set does <i>not</i> contains the empty set.
	 * 
	 * @param min
	 *            minimum subset size (greater than zero)
	 * @param max
	 *            maximum subset size
	 * @return the power-set size
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
	 * Adds the last item of the given set to the current set 
	 * 
	 * @param set
	 *            set where to pick the last item
	 * @return <code>true</code> if this set did not already contain the
	 *         specified element
	 */
	public boolean addLast(SortedSet<T> set) {
		return add(set.last());
	}

	/**
	 * Adds the first item of the given set to the current set
	 * 
	 * @param set
	 *            set where to pick the first item
	 * @return <code>true</code> if this set did not already contain the
	 *         specified element
	 */
	public boolean addFirst(SortedSet<T> set) {
		return add(set.first());
	}

	/**
	 * Removes the last item of the given set from the current set 
	 * 
	 * @param set
	 *            set where to pick the last item
	 * @return <code>true</code> if this set already contained the specified
	 *         element
	 */
	public boolean removeLast(SortedSet<T> set) {
		return remove(set.last());
	}

	/**
	 * Removes the first item of the given set from the current set 
	 * 
	 * @param set
	 *            set where to pick the first item
	 * @return <code>true</code> if this set already contained the specified
	 *         element
	 */
	public boolean removeFirst(SortedSet<T> set) {
		return remove(set.first());
	}

	/**
	 * Prints debug info
	 * 
	 * @return a string that describes the internal representation of the instance
	 */
	public abstract String debugInfo();

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>NOTE:</b> it supposes that items of type <code>T</code> implements
	 * the interface {@link Comparable}. When this is not the case, subclasses
	 * override the method (i.e. {@link IndexedSet#compareTo(ExtendedSet)})
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
	 * Used by {@link ExtendedSet#headSet(T)},
	 * {@link ExtendedSet#tailSet(T)} and {@link ExtendedSet#subSet(T, T)}
	 * to offer a restricted view of the entire set
	 */
	private class ExtendedSubSet extends ExtendedSet<T> {
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
		private ExtendedSubSet(T min, T max) {
			if (min == null && max == null)
				throw new IllegalArgumentException();

			if (min != null && max != null
					&& localComparator.compare(min, max) > 0)
				throw new IllegalArgumentException("min > max");

			this.min = min;
			this.max = max;

			// add all elements that are strictly less than "max"
			mask = ExtendedSet.this.emptySet();
			if (max != null) {
				mask.add(max);
				mask.complement();

				// remove all elements that are strictly less than "min"
				if (min != null) {
					ExtendedSet<T> tmp = ExtendedSet.this.emptySet();
					tmp.add(min);
					tmp.complement();
					mask.removeAll(tmp);
				}
			} else {
				mask.add(min);
				mask.complement();
			}
		}

		/**
		 * Comparator for elements of type <code>T</code>
		 */
		private final Comparator<? super T> localComparator;

		// initialize the comparator
		{
			final Comparator<? super T> c = ExtendedSet.this.comparator();
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
			return (max == null || localComparator.compare(other.last(), max) < 0)
					&& (min == null || localComparator.compare(other.first(),
							min) >= 0);
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
				return toFilter.getIntersection(mask);
			return toFilter.getDifference(mask);
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

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Comparator<? super T> comparator() {
			return ExtendedSet.this.comparator();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public T first() {
			return filterByMask(ExtendedSet.this).first();
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
			return filterByMask(ExtendedSet.this).last();
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
			if (e == null)
				throw new NullPointerException();
			if (!completelyContains(e))
				throw new IllegalArgumentException();
			return ExtendedSet.this.add(e);
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean addAll(Collection<? extends T> c) {
			if (c.isEmpty())
				return false;
			
			if (c instanceof ExtendedSet) {
				ExtendedSet<T> b = (ExtendedSet) c;
				if (!completelyContains(b))
					throw new IllegalArgumentException();
				return ExtendedSet.this.addAll(b);
			} 
				
			return super.addAll(c);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void clear() {
			clearByMask(ExtendedSet.this);
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean contains(Object o) {
			return completelyContains((T) o) && ExtendedSet.this.contains(o);
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean containsAll(Collection<?> c) {
			if (c instanceof ExtendedSet) {
				ExtendedSet<T> b = (ExtendedSet) c;
				if (!completelyContains(b))
					return false;
				return ExtendedSet.this.containsAll(b);
			} 
				
			return super.containsAll(c);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isEmpty() {
			return filterByMask(ExtendedSet.this).isEmpty();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator<T> iterator() {
			return filterByMask(ExtendedSet.this).iterator();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator<T> descendingIterator() {
			return filterByMask(ExtendedSet.this).descendingIterator();
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean remove(Object o) {
			if (o == null)
				throw new NullPointerException();
			if (!completelyContains((T) o))
				return false;
			return ExtendedSet.this.remove(o);
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean removeAll(Collection<?> c) {
			if (c.isEmpty())
				return false;
			if (c instanceof ExtendedSet) {
				return ExtendedSet.this.removeAll(filterByMask((ExtendedSet) c));
			} 
			
			return super.removeAll(c);
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean retainAll(Collection<?> c) {
			if (c instanceof ExtendedSet) {
				ExtendedSet<T> b = (ExtendedSet) c;
				if (completelyContains(ExtendedSet.this)) 
					return ExtendedSet.this.retainAll(b);

				int sizeBefore = ExtendedSet.this.size();
				ExtendedSet<T> res = ExtendedSet.this.getIntersection(b);
				clearByMask(ExtendedSet.this);
				ExtendedSet.this.addAll(res);
				return ExtendedSet.this.size() != sizeBefore;
			} 
				
			return super.retainAll(c);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int size() {
			if (completelyContains(ExtendedSet.this))
				return ExtendedSet.this.size();
			if (max != null)
				return ExtendedSet.this.intersectionSize(mask);
			return ExtendedSet.this.differenceSize(mask);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object o) {
			return filterByMask(ExtendedSet.this).equals(o);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			return filterByMask(ExtendedSet.this).hashCode();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int compareTo(ExtendedSet<T> o) {
			return filterByMask(ExtendedSet.this).compareTo(o);
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void complement() {
			clearByMask(ExtendedSet.this);
			ExtendedSet.this.addAll(getComplement());
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
		public ExtendedSet<T> getComplement() {
			return filterByMask(filterByMask(ExtendedSet.this).getComplement());
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public ExtendedSet<T> getDifference(Collection<? extends T> other) {
			return filterByMask(ExtendedSet.this.getDifference(other));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public ExtendedSet<T> emptySet() {
			return ExtendedSet.this.emptySet();
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public ExtendedSet<T> getSymmetricDifference(Collection<? extends T> other) {
			return filterByMask(ExtendedSet.this.getSymmetricDifference(other));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public ExtendedSet<T> getIntersection(Collection<? extends T> other) {
			return filterByMask(ExtendedSet.this.getIntersection(other));
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>NOTE:</b> The returned set is <i>not</i> a subset, thus it is
		 * not restricted to the specified <code>min</code> and
		 * <code>max</code> bounds
		 */
		@Override
		public ExtendedSet<T> getUnion(Collection<? extends T> other) {
			return filterByMask(ExtendedSet.this.getUnion(other));
		}

		/**
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unchecked")
		@Override
		public int intersectionSize(Collection<? extends T> other) {
			if (other instanceof ExtendedSet)
				return ExtendedSet.this.intersectionSize(filterByMask((ExtendedSet) other));
			return ExtendedSet.this.getIntersection(other).size();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public double bitmapCompressionRatio() {
			return filterByMask(ExtendedSet.this).bitmapCompressionRatio();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public double collectionCompressionRatio() {
			return filterByMask(ExtendedSet.this).collectionCompressionRatio();
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public String debugInfo() {
			return String.format("min = %s, max = %s\nmask = %s\nelements = %s", 
					min.toString(), max.toString(), mask.debugInfo(), ExtendedSet.this.toString());
		}
	}
}
