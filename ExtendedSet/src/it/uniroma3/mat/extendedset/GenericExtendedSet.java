package it.uniroma3.mat.extendedset;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
	 * Set of all integers &gt;= 0
	 */
	public static final Collection<Integer> ALL_POSITIVE_INTEGERS = new AbstractCollection<Integer>() {
		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				private Integer curr = 0;
				
				@Override
				public boolean hasNext() {
					return curr != null;
				}

				@Override
				public Integer next() {
					Integer prev = curr;
					if (curr < Integer.MAX_VALUE)
						curr++;
					else
						curr = null;
					return prev;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		/** {@inheritDoc} */
		@Override
		public int size() {
			return Integer.MAX_VALUE;
		}
	};

	/**
	 * Empty-set constructor
	 * 
	 * @param setClass
	 *            {@link Set}-derived class
	 * @param universe
	 *            all possible elements. If <code>null</code>,
	 *            {@link #complement()} will throw
	 *            {@link UnsupportedOperationException}.
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
		if (isEmpty())
			return 0D;
		return size() / Math.ceil(size() / 32D);
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
	 * Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
//		GenericExtendedSet<Integer> s = new GenericExtendedSet<Integer>(HashSet.class, ALL_POSITIVE_INTEGERS);
		GenericExtendedSet<Integer> s = new GenericExtendedSet<Integer>(TreeSet.class, ALL_POSITIVE_INTEGERS);
		s.add(4);
		s.add(40);
		s.add(3);
		s.add(1);
		s.add(11000);
		System.out.println(s);
		
		GenericExtendedSet<Integer> t = s.empty();

		t.add(2);
		t.add(4);
		t.add(3);
		t.add(10);
		t.add(11);
		t.add(20);
		System.out.println(t);
		
		System.out.println(t.intersection(s));
		
		System.out.println(s.debugInfo());
		
		System.out.println(t.complemented());
		System.out.println(t.complemented().complemented());
	}
}
