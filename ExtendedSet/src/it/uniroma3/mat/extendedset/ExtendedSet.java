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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * A {@link SortedSet} with fast intersection/union/difference and other set
 * operations.
 * <p>
 * It collects all the basic functionalities and the interface of
 * {@link AbstractExtendedSet}, {@link ConciseSet}, {@link FastSet}, and
 * {@link IndexedSet}. {@link AbstractExtendedSet} is a base abstract class for
 * all other classes.
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @param <T>
 *            the type of elements maintained by this set
 * 
 * @see AbstractExtendedSet
 * @see ConciseSet
 * @see FastSet
 * @see IndexedSet
 */
public interface ExtendedSet<T> extends SortedSet<T>, Cloneable, Comparable<ExtendedSet<T>> {
	/**
	 * Statistical data.
	 * <p>
	 * In particular:
	 * <ul>
	 * <li> {@link #getIntersectionCount()} counts the calls to
	 * {@link AbstractExtendedSet#intersection(Collection)}
	 * <li> {@link #getUnionCount()} counts the calls to
	 * {@link AbstractExtendedSet#union(Collection)}
	 * <li> {@link #getSymmetricDifferenceCount()} counts the calls to
	 * {@link AbstractExtendedSet#symmetricDifference(Collection)}
	 * <li> {@link #getDifferenceCount()} counts the calls to
	 * {@link AbstractExtendedSet#difference(Collection)}
	 * <li> {@link #getSizeCheckCount()} counts the calls to
	 * {@link AbstractExtendedSet#intersectionSize(Collection)},
	 * {@link AbstractExtendedSet#unionSize(Collection)},
	 * {@link AbstractExtendedSet#symmetricDifferenceSize(Collection)},
	 * {@link AbstractExtendedSet#differenceSize(Collection)}
	 * <li> {@link #getEqualsCount()} counts the calls to
	 * {@link AbstractExtendedSet#equals(Object)}
	 * </ul>
	 * <b>NOTE:</b> no counting is done for
	 * {@link AbstractExtendedSet#complemented()} and
	 * {@link AbstractExtendedSet#complementSize()} since they are very fast.
	 */
	public static class Statistics {
		/*private*/ static long intersectionCount = 0;
		/*private*/ static long unionCount = 0;
		/*private*/ static long symmetricDifferenceCount = 0;
		/*private*/ static long differenceCount = 0;
		/*private*/ static long sizeCheckCount = 0;
		/*private*/ static long equalsCount = 0;


		/*
		 * Reset
		 */

		/** Resets all counters */
		public static void resetAll() {intersectionCount = unionCount = symmetricDifferenceCount = differenceCount = sizeCheckCount = equalsCount = 0;}
		
		/** Resets the counter of performed intersections */
		public static void resetIntersectionCount() {intersectionCount = 0;}

		/** Resets the counter of performed unions */
		public static void resetUnionCount() {unionCount = 0;}

		/** Resets the counter of performed symmetric differences */
		public static void resetSymmetricDifferenceCount() {symmetricDifferenceCount = 0;}

		/** Resets the counter of performed differences */
		public static void resetDifferenceCount() {differenceCount = 0;}

		/** Resets the counter of performed size checks (<code>complementSize()</code> excluded) */
		public static void resetSizeCheckCount() {sizeCheckCount = 0;}

		/** Resets the counter of performed equals */
		public static void resetEqualsCount() {equalsCount = 0;}


		/*
		 * Getters
		 */
		
		/** @return the counter of performed intersections */
		public static long getIntersectionCount() {return intersectionCount;}

		/** @return the counter of performed unions */
		public static long getUnionCount() {return unionCount;}

		/** @return the counter of performed symmetric differences */
		public static long getSymmetricDifferenceCount() {return symmetricDifferenceCount;}

		/** @return the counter of performed differences */
		public static long getDifferenceCount() {return differenceCount;}

		/** @return the counter of performed size checks (<code>complementSize()</code> excluded) */
		public static long getSizeCheckCount() {return sizeCheckCount;}

		/** @return the counter of performed equals */
		public static long getEqualsCount() {return equalsCount;}

