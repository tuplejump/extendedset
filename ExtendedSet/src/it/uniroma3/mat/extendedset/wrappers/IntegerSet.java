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

package it.uniroma3.mat.extendedset.wrappers;

import it.uniroma3.mat.extendedset.AbstractExtendedSet;
import it.uniroma3.mat.extendedset.ExtendedSet;
import it.uniroma3.mat.extendedset.IntSet;
import it.uniroma3.mat.extendedset.IntSet.ExtendedIntIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;

/**
 * This class provides a "wrapper" for any {@link IntSet} instance in
 * order to be used as an {@link ExtendedSet} instance.
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 */
public class IntegerSet extends AbstractExtendedSet<Integer> {
	/** the collection of <code>int</code> numbers */
	private final IntSet items;

	/**
	 * Wraps an instance of {@link IntSet}
	 * 
	 * @param items
	 *            the {@link IntSet} to wrap
	 */
	public IntegerSet(IntSet items) {
		this.items = items;
	}

	/**
	 * Converts a generic collection of {@link Integer} instances to a
	 * {@link IntSet} instance. If the given collection is an
	 * {@link IntegerSet} instance, it returns the contained
	 * {@link #items} object.
	 * 
	 * @param c
	 *            the generic collection of {@link Integer} instances
	 * @return the resulting {@link IntSet} instance
	 */
	private IntSet toExtendedIntSet(Collection<?> c) {
		// nothing to convert
		if (c == null)
			return null;
		if (c instanceof IntegerSet) 
			return ((IntegerSet) c).items;
		
		// extract integers from the given collection
		IntSet res = items.empty();
		List<Integer> sorted = new ArrayList<Integer>(c.size());
		for (Object i : c) {
			try {
				sorted.add((Integer) i);
			} catch (ClassCastException e) {
				// do nothing
			}
		}
		Collections.sort(sorted);
		for (Integer i : sorted) 
			res.add(i.intValue());
		return res;
	}

