/* $Id:$
 * 
 * (c) 2010 Alessandro Colantonio
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

package it.uniroma3.mat.colantonio.extendedset;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Map each bit position with an integer greater or equal to zero
 * 
 * @author Alessandro Colantonio
 * @author <a href="mailto:colanton@mat.uniroma3.it">colanton@mat.uniroma3.it</a>
 * @author <a href="http://ricerca.mat.uniroma3.it/users/colanton">http://ricerca.mat.uniroma3.it/users/colanton</a>
 * 
 * @version 1.0
 * 
 * @see ExtendedSet
 * @see ConciseSet
 * @see IndexedSet
 */
public class FastSet extends ExtendedSet<Integer> {
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
	 * Create a bit-string from an existing collection
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
	public int compareTo(ExtendedSet<Integer> o) {
		//TODO: controllare che funzioni...
		Iterator<Integer> thisIterator = this.descendingIterator();
		Iterator<Integer> otherIterator = o.descendingIterator();
		while (thisIterator.hasNext() && otherIterator.hasNext()) {
			Integer thisItem = thisIterator.next();
			Integer otherItem = otherIterator.next();
			int res = thisItem.compareTo(otherItem);
			if (res != 0)
				return res;
		}
		return thisIterator.hasNext() ? 1 : (otherIterator.hasNext() ? -1 : 0);
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

		if (c instanceof FastSet) 
			return this.intersectionSize((FastSet) c) == size;
		
		return super.containsAll(c);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsAny(ExtendedSet<Integer> other) {
		if (other == null || other.isEmpty() || isEmpty())
			return false;
		
		if (other instanceof FastSet) 
			return this.bits.intersects(((FastSet) other).bits);

		return intersectionSize(other) > 0;
	}

	/**
	 * Iterate over set bits
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
	public FastSet getDifference(ExtendedSet<Integer> other) {
		Statistics.increaseDifferenceCount();
		FastSet cloned = this.clone();
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
	public FastSet getSymmetricDifference(ExtendedSet<Integer> other) {
		Statistics.increaseSymmetricDifferenceCount();
		FastSet cloned = this.clone();
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
	public FastSet getIntersection(ExtendedSet<Integer> other) {
		Statistics.increaseIntersectionCount();
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
	public FastSet getUnion(ExtendedSet<Integer> other) {
		Statistics.increaseUnionCount();
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
	public int intersectionSize(ExtendedSet<Integer> other) {
		Statistics.increaseSizeCheckCount();
		return getIntersection(other).size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int unionSize(ExtendedSet<Integer> other) {
		Statistics.increaseSizeCheckCount();
		return getUnion(other).size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int symmetricDifferenceSize(ExtendedSet<Integer> other) {
		Statistics.increaseSizeCheckCount();
		return getSymmetricDifference(other).size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int differenceSize(ExtendedSet<Integer> other) {
		Statistics.increaseSizeCheckCount();
		return getDifference(other).size;
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
	 * Convert a given {@link Collection} instance to a {@link FastSet}
	 * instance
	 * 
	 * @param c
	 *            collection to use to generate the {@link FastSet}
	 *            instance
	 * @return the generated {@link FastSet} instance. <b>NOTE:</b> if
	 *         the parameter is an instance of {@link FastSet}, the
	 *         method returns this instance.
	 * @see #asFastIntegerSet(Object[])
	 */
	@SuppressWarnings("unchecked")
	public static FastSet asFastIntegerSet(Collection<?> c) {
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
	 * Convert a given integer element to a {@link FastSet} instance
	 * 
	 * @param e
	 *            integer to put within the new instance of
	 *            {@link FastSet}
	 * @return new instance of {@link FastSet}
	 * @see #asFastIntegerSet(Collection)
	 */
	public static FastSet asFastIntegerSet(Object... e) {
		if (e.length == 1) {
			FastSet res = new FastSet();
			res.add((Integer) e[0]);
			return res;
		} 
		
		return asFastIntegerSet(Arrays.asList(e));
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
	 * Check if a {@link FastSet} and a {@link TreeSet}
	 * contains the same elements (debug only)
	 * 
	 * @param b
	 *            bit-set to check
	 * @param c
	 *            collection that must contain the same elements of the bit-set
	 * @return <code>true</code> if the given {@link FastSet} and
	 *         {@link TreeSet} are equals in terms of contained elements
	 */
	private static boolean checkContent(FastSet b, TreeSet<Integer> c) {
		if (b.size() != c.size())
			return false;
		if (b.isEmpty())
			return true;
		for (Integer i : b) 
			if (!c.contains(i)) 
				return false;
		return b.first().equals(c.first())
				&& b.last().equals(c.last());
	}

	/**
	 * Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// append test
		if (false) {
			FastSet bitSet = new FastSet();
			TreeSet<Integer> elements = new TreeSet<Integer>();

			System.out.println(bitSet);

			// elements to put
			elements.add(1000);
			elements.add(1001);
			elements.add(1023);
			elements.add(2000);
			elements.add(2046);
			for (int i = 0; i < 62; i++) {
				elements.add(2048 + i);
			}
			elements.add(2158);
			elements.add(1000000);

			// put elements
			for (Integer i : elements) {
				System.out.println("Appending " + i);
				bitSet.add(i);
				System.out.println(bitSet);
			}
			
			return;
		}

		// random append test
		if (false) {
			FastSet bitSet = new FastSet();
			TreeSet<Integer> elements = new TreeSet<Integer>();

			Random rnd = new Random();

			bitSet.clear();
			elements.clear();
			System.out.println("Orig el.: " + elements);
			for (int i = 0; i < 10000; i++)
				elements.add(rnd.nextInt(10000000 + 1));
			for (Integer i : elements)
				bitSet.add(i);
			System.out.println("Correct: " + checkContent(bitSet, elements));
			System.out.println("Orig el.: " + elements);
			System.out.println(bitSet);

			return;
		}

		// simple intersection
		if (false) {
			System.out.println("First set");
			FastSet bitSet1 = new FastSet();
			TreeSet<Integer> elements1 = new TreeSet<Integer>();
			elements1.add(1);
			elements1.add(2);
			elements1.add(3);
			elements1.add(100);
			elements1.add(1000);
			for (Integer i : elements1)
				bitSet1.add(i);
			System.out.println("Correct: " + checkContent(bitSet1, elements1));
			System.out.println("Orig el.: " + elements1);
			System.out.println(bitSet1);

			System.out.println("Second set");
			FastSet bitSet2 = new FastSet();
			TreeSet<Integer> elements2 = new TreeSet<Integer>();
			elements2.add(100);
			elements2.add(101);
			for (Integer i : elements2)
				bitSet2.add(i);
			System.out.println("Correct: " + checkContent(bitSet2, elements2));
			System.out.println("Orig el.: " + elements2);
			System.out.println(bitSet2);

			System.out.println("Intersection");
			FastSet bitSet3 = bitSet1.getIntersection(bitSet2);
			TreeSet<Integer> elements3 = new TreeSet<Integer>(elements1);
			elements3.retainAll(elements2);
			System.out.println("Correct: " + checkContent(bitSet3, elements3));
			System.out.println("Orig el.: " + elements3);
			System.out.println(bitSet3);
			
			return;
		}

		// more complex intersection
		if (false) {
			Random rnd = new Random();

			System.out.println("First set");
			FastSet bitSet1 = new FastSet();
			TreeSet<Integer> elements1 = new TreeSet<Integer>();
			for (int i = 0; i < 30; i++)
				elements1.add(rnd.nextInt(1000 + 1));
			for (int i = 0; i < 1000; i++)
				elements1.add(rnd.nextInt(200 + 1));
			bitSet1.addAll(elements1);
			System.out.println("Correct: " + checkContent(bitSet1, elements1));
			System.out.println("Orig el.: " + elements1);
			System.out.println(bitSet1);

			System.out.println("Second set");
			FastSet bitSet2 = new FastSet();
			TreeSet<Integer> elements2 = new TreeSet<Integer>();
			for (int i = 0; i < 30; i++)
				elements2.add(rnd.nextInt(1000 + 1));
			for (int i = 0; i < 1000; i++)
				elements2.add(150 + rnd.nextInt(300 - 150 + 1));
			bitSet2.addAll(elements2);
			System.out.println("Correct: " + checkContent(bitSet2, elements2));
			System.out.println("Orig el.: " + elements2);
			System.out.println(bitSet2);

			System.out.println("Intersection");
			FastSet bitSet3 = bitSet1.getIntersection(bitSet2);
			TreeSet<Integer> elements3 = new TreeSet<Integer>(elements1);
			elements3.retainAll(elements2);
			System.out.println("Correct: " + checkContent(bitSet3, elements3));
			System.out.println("Orig el.: " + elements3);
			System.out.println(bitSet3);
			
			System.out.print("Intersection size: ");
			System.out.println(elements3.size() == bitSet1.intersectionSize(bitSet2));
			
			return;
		}

		// union test
		if (false) {
			System.out.println("First set");
			FastSet bitSet1 = new FastSet();
			TreeSet<Integer> elements1 = new TreeSet<Integer>();
			elements1.add(1);
			elements1.add(2);
			elements1.add(30000);
			for (Integer i : elements1)
				bitSet1.add(i);
			System.out.println("Correct: " + checkContent(bitSet1, elements1));
			System.out.println("Orig el.: " + elements1);
			System.out.println(bitSet1);

			System.out.println("Second set");
			FastSet bitSet2 = new FastSet();
			TreeSet<Integer> elements2 = new TreeSet<Integer>();
			elements2.add(100);
			elements2.add(101);
			elements2.add(1000000);
			for (int i = 0; i < 62; i++)
				elements2.add(341 + i);
			for (Integer i : elements2) 
				bitSet2.add(i);
			System.out.println("Correct: " + checkContent(bitSet2, elements2));
			System.out.println("Orig el.: " + elements2);
			System.out.println(bitSet2);

			System.out.println("Union");
			FastSet bitSet3 = bitSet1.getUnion(bitSet2);
			TreeSet<Integer> elements3 = new TreeSet<Integer>(elements1);
			elements3.addAll(elements2);
			System.out.println("Correct: " + checkContent(bitSet3, elements3));
			System.out.println("Orig el.: " + elements3);
			System.out.println(bitSet3);
			
			System.out.print("Union size: ");
			System.out.println(elements3.size() == bitSet1.unionSize(bitSet2));

			return;
		}

		// complement test
		if (false) {
			System.out.println("Original");
			FastSet bitSet1 = new FastSet();
			bitSet1.add(1);
			bitSet1.add(2);
			bitSet1.add(30000);
			System.out.println(bitSet1);

			System.out.print("Complement size: ");
			System.out.println(bitSet1.complementSize());
			System.out.println();
			
			System.out.println("Complement");
			bitSet1 = bitSet1.getComplement();
			System.out.println(bitSet1);

			System.out.print("Complement size: ");
			System.out.println(bitSet1.complementSize());
			System.out.println();
			
			System.out.println("Complement");
			bitSet1 = bitSet1.getComplement();
			System.out.println(bitSet1);

			System.out.print("Complement size: ");
			System.out.println(bitSet1.complementSize());
			System.out.println();
			
			System.out.println("Complement");
			bitSet1 = bitSet1.getComplement();
			System.out.println(bitSet1);

			System.out.print("Complement size: ");
			System.out.println(bitSet1.complementSize());
			System.out.println();
			
			System.out.println("Complement");
			bitSet1 = bitSet1.getComplement();
			System.out.println(bitSet1);

			return;
		}

		// mixed stuff
		if (false) {
			boolean debugInfo = true;

			FastSet bitSet1 = new FastSet();
			bitSet1.add(1);
			bitSet1.add(100);
			bitSet1.add(2);
			bitSet1.add(3);
			bitSet1.add(2);
			bitSet1.add(100);
			System.out.println("A: " + bitSet1);
			if (debugInfo)
				System.out.println(bitSet1);

			FastSet bitSet2 = new FastSet();
			bitSet2.add(1);
			bitSet2.add(1000000);
			bitSet2.add(2);
			bitSet2.add(30000);
			bitSet2.add(1000000);
			System.out.println("B: " + bitSet2);
			if (debugInfo)
				System.out.println(bitSet2);

			System.out.println("A.getSymmetricDifference(B): "
					+ bitSet1.getSymmetricDifference(bitSet2));
			if (debugInfo)
				System.out.println(bitSet1.getSymmetricDifference(bitSet2)
						);

			System.out.println("A.getComplement(): " + bitSet1.getComplement());
			if (debugInfo)
				System.out.println(bitSet1.getComplement());

			bitSet1.removeAll(bitSet2);
			System.out.println("A.removeAll(B): " + bitSet1);
			if (debugInfo)
				System.out.println(bitSet1);

			bitSet1.addAll(bitSet2);
			System.out.println("A.addAll(B): " + bitSet1);
			if (debugInfo)
				System.out.println(bitSet1);

			bitSet1.retainAll(bitSet2);
			System.out.println("A.retainAll(B): " + bitSet1);
			if (debugInfo)
				System.out.println(bitSet1);

			bitSet1.remove(1);
			System.out.println("B.remove(1): " + bitSet1);
			if (debugInfo)
				System.out.println(bitSet1);
			
			return;
		}
		
		/*
		 * Addition stress test. 
		 * It starts from a very sparse set (most of the words will be 0's 
		 * sequences) and progressively become very dense (words first
		 * become 0's sequences with 1 set bit and there will be almost one 
		 * word per item, then words become literals, and finally they 
		 * become 1's sequences and drastically reduce in number)
		 */
		if (false) {
			FastSet previousBitSet = new FastSet();
			FastSet bitSet = new FastSet();
			FastSet singleBitSet = new FastSet();
			FastSet otherBitSet = null;
			TreeSet<Integer> elements = new TreeSet<Integer>();

			Random rnd = new Random(System.currentTimeMillis());

			for (int j = 0; j < 100000; j++) {
				int item = rnd.nextInt(10000 + 1);
				
				System.out.println("Adding " + item);
				boolean contBeforeEl = elements.contains(item);
				boolean addedEl = elements.add(item);
				boolean contAfterEl = elements.contains(item);
				
				previousBitSet = bitSet;
				bitSet = bitSet.clone();

				boolean contBeforeBit = bitSet.contains(item);
				boolean addedBit = bitSet.add(item);
				boolean contAfterBit = bitSet.contains(item);
				if (addedEl ^ addedBit) {
					System.out.println("Flag add error!");
					return;
				}
				if (contBeforeEl ^ contBeforeBit) {
					System.out.println("Flag cont before error!");
					return;
				}
				if (contAfterEl ^ contAfterBit) {
					System.out.println("Flag cont after error!");
					return;
				}

				// check the list of elements
				boolean correct = checkContent(bitSet, elements);
				if (!correct) {
					System.out.println("Union not correct!");
					System.out.println("Same elements: " + (elements.toString().equals(bitSet.toString())));
					System.out.println("Orig el.: " + elements);
					System.out.println(bitSet);
					System.out.println(previousBitSet);
					
					return;
				}
				
				// check the representation
				otherBitSet = new FastSet();
				otherBitSet.addAll(elements);
				correct = otherBitSet.hashCode() == bitSet.hashCode();
				if (!correct) {
					System.out.println("Representation not correct!");
					System.out.println(bitSet);
					System.out.println(otherBitSet);
					System.out.println(previousBitSet);

					return;
				}

				// check the union size
				singleBitSet.clear();
				singleBitSet.add(item);
				correct = elements.size() == bitSet.unionSize(singleBitSet);
				if (!correct) {
					System.out.println("Union size not correct");
					System.out.println("Orig el.: " + elements);
					System.out.println(bitSet);
					System.out.println(previousBitSet);

					return;
				}
			}

			System.out.println(bitSet);
			
			return;
		}

		/*
		 * Removal stress test. 
		 * It starts from a very dense set (most of the words will be 1's 
		 * sequences) and progressively become very sparse (words first
		 * become 1's sequences with 1 unset bit and there will be few 
		 * words per item, then words become literals, and finally they 
		 * become 0's sequences and drastically reduce in number)
		 */
		if (false) {
			FastSet previousBitSet = new FastSet();
			FastSet bitSet = new FastSet();
			FastSet singleBitSet = new FastSet();
			FastSet otherBitSet = null;
			TreeSet<Integer> elements = new TreeSet<Integer>();

			Random rnd = new Random(System.currentTimeMillis());

			// create a 1-filled bitset
			bitSet.add(10001);
			bitSet.complement();
			elements.addAll(bitSet);
			if (elements.size() != 10001) {
				System.out.println("Unexpected error!");
			}
			
			for (int j = 0; j < 100000 & !bitSet.isEmpty(); j++) {
				int item = rnd.nextInt(10000 + 1);
				
				System.out.println("Removing " + item);
				boolean contBeforeEl = elements.contains(item);
				boolean removedEl = elements.remove(item);
				boolean contAfterEl = elements.contains(item);
				
				previousBitSet = bitSet;
				bitSet = bitSet.clone();

				boolean contBeforeBit = bitSet.contains(item);
				boolean removedBit = bitSet.remove(item);
				boolean contAfterBit = bitSet.contains(item);
				if (removedEl ^ removedBit) {
					System.out.println("Flag add error!");
					return;
				}
				if (contBeforeEl ^ contBeforeBit) {
					System.out.println("Flag cont before error!");
					return;
				}
				if (contAfterEl ^ contAfterBit) {
					System.out.println("Flag cont after error!");
					return;
				}

				// check the list of elements
				boolean correct = checkContent(bitSet, elements);
				if (!correct) {
					System.out.println("Difference not correct!");
					System.out.println("Same elements: " + (elements.toString().equals(bitSet.toString())));
					System.out.println("Orig el.: " + elements);
					System.out.println(bitSet);
					System.out.println(previousBitSet);
					
					return;
				}
				
				// check the representation
				otherBitSet = new FastSet();
				otherBitSet.addAll(elements);
				correct = otherBitSet.hashCode() == bitSet.hashCode();
				if (!correct) {
					System.out.println("Representation not correct!");
					System.out.println(bitSet);
					System.out.println(otherBitSet);
					System.out.println(previousBitSet);

					return;
				}

				// check the union size
				singleBitSet.clear();
				singleBitSet.add(item);
				correct = elements.size() == bitSet.differenceSize(singleBitSet);
				if (!correct) {
					System.out.println("Difference size not correct");
					System.out.println("Orig el.: " + elements);
					System.out.println(bitSet);
					System.out.println(previousBitSet);

					return;
				}
			}

			System.out.println(bitSet);
			System.out.println();

			return;
		}

		/*
		 * Subset stress test (addition)
		 */
		if (false) {
			TreeSet<Integer> elements = new TreeSet<Integer>();
			FastSet previousBitSet = new FastSet();
			FastSet bitSet = new FastSet();
			FastSet otherBitSet = null;

			Random rnd = new Random(System.currentTimeMillis());

			for (int j = 0; j < 100000; j++) {
				int min = rnd.nextInt(10000);
				int max = min + 1 + rnd.nextInt(10000 - (min + 1) + 1);
				int item = min + rnd.nextInt(max - 1 - min + 1);
				
				System.out.println("Adding " + item + " to the subview from " + min + " to " + max + " - 1");
				previousBitSet = bitSet;
				bitSet = bitSet.clone();
				bitSet.subSet(min, max).add(item);
				elements.subSet(min, max).add(item);
				
				boolean correct = checkContent(bitSet, elements);
				if (!correct) {
					System.out.println("Subview not correct!");
					System.out.println("Same elements: " + (elements.toString().equals(bitSet.toString())));
					System.out.println("Orig el.: " + elements);
					System.out.println(bitSet);
					System.out.println(previousBitSet);
					
					return;
				}
				
				// check the representation
				otherBitSet = new FastSet();
				otherBitSet.addAll(elements);
				correct = otherBitSet.hashCode() == bitSet.hashCode();
				if (!correct) {
					System.out.println("Representation not correct!");
					System.out.println(bitSet);
					System.out.println(otherBitSet);
					System.out.println(previousBitSet);

					return;
				}
			}

			System.out.println(bitSet);

			return;
		}

		/*
		 * Subset stress test (removal)
		 */
		if (true) {
			TreeSet<Integer> elements = new TreeSet<Integer>();
			FastSet previousBitSet = new FastSet();
			FastSet bitSet = new FastSet();
			FastSet otherBitSet = null;

			// create a 1-filled bitset
			bitSet.add(10001);
			bitSet.complement();
			elements.addAll(bitSet);
			if (elements.size() != 10001) {
				System.out.println("Unexpected error!");
			}

			Random rnd = new Random(System.currentTimeMillis());

			for (int j = 0; j < 100000; j++) {
				int min = rnd.nextInt(10000);
				int max = min + 1 + rnd.nextInt(10000 - (min + 1) + 1);
				int item = rnd.nextInt(10000 + 1);
				
				System.out.println("Removing " + item + " from the subview from " + min + " to " + max + " - 1");
				previousBitSet = bitSet;
				bitSet = bitSet.clone();
				SortedSet<Integer> s1 = bitSet.subSet(min, max);
				SortedSet<Integer> s2 = elements.subSet(min, max);
				boolean r1 = s1.remove(item);
				boolean r2 = s2.remove(item);

				if (r1 != r2 || s1.size() != s2.size() || !s1.toString().equals(s2.toString())) {
					System.out.println("Errore nei sottoinsiemi!");
					return;
				}
				
				boolean correct = checkContent(bitSet, elements);
				if (!correct) {
					System.out.println("Subview not correct!");
					System.out.println("Same elements: " + (elements.toString().equals(bitSet.toString())));
					System.out.println("Orig el.: " + elements);
					System.out.println(bitSet);
					System.out.println(previousBitSet);
					
					return;
				}
				
				// check the representation
				otherBitSet = new FastSet();
				otherBitSet.addAll(elements);
				correct = otherBitSet.hashCode() == bitSet.hashCode();
				if (!correct) {
					System.out.println("Representation not correct!");
					System.out.println(bitSet);
					System.out.println(otherBitSet);
					System.out.println(previousBitSet);

					return;
				}
			}

			System.out.println(bitSet);

			return;
		}
	}
}