		/** @return the summary information string */
		public static String summary() {
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
	 * Generates the intersection set
	 * 
	 * @param other
	 *            {@link AbstractExtendedSet} instance that represents the right
	 *            operand
	 * @return the result of the operation
	 * 
	 * @see #retainAll(java.util.Collection)
	 */
	public ExtendedSet<T> intersection(Collection<? extends T> other);

	/**
	 * Generates the union set
	 * 
	 * @param other
	 *            {@link ExtendedSet} instance that represents the right
	 *            operand
	 * @return the result of the operation
	 * 
	 * @see #addAll(java.util.Collection)
	 */
	public ExtendedSet<T> union(Collection<? extends T> other);

	/**
	 * Generates the difference set
	 * 
	 * @param other
	 *            {@link ExtendedSet} instance that represents the right
	 *            operand
	 * @return the result of the operation
	 * 
	 * @see #removeAll(java.util.Collection)
	 */
	public ExtendedSet<T> difference(Collection<? extends T> other);

	/**
	 * Generates the symmetric difference set
	 * 
	 * @param other
	 *            {@link ExtendedSet} instance that represents the right
	 *            operand
	 * @return the result of the operation
	 * @see #flip(Object)
	 */
	public ExtendedSet<T> symmetricDifference(Collection<? extends T> other);

	/**
	 * Generates the complement set. The returned set is represented by all the
	 * elements strictly less than {@link #last()} that do not exist in the
	 * current set.
	 * 
	 * @return the complement set
	 * 
	 * @see ExtendedSet#complement()
	 */
	public ExtendedSet<T> complemented();

	/**
	 * Complements the current set. The modified set is represented by all the
	 * elements strictly less than {@link #last()} that do not exist in the
	 * current set.
	 * 
	 * @see ExtendedSet#complemented()
	 */
	public void complement();

	/**
	 * Returns <code>true</code> if the specified {@link Collection} instance
	 * contains any elements that are also contained within this
	 * {@link ExtendedSet} instance
	 * 
	 * @param other
	 *            {@link ExtendedSet} to intersect with
	 * @return a boolean indicating whether this {@link ExtendedSet} intersects
	 *         the specified {@link ExtendedSet}.
	 */
	public boolean containsAny(Collection<? extends T> other);

	/**
	 * Returns <code>true</code> if the specified {@link Collection} instance
	 * contains at least <code>minElements</code> elements that are also
	 * contained within this {@link ExtendedSet} instance
	 * 
	 * @param other
	 *            {@link Collection} instance to intersect with
	 * @param minElements
	 *            minimum number of elements to be contained within this
	 *            {@link ExtendedSet} instance
	 * @return a boolean indicating whether this {@link ExtendedSet} intersects
	 *         the specified {@link Collection}.
	 * @throws IllegalArgumentException
	 *             if <code>minElements &lt; 1</code>
	 */
	public boolean containsAtLeast(Collection<? extends T> other, int minElements);

	/**
	 * Computes the intersection set size.
	 * <p>
	 * This is faster than calling {@link #intersection(Collection)} and
	 * then {@link #size()}
	 * 
	 * @param other
	 *            {@link Collection} instance that represents the right
	 *            operand
	 * @return the size
	 */
	public int intersectionSize(Collection<? extends T> other);

	/**
	 * Computes the union set size.
	 * <p>
	 * This is faster than calling {@link #union(Collection)} and then
	 * {@link #size()}
	 * 
	 * @param other
	 *            {@link Collection} instance that represents the right
	 *            operand
	 * @return the size
	 */
	public int unionSize(Collection<? extends T> other);

	/**
	 * Computes the symmetric difference set size.
	 * <p>
	 * This is faster than calling
	 * {@link #symmetricDifference(Collection)} and then {@link #size()}
	 * 
	 * @param other
	 *            {@link Collection} instance that represents the right
	 *            operand
	 * @return the size
	 */
	public int symmetricDifferenceSize(Collection<? extends T> other);

	/**
	 * Computes the difference set size.
	 * <p>
	 * This is faster than calling {@link #difference(Collection)} and
	 * then {@link #size()}
	 * 
	 * @param other
	 *            {@link Collection} instance that represents the right
	 *            operand
	 * @return the size
	 */
	public int differenceSize(Collection<? extends T> other);

	/**
	 * Computes the complement set size.
	 * <p>
	 * This is faster than calling {@link #complemented()} and then
	 * {@link #size()}
	 * 
	 * @return the size
	 */
	public int complementSize();

	/**
	 * Generates an empty set
	 * 
	 * @return the empty set
	 */
	public ExtendedSet<T> empty();

	/**
	 * See the <code>clone()</code> of {@link Object}
	 * 
	 * @return cloned object
	 */
	public ExtendedSet<T> clone();

	/**
	 * Computes the compression factor of the equivalent bitmap representation
	 * (1 means not compressed, namely a memory footprint similar to
	 * {@link BitSet}, 2 means twice the size of {@link BitSet}, etc.)
	 * 
	 * @return the compression factor
	 */
	public double bitmapCompressionRatio();

	/**
	 * Computes the compression factor of the equivalent integer collection (1
	 * means not compressed, namely a memory footprint similar to
	 * {@link ArrayList}, 2 means twice the size of {@link ArrayList}, etc.)
	 * 
	 * @return the compression factor
	 */
	public double collectionCompressionRatio();

	/**
	 * Extended version of the {@link Iterator} interface that allows to "skip"
	 * some elements of the set
	 * 
	 * @param <X>
	 *            the type of elements maintained by this set
	 */
	public interface ExtendedIterator<X> extends Iterator<X> {
		/**
		 * Skips all the elements before the the specified element, so that
		 * {@link Iterator#next()} gives the given element or, if it does not
		 * exist, the element immediately after according to the sorting
		 * provided by this {@link SortedSet} instance.
		 * <p>
		 * If <code>element</code> is less than the next element, it does
		 * nothing
		 * 
		 * @param element
		 *            first element to not skip
		 */
		public void skipAllBefore(X element);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<T> iterator();

	/**
	 * Gets the descending order iterator over the elements of type
	 * <code>T</code>
	 * 
	 * @return descending iterator
	 */
	public ExtendedIterator<T> descendingIterator();

	/**
	 * Allows to use the Java "for-each" statement in descending order
	 * 
	 * @return {@link Iterable} instance to iterate items in descending
	 *         order
	 */
	public Iterable<T> descending();

	/**
	 * Computes the power-set of the current set.
	 * <p>
	 * It is a particular implementation of the algorithm <i>Apriori</i> (see:
	 * Rakesh Agrawal, Ramakrishnan Srikant, <i>Fast Algorithms for Mining
	 * Association Rules in Large Databases</i>, in Proceedings of the
	 * 20<sup>th</sup> International Conference on Very Large Data Bases,
	 * p.487-499, 1994). The power-set does <i>not</i> contains the empty set.
	 * <p>
	 * The subsets composing the powerset are returned in a list that is sorted
	 * according to the lexicographical order provided by the sorted set.
	 * 
	 * @return the power-set
	 * @see #powerSet(int, int)
	 * @see #powerSetSize()
	 */
	public List<? extends ExtendedSet<T>> powerSet();

	/**
	 * Computes a subset of the power-set of the current set, composed by those
	 * subsets that have cardinality between <code>min</code> and
	 * <code>max</code>.
	 * <p>
	 * It is a particular implementation of the algorithm <i>Apriori</i> (see:
	 * Rakesh Agrawal, Ramakrishnan Srikant, <i>Fast Algorithms for Mining
	 * Association Rules in Large Databases</i>, in Proceedings of the
	 * 20<sup>th</sup> International Conference on Very Large Data Bases,
	 * p.487-499, 1994). The power-set does <i>not</i> contains the empty set.
	 * <p>
	 * The subsets composing the powerset are returned in a list that is sorted
	 * according to the lexicographical order provided by the sorted set.
	 * 
	 * @param min
	 *            minimum subset size (greater than zero)
	 * @param max
	 *            maximum subset size
	 * @return the power-set
	 * @see #powerSet()
	 * @see #powerSetSize(int, int)
	 */
	public List<? extends ExtendedSet<T>> powerSet(int min, int max);

	/**
	 * Computes the power-set size of the current set.
	 * <p>
	 * The power-set does <i>not</i> contains the empty set.
	 * 
	 * @return the power-set size
	 * @see #powerSet()
	 */
	public int powerSetSize();

	/**
	 * Computes the power-set size of the current set, composed by those
	 * subsets that have cardinality between <code>min</code> and
	 * <code>max</code>.
	 * <p>
	 * The power-set does <i>not</i> contains the empty set.
	 * 
	 * @param min
	 *            minimum subset size (greater than zero)
	 * @param max
	 *            maximum subset size
	 * @return the power-set size
	 * @see #powerSet(int, int)
	 */
	public int powerSetSize(int min, int max);

	/**
	 * Adds the last item of the given set to the current set
	 * 
	 * @param set
	 *            set where to pick the last item
	 * @return <code>true</code> if this set did not already contain the
	 *         specified element
	 */
	public boolean addLastOf(SortedSet<T> set);

	/**
	 * Adds the first item of the given set to the current set
	 * 
	 * @param set
	 *            set where to pick the first item
	 * @return <code>true</code> if this set did not already contain the
	 *         specified element
	 */
	public boolean addFirstOf(SortedSet<T> set);

	/**
	 * Removes the last item of the given set from the current set
	 * 
	 * @param set
	 *            set where to pick the last item
	 * @return <code>true</code> if this set already contained the
	 *         specified element
	 */
	public boolean removeLastOf(SortedSet<T> set);

	/**
	 * Removes the first item of the given set from the current set
	 * 
	 * @param set
	 *            set where to pick the first item
	 * @return <code>true</code> if this set already contained the
	 *         specified element
	 */
	public boolean removeFirstOf(SortedSet<T> set);

	/**
	 * Prints debug info about the given {@link ExtendedSet} implementation
	 * 
	 * @return a string that describes the internal representation of the
	 *         instance
	 */
	public String debugInfo();

	/**
	 * Adds to the set all the elements between <code>first</code> and
	 * <code>last</code>, both included. It supposes that there is an ordering
	 * of the elements of type <code>T</code> and that the universe of all
	 * possible elements is known.
	 * 
	 * @param from
	 *            first element
	 * @param to
	 *            last element
	 */
	public void fill(T from, T to);

	/**
	 * Removes from the set all the elements between <code>first</code> and
	 * <code>last</code>, both included. It supposes that there is an ordering
	 * of the elements of type <code>T</code> and that the universe of all
	 * possible elements is known.
	 * 
	 * @param from
	 *            first element
	 * @param to
	 *            last element
	 */
	public void clear(T from, T to);

	/**
	 * Adds the element if it not existing, or removes it if existing
	 * 
	 * @param e
	 *            element to flip
	 * @see #symmetricDifference(Collection)
	 */
	public void flip(T e);
	
	/**
	 * Gets the read-only version of the current set
	 * 
	 * @return the read-only version of the current set
	 */
	public ExtendedSet<T> unmodifiable();
	
	/**
	 * Gets the <code>i</code><sup>th</sup> element of the set
	 * 
	 * @param i
	 *            position of the element in the sorted set
	 * @return the <code>i</code><sup>th</sup> element of the set
	 * @throws IndexOutOfBoundsException
	 *             if <code>i</code> is less than zero, or greater or equal to
	 *             {@link #size()}
	 */
	public T get(int i);
	
	/**
	 * Provides position of element within the set.
	 * <p>
	 * It returns -1 if the element does not exist within the set.
	 * 
	 * @param e
	 *            element of the set
	 * @return the element position
	 */
	public int indexOf(T e);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<T> tailSet(T fromElement);
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<T> headSet(T toElement);
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<T> subSet(T fromElement, T toElement);

	/**
	 * Converts a given {@link Collection} instance into an instance of the
	 * current class. <b>NOTE:</b> when the collection is already an instance of
	 * the current class, the method returns the collection itself.
	 * 
	 * @param c
	 *            collection to use to generate the new instance
	 * @return the converted collection
	 * @see #convert(Object...)
	 */
	public ExtendedSet<T> convert(Collection<?> c);

	/**
	 * Converts a given integer array into an instance of the current class
	 * 
	 * @param e
	 *            objects to use to generate the new instance
	 * @return the converted collection
	 * @see #convert(Collection)
	 */
	public  ExtendedSet<T> convert(Object... e);	
	
	/**
	 * Computes the Jaccard similarity coefficient between this set and the
	 * given set.
	 * <p>
	 * The coefficient is defined as
	 * <code>|A intersection B| / |A union B|</code>.
	 * 
	 * @param other
	 *            the other set
	 * @return the Jaccard similarity coefficient
	 * @see #jaccardDistance(ExtendedSet)
	 */
	public double jaccardSimilarity(ExtendedSet<T> other);

	/**
	 * Computes the Jaccard distance between this set and the given set.
	 * <p>
	 * The coefficient is defined as 
	 * <code>1 - </code> {@link #jaccardSimilarity(ExtendedSet)}.
	 * 
	 * @param other
	 *            the other set
	 * @return the Jaccard distance
	 * @see #jaccardSimilarity(ExtendedSet)
	 */
	public double jaccardDistance(ExtendedSet<T> other);

	/**
	 * Computes the weighted version of the Jaccard similarity coefficient
	 * between this set and the given set.
	 * <p>
	 * The coefficient is defined as
	 * <code>sum of min(A_i, B_i) / sum of max(A_i, B_i)</code>.
	 * <p>
	 * <b>NOTE:</b> <code>T</code> must be a number, namely one of
	 * {@link Integer}, {@link Double}, {@link Float}, {@link Byte},
	 * {@link Long}, {@link Short}.
	 * 
	 * @param other
	 *            the other set
	 * @return the weighted Jaccard similarity coefficient
	 * @throws IllegalArgumentException
	 *             if <code>T</code> is not a number
	 * @see #weightedJaccardDistance(ExtendedSet)
	 */
	public double weightedJaccardSimilarity(ExtendedSet<T> other);

	/**
	 * Computes the weighted version of the Jaccard distance between this set
	 * and the given set.
	 * <p>
	 * The coefficient is defined as <code>1 - </code>
	 * {@link #weightedJaccardSimilarity(ExtendedSet)}.
	 * <p>
	 * <b>NOTE:</b> <code>T</code> must be a number, namely one of
	 * {@link Integer}, {@link Double}, {@link Float}, {@link Byte},
	 * {@link Long}, {@link Short}.
	 * 
	 * @param other
	 *            the other set
	 * @return the weighted Jaccard distance
	 * @throws IllegalArgumentException
	 *             if <code>T</code> is not a number
	 * @see #weightedJaccardSimilarity(ExtendedSet)
	 */
	public double weightedJaccardDistance(ExtendedSet<T> other);
}	