	/**
	 * Returns an {@link Integer} instance representing the specified
	 * <code>int</code> value
	 * <p>
	 * <b>IMPORTANT!</b> It generates new {@link Integer} instances!
	 * 
	 * @param i
	 *            integer to convert
	 * @return the {@link Integer} instance
	 */
	private static Integer toInteger(int i) {
		// TODO: to yield better space and time performance, cache frequently requested values.
		return Integer.valueOf(i);
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean addAll(Collection<? extends Integer> c) {
		Statistics.incUnionCount();
		return items.addAll(toExtendedIntSet(c));
	}

	/** {@inheritDoc} */
	@Override
	public boolean addFirstOf(SortedSet<Integer> set) {
		return items.add(toExtendedIntSet(set).first());
	}

	/** {@inheritDoc} */
	@Override
	public boolean addLastOf(SortedSet<Integer> set) {
		return items.add(toExtendedIntSet(set).last());
	}

	/** {@inheritDoc} */
	@Override
	public double bitmapCompressionRatio() {
		return items.bitmapCompressionRatio();
	}

	/** {@inheritDoc} */
	@Override
	public void clear(Integer from, Integer to) {
		items.clear(from.intValue(), to.intValue());
	}

	/** {@inheritDoc} */
	@Override
	public IntegerSet clone() {
		// NOTE: do not use super.clone() since it is 10 times slower!
		return new IntegerSet(items.clone());
	}

	/** {@inheritDoc} */
	@Override
	public double collectionCompressionRatio() {
		return items.collectionCompressionRatio();
	}

	/** {@inheritDoc} */
	@Override
	public int compareTo(ExtendedSet<Integer> o) {
		Statistics.incEqualsCount();
		return items.compareTo(toExtendedIntSet(o));
	}

	/** {@inheritDoc} */
	@Override
	public IntegerSet complemented() {
		return new IntegerSet(items.complemented());
	}

	/** {@inheritDoc} */
	@Override
	public int complementSize() {
		return items.complementSize();
	}

	/** {@inheritDoc} */
	@Override
	public boolean containsAny(Collection<? extends Integer> other) {
		Statistics.incSizeCheckCount();
		return items.containsAny(toExtendedIntSet(other));
	}

	/** {@inheritDoc} */
	@Override
	public boolean containsAtLeast(Collection<? extends Integer> other, int minElements) {
		Statistics.incSizeCheckCount();
		return items.containsAtLeast(toExtendedIntSet(other), minElements);
	}

	/** {@inheritDoc} */
	@Override
	public IntegerSet convert(Collection<?> c) {
		return new IntegerSet(toExtendedIntSet(c));
	}

	/** {@inheritDoc} */
	@Override
	public IntegerSet convert(Object... e) {
		return convert(Arrays.asList(e));
	}

	/** {@inheritDoc} */
	@Override
	public String debugInfo() {
		return getClass().getSimpleName() + "\n" + items.debugInfo();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<Integer> descendingIterator() {
		return new ExtendedIterator<Integer>() {
			final ExtendedIntIterator itr = items.descendingIntIterator();
			@Override public void remove() {itr.remove();}
			@Override public Integer next() {return toInteger(itr.next());}
			@Override public boolean hasNext() {return itr.hasNext();}
			@Override public void skipAllBefore(Integer element) {itr.skipAllBefore(element.intValue());}
		};
	}

	/** {@inheritDoc} */
	@Override
	public IntegerSet difference(Collection<? extends Integer> other) {
		Statistics.incDifferenceCount();
		return new IntegerSet(items.difference(toExtendedIntSet(other)));
	}

	/** {@inheritDoc} */
	@Override
	public int differenceSize(Collection<? extends Integer> other) {
		Statistics.incSizeCheckCount();
		return items.differenceSize(toExtendedIntSet(other));
	}

	/** {@inheritDoc} */
	@Override
	public IntegerSet empty() {
		return new IntegerSet(items.empty());
	}

	/** {@inheritDoc} */
	@Override
	public boolean equals(Object o) {
		Statistics.incEqualsCount();
		if (this == o)
			return true;
		if (!(o instanceof IntegerSet))
			return false;
		return items.equals(((IntegerSet) o).items);
	}

	/** {@inheritDoc} */
	@Override
	public void fill(Integer from, Integer to) {
		items.fill(from.intValue(), to.intValue());
	}

	/** 
	 * {@inheritDoc} 
	 */
	@Override
	public Integer first() {
		return toInteger(items.first());
	}

	/** {@inheritDoc} */
	@Override
	public void flip(Integer e) {
		items.flip(e.intValue());
	}

	/** 
	 * {@inheritDoc} 
	 */
	@Override
	public Integer get(int i) {
		return toInteger(items.get(i));
	}

	/** {@inheritDoc} */
	@Override
	public int indexOf(Integer e) {
		return items.indexOf(e.intValue());
	}

	/** {@inheritDoc} */
	@Override
	public IntegerSet intersection(Collection<? extends Integer> other) {
		Statistics.incIntersectionCount();
		return new IntegerSet(items.intersection(toExtendedIntSet(other)));
	}

	/** {@inheritDoc} */
	@Override
	public int intersectionSize(Collection<? extends Integer> other) {
		Statistics.incSizeCheckCount();
		return items.intersectionSize(toExtendedIntSet(other));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<Integer> iterator() {
		return new ExtendedIterator<Integer>() {
			final ExtendedIntIterator itr = items.intIterator();
			@Override public void remove() {itr.remove();}
			@Override public Integer next() {return toInteger(itr.next());}
			@Override public boolean hasNext() {return itr.hasNext();}
			@Override public void skipAllBefore(Integer element) {itr.skipAllBefore(element.intValue());}
		};
	}

	/** 
	 * {@inheritDoc} 
	 */
	@Override
	public Integer last() {
		return toInteger(items.last());
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends IntegerSet> powerSet() {
		return (List<? extends IntegerSet>) super.powerSet();
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends IntegerSet> powerSet(int min, int max) {
		return (List<? extends IntegerSet>) super.powerSet(min, max);
	}

	/** {@inheritDoc} */
	@Override
	public boolean removeAll(Collection<?> c) {
		Statistics.incDifferenceCount();
		return items.removeAll(toExtendedIntSet(c));
	}

	/** {@inheritDoc} */
	@Override
	public boolean removeFirstOf(SortedSet<Integer> set) {
		return items.remove(toExtendedIntSet(set).first());
	}

	/** {@inheritDoc} */
	@Override
	public boolean removeLastOf(SortedSet<Integer> set) {
		return items.remove(toExtendedIntSet(set).last());
	}

	/** {@inheritDoc} */
	@Override
	public boolean retainAll(Collection<?> c) {
		Statistics.incIntersectionCount();
		return items.retainAll(toExtendedIntSet(c));
	}

	/** {@inheritDoc} */
	@Override
	public IntegerSet symmetricDifference(Collection<? extends Integer> other) {
		Statistics.incSymmetricDifferenceCount();
		return new IntegerSet(items.symmetricDifference(toExtendedIntSet(other)));
	}

	/** {@inheritDoc} */
	@Override
	public int symmetricDifferenceSize(Collection<? extends Integer> other) {
		Statistics.incSizeCheckCount();
		return items.symmetricDifferenceSize(toExtendedIntSet(other));
	}

	/** {@inheritDoc} */
	@Override
	public IntegerSet union(Collection<? extends Integer> other) {
		Statistics.incUnionCount();
		return new IntegerSet(items.union(toExtendedIntSet(other)));
	}

	/** {@inheritDoc} */
	@Override
	public int unionSize(Collection<? extends Integer> other) {
		Statistics.incSizeCheckCount();
		return items.unionSize(toExtendedIntSet(other));
	}

	/** {@inheritDoc} */
	@Override
	public int hashCode() {
		return items.hashCode();
	}

	/** {@inheritDoc} */
	@Override
	public void complement() {
		items.complement();
	}

	/** {@inheritDoc} */
	@Override
	public Comparator<? super Integer> comparator() {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public boolean add(Integer e) {
		return items.add(e.intValue());
	}

	/** {@inheritDoc} */
	@Override
	public void clear() {
		items.clear();
	}

	/** {@inheritDoc} */
	@Override
	public boolean contains(Object o) {
		return o instanceof Integer && items.contains(((Integer) o).intValue());
	}

	/** {@inheritDoc} */
	@Override
	public boolean containsAll(Collection<?> c) {
		Statistics.incSizeCheckCount();
		return items.containsAll(toExtendedIntSet(c));
	}

	/** {@inheritDoc} */
	@Override
	public boolean isEmpty() {
		return items.isEmpty();
	}

	/** {@inheritDoc} */
	@Override
	public boolean remove(Object o) {
		return o instanceof Integer && items.remove(((Integer) o).intValue());
	}

	/** {@inheritDoc} */
	@Override
	public int size() {
		return items.size();
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		// NOTE: by not calling super.toString(), we avoid to iterate over new
		// Integer instances, thus avoiding to waste time and memory with garbage
		// collection
		return items.toString();
	}
}
