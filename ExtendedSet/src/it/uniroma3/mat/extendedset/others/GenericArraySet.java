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


package it.uniroma3.mat.extendedset.others;


import it.uniroma3.mat.extendedset.AbstractExtendedSet;
import it.uniroma3.mat.extendedset.ExtendedSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * {@link ExtendedSet}-based class internally managed by a sorted array.
 * <p>
 * Objects must be {@link Comparable} or, alternatively, a comparator must be
 * defined.
 * 
 * @param <T>
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 */
public class GenericArraySet<T> extends AbstractExtendedSet<T> {
	/** elements of the set */
	private T[] elements;
	
	/** set cardinality */
	private int size;
	
	private final Comparator<T> comparator;
	
	/**
	 * Comparator used when there is a natural ordering of the elements
	 */
	private class TrivialComparator implements Comparator<T> {
		@SuppressWarnings("unchecked")
		@Override
		public int compare(T o1, T o2) {
			return ((Comparable<T>) o1).compareTo(o2);
		}
	}
	
	/**
	 * Empty-set constructor
	 */
	public GenericArraySet() {
		size = 0;
		elements = null;
		comparator = new TrivialComparator();
	}

	/**
	 * Empty-set constructor
	 * @param comparator 
	 */
	public GenericArraySet(Comparator<T> comparator) {
		size = 0;
		elements = null;
		this.comparator = comparator;
	}

