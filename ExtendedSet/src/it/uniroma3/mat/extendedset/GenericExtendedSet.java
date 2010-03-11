package it.uniroma3.mat.extendedset;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

/**
 * {@link ExtendedSet}-based class internally managed by any {@link Set} instance
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @param <T>
 *            the type of elements maintained by this set
 */
public class GenericExtendedSet<T extends Comparable<T>> extends AbstractExtendedSet<T> {
	private /*final*/ Set<T> elements;
	
	@SuppressWarnings("unchecked")
	private final Class<? extends Set> setClass;

	private final Collection<T> universe;

	/**
	 * Set of all integers &gt;= 0.
	 * <p>
	 * To be used with
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
	 *            {@link Set}-derived class
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
	public GenericExtendedSet(Class<? extends Set> setClass, Collection<T> universe) {
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
		if (elements instanceof SortedSet<?>) {
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
		GenericExtendedSet<T> c = (GenericExtendedSet<T>) super.clone();
		try {
			if (elements instanceof Cloneable) {
				c.elements = (Set<T>) elements.getClass().getMethod("clone").invoke(elements);
			} else {
				c.elements = setClass.newInstance();
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
	 * Set methods
	 */
	/** {@inheritDoc} */ @Override public boolean add(T e) {return elements.add(e);}
	/** {@inheritDoc} */ @Override public int size() {return elements.size();}
	/** {@inheritDoc} */ @Override public boolean isEmpty() {return elements.isEmpty();}
	/** {@inheritDoc} */ @Override public boolean contains(Object o) {return elements.contains(o);}
	/** {@inheritDoc} */ @Override public boolean remove(Object o) {return elements.remove(o);}
	/** {@inheritDoc} */ @Override public boolean containsAll(Collection<?> c) {return elements.containsAll(c);}
	/** {@inheritDoc} */ @Override public boolean addAll(Collection<? extends T> c) {return elements.addAll(c);}
	/** {@inheritDoc} */ @Override public boolean retainAll(Collection<?> c) {return elements.retainAll(c);}
	/** {@inheritDoc} */ @Override public boolean removeAll(Collection<?> c) {return elements.removeAll(c);}
	/** {@inheritDoc} */ @Override public void clear() {elements.clear();}
	/** {@inheritDoc} */ @Override public boolean equals(Object o) {return elements.equals(o);}
	/** {@inheritDoc} */ @Override public int hashCode() {return elements.hashCode();}

	
	/* 
	 * SortedSet methods
	 */
	
	/** {@inheritDoc} */ 
	@Override 
	public Comparator<? super T> comparator() {
		return null;
	}
	
	/** {@inheritDoc} */ 
	@Override 
	public T first() {
		if (elements instanceof SortedSet<?>)
			return ((SortedSet<T>) elements).first();
		return super.first();
	}

	/** {@inheritDoc} */ 
	@Override 
	public T last() {
		if (elements instanceof SortedSet<?>)
			return ((SortedSet<T>) elements).last();
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
	public GenericExtendedSet<T> intersection(Collection<? extends T> other) {
		return (GenericExtendedSet<T>) super.intersection(other);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericExtendedSet<T> union(Collection<? extends T> other) {
		return (GenericExtendedSet<T>) super.union(other);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericExtendedSet<T> difference(Collection<? extends T> other) {
		return (GenericExtendedSet<T>) super.difference(other);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericExtendedSet<T> symmetricDifference(Collection<? extends T> other) {
		return (GenericExtendedSet<T>) super.symmetricDifference(other);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complement() {
		if (universe == null)
			throw new UnsupportedOperationException("missing universe");
		Iterator<T> itr = universe.iterator();
		GenericExtendedSet<T> clone = clone();
		clear();
		T u;
		for (T e : clone)
			while ((u = itr.next()).compareTo(e) < 0)  
				add(u);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public ExtendedSet<T> unmodifiable() {
		GenericExtendedSet<T> c = empty();
		c.elements = Collections.unmodifiableSet(elements);
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
		return (GenericExtendedSet<T>) super.convert(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public GenericExtendedSet<T> convert(T... e) {
		return (GenericExtendedSet<T>) super.convert(e);
	}
	
	/**
	 * Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		GenericExtendedSet<Integer> s = new GenericExtendedSet<Integer>(HashSet.class, ALL_POSITIVE_INTEGERS);
		s = s.convert(4, 40, 3, 1, 11000);
		System.out.println("s = " + s.debugInfo() + " ---> " + s);
		
		GenericExtendedSet<Integer> t = s.convert(2, 4, 3, 10, 11, 20, 40);
		System.out.println("t = " + t.debugInfo() + " ---> " + t);
		
		System.out.println("t.intersection(s) = " + t.intersection(s));
		System.out.println("t.subSet(3, 11).intersection(s) = " + t.subSet(3, 11).intersection(s));
		
		System.out.println("t.complemented() = " + t.complemented());
		
		t.fill(10, 50);
		t.clear(20, 30);
		System.out.println("t + from 10 to 50, 20-30 excuded: " + t);
	}
}
