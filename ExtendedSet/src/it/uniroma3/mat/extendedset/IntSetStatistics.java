package it.uniroma3.mat.extendedset;

import it.uniroma3.mat.extendedset.intset.IntSet;

import java.util.Collection;
import java.util.Formatter;

/**
 * @author colantonio
 */
public class IntSetStatistics implements IntSet {
	/** instance to monitor */
	private final IntSet container;

	
	/*
	 * Monitored characteristics
	 */
	private static long unionCount = 0;
	private static long intersectionCount = 0;
	private static long differenceCount = 0;
	private static long symmetricDifferenceCount = 0;
	private static long complementCount = 0;
	private static long unionSizeCount = 0;
	private static long intersectionSizeCount = 0;
	private static long differenceSizeCount = 0;
	private static long symmetricDifferenceSizeCount = 0;
	private static long complementSizeCount = 0;
	private static long equalsCount = 0;
	private static long containsAllCount = 0;
	private static long containsAnyCount = 0;
	private static long containsAtLeastCount = 0;
	
	
	/*
	 * Statistics getters
	 */
	
	/** @return number of union operations (i.e., {@link #addAll(IntSet)}, {@link #union(IntSet)}) */
	public static long getUnionCount() {return unionCount;}
	/** @return number of intersection operations (i.e., {@link #retainAll(IntSet)}, {@link #intersection(IntSet)}) */
	public static long getIntersectionCount() {return intersectionCount;}
	/** @return number of difference operations (i.e., {@link #removeAll(IntSet)}, {@link #difference(IntSet)}) */
	public static long getDifferenceCount() {return differenceCount;}
	/** @return number of symmetric difference operations (i.e., {@link #symmetricDifference(IntSet)}) */
	public static long getSymmetricDifferenceCount() {return symmetricDifferenceCount;}
	/** @return number of complement operations (i.e., {@link #complement()}, {@link #complemented()}) */
	public static long getComplementCount() {return complementCount;}
	/** @return cardinality of union operations (i.e., {@link #addAll(IntSet)}, {@link #union(IntSet)}) */
	public static long getUnionSizeCount() {return unionSizeCount;}
	/** @return cardinality of intersection operations (i.e., {@link #retainAll(IntSet)}, {@link #intersection(IntSet)}) */
	public static long getIntersectionSizeCount() {return intersectionSizeCount;}
	/** @return cardinality of difference operations (i.e., {@link #removeAll(IntSet)}, {@link #difference(IntSet)}) */
	public static long getDifferenceSizeCount() {return differenceSizeCount;}
	/** @return cardinality of symmetric difference operations (i.e., {@link #symmetricDifference(IntSet)}) */
	public static long getSymmetricDifferenceSizeCount() {return symmetricDifferenceSizeCount;}
	/** @return cardinality of complement operations (i.e., {@link #complement()}, {@link #complemented()}) */
	public static long getComplementSizeCount() {return complementSizeCount;}
	/** @return number of equality check operations (i.e., {@link #equals(Object)}) */
	public static long getEqualsCount() {return equalsCount;}
	/** @return number of {@link #containsAll(IntSet)} calls */
	public static long getContainsAllCount() {return containsAllCount;}
	/** @return number of {@link #containsAny(IntSet)} calls */
	public static long getContainsAnyCount() {return containsAnyCount;}
	/** @return number of {@link #containsAtLeast(IntSet, int)} calls */
	public static long getContainsAtLeastCount() {return containsAtLeastCount;}
	
	
	/*
	 * Other statistical methods
	 */
	
	/** Resets all counters */
	public static void resetCounters() {
		unionCount = intersectionCount = differenceCount = symmetricDifferenceCount = complementCount = 
			unionSizeCount = intersectionSizeCount = differenceSizeCount = symmetricDifferenceSizeCount = complementSizeCount = 
				equalsCount = containsAllCount = containsAnyCount = containsAtLeastCount = 0;
	}
	
	/** @return the summary information string */
	public static String summary() {
		final StringBuilder s = new StringBuilder();
		final Formatter f = new Formatter(s);
		
		f.format("unionCount: %d\n", unionCount);
		f.format("intersectionCount: %d\n", intersectionCount);
		f.format("differenceCount: %d\n", differenceCount);
		f.format("symmetricDifferenceCount: %d\n", symmetricDifferenceCount);
		f.format("complementCount: %d\n", complementCount);
		f.format("unionSizeCount: %d\n", unionSizeCount);
		f.format("intersectionSizeCount: %d\n", intersectionSizeCount);
		f.format("differenceSizeCount: %d\n", differenceSizeCount);
		f.format("symmetricDifferenceSizeCount: %d\n", symmetricDifferenceSizeCount);
		f.format("complementSizeCount: %d\n", complementSizeCount);
		f.format("equalsCount: %d\n", equalsCount);
		f.format("containsAllCount: %d\n", containsAllCount);
		f.format("containsAnyCount: %d\n", containsAnyCount);
		f.format("containsAtLeastCount: %d\n", containsAtLeastCount);
		
		return s.toString();
	}

	
	/**
	 * Wraps an {@link IntSet} instance with an {@link IntSetStatistics}
	 * instance
	 * 
	 * @param container
	 *            {@link IntSet} to wrap
	 */
	public IntSetStatistics(IntSet container) {
		this.container = container;
	}