	/**
	 * Replace the content of the current instance with the content of another
	 * instance
	 * 
	 * @param other
	 */
	private void replaceWith(GenericArraySet<T> other) {
		size = other.size;
		elements = other.elements;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double bitmapCompressionRatio() {
		if (isEmpty())
			return 0D;
//		return size() / Math.ceil(elements[size - 1] / 32D);
		//TODO
		throw new UnsupportedOperationException("TODO");
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
	public GenericArraySet<T> empty() {
		return new GenericArraySet<T>(comparator);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<T> iterator() {
		return new ExtendedIterator<T>() {
			int next = 0;
			@Override
			public void skipAllBefore(T e) {
				if (comparator.compare(e, elements[next]) <= 0)
					return;
				next = Arrays.binarySearch(elements, next + 1, size, e, comparator);
				if (next < 0)
					next = -(next + 1);
			}
			@Override public boolean hasNext() {
				return next < size;
			}
			@Override public T next() {
				if (!hasNext())
					throw new NoSuchElementException();
				return elements[next++];
			}
			@Override public void remove() {
				next--;
				size--;
				System.arraycopy(elements, next + 1, elements, next, size - next);
				compact();
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<T> descendingIterator() {
		return new ExtendedIterator<T>() {
			int next = size - 1;
			@Override
			public void skipAllBefore(T e) {
				if (comparator.compare(e, elements[next]) >= 0)
					return;
				next = Arrays.binarySearch(elements, 0, next, e, comparator);
				if (next < 0)
					next = -(next + 1) - 1;
			}
			@Override public boolean hasNext() {
				return next > 0;
			}
			@Override public T next() {
				if (!hasNext())
					throw new NoSuchElementException();
				return elements[next--];
			}
			@Override public void remove() {
				next++;
				size--;
				System.arraycopy(elements, next + 1, elements, next, size - next);
				compact();
			}
		};
	}

	/** 
	 * {@inheritDoc} 
	 */ 
	@Override
	public GenericArraySet<T> clone() {
		// NOTE: do not use super.clone() since it is 10 times slower!
		GenericArraySet<T> c = empty();
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
	@SuppressWarnings("unchecked")
	private void ensureCapacity() {
		int capacity = elements == null ? 0 : elements.length;
		if (capacity >= size)
			return;
		capacity = Math.max(capacity << 1, size);

		if (elements == null) {
			// nothing to copy
			elements = (T[]) new Object[capacity];
			return;
		}
		elements = Arrays.copyOf(elements, capacity);
	}

	/**
	 * Removes unused allocated words at the end of {@link #words} only when they
	 * are more than twice of the needed space
	 */
	private void compact() {
		if (size == 0) {
			elements = null;
			return;
		}
		if (elements != null && (size << 1) < elements.length)
			elements = Arrays.copyOf(elements, size);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override 
	public boolean add(T element) {
		// append 
		if (isEmpty() || comparator.compare(elements[size - 1], element) < 0) {
			size++;
			ensureCapacity();
			elements[size - 1] = element;
			return true;
		}
		
		// insert
		int pos = Arrays.binarySearch(elements, 0, size, element, comparator);
		if (pos >= 0)
			return false;
		
		size++;
		ensureCapacity();
		pos = -(pos + 1);
		System.arraycopy(elements, pos, elements, pos + 1, size - pos - 1);
		elements[pos] = element;
		return true;
	}
	
	/** 
	 * {@inheritDoc} 
	 */ 
	@SuppressWarnings("unchecked")
	@Override 
	public boolean remove(Object element) {
		if (element == null)
			return false;
		
		int pos;
		try {
			pos = Arrays.binarySearch(elements, 0, size, (T) element, comparator);
			if (pos < 0)
				return false;
		} catch (ClassCastException e) {
			return false;
		}
		
		size--;
		System.arraycopy(elements, pos + 1, elements, pos, size - pos);
		compact();
		return true;
	}
	
	/** 
	 * {@inheritDoc} 
	 */ 
	@Override
	public void flip(T element) {
		// first
		if (isEmpty()) {
			size++;
			ensureCapacity();
			elements[size - 1] = element;
			return;
		}
		
		int pos = Arrays.binarySearch(elements, 0, size, element, comparator);

		// add
		if (pos < 0) {
			size++;
			ensureCapacity();
			pos = -(pos + 1);
			System.arraycopy(elements, pos, elements, pos + 1, size - pos - 1);
			elements[pos] = element;
			return;
		}
		
		// remove
		size--;
		System.arraycopy(elements, pos + 1, elements, pos, size - pos);
		compact();
	}

	/** 
	 * {@inheritDoc} 
	 */ 
	@SuppressWarnings("unchecked")
	@Override 
	public boolean contains(Object element) {
		if (element == null || isEmpty())
			return false;
		try {
			return Arrays.binarySearch(elements, 0, size, (T) element, comparator) >= 0;
		} catch (ClassCastException e) {
			return false;
		}
	}

	/** 
	 * {@inheritDoc} 
	 */ 
	@Override 
	public boolean containsAll(Collection<?> c) {
		if (c == null || c.isEmpty() || c == this)
			return true;
		if (isEmpty())
			return false;
		
		final GenericArraySet<T> o = convert(c);
		T[] otherElements = o.elements;
		int otherSize = o.size;
		int thisIndex = -1;
		int otherIndex = -1;
		while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
			thisIndex++;
			otherIndex++;
			while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) < 0) {
				if (thisIndex == size - 1)
					return false;
				thisIndex++;
			}
			if (comparator.compare(elements[thisIndex], otherElements[otherIndex]) > 0)
				return false;
		}
		return otherIndex == otherSize - 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(Collection<? extends T> other) {
		if (other == null || other.isEmpty() || other == this)
			return true;
		if (isEmpty())
			return false;

		final GenericArraySet<T> o = convert(other);
		T[] otherElements = o.elements;
		int otherSize = o.size;
		int thisIndex = -1;
		int otherIndex = -1;
		while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
			thisIndex++;
			otherIndex++;
			while (!elements[thisIndex].equals(otherElements[otherIndex])) {
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) > 0) {
					if (otherIndex == otherSize - 1)
						return false;
					otherIndex++;
				}
				if (elements[thisIndex].equals(otherElements[otherIndex]))
					break;
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) < 0) {
					if (thisIndex == size - 1)
						return false;
					thisIndex++;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAtLeast(Collection<? extends T> other, int minElements) {
		if (minElements < 1)
			throw new IllegalArgumentException();
		if ((size >= 0 && size < minElements) || other == null || other.isEmpty() || isEmpty())
			return false;
		if (this == other)
			return size() >= minElements;
			
		final GenericArraySet<T> o = convert(other);
		T[] otherElements = o.elements;
		int otherSize = o.size;
		int thisIndex = -1;
		int otherIndex = -1;
		int res = 0;
		while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
			thisIndex++;
			otherIndex++;
			while (!elements[thisIndex].equals(otherElements[otherIndex])) {
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) > 0) {
					if (otherIndex == otherSize - 1)
						return false;
					otherIndex++;
				}
				if (elements[thisIndex].equals(otherElements[otherIndex]))
					break;
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) < 0) {
					if (thisIndex == size - 1)
						return false;
					thisIndex++;
				}
			}
			res++;
			if (res >= minElements)
				return true;
		}
		return false;
	}

	/** 
	 * {@inheritDoc} 
	 */
	@Override
	public boolean addAll(Collection<? extends T> c) {
		GenericArraySet<T> res = union(c);
		boolean r = !equals(res);
		replaceWith(res);
		return r;
	}

