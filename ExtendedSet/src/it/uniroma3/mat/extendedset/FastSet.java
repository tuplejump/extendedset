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

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;

/**
 * An {@link AbstractExtendedSet} implementation based on {@link BitSet}
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @see ExtendedSet
 * @see AbstractExtendedSet
 * @see ConciseSet
 * @see IndexedSet
 */
public class FastSet extends AbstractExtendedSet<Integer> {
	// plain bitmap representation
	private final BitSet bits;
	
	// set size (only for fast size() call)
	private int size;
	
	/**
	 * Empty integer set
	 */
	public FastSet() {
		bits = new BitSet();
		size = 0;
	}
	
	/**
	 * Creates a bit-string from an existing collection
	 * 
	 * @param c
	 */
	public FastSet(Collection<? extends Integer> c) {
		this();
		addAll(c);
	}

	/**
	 * Cloning constructor
	 * 
	 * @param bits
	 *            bits of the other {@link FastSet} instance. The object will be duplicated.
	 * @param size
	 *            size of the other {@link FastSet} instance
	 */
	private FastSet(BitSet bits, int size) {
		this.bits = (BitSet) bits.clone();
		this.size = size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet clone() {
		return new FastSet(bits, size);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return bits.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		Statistics.increaseEqualsCount();
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final FastSet other = (FastSet) obj;
		if (size != other.size)
			return false;
		if (bits == null) {
			if (other.bits != null)
				return false;
		} else if (!bits.equals(other.bits))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return bits.isEmpty();
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
	public boolean add(Integer i) {
		if (i < 0)
			throw new IndexOutOfBoundsException();
		if (bits.get(i)) 
			return false;

		bits.set(i);
		size++;
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean addAll(Collection<? extends Integer> c) {
		Statistics.increaseUnionCount();
		if (c == null || c.isEmpty())
			return false;

		if (c instanceof FastSet) {
			int sizeBefore = size;
			bits.or(((FastSet) c).bits);
			size = bits.cardinality();
			return sizeBefore != size;
		} 
		
		return super.addAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		bits.clear();
		size = 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean contains(Object o) {
		if (o == null || isEmpty())
			return false;
		return bits.get((Integer) o);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAll(Collection<?> c) {
		if (c == null || c.isEmpty())
			return true;
		if (isEmpty())
			return false;
		if (c.size() > size)
			return false;

		if (c instanceof FastSet) 
			return this.intersectionSize((FastSet) c) == size;
		
		return super.containsAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(Collection<? extends Integer> other) {
		if (other == null || other.isEmpty() || isEmpty())
			return false;
		
		if (other instanceof FastSet) 
			return this.bits.intersects(((FastSet) other).bits);

		return intersectionSize(other) > 0;
	}

	/**
	 * Iterates over bits
	 */
	private class BitIterator implements Iterator<Integer> {
		// current bit
		private Integer curr;
		
		// next bit to poll
		private Integer next;

		/**
		 * Constructor
		 */
		public BitIterator() {
			curr = -1;
			next = bits.nextSetBit(0);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return next >= 0;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Integer next() {
			if (!hasNext())
				throw new NoSuchElementException();
			curr = next;
			next = bits.nextSetBit(curr + 1);
			return curr;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			if (curr < 0 || !bits.get(curr))
				throw new IllegalStateException();
			bits.clear(curr);
		}
	}

	/**
	 * Iterates over bits
	 */
	private class ReverseBitIterator implements Iterator<Integer> {
		// current bit
		private Integer curr;
		
		// next bit to poll
		private Integer next;

		/**
		 * Constructor
		 */
		public ReverseBitIterator() {
			curr = -1;
			next = bits.length() - 1;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasNext() {
			return next >= 0;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Integer next() {
			if (!hasNext())
				throw new NoSuchElementException();

			// NOTE: it searches for the next set bit. There is no faster way to
			// get it, since there is no method similar to
			// bits.nextSetBit(fromIndex) for scanning bits from MSB to LSB.
			curr = next--;
			while (hasNext() && !bits.get(next)) 
				next--;
			return curr;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void remove() {
			if (curr < 0 || !bits.get(curr))
				throw new IllegalStateException();
			bits.clear(curr);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<Integer> iterator() {
		return new BitIterator();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<Integer> descendingIterator() {
		return new ReverseBitIterator();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean remove(Object o) {
		if (o == null || isEmpty())
			return false;
		
		int i = (Integer) o;
		if (this.bits.get(i)) {
			this.bits.clear(i);
			size--;
			return true;
		} 

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean removeAll(Collection<?> c) {
		Statistics.increaseDifferenceCount();
		if (c == null || c.isEmpty() || isEmpty())
			return false;
		
		if (c instanceof FastSet) {
			int sizeBefore = size;
			bits.andNot(((FastSet) c).bits);
			size = bits.cardinality();
			return sizeBefore != size;
		} 
			
		return super.removeAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean retainAll(Collection<?> c) {
		Statistics.increaseIntersectionCount();
		if (c == null || c.isEmpty() || isEmpty())
			return false;
		
		if (c instanceof FastSet) {
			int sizeBefore = size;
			bits.and(((FastSet) c).bits);
			size = bits.cardinality();
			return sizeBefore != size;
		} 
		
		return super.retainAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
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
		return bits.nextSetBit(0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Integer last() {
		if (isEmpty())
			throw new NoSuchElementException();
		return bits.length() - 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complement() {
		final BitSet old = (BitSet) bits.clone();
		final int lastBit = last();
		bits.clear();
		bits.set(0, lastBit);
		bits.xor(old);
		bits.clear(lastBit);
		size = lastBit - size + 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int complementSize() {
		if (isEmpty())
			return 0;
		return bits.length() - size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet getComplement() {
		final FastSet res = this.clone();
		res.complement();
		return res;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet getDifference(Collection<? extends Integer> other) {
		Statistics.increaseDifferenceCount();
		if (other == null)
			return clone();

		FastSet cloned = clone();
		if (other instanceof FastSet)
			cloned.bits.andNot(((FastSet) other).bits);
		else
			cloned.removeAll(other);
		cloned.size = cloned.bits.cardinality();
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet emptySet() {
		return new FastSet();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet getSymmetricDifference(Collection<? extends Integer> other) {
		Statistics.increaseSymmetricDifferenceCount();
		if (other == null)
			return clone();
		
		FastSet cloned = clone();
		if (other instanceof FastSet) {
			cloned.bits.xor(((FastSet) other).bits);
		} else {
			cloned.addAll(other);
			cloned.removeAll(this.getIntersection(other));
		}
		cloned.size = cloned.bits.cardinality();
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet getIntersection(Collection<? extends Integer> other) {
		Statistics.increaseIntersectionCount();
		if (other == null)
			return new FastSet();
		
		FastSet cloned = this.clone();
		if (other instanceof FastSet)
			cloned.bits.and(((FastSet) other).bits);
		else
			cloned.retainAll(other);
		cloned.size = cloned.bits.cardinality();
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FastSet getUnion(Collection<? extends Integer> other) {
		Statistics.increaseUnionCount();
		if (other == null)
			return clone();
		
		FastSet cloned = this.clone();
		if (other instanceof FastSet)
			cloned.bits.or(((FastSet) other).bits);
		else
			cloned.addAll(other);
		cloned.size = cloned.bits.cardinality();
		return cloned;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public double bitmapCompressionRatio() {
		if (isEmpty())
			return 0D;
		return 1D;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public double collectionCompressionRatio() {
		if (isEmpty())
			return 0D;
		return bits.size() / (32D * size);
	}

	/**
	 * Converts a given {@link Collection} instance to a {@link FastSet}
	 * instance
	 * 
	 * @param c
	 *            collection to use to generate the {@link FastSet}
	 *            instance
	 * @return the generated {@link FastSet} instance. <b>NOTE:</b> if
	 *         the parameter is an instance of {@link FastSet}, the
	 *         method returns this instance.
	 * @see #asFastSet(Object[])
	 */
	@SuppressWarnings("unchecked")
	public static FastSet asFastSet(Collection<?> c) {
		if (c == null)
			return new FastSet();
		
		// useless to convert...
		if (c instanceof FastSet)
			return (FastSet) c;

		// try to convert the collection
		FastSet res = new FastSet();
		final Collection<? extends Integer> integerSet = (Collection<? extends Integer>) c;
		res.addAll(integerSet);
		return res;
	}

	/**
	 * Converts a given integer array to a {@link FastSet} instance
	 * 
	 * @param e
	 *            integers to put within the new instance of
	 *            {@link FastSet}
	 * @return new instance of {@link FastSet}
	 * @see #asFastSet(Collection)
	 */
	public static FastSet asFastSet(Object... e) {
		if (e == null)
			return new FastSet();
		if (e.length == 1) {
			FastSet res = new FastSet();
			res.add((Integer) e[0]);
			return res;
		} 
		
		return asFastSet(Arrays.asList(e));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends FastSet> powerSet() {
		return (List<? extends FastSet>) super.powerSet();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends FastSet> powerSet(int min, int max) {
		return (List<? extends FastSet>) super.powerSet(min, max);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fill(Integer from, Integer to) {
		bits.set(from, to);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear(Integer from, Integer to) {
		bits.clear(from, to);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String debugInfo() {
		return String.format("size = %d, elements = %s\nbitmap compression: %.2f%%\ncollection compression: %.2f%%\n", 
				size, bits.toString(), 100D * bitmapCompressionRatio(), 100D * collectionCompressionRatio());
	}
	
	/**
	 * Read-only view of the set
	 * <p>
	 * This class override <i>all</i> public and protected methods of the
	 * parent class {@link FastSet} so that any subclass will be correctly
	 * handled.
	 */
	protected class UnmodifiableFastSet extends FastSet {
		/*
		 * Writing methods
		 */
		/** {@inheritDoc} */ @Override public boolean add(Integer e) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean addAll(Collection<? extends Integer> c) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean addFirstOf(SortedSet<Integer> set) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean addLastOf(SortedSet<Integer> set) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean remove(Object o) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean removeAll(Collection<?> c) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean removeFirstOf(SortedSet<Integer> set) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean removeLastOf(SortedSet<Integer> set) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public boolean retainAll(Collection<?> c) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public void clear() {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public void clear(Integer from, Integer to) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public void fill(Integer from, Integer to) {throw UNSUPPORTED;}
		/** {@inheritDoc} */ @Override public void complement() {throw UNSUPPORTED;}
		
		/** {@inheritDoc} */ @Override
		public Iterator<Integer> iterator() {
			final Iterator<Integer> itr = FastSet.this.iterator();
			return new Iterator<Integer>() {
				@Override public boolean hasNext() {return itr.hasNext();}
				@Override public Integer next() {return itr.next();}
				@Override public void remove() {throw UNSUPPORTED;}
			};
		}

		/*
		 * Read-only methods
		 */
		/** {@inheritDoc} */ @Override public FastSet getIntersection(Collection<? extends Integer> other) {return FastSet.this.getIntersection(other);}
		/** {@inheritDoc} */ @Override public FastSet getDifference(Collection<? extends Integer> other) {return FastSet.this.getDifference(other);}
		/** {@inheritDoc} */ @Override public FastSet getUnion(Collection<? extends Integer> other) {return FastSet.this.getUnion(other);}
		/** {@inheritDoc} */ @Override public FastSet getSymmetricDifference(Collection<? extends Integer> other) {return FastSet.this.getSymmetricDifference(other);}
		/** {@inheritDoc} */ @Override public FastSet getComplement() {return FastSet.this.getComplement();}
		/** {@inheritDoc} */ @Override public FastSet emptySet() {return FastSet.this.emptySet();}
		/** {@inheritDoc} */ @Override public int intersectionSize(Collection<? extends Integer> other) {return FastSet.this.intersectionSize(other);}
		/** {@inheritDoc} */ @Override public int differenceSize(Collection<? extends Integer> other) {return FastSet.this.differenceSize(other);}
		/** {@inheritDoc} */ @Override public int unionSize(Collection<? extends Integer> other) {return FastSet.this.unionSize(other);}
		/** {@inheritDoc} */ @Override public int symmetricDifferenceSize(Collection<? extends Integer> other) {return FastSet.this.symmetricDifferenceSize(other);}
		/** {@inheritDoc} */ @Override public int complementSize() {return FastSet.this.complementSize();}
		/** {@inheritDoc} */ @Override public int powerSetSize() {return FastSet.this.powerSetSize();}
		/** {@inheritDoc} */ @Override public int powerSetSize(int min, int max) {return FastSet.this.powerSetSize(min, max);}
		/** {@inheritDoc} */ @Override public int size() {return FastSet.this.size();}
		/** {@inheritDoc} */ @Override public boolean isEmpty() {return FastSet.this.isEmpty();}
		/** {@inheritDoc} */ @Override public boolean contains(Object o) {return FastSet.this.contains(o);}
		/** {@inheritDoc} */ @Override public boolean containsAll(Collection<?> c) {return FastSet.this.containsAll(c);}
		/** {@inheritDoc} */ @Override public boolean containsAny(Collection<? extends Integer> other) {return FastSet.this.containsAny(other);}
		/** {@inheritDoc} */ @Override public boolean containsAtLeast(Collection<? extends Integer> other, int minElements) {return FastSet.this.containsAtLeast(other, minElements);}
		/** {@inheritDoc} */ @Override public Integer first() {return FastSet.this.first();}
		/** {@inheritDoc} */ @Override public Integer last() {return FastSet.this.last();}
		/** {@inheritDoc} */ @Override public Comparator<? super Integer> comparator() {return FastSet.this.comparator();}
		/** {@inheritDoc} */ @Override public int compareTo(ExtendedSet<Integer> o) {return FastSet.this.compareTo(o);}
		/** {@inheritDoc} */ @Override public boolean equals(Object o) {return FastSet.this.equals(o);}
		/** {@inheritDoc} */ @Override public int hashCode() {return FastSet.this.hashCode();}
		/** {@inheritDoc} */ @Override public Iterable<Integer> descending() {return FastSet.this.descending();}
		/** {@inheritDoc} */ @Override public Iterator<Integer> descendingIterator() {return FastSet.this.descendingIterator();}
		/** {@inheritDoc} */ @Override public List<? extends FastSet> powerSet() {return FastSet.this.powerSet();}
		/** {@inheritDoc} */ @Override public List<? extends FastSet> powerSet(int min, int max) {return FastSet.this.powerSet(min, max);}
		/** {@inheritDoc} */ @Override public double bitmapCompressionRatio() {return FastSet.this.bitmapCompressionRatio();}
		/** {@inheritDoc} */ @Override public double collectionCompressionRatio() {return FastSet.this.collectionCompressionRatio();}
		/** {@inheritDoc} */ @Override public String debugInfo() {return FastSet.this.debugInfo();}
		/** {@inheritDoc} */ @Override public Object[] toArray() {return FastSet.this.toArray();}
		/** {@inheritDoc} */ @Override public <X> X[] toArray(X[] a) {return FastSet.this.toArray(a);}
		/** {@inheritDoc} */ @Override public String toString() {return FastSet.this.toString();}

		/*
		 * Special purpose methods
		 */
		/* NOTE: the following methods do not have to be overridden:
		 * - public FastSet headSet(T toElement) {}
		 * - public FastSet subSet(T fromElement, T toElement) {}
		 * - public FastSet tailSet(T fromElement) {
		 * In this way, modification to the subview will not be permitted
		 */
		/** {@inheritDoc} */ @Override 
		public FastSet clone() {
			// useless to clone
			return this; 
		}
		/** {@inheritDoc} */ @Override 
		public FastSet unmodifiable() {
			// useless to create another instance
			return this;
		}
	}
	
	/**
	 * @return the read-only version of the current set
	 */
	@Override
	public FastSet unmodifiable() {
		return new UnmodifiableFastSet();
	}
}
