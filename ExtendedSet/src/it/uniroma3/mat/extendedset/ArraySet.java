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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * {@link ExtendedSet}-based class internally managed by an array
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 */
//TODO to complete and override AbstractExtendedSet methods...
public class ArraySet extends AbstractExtendedSet<Integer> {
	/** elements of the set */
	private /*final*/ int[] elements;
	
	/** set cardinality */
	private int size;
	
	/**
	 * Empty-set constructor
	 */
	public ArraySet() {
		size = 0;
		elements = null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double bitmapCompressionRatio() {
		if (isEmpty())
			return 0D;
		return size() / Math.ceil(elements[size - 1] / 32D);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double collectionCompressionRatio() {
		return isEmpty() ? 0D : 1D;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ArraySet empty() {
		return new ArraySet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<Integer> iterator() {
		return new ExtendedIterator<Integer>() {
			int next = 0;
			@Override
			public void skipAllBefore(Integer e) {
				if (e <= elements[next])
					return;
				next = Arrays.binarySearch(elements, 0, size, e);
				if (next < 0)
					next = -(next + 1);
			}
			@Override public boolean hasNext() {
				return next < size;
			}
			@Override public Integer next() {
				if (!hasNext())
					throw new NoSuchElementException();
				return elements[next++];
			}
			@Override public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/** 
	 * {@inheritDoc} 
	 */ 
	@Override
	public ArraySet clone() {
		// NOTE: do not use super.clone() since it is 10 times slower!
		ArraySet c = empty();
		if (!isEmpty()) {
			c.elements = Arrays.copyOf(elements, elements.length);
			c.size = size;
		}
		return c;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String debugInfo() {
		return toString();
	}

	/**
	 * Assures that the size of {@link #elements} is sufficient to contain
	 * {@link #size} elements.
	 */
	private void ensureCapacity() {
		int capacity = elements == null ? 0 : elements.length;
		if (capacity >= size)
			return;
		capacity = Math.max(capacity << 1, size);

		if (elements == null) {
			// nothing to copy
			elements = new int[capacity];
			return;
		}
		elements = Arrays.copyOf(elements, capacity);
	}

	/* 
	 * Collection methods
	 */

	/**
	 * @param i
	 */
	private void append(int i) {
		assert size == 0 || elements[size - 1] < i;
		size++;
		ensureCapacity();
		elements[size - 1] = i;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override 
	public boolean add(Integer e) {
		if (isEmpty()) {
			append(e);
			return true;
		}
		
		int pos = Arrays.binarySearch(elements, 0, size, e);
		if (pos >= 0)
			return false;
		size++;
		ensureCapacity();
		pos = -(pos + 1);
		System.arraycopy(elements, pos, elements, pos + 1, size - pos - 1);
		elements[pos] = e;
		return true;
	}
	
	/** 
	 * {@inheritDoc} 
	 */ 
	@Override 
	public boolean remove(Object o) {
		if (!(o instanceof Integer))
			return false;
		
		int e = (Integer) o;
		int pos = Arrays.binarySearch(elements, 0, size, e);
		if (pos < 0)
			return false;
		size--;
		System.arraycopy(elements, pos + 1, elements, pos, size - pos);
		return true;
	}
	
	/** 
	 * {@inheritDoc} 
	 */ 
	@Override 
	public boolean contains(Object o) {
		if (!(o instanceof Integer) || isEmpty())
			return false;
		return Arrays.binarySearch(elements, 0, size, (Integer) o) >= 0;
	}

	/** 
	 * {@inheritDoc} 
	 */ 
	@Override 
	public boolean containsAll(Collection<?> c) {
		Statistics.sizeCheckCount++;
		if (isEmpty() || c == null || c.isEmpty())
			return false;
		if (this == c)
			return true;
		
		if (c instanceof ArraySet) {
			int[] otherElements = ((ArraySet) c).elements;
			int otherSize = c.size();
			int thisIndex = -1;
			int otherIndex = -1;
			while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
				thisIndex++;
				otherIndex++;
				while (elements[thisIndex] < otherElements[otherIndex]) {
					if (thisIndex == size - 1)
						return false;
					thisIndex++;
				}
				if (elements[thisIndex] > otherElements[otherIndex])
					return false;
			}
			return otherIndex == otherSize - 1;
		}

		return super.containsAll(c);
	}

	/** 
	 * {@inheritDoc} 
	 */
	@Override
	public boolean addAll(Collection<? extends Integer> c) {
		ArraySet res = union(c);
		boolean r = !equals(res);
		elements = res.elements;
		size = res.size;
		return r;
	}

	/** 
	 * {@inheritDoc} 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean retainAll(Collection<?> c) {
		try {
			ArraySet res = intersection((Collection<Integer>) c);
			boolean r = !equals(res);
			elements = res.elements;
			size = res.size;
			return r;
		} catch (ClassCastException e) {
			return false;
		}
	}

	/** 
	 * {@inheritDoc} 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean removeAll(Collection<?> c) {
		try {
			ArraySet res = difference((Collection<Integer>) c);
			boolean r = !equals(res);
			elements = res.elements;
			size = res.size;
			return r;
		} catch (ClassCastException e) {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
        if (elements == null)
            return 0;
        int h = 1;
        for (int i = 0; i < size; i++)
            h = (h << 5) - h + elements[i];
        return h;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ArraySet))
			return false;
		ArraySet other = (ArraySet) obj;
		if (size != other.size)
			return false;
        for (int i = 0; i < size; i++)
        	if (elements[i] != other.elements[i])
        		return false;
		return true;
	}

	/** 
	 * {@inheritDoc} 
	 */
	@Override
	public int size() {
		return size;
	}

	/** 
	 * {@inheritDoc} 
	 */
	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/** 
	 * {@inheritDoc} 
	 */
	@Override
	public void clear() {
		elements = null; 
		size = 0;
	}

	
	/* 
	 * SortedSet methods
	 */
	
	/** {@inheritDoc} */ 
	@Override 
	public Comparator<? super Integer> comparator() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override 
	public Integer first() {
		if (isEmpty()) 
			throw new NoSuchElementException();
		return elements[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override 
	public Integer last() {
		if (isEmpty()) 
			throw new NoSuchElementException();
		return elements[size - 1];
	}
	
	/*
	 * ExtendedSet methods 
	 */
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int intersectionSize(Collection<? extends Integer> other) {
		Statistics.sizeCheckCount++;
		if (isEmpty() || other == null || other.isEmpty())
			return 0;
		if (this == other)
			return size();

		if (other instanceof ArraySet) {
			int[] otherElements = ((ArraySet) other).elements;
			int otherSize = other.size();
			int thisIndex = -1;
			int otherIndex = -1;
			int res = 0;
			while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
				thisIndex++;
				otherIndex++;
				while (elements[thisIndex] != otherElements[otherIndex]) {
					while (elements[thisIndex] > otherElements[otherIndex]) {
						if (otherIndex == otherSize - 1)
							return res;
						otherIndex++;
					}
					if (elements[thisIndex] == otherElements[otherIndex])
						break;
					while (elements[thisIndex] < otherElements[otherIndex]) {
						if (thisIndex == size - 1)
							return res;
						thisIndex++;
					}
				}
				res++;
			}
			return res;
		}

		Statistics.sizeCheckCount--;
		return super.intersectionSize(other);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ArraySet intersection(Collection<? extends Integer> other) {
		Statistics.intersectionCount++;
		if (isEmpty() || other == null || other.isEmpty())
			return empty();
		if (this == other)
			return clone();
		
		if (other instanceof ArraySet) {
			int[] otherElements = ((ArraySet) other).elements;
			int otherSize = other.size();
			int thisIndex = -1;
			int otherIndex = -1;
			ArraySet res = empty();
			while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
				thisIndex++;
				otherIndex++;
				while (elements[thisIndex] != otherElements[otherIndex]) {
					while (elements[thisIndex] > otherElements[otherIndex]) {
						if (otherIndex == otherSize - 1)
							return res;
						otherIndex++;
					}
					if (elements[thisIndex] == otherElements[otherIndex])
						break;
					while (elements[thisIndex] < otherElements[otherIndex]) {
						if (thisIndex == size - 1)
							return res;
						thisIndex++;
					}
				}
				res.append(elements[thisIndex]);
			}
			return res;
		}

		ArraySet clone = clone();
		clone.retainAll(other);
		return clone;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ArraySet union(Collection<? extends Integer> other) {
		Statistics.unionCount++;
		if (this == other || other == null || other.isEmpty())
			return clone();
		if (isEmpty()) {
			List<Integer> sorted = new ArrayList<Integer>(other);
			Collections.sort(sorted);
			ArraySet res = empty();
			for (Integer e : sorted) 
				if (res.isEmpty() || !res.last().equals(e))
					res.append(e);
			return res;
		}
		
		if (other instanceof ArraySet) {
			int[] otherElements = ((ArraySet) other).elements;
			int otherSize = other.size();
			int thisIndex = -1;
			int otherIndex = -1;
			ArraySet res = empty();
			mainLoop:
			while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
				thisIndex++;
				otherIndex++;
				while (elements[thisIndex] != otherElements[otherIndex]) {
					while (elements[thisIndex] > otherElements[otherIndex]) {
						res.append(otherElements[otherIndex]);
						if (otherIndex == otherSize - 1) {
							res.append(elements[thisIndex]);
							break mainLoop;
						}
						otherIndex++;
					}
					if (elements[thisIndex] == otherElements[otherIndex])
						break;
					while (elements[thisIndex] < otherElements[otherIndex]) {
						res.append(elements[thisIndex]);
						if (thisIndex == size - 1) {
							res.append(otherElements[otherIndex]);
							break mainLoop;
						}
						thisIndex++;
					}
				}
				res.append(elements[thisIndex]);
			}
			while (thisIndex < size - 1)
				res.append(elements[++thisIndex]);
			while (otherIndex < otherSize - 1)
				res.append(otherElements[++otherIndex]);
			return res;
		}

		ArraySet clone = clone();
		for (Integer e : other)
			clone.add(e);
		return clone;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ArraySet difference(Collection<? extends Integer> other) {
		Statistics.differenceCount++;
		if (isEmpty() || this == other)
			return empty();
		if (other == null || other.isEmpty()) 
			return clone();
		
		if (other instanceof ArraySet) {
			int[] otherElements = ((ArraySet) other).elements;
			int otherSize = other.size();
			int thisIndex = -1;
			int otherIndex = -1;
			ArraySet res = empty();
			mainLoop:
			while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
				thisIndex++;
				otherIndex++;
				while (elements[thisIndex] != otherElements[otherIndex]) {
					while (elements[thisIndex] > otherElements[otherIndex]) {
						if (otherIndex == otherSize - 1) {
							res.append(elements[thisIndex]);
							break mainLoop;
						}
						otherIndex++;
					}
					if (elements[thisIndex] == otherElements[otherIndex])
						break;
					while (elements[thisIndex] < otherElements[otherIndex]) {
						res.append(elements[thisIndex]);
						if (thisIndex == size - 1) {
							break mainLoop;
						}
						thisIndex++;
					}
				}
			}
			while (thisIndex < size - 1)
				res.append(elements[++thisIndex]);
			return res;
		}

		ArraySet clone = clone();
		for (Integer e : other)
			clone.remove(e);
		return clone;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ArraySet symmetricDifference(Collection<? extends Integer> other) {
		Statistics.symmetricDifferenceCount++;
		if (this == other || other == null || other.isEmpty())
			return clone();
		if (isEmpty()) {
			ArraySet res = empty();
			res.addAll(other);
			return res;
		}
		
		if (other instanceof ArraySet) {
			int[] otherElements = ((ArraySet) other).elements;
			int otherSize = other.size();
			int thisIndex = -1;
			int otherIndex = -1;
			ArraySet res = empty();
			mainLoop:
			while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
				thisIndex++;
				otherIndex++;
				while (elements[thisIndex] != otherElements[otherIndex]) {
					while (elements[thisIndex] > otherElements[otherIndex]) {
						res.append(otherElements[otherIndex]);
						if (otherIndex == otherSize - 1) {
							res.append(elements[thisIndex]);
							break mainLoop;
						}
						otherIndex++;
					}
					if (elements[thisIndex] == otherElements[otherIndex])
						break;
					while (elements[thisIndex] < otherElements[otherIndex]) {
						res.append(elements[thisIndex]);
						if (thisIndex == size - 1) {
							res.append(otherElements[otherIndex]);
							break mainLoop;
						}
						thisIndex++;
					}
				}
			}
			while (thisIndex < size - 1)
				res.append(elements[++thisIndex]);
			while (otherIndex < otherSize - 1)
				res.append(otherElements[++otherIndex]);
		}
		
		ArraySet clone = union(other);
		clone.removeAll(intersection(other));
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complement() {
		Iterator<Integer> thisItr = clone().iterator(); // avoid concurrency
		clear();
		int u = -1;
		while (thisItr.hasNext()) {
			Integer c = thisItr.next();
			while (++u < c)  
				append(u);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fill(Integer from, Integer to) {
		int curr = from;
		do add(curr++);
		while (curr <= to);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear(Integer from, Integer to) {
		int curr = from;
		do remove(curr++);
		while (curr <= to);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ArraySet convert(Collection<?> c) {
		ArraySet res = empty();
		res.addAll((Collection<Integer>) c);
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ArraySet convert(Object... e) {
		return convert(Arrays.asList(e));
	}
	
	/**
	 * Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ArraySet s = new ArraySet();
		s = s.convert(4, 40, 3, 1, 11000);
		System.out.println("s = " + s.debugInfo() + " ---> " + s);
		
		ArraySet t = s.convert(2, 4, 3, 10, 11, 20, 40);
		System.out.println("t = " + t.debugInfo() + " ---> " + t);
		
		System.out.println("t.intersection(s) = " + t.intersection(s));
		System.out.println("t.union(s) = " + t.union(s));
		System.out.println("t.difference(s) = " + t.difference(s));
		System.out.println("t.symmetricDifference(s) = " + t.symmetricDifference(s));
		System.out.println("t.intersectionSize(s) = " + t.intersectionSize(s));
		System.out.println("t.subSet(3, 11).intersection(s) = " + t.subSet(3, 11).intersection(s));
		
		System.out.println("t.complemented() = " + t.complemented());
		
		t.fill(10, 50);
		t.clear(20, 30);
		System.out.println("t + from 10 to 50, 20-30 excluded: " + t);
	}
}
