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

package it.uniroma3.mat.extendedset.intset;


import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * This class provides a skeletal implementation of the {@link IntSet}
 * interface to minimize the effort required to implement this interface.
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @see ConciseSet
 * @see FastSet
 */
public abstract class AbstractIntSet implements IntSet {
	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract IntSet intersection(IntSet other);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract IntSet union(IntSet other);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract IntSet difference(IntSet other);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract IntSet symmetricDifference(IntSet other);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract IntSet complemented();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract void complement();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean containsAny(IntSet other);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean containsAtLeast(IntSet other, int minElements);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract int intersectionSize(IntSet other);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public int unionSize(IntSet other) {
		return other == null ? size() : size() + other.size() - intersectionSize(other);
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public int symmetricDifferenceSize(IntSet other) {
		return other == null ? size() : size() + other.size() - 2 * intersectionSize(other);
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public int differenceSize(IntSet other) {
		return other == null ? size() : size() - intersectionSize(other);
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public int complementSize() {
		if (isEmpty())
			return 0;
		return last() - size() + 1;
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract IntSet empty();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract IntSet clone();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract double bitmapCompressionRatio();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract double collectionCompressionRatio();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract IntIterator iterator();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract IntIterator descendingIterator();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract String debugInfo();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract void fill(int from, int to);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract void clear(int from, int to);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract void flip(int e);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract int get(int i);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract int indexOf(int e);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract IntSet convert(int... a);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract IntSet convert(Collection<Integer> c);
	
	/** 
	 * {@inheritDoc}
	 */
	@Override
	public int first() {
		if (isEmpty())
			throw new NoSuchElementException();
		return iterator().next();
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract int last();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract int size();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean isEmpty();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean contains(int i);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean add(int i);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean remove(int i);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean containsAll(IntSet c);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean addAll(IntSet c);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean retainAll(IntSet c);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract boolean removeAll(IntSet c);

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public abstract void clear();

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public int[] toArray() {
		if (isEmpty())
			return null;
		return toArray(new int[size()]);
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public int[] toArray(int[] a) {
		if (a.length < size())
			throw new IllegalArgumentException();
		if (isEmpty())
			return a;
		IntIterator itr = iterator();
		int i = 0;
		while (itr.hasNext()) 
			a[i++] = itr.next();
		return a;
	}
	
	/** 
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
        IntIterator itr = iterator();
    	if (!itr.hasNext())
    	    return "[]";

		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (;;) {
			int e = itr.next();
			sb.append(e);
			if (!itr.hasNext())
				return sb.append(']').toString();
			sb.append(", ");
		}
	}
}