	/** 
	 * {@inheritDoc} 
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		@SuppressWarnings("unchecked")
		GenericArraySet<T> res = intersection((Collection<? extends T>) c);
		boolean r = !equals(res);
		replaceWith(res);
		return r;
	}

	/** 
	 * {@inheritDoc} 
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		@SuppressWarnings("unchecked")
		GenericArraySet<T> res = difference((Collection<? extends T>) c);
		boolean r = !equals(res);
		replaceWith(res);
		return r;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
        if (isEmpty())
            return 0;
        int h = 1;
        for (int i = 0; i < size; i++)
            h = (h << 5) - h + elements[i].hashCode();
        return h;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof GenericArraySet<?>))
			return false;
		final GenericArraySet<?> other = (GenericArraySet<?>) obj;
		if (size != other.size)
			return false;
        for (int i = 0; i < size; i++)
        	if (!elements[i].equals(other.elements[i]))
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

	/**
	 * {@inheritDoc}
	 */
	@Override 
	public T first() {
		if (isEmpty()) 
			throw new NoSuchElementException();
		return elements[0];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override 
	public T last() {
		if (isEmpty()) 
			throw new NoSuchElementException();
		return elements[size - 1];
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int intersectionSize(Collection<? extends T> other) {
		if (isEmpty() || other == null || other.isEmpty())
			return 0;
		if (this == other)
			return size();

		final GenericArraySet<T> o = convert(other);
		T[] otherElements = o.elements;
		int otherSize = o.size;
		int thisIndex = -1;
		int otherIndex = -1;
		int res = 0;
		while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
			thisIndex++;
			otherIndex++;
			while (!elements[thisIndex].equals(otherElements[otherIndex])) {
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) > 0) {
					if (otherIndex == otherSize - 1)
						return res;
					otherIndex++;
				}
				if (elements[thisIndex].equals(otherElements[otherIndex]))
					break;
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) < 0) {
					if (thisIndex == size - 1)
						return res;
					thisIndex++;
				}
			}
			res++;
		}
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericArraySet<T> intersection(Collection<? extends T> other) {
		if (isEmpty() || other == null || other.isEmpty())
			return empty();
		if (this == other)
			return clone();
		
		final GenericArraySet<T> o = convert(other);
		T[] otherElements = o.elements;
		int otherSize = o.size;
		int thisIndex = -1;
		int otherIndex = -1;
		@SuppressWarnings("unchecked")
		T[] resElements = (T[]) new Object[Math.min(size, otherSize)];
		int resSize = 0;
		while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
			thisIndex++;
			otherIndex++;
			while (!elements[thisIndex].equals(otherElements[otherIndex])) {
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) > 0) {
					if (otherIndex == otherSize - 1) {
						GenericArraySet<T> res = empty();
						res.elements = resElements;
						res.size = resSize;
						res.compact();
						return res;
					}
					otherIndex++;
				}
				if (elements[thisIndex].equals(otherElements[otherIndex]))
					break;
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) < 0) {
					if (thisIndex == size - 1) {
						GenericArraySet<T> res = empty();
						res.elements = resElements;
						res.size = resSize;
						res.compact();
						return res;
					}
					thisIndex++;
				}
			}
			resElements[resSize++] = elements[thisIndex];
		}
		
		GenericArraySet<T> res = empty();
		res.elements = resElements;
		res.size = resSize;
		res.compact();
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericArraySet<T> union(Collection<? extends T> other) {
		if (this == other || other == null || other.isEmpty())
			return clone();
		if (isEmpty()) {
			GenericArraySet<T> cloned = convert(other);
			if (cloned == other)
				cloned = cloned.clone();
			return cloned;
		}
		
		final GenericArraySet<T> o = convert(other);
		T[] otherElements = o.elements;
		int otherSize = o.size;
		int thisIndex = -1;
		int otherIndex = -1;
		@SuppressWarnings("unchecked")
		T[] resElements = (T[]) new Object[size + otherSize];
		int resSize = 0;
		mainLoop:
		while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
			thisIndex++;
			otherIndex++;
			while (!elements[thisIndex].equals(otherElements[otherIndex])) {
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) > 0) {
					resElements[resSize++] = otherElements[otherIndex];
					if (otherIndex == otherSize - 1) {
						resElements[resSize++] = elements[thisIndex];
						break mainLoop;
					}
					otherIndex++;
				}
				if (elements[thisIndex].equals(otherElements[otherIndex]))
					break;
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) < 0) {
					resElements[resSize++] = elements[thisIndex];
					if (thisIndex == size - 1) {
						resElements[resSize++] = otherElements[otherIndex];
						break mainLoop;
					}
					thisIndex++;
				}
			}
			resElements[resSize++] = elements[thisIndex];
		}
		while (thisIndex < size - 1)
			resElements[resSize++] = elements[++thisIndex];
		while (otherIndex < otherSize - 1)
			resElements[resSize++] = otherElements[++otherIndex];
		
		GenericArraySet<T> res = empty();
		res.elements = resElements;
		res.size = resSize;
		res.compact();
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericArraySet<T> difference(Collection<? extends T>  other) {
		if (isEmpty() || this == other)
			return empty();
		if (other == null || other.isEmpty()) 
			return clone();
		
		final GenericArraySet<T> o = convert(other);
		T[] otherElements = o.elements;
		int otherSize = o.size;
		int thisIndex = -1;
		int otherIndex = -1;
		@SuppressWarnings("unchecked")
		T[] resElements = (T[]) new Object[size];
		int resSize = 0;
		mainLoop:
		while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
			thisIndex++;
			otherIndex++;
			while (!elements[thisIndex].equals(otherElements[otherIndex])) {
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) > 0) {
					if (otherIndex == otherSize - 1) {
						resElements[resSize++] = elements[thisIndex];
						break mainLoop;
					}
					otherIndex++;
				}
				if (elements[thisIndex].equals(otherElements[otherIndex]))
					break;
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) < 0) {
					resElements[resSize++] = elements[thisIndex];
					if (thisIndex == size - 1) {
						break mainLoop;
					}
					thisIndex++;
				}
			}
		}
		while (thisIndex < size - 1)
			resElements[resSize++] = elements[++thisIndex];
		
		GenericArraySet<T> res = empty();
		res.elements = resElements;
		res.size = resSize;
		res.compact();
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericArraySet<T> symmetricDifference(Collection<? extends T> other) {
		if (this == other || other == null || other.isEmpty())
			return clone();
		if (isEmpty()) 
			return convert(other).clone();
		
		final GenericArraySet<T> o = convert(other);
		T[] otherElements = o.elements;
		int otherSize = o.size;
		int thisIndex = -1;
		int otherIndex = -1;
		@SuppressWarnings("unchecked")
		T[] resElements = (T[]) new Object[size + otherSize];
		int resSize = 0;
		mainLoop:
		while (thisIndex < (size - 1) && otherIndex < (otherSize - 1)) {
			thisIndex++;
			otherIndex++;
			while (!elements[thisIndex].equals(otherElements[otherIndex])) {
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) > 0) {
					resElements[resSize++] = otherElements[otherIndex];
					if (otherIndex == otherSize - 1) {
						resElements[resSize++] = elements[thisIndex];
						break mainLoop;
					}
					otherIndex++;
				}
				if (elements[thisIndex].equals(otherElements[otherIndex]))
					break;
				while (comparator.compare(elements[thisIndex], otherElements[otherIndex]) < 0) {
					resElements[resSize++] = elements[thisIndex];
					if (thisIndex == size - 1) {
						resElements[resSize++] = otherElements[otherIndex];
						break mainLoop;
					}
					thisIndex++;
				}
			}
		}
		while (thisIndex < size - 1)
			resElements[resSize++] = elements[++thisIndex];
		while (otherIndex < otherSize - 1)
			resElements[resSize++] = otherElements[++otherIndex];

		GenericArraySet<T> res = empty();
		res.elements = resElements;
		res.size = resSize;
		res.compact();
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
//	@SuppressWarnings("unchecked")
	@Override
	public void complement() {
		if (isEmpty())
			return;
		
		//TODO
		throw new UnsupportedOperationException("TODO");
		
//		ExtendedIterator<T> thisItr = clone().iterator(); // avoid concurrency
//		elements = (T[]) new Object[complementSize()];
//		size = 0;
//		T u = null;
//		while (thisItr.hasNext()) {
//			T c = thisItr.next();
//			while (comparator.compare((T) (++u), c) < 0)  
//				elements[size++] = (T) u;
//		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fill(T from, T to) {
		//TODO
		throw new UnsupportedOperationException("TODO");
		
//		if (comparator.compare(from, to) > 0)
//			throw new IndexOutOfBoundsException("from: " + from + " > to: " + to);
//		if (from.equals(to)) {
//			add(from);
//			return;
//		}
//		if (isEmpty()) {
//			size = to - from + 1;
//			ensureCapacity();
//			for (int i = 0; i < size; i++) 
//				elements[i] = from++;
//			return;
//		}
//
//		// increase capacity, if necessary
//		int posFrom = Arrays.binarySearch(elements, 0, size, from, comparator);
//		boolean fromMissing = posFrom < 0;
//		if (fromMissing) 
//			posFrom = -posFrom - 1;
//
//		int posTo = Arrays.binarySearch(elements, posFrom, size, to, comparator);
//		boolean toMissing = posTo < 0;
//		if (toMissing) 
//			posTo = -posTo - 1;
//
//		int delta = 0;
//		if (toMissing || (fromMissing && (posFrom == posTo + 1)))
//			delta = 1;
//		
//		int gap = to - from; 
//		delta += gap - (posTo - posFrom);
//		if (delta > 0) {
//			size += delta;
//			ensureCapacity();
//			System.arraycopy(elements, posTo, elements, posTo + delta, size - delta - posTo);
//			posTo = posFrom + gap;
//
//			// set values
//			for (int i = posFrom; i <= posTo; i++) 
//				elements[i] = from++;
//		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear(T from, T to) {
		if (isEmpty())
			return;
		if (comparator.compare(from, to) > 0)
			throw new IndexOutOfBoundsException("from: " + from + " > to: " + to);
		if (from.equals(to)) {
			remove(from);
			return;
		}

		int posFrom = Arrays.binarySearch(elements, 0, size, from, comparator);
		if (posFrom < 0)
			posFrom = -posFrom - 1;
		if (posFrom >= size)
			return;
		int posTo = Arrays.binarySearch(elements, posFrom, size, to, comparator);
		if (posTo >= 0)
			posTo++;
		else
			posTo = -posTo - 1;
		if (posFrom == posTo)
			return;
		System.arraycopy(elements, posTo, elements, posFrom, size - posTo);
		size -= posTo - posFrom;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public GenericArraySet<T> convert(Object... a) {
		T[] resElements = null;
		int resSize = 0;
		T last = null;
		if (a != null) {
			resElements = (T[]) new Object[a.length];
			a = Arrays.copyOf(a, a.length);
			Arrays.sort(a);
			for (Object i : a)
				if (!i.equals(last))
					resElements[resSize++] = last = (T) i;
		}

		GenericArraySet<T> res = empty();
		res.elements = resElements;
		res.size = resSize;
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public GenericArraySet<T> convert(Collection<?> c) {
		Collection<T> sorted;
		T[] resElements = null;
		int resSize = 0;
		T last = null;
		if (c != null) {
			resElements = (T[]) new Object[c.size()];
			if (c instanceof SortedSet<?> && ((SortedSet<?>) c).comparator() == null) {
				sorted = (Collection<T>) c;
			} else {
				sorted = new ArrayList<T>((Collection<T>) c);
				Collections.sort((List<T>) sorted, comparator);
			}
			for (T i : sorted)
				if (!i.equals(last))
					resElements[resSize++] = last = i;
		}

		GenericArraySet<T> res = empty();
		res.elements = resElements;
		res.size = resSize;
		return res;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericArraySet<T> complemented() {
		GenericArraySet<T> res = clone();
		res.complement();
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T get(int i) {
		if (i < 0 || i >= size)
			throw new IndexOutOfBoundsException(Integer.toString(i));
		return elements[i];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int indexOf(T e) {
		int pos = Arrays.binarySearch(elements, 0, size, e, comparator);
		if (pos < 0)
			return -1;
		return pos;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(ExtendedSet<T> o) {
		ExtendedIterator<T> thisIterator = this.descendingIterator();
		ExtendedIterator<T> otherIterator = o.descendingIterator();
		while (thisIterator.hasNext() && otherIterator.hasNext()) {
			T thisItem = thisIterator.next();
			T otherItem = otherIterator.next();
			if (comparator.compare(thisItem, otherItem) < 0)
				return -1;
			if (comparator.compare(thisItem, otherItem) > 0)
				return 1;
		}
		return thisIterator.hasNext() ? 1 : (otherIterator.hasNext() ? -1 : 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Comparator<? super T> comparator() {
		if (comparator instanceof GenericArraySet<?>.TrivialComparator)
			return null;
		return comparator;
	}
}