	/*
	 * MONITORED METHODS
	 */

	/** {@inheritDoc} */ @Override public boolean addAll(IntSet c) {unionCount++; return container.addAll(c);}
	/** {@inheritDoc} */ @Override public IntSet union(IntSet other) {unionCount++; return new IntSetStatistics(container.union(other));}
	/** {@inheritDoc} */ @Override public boolean retainAll(IntSet c) {intersectionCount++; return container.retainAll(c);}
	/** {@inheritDoc} */ @Override public IntSet intersection(IntSet other) {intersectionCount++; return new IntSetStatistics(container.intersection(other));}
	/** {@inheritDoc} */ @Override public boolean removeAll(IntSet c) {differenceCount++; return container.removeAll(c);}
	/** {@inheritDoc} */ @Override public IntSet difference(IntSet other) {differenceCount++; return new IntSetStatistics(container.difference(other));}
	/** {@inheritDoc} */ @Override public IntSet symmetricDifference(IntSet other) {symmetricDifferenceCount++; return container.symmetricDifference(other);}
	/** {@inheritDoc} */ @Override public void complement() {complementCount++; container.complement();}
	/** {@inheritDoc} */ @Override public IntSet complemented() {complementCount++; return new IntSetStatistics(container.complemented());}
	/** {@inheritDoc} */ @Override public int unionSize(IntSet other) {unionSizeCount++; return container.unionSize(other);}
	/** {@inheritDoc} */ @Override public int intersectionSize(IntSet other) {intersectionSizeCount++; return container.intersectionSize(other);}
	/** {@inheritDoc} */ @Override public int differenceSize(IntSet other) {differenceSizeCount++; return container.differenceSize(other);}
	/** {@inheritDoc} */ @Override public int symmetricDifferenceSize(IntSet other) {symmetricDifferenceSizeCount++; return container.symmetricDifferenceSize(other);}
	/** {@inheritDoc} */ @Override public int complementSize() {complementSizeCount++; return container.complementSize();}
	/** {@inheritDoc} */ @Override public boolean equals(Object obj) {equalsCount++; return container.equals(obj);}
	/** {@inheritDoc} */ @Override public boolean containsAll(IntSet c) {containsAllCount++; return container.containsAll(c);}
	/** {@inheritDoc} */ @Override public boolean containsAny(IntSet other) {containsAnyCount++; return container.containsAny(other);}
	/** {@inheritDoc} */ @Override public boolean containsAtLeast(IntSet other, int minElements) {containsAtLeastCount++; return container.containsAtLeast(other, minElements);}

	
	/*
	 * SIMPLE REDIRECTION
	 */

	/** {@inheritDoc} */ @Override public double bitmapCompressionRatio() {return container.bitmapCompressionRatio();}
	/** {@inheritDoc} */ @Override public double collectionCompressionRatio() {return container.collectionCompressionRatio();}
	/** {@inheritDoc} */ @Override public void clear(int from, int to) {container.clear(from, to);}
	/** {@inheritDoc} */ @Override public void fill(int from, int to) {container.fill(from, to);}
	/** {@inheritDoc} */ @Override public void clear() {container.clear();}
	/** {@inheritDoc} */ @Override public boolean add(int i) {return container.add(i);}
	/** {@inheritDoc} */ @Override public boolean remove(int i) {return container.remove(i);}
	/** {@inheritDoc} */ @Override public void flip(int e) {container.flip(e);}
	/** {@inheritDoc} */ @Override public int get(int i) {return container.get(i);}
	/** {@inheritDoc} */ @Override public int indexOf(int e) {return container.indexOf(e);}
	/** {@inheritDoc} */ @Override public boolean contains(int i) {return container.contains(i);}
	/** {@inheritDoc} */ @Override public int first() {return container.first();}
	/** {@inheritDoc} */ @Override public int last() {return container.last();}
	/** {@inheritDoc} */ @Override public boolean isEmpty() {return container.isEmpty();}
	/** {@inheritDoc} */ @Override public int size() {return container.size();}
	/** {@inheritDoc} */ @Override public IntIterator iterator() {return container.iterator();}
	/** {@inheritDoc} */ @Override public IntIterator descendingIterator() {return container.descendingIterator();}
	/** {@inheritDoc} */ @Override public int[] toArray() {return container.toArray();}
	/** {@inheritDoc} */ @Override public int[] toArray(int[] a) {return container.toArray(a);}
	/** {@inheritDoc} */ @Override public int compareTo(IntSet o) {return container.compareTo(o);}
	
	/*
	 * OTHERS
	 */
	/** {@inheritDoc} */ @Override public IntSet empty() {return new IntSetStatistics(container.empty());}
	/** {@inheritDoc} */ @Override public IntSet clone() {return new IntSetStatistics(container.clone());}
	/** {@inheritDoc} */ @Override public IntSet convert(int... a) {return new IntSetStatistics(container.convert(a));}
	/** {@inheritDoc} */ @Override public IntSet convert(Collection<Integer> c) {return new IntSetStatistics(container.convert(c));}
	/** {@inheritDoc} */ @Override public String debugInfo() {return "STATISTICS OF " + container.debugInfo();}
}
