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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * {@link ExtendedSet}-based class internally managed by an instance of any
 * class implementing {@link Collection}
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @param <T>
 *            the type of elements maintained by this set
 */
public class GenericExtendedSet<T extends Comparable<T>> extends AbstractExtendedSet<T> {
	/** elements of the set */
	private /*final*/ Collection<T> elements;
	
	/** class implementing {@link Collection} that is used to collect elements */
	@SuppressWarnings("unchecked")
	private final Class<? extends Collection> setClass;

	/**
	 * collection of all possible elements. If <code>null</code>,
	 * {@link #fill(Comparable, Comparable)},
	 * {@link #clear(Comparable, Comparable)}, and {@link #complement()} cannot
	 * be used!
	 */
	private final Collection<T> universe;

	/**
	 * Set of all integers &gt;= 0.
	 * <p>
	 * To be used as a universe with
	 * {@link GenericExtendedSet#GenericExtendedSet(Class, Collection)} when the
	 * type <code>T</code> is {@link Integer}
	 */
	public static final SortedSet<Integer> ALL_POSITIVE_INTEGERS = new IntegerUniverse();
	private static class IntegerUniverse extends AbstractSet<Integer> implements SortedSet<Integer> {
		int min = 0;
		int max = Integer.MAX_VALUE;
		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				private Integer curr = min;
				@Override public boolean hasNext() {return curr != null;}
				@Override public Integer next() {
					Integer prev = curr;
					if (curr < max) curr++;
					else curr = null;
					return prev;
				}
				@Override public void remove() {throw new UnsupportedOperationException();}
			};
		}
		@Override public boolean contains(Object o) {return o != null && o instanceof Integer && ((Integer) o) >= min && ((Integer) o) <= max;}
		@Override public int size() {return max - min + 1;}
		@Override public Comparator<? super Integer> comparator() {return null;}
		@Override public Integer first() {return min;}
		@Override public Integer last() {return max;}
		@Override public SortedSet<Integer> subSet(Integer fromElement, Integer toElement) {
			if (fromElement < 0 || fromElement > toElement)
				throw new IllegalArgumentException();
			IntegerUniverse sub = new IntegerUniverse();
			sub.min = fromElement;
			sub.max = toElement;
			return sub;
		}
		@Override public SortedSet<Integer> headSet(Integer toElement) {return subSet(0, toElement);}
		@Override public SortedSet<Integer> tailSet(Integer fromElement) {return subSet(fromElement, Integer.MAX_VALUE);}
	}

	/**
	 * Empty-set constructor
	 * 
	 * @param setClass
	 *            {@link Collection}-derived class
	 * @param universe
	 *            all possible elements manageable by the instance. It is used
	 *            by, {@link ExtendedSet#complement()},
	 *            {@link ExtendedSet#complemented()},
	 *            {@link ExtendedSet#fill(Object, Object)}, and
	 *            {@link ExtendedSet#clear(Object, Object)}, which will throw
	 *            {@link UnsupportedOperationException} if the universe equals
	 *            <code>null</code>. Notice that the universe <i>must</i> be
	 *            aligned with the actual element set managed by the instance.
	 *            No checks will be done to ensure that each element of the set
	 *            is actually within the given universe.
	 * @see GenericExtendedSet#ALL_POSITIVE_INTEGERS           
	 */
	@SuppressWarnings("unchecked")
	public GenericExtendedSet(Class<? extends Collection> setClass, Collection<T> universe) {
		this.setClass = setClass;
		this.universe = universe;
		try {
			elements = setClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double bitmapCompressionRatio() {
		if (universe == null)
			throw new UnsupportedOperationException("missing universe");
		if (isEmpty())
			return 0D;
		if (universe instanceof SortedSet<?>) {
			int last = ((SortedSet<T>) universe).headSet(last()).size();
			return size() / Math.ceil(last / 32D);
		}
		throw new UnsupportedOperationException();
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
	public GenericExtendedSet<T> empty() {
		return new GenericExtendedSet<T>(setClass, universe);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedIterator<T> iterator() {
		// prepare the sorted set
		final Collection<T> sorted;
		if (elements instanceof SortedSet<?> || elements instanceof List<?>) {
			//NOTE: SortedSet.comparator() is null
			sorted = elements;
		} else {
			sorted = new ArrayList<T>(elements);
			Collections.sort((List<T>) sorted);
		}
		
		// iterate over the sorted set
		return new ExtendedIterator<T>() {
			final Iterator<T> itr = sorted.iterator();
			T current;
			{
				current = itr.hasNext() ? itr.next() : null;
			}
			@Override
			public void skipAllBefore(T element) {
				while (element.compareTo(current) > 0) 
					next();
			}
			@Override public boolean hasNext() {
				return current != null;
			}
			@Override public T next() {
				if (!hasNext())
					throw new NoSuchElementException();
				T prev = current;
				current = itr.hasNext() ? itr.next() : null;
				return prev;
			}
			@Override public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/** 
	 * {@inheritDoc} 
	 */ 
	@SuppressWarnings("unchecked")
	@Override
	public GenericExtendedSet<T> clone() {
		// NOTE: do not use super.clone() since it is 10 times slower!
		GenericExtendedSet<T> c = empty();
		try {
			if (elements instanceof Cloneable) {
				c.elements = (Collection<T>) elements.getClass().getMethod("clone").invoke(elements);
			} else {
				c.elements = setClass.newInstance();
				Statistics.decIntersectionCount();
				c.elements.addAll(elements);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return c;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String debugInfo() {
		return setClass.getSimpleName() + ": " + elements.toString();
	}
	
	
	
	/* 
	 * Collection methods
	 */

	/**
	 * {@inheritDoc}
	 */
	@Override 
	public boolean add(T e) {
		if (elements instanceof List<?>) {
			final List<T> l = (List<T>) elements;
			int pos = Collections.binarySearch(l, e);
			if (pos >= 0)
				return false;
			l.add(-(pos + 1), e);
			return true;
		}
		return elements.add(e);
	}
	
	/** 
	 * {@inheritDoc} 
	 */ 
	@SuppressWarnings("unchecked")
	@Override 
	public boolean remove(Object o) {
		if (elements instanceof List<?>) {
			try {
				final List<T> l = (List<T>) elements;
				int pos = Collections.binarySearch(l, (T) o);
				if (pos < 0)
					return false;
				l.remove(pos);
				return true;
			} catch (ClassCastException e) {
				return false;
			}
		}
		return elements.remove(o);
	}
	
	/** 
	 * {@inheritDoc} 
	 */ 
	@SuppressWarnings("unchecked")
	@Override 
	public boolean contains(Object o) {
		if (elements instanceof List<?>) {
			try {
				return Collections.binarySearch((List<T>) elements, (T) o) >= 0;
			} catch (ClassCastException e) {
				return false;
			}
		}
		return elements.contains(o);
	}

	/** 
	 * {@inheritDoc} 
	 */ 
	@SuppressWarnings("unchecked")
	@Override 
	public boolean containsAll(Collection<?> c) {
		Statistics.incSizeCheckCount();
		if (isEmpty() || c == null || c.isEmpty())
			return false;
		if (this == c)
			return true;
		
		if (elements instanceof List<?> && c instanceof GenericExtendedSet<?> && ((GenericExtendedSet<?>) c).elements instanceof List<?>) {
			Iterator<T> thisItr = elements.iterator();
			Iterator<T> otherItr = ((GenericExtendedSet<T>) c).elements.iterator();
			while (thisItr.hasNext() && otherItr.hasNext()) {
				T thisValue = thisItr.next();
				T otherValue = otherItr.next();

				int r;
				while ((r = otherValue.compareTo(thisValue)) > 0) {
					if (!thisItr.hasNext())
						return false;
					thisValue = thisItr.next();
				}
				if (r < 0)
					return false;
			}
			return !otherItr.hasNext();
		}

		return elements.containsAll(c);
	}

	/** 
	 * {@inheritDoc} 
	 */
	@Override
	public boolean addAll(Collection<? extends T> c) {
		if (elements instanceof List<?>) {
			Collection<T> res = union(c).elements;
			boolean r = !res.equals(elements);
			elements = res;
			return r;
		}
		Statistics.incUnionCount();
		return elements.addAll(c);
	}

	/** 
	 * {@inheritDoc} 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean retainAll(Collection<?> c) {
		if (elements instanceof List<?>) {
			try {
				Collection<T> res = intersection((Collection<T>) c).elements;
				boolean r = !res.equals(elements);
				elements = res;
				return r;
			} catch (ClassCastException e) {
				return false;
			}
		}
		Statistics.incIntersectionCount();
		return elements.retainAll(c);
	}

	/** 
	 * {@inheritDoc} 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean removeAll(Collection<?> c) {
		if (elements instanceof List<?>) {
			try {
				Collection<T> res = difference((Collection<T>) c).elements;
				boolean r = !res.equals(elements);
				elements = res;
				return r;
			} catch (ClassCastException e) {
				return false;
			}
		}
		Statistics.incDifferenceCount();
		return elements.removeAll(c);
	}

	/** 
	 * {@inheritDoc} 
	 */ 
	@Override 
	public boolean equals(Object o) {
		return o instanceof GenericExtendedSet<?> && ((GenericExtendedSet<?>) o).elements.equals(elements);
	}

	/** {@inheritDoc} */ @Override public int size() {return elements.size();}
	/** {@inheritDoc} */ @Override public boolean isEmpty() {return elements.isEmpty();}
	/** {@inheritDoc} */ @Override public void clear() {elements.clear();}
	/** {@inheritDoc} */ @Override public int hashCode() {return elements.hashCode();}

	
	/* 
	 * SortedSet methods
	 */
	
	/** {@inheritDoc} */ 
	@Override 
	public Comparator<? super T> comparator() {
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override 
	public T first() {
		if (elements instanceof SortedSet<?>)
			return ((SortedSet<T>) elements).first();
		if (elements instanceof List<?>)
			return ((List<T>) elements).get(0);
		return super.first();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override 
	public T last() {
		if (elements instanceof SortedSet<?>)
			return ((SortedSet<T>) elements).last();
		if (elements instanceof List<?>)
			return ((List<T>) elements).get(elements.size() - 1);
		return super.last();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<T> headSet(T toElement) {
		if (elements instanceof SortedSet<?>) {
			GenericExtendedSet<T> c = empty();
			c.elements = ((SortedSet<T>) elements).headSet(toElement);
			return c;
		}
		return super.headSet(toElement);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<T> tailSet(T fromElement) {
		if (elements instanceof SortedSet<?>) {
			GenericExtendedSet<T> c = empty();
			c.elements = ((SortedSet<T>) elements).tailSet(fromElement);
			return c;
		}
		return super.headSet(fromElement);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<T> subSet(T fromElement, T toElement) {
		if (elements instanceof SortedSet<?>) {
			GenericExtendedSet<T> c = empty();
			c.elements = ((SortedSet<T>) elements).subSet(fromElement, toElement);
			return c;
		}
		return super.headSet(toElement);
	}

	
	/*
	 * ExtendedSet methods 
	 */
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int intersectionSize(Collection<? extends T> other) {
		Statistics.incSizeCheckCount();
		if (isEmpty() || other == null || other.isEmpty())
			return 0;
		if (this == other)
			return size();
		
		if (elements instanceof List<?> && other instanceof GenericExtendedSet<?> && ((GenericExtendedSet<?>) other).elements instanceof List<?>) {
			int res = 0;
			Iterator<T> thisItr = elements.iterator();
			@SuppressWarnings("unchecked")
			Iterator<T> otherItr = ((GenericExtendedSet<T>) other).elements.iterator();
			while (thisItr.hasNext() && otherItr.hasNext()) {
				T thisValue = thisItr.next();
				T otherValue = otherItr.next();

				int r = thisValue.compareTo(otherValue);
				while (r != 0) {
					while ((r = thisValue.compareTo(otherValue)) > 0) {
						if (!otherItr.hasNext())
							return res;
						otherValue = otherItr.next();
					}
					if (r == 0)
						break;
					while ((r = otherValue.compareTo(thisValue)) > 0) {
						if (!thisItr.hasNext())
							return res;
						thisValue = thisItr.next();
					}
				}
				
				res++;
			}
			return res;
		}

		Statistics.decSizeCheckCount();
		return super.intersectionSize(other);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericExtendedSet<T> intersection(Collection<? extends T> other) {
		Statistics.incIntersectionCount();
		if (isEmpty() || other == null || other.isEmpty())
			return empty();
		if (this == other)
			return clone();
		
		if (elements instanceof List<?> && other instanceof GenericExtendedSet<?> && ((GenericExtendedSet<?>) other).elements instanceof List<?>) {
			GenericExtendedSet<T> res = empty();
			Iterator<T> thisItr = elements.iterator();
			@SuppressWarnings("unchecked")
			Iterator<T> otherItr = ((GenericExtendedSet<T>) other).elements.iterator();
			while (thisItr.hasNext() && otherItr.hasNext()) {
				T thisValue = thisItr.next();
				T otherValue = otherItr.next();

				int r = thisValue.compareTo(otherValue);
				while (r != 0) {
					while ((r = thisValue.compareTo(otherValue)) > 0) {
						if (!otherItr.hasNext())
							return res;
						otherValue = otherItr.next();
					}
					if (r == 0)
						break;
					while ((r = otherValue.compareTo(thisValue)) > 0) {
						if (!thisItr.hasNext())
							return res;
						thisValue = thisItr.next();
					}
				}
				
				res.elements.add(thisValue);
			}
			return res;
		}

		GenericExtendedSet<T> clone = clone();
		clone.elements.retainAll(other);
		return clone;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericExtendedSet<T> union(Collection<? extends T> other) {
		Statistics.incUnionCount();
		if (this == other || other == null || other.isEmpty())
			return clone();
		if (isEmpty()) {
			GenericExtendedSet<T> res = empty();
			res.elements.addAll(other);
			return res;
		}
		
		if (elements instanceof List<?> && other instanceof GenericExtendedSet<?> && ((GenericExtendedSet<?>) other).elements instanceof List<?>) {
			GenericExtendedSet<T> res = empty();
			Iterator<T> thisItr = elements.iterator();
			@SuppressWarnings("unchecked")
			Iterator<T> otherItr = ((GenericExtendedSet<T>) other).elements.iterator();
			mainLoop:
			while (thisItr.hasNext() && otherItr.hasNext()) {
				T thisValue = thisItr.next();
				T otherValue = otherItr.next();

				int r = thisValue.compareTo(otherValue);
				while (r != 0) {
					while ((r = thisValue.compareTo(otherValue)) > 0) {
						res.elements.add(otherValue);
						if (!otherItr.hasNext()) {
							res.elements.add(thisValue);
							break mainLoop;
						}
						otherValue = otherItr.next();
					}
					if (r == 0)
						break;
					while ((r = otherValue.compareTo(thisValue)) > 0) {
						res.elements.add(thisValue);
						if (!thisItr.hasNext()) {
							res.elements.add(otherValue);
							break mainLoop;
						}
						thisValue = thisItr.next();
					}
				}
				
				res.elements.add(thisValue);
			}
			while (thisItr.hasNext())
				res.elements.add(thisItr.next());
			while (otherItr.hasNext())
				res.elements.add(otherItr.next());
			return res;
		}

		GenericExtendedSet<T> clone = clone();
		for (T e : other)
			clone.add(e);
		return clone;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericExtendedSet<T> difference(Collection<? extends T> other) {
		Statistics.incDifferenceCount();
		if (isEmpty() || this == other)
			return empty();
		if (other == null || other.isEmpty()) 
			return clone();
		
		if (elements instanceof List<?> && other instanceof GenericExtendedSet<?> && ((GenericExtendedSet<?>) other).elements instanceof List<?>) {
			GenericExtendedSet<T> res = empty();
			Iterator<T> thisItr = elements.iterator();
			@SuppressWarnings("unchecked")
			Iterator<T> otherItr = ((GenericExtendedSet<T>) other).elements.iterator();
			mainLoop:
			while (thisItr.hasNext() && otherItr.hasNext()) {
				T thisValue = thisItr.next();
				T otherValue = otherItr.next();

				int r = thisValue.compareTo(otherValue);
				while (r != 0) {
					while ((r = thisValue.compareTo(otherValue)) > 0) {
						if (!otherItr.hasNext()) {
							res.elements.add(thisValue);
							break mainLoop;
						}
						otherValue = otherItr.next();
					}
					if (r == 0)
						break;
					while ((r = otherValue.compareTo(thisValue)) > 0) {
						res.elements.add(thisValue);
						if (!thisItr.hasNext())
							break mainLoop;
						thisValue = thisItr.next();
					}
				}
			}
			while (thisItr.hasNext())
				res.elements.add(thisItr.next());
			return res;
		}
		
		GenericExtendedSet<T> clone = clone();
		clone.elements.removeAll(other);
		return clone;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericExtendedSet<T> symmetricDifference(Collection<? extends T> other) {
		Statistics.incSymmetricDifferenceCount();
		if (this == other || other == null || other.isEmpty())
			return clone();
		if (isEmpty()) {
			GenericExtendedSet<T> res = empty();
			res.elements.addAll(other);
			return res;
		}
		
		if (elements instanceof List<?> && other instanceof GenericExtendedSet<?> && ((GenericExtendedSet<?>) other).elements instanceof List<?>) {
			GenericExtendedSet<T> res = empty();
			Iterator<T> thisItr = elements.iterator();
			@SuppressWarnings("unchecked")
			Iterator<T> otherItr = ((GenericExtendedSet<T>) other).elements.iterator();
			mainLoop:
			while (thisItr.hasNext() && otherItr.hasNext()) {
				T thisValue = thisItr.next();
				T otherValue = otherItr.next();

				int r = thisValue.compareTo(otherValue);
				while (r != 0) {
					while ((r = thisValue.compareTo(otherValue)) > 0) {
						res.elements.add(otherValue);
						if (!otherItr.hasNext()) {
							res.elements.add(thisValue);
							break mainLoop;
						}
						otherValue = otherItr.next();
					}
					if (r == 0)
						break;
					while ((r = otherValue.compareTo(thisValue)) > 0) {
						res.elements.add(thisValue);
						if (!thisItr.hasNext()) {
							res.elements.add(otherValue);
							break mainLoop;
						}
						thisValue = thisItr.next();
					}
				}
			}
			while (thisItr.hasNext())
				res.elements.add(thisItr.next());
			while (otherItr.hasNext())
				res.elements.add(otherItr.next());
			return res;
		}
		
		GenericExtendedSet<T> clone = union(other);
		clone.removeAll(intersection(other));
		return clone;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complement() {
		if (universe == null)
			throw new UnsupportedOperationException("missing universe");
		Iterator<T> universeItr = universe.iterator();
		Iterator<T> thisItr = clone().iterator(); // avoid concurrency
		clear();
		T u;
		while (thisItr.hasNext()) {
			T c = thisItr.next();
			while ((u = universeItr.next()).compareTo(c) < 0)  
				elements.add(u);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<T> unmodifiable() {
		GenericExtendedSet<T> c = empty();
		c.elements = Collections.unmodifiableCollection(elements);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fill(T from, T to) {
		if (universe == null)
			throw new UnsupportedOperationException("missing universe");
		if (universe instanceof SortedSet<?>) { 
			addAll(((SortedSet<T>) universe).subSet(from, to));
		} else {
			Iterator<T> uniItr = universe.iterator();
			T curr = null;
			while (uniItr.hasNext() && (curr = uniItr.next()).compareTo(from) < 0) {/*empty*/}
			do add(curr);
			while (uniItr.hasNext() && (curr = uniItr.next()).compareTo(to) <= 0);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear(T from, T to) {
		if (universe == null)
			throw new UnsupportedOperationException("missing universe");
		if (universe instanceof SortedSet<?>) { 
			Statistics.decDifferenceCount();
			removeAll(((SortedSet<T>) universe).subSet(from, to));
		} else {
			Iterator<T> uniItr = universe.iterator();
			T curr = null;
			while (uniItr.hasNext() && (curr = uniItr.next()).compareTo(from) < 0) {/*empty*/}
			do remove(curr);
			while (uniItr.hasNext() && (curr = uniItr.next()).compareTo(to) <= 0);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericExtendedSet<T> convert(Collection<?> c) {
		GenericExtendedSet<T> res = (GenericExtendedSet<T>) super.convert(c);
		if (res.elements instanceof List<?>)
			Collections.sort((List<T>) res.elements);
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericExtendedSet<T> convert(Object... e) {
		GenericExtendedSet<T> res = (GenericExtendedSet<T>) super.convert(e);
		if (res.elements instanceof List<?>)
			Collections.sort((List<T>) res.elements);
		return res;
	}
	
	/**
	 * Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
//		GenericExtendedSet<Integer> s = new GenericExtendedSet<Integer>(HashSet.class, ALL_POSITIVE_INTEGERS);
		GenericExtendedSet<Integer> s = new GenericExtendedSet<Integer>(ArrayList.class, ALL_POSITIVE_INTEGERS);
		s = s.convert(4, 40, 3, 1, 11000);
		System.out.println("s = " + s.debugInfo() + " ---> " + s);
		
		GenericExtendedSet<Integer> t = s.convert(2, 4, 3, 10, 11, 20, 40);
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
