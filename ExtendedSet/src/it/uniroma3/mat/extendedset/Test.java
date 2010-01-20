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

import it.uniroma3.mat.extendedset.ExtendedSet.Statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Test class for {@link ConciseSet}, {@link FastSet}, and {@link IndexedSet}.
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 */
public class Test {
	/**
	 * Checks if a {@link ExtendedSet} instance and a {@link TreeSet} instance
	 * contains the same elements. {@link TreeSet} is used because it is the
	 * most similar class to {@link ExtendedSet}.
	 * 
	 * @param <T>
	 *            type of elements within the set
	 * @param bits
	 *            bit-set to check
	 * @param items
	 *            {@link TreeSet} instance that must contain the same elements
	 *            of the bit-set
	 * @return <code>true</code> if the given {@link ConciseSet} and
	 *         {@link TreeSet} are equals in terms of contained elements
	 */
	private static <T> boolean checkContent(ExtendedSet<T> bits, TreeSet<T> items) {
		if (bits.size() != items.size())
			return false;
		if (bits.isEmpty() && items.isEmpty())
			return true;
		for (T i : bits) 
			if (!items.contains(i)) 
				return false;
		for (T i : items) 
			if (!bits.contains(i)) 
				return false;
		return bits.last().equals(items.last());
	}
	
	/**
	 * Simple append test 
	 * <p>
	 * It appends sequential numbers, thus generating 2 blocks of 1's
	 * 
	 * @param c class to test
	 */
	@SuppressWarnings("unchecked")
	private static void testForAppendSimple(Class<? extends ExtendedSet> c) {
		ExtendedSet<Integer> bits;
		try {
			bits = c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		for (int i = 0; i < 62; i++) {
			System.out.format("Appending %d...\n", i);
			bits.add(i);
			System.out.println(bits.debugInfo());
		}
	}
	
	/**
	 * Another append test
	 * <p>
	 * Different from {@link #testForAppendSimple()}, it tests some particular
	 * cases
	 * 
	 * @param c class to test
	 */
	@SuppressWarnings("unchecked")
	private static void testForAppendComplex(Class<? extends ExtendedSet> c) {
		ExtendedSet<Integer> bits;
		try {
			bits = c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		TreeSet<Integer> items = new TreeSet<Integer>();

		// elements to append
		items.add(1000);
		items.add(1001);
		items.add(1023);
		items.add(2000);
		items.add(2046);
		for (int i = 0; i < 62; i++) 
			items.add(2048 + i);
		items.add(2158);
		items.add(ConciseSet.MAX_ALLOWED_SET_BIT);

		// append elements
		for (Integer i : items) {
			System.out.format("Appending %d...\n", i);
			try {
				bits.add(i);
			} catch (OutOfMemoryError e) {
				// it will happen with FastSet when adding
				// ConciseSet.MAX_ALLOWED_SET_BIT
				System.out.println("out of memory!");
				return;
			}
			System.out.println(bits.debugInfo());
		}
		
		// check the result
		if (checkContent(bits, items)) {
			System.out.println("OK!");
		} else {
			System.out.println("ERRORS!");
		}
	}
	
	/**
	 * Random append test
	 * <p>
	 * It adds randomly generated numbers
	 */
	@SuppressWarnings("unchecked")
	private static void testForAppendRandom() {
		ConciseSet bits = new ConciseSet();
		TreeSet<Integer> items = new TreeSet<Integer>();

		// random number generator
		Random rnd = new Random();

		bits.clear();
		items.clear();
		System.out.println("SPARSE ITEMS");
		for (int i = 0; i < 10000; i++)
			items.add(rnd.nextInt(1000000000));
		for (Integer i : items)
			bits.add(i);
		System.out.println("Correct: " + checkContent(bits, items));
		System.out.println("Original items: " + items);
		System.out.println(bits.debugInfo());

		bits.clear();
		items.clear();
		System.out.println("DENSE ITEMS");
		for (int i = 0; i < 10000; i++)
			items.add(rnd.nextInt(10000 + 1));
		for (Integer i : items)
			bits.add(i);
		System.out.println("Correct: " + checkContent(bits, items));
		System.out.println("Original items: " + items);
		System.out.println(bits.debugInfo());

		bits.clear();
		items.clear();
		System.out.println("MORE DENSE ITEMS");
		for (int i = 0; i < 2000; i++)
			items.add(rnd.nextInt(310 + 1));
		for (int i = 0; i < 2000; i++)
			items.add(714 + rnd.nextInt(805 - 714 + 1));
		for (int i = 0; i < 2000; i++)
			items.add(850 + rnd.nextInt(900 - 850 + 1));
		for (int i = 0; i < 4000; i++)
			items.add(700 + rnd.nextInt(100000 - 700 + 1));
		for (Integer e : items)
			bits.add(e);
		System.out.println("Correct: " + checkContent(bits, items));
		System.out.println("Original items: " + items);
		System.out.println(bits.debugInfo());
	}
	
	/**
	 * Simple test for the method {@link ExtendedSet#getIntersection(ExtendedSet)}
	 * 
	 * @param c class to test
	 */
	@SuppressWarnings("unchecked")
	private static void testForIntersectionSimple(Class<? extends ExtendedSet> c) {
		System.out.println("FIRST SET");
		ExtendedSet<Integer> bitsLeft;
		ExtendedSet<Integer> bitsRight;
		try {
			bitsLeft = c.newInstance();
			bitsRight = c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		TreeSet<Integer> itemsLeft = new TreeSet<Integer>();
		TreeSet<Integer> itemsRight = new TreeSet<Integer>();
		itemsLeft.add(1);
		itemsLeft.add(2);
		itemsLeft.add(3);
		itemsLeft.add(100);
		itemsLeft.add(1000);
		for (Integer i : itemsLeft)
			bitsLeft.add(i);
		System.out.println("Correct: " + checkContent(bitsLeft, itemsLeft));
		System.out.println("Original items: " + itemsLeft);
		System.out.println(bitsLeft.debugInfo());

		System.out.println("SECOND SET");
		itemsRight.add(100);
		itemsRight.add(101);
		for (Integer i : itemsRight)
			bitsRight.add(i);
		System.out.println("Correct: " + checkContent(bitsRight, itemsRight));
		System.out.println("Original items: " + itemsRight);
		System.out.println(bitsRight.debugInfo());

		System.out.println("INTERSECTION SET");
		ExtendedSet<Integer> bitsIntersection = bitsLeft.getIntersection(bitsRight);
		TreeSet<Integer> itemsIntersection = new TreeSet<Integer>(itemsLeft);
		itemsIntersection.retainAll(itemsRight);
		System.out.println("Correct: " + checkContent(bitsIntersection, itemsIntersection));
		System.out.println("Original items: " + itemsIntersection);
		System.out.println(bitsIntersection.debugInfo());
	}
	
	/**
	 * More complex test for the methods
	 * {@link ExtendedSet#getIntersection(ExtendedSet)} and
	 * {@link ExtendedSet#intersectionSize(ExtendedSet)}
	 * 
	 * @param c class to test
	 */
	@SuppressWarnings("unchecked")
	private static void testForIntersectionComplex(Class<? extends ExtendedSet> c) {
		// generate items to intersect completely at random
		Random rnd = new Random();

		ExtendedSet<Integer> bitsLeft;
		ExtendedSet<Integer> bitsRight;
		try {
			bitsLeft = c.newInstance();
			bitsRight = c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		TreeSet<Integer> itemsLeft = new TreeSet<Integer>();
		TreeSet<Integer> itemsRight = new TreeSet<Integer>();

		System.out.println("FIRST SET");
		for (int i = 0; i < 30; i++)
			itemsLeft.add(rnd.nextInt(1000 + 1));
		for (int i = 0; i < 1000; i++)
			itemsLeft.add(rnd.nextInt(200 + 1));
		bitsLeft.addAll(itemsLeft);
		System.out.println("Correct: " + checkContent(bitsLeft, itemsLeft));
		System.out.println("Original items: " + itemsLeft);
		System.out.println(bitsLeft.debugInfo());

		System.out.println("SECOND SET");
		for (int i = 0; i < 30; i++)
			itemsRight.add(rnd.nextInt(1000 + 1));
		for (int i = 0; i < 1000; i++)
			itemsRight.add(150 + rnd.nextInt(300 - 150 + 1));
		bitsRight.addAll(itemsRight);
		System.out.println("Correct: " + checkContent(bitsRight, itemsRight));
		System.out.println("Original items: " + itemsRight);
		System.out.println(bitsRight.debugInfo());

		System.out.println("INTERSECTION SET");
		ExtendedSet<Integer> bitsIntersection = bitsLeft.getIntersection(bitsRight);
		TreeSet<Integer> itemsIntersection = new TreeSet<Integer>(itemsLeft);
		itemsIntersection.retainAll(itemsRight);
		System.out.println("Correct: " + checkContent(bitsIntersection, itemsIntersection));
		System.out.println("Original items: " + itemsIntersection);
		System.out.println(bitsIntersection.debugInfo());
		
		System.out.println("INTERSECTION SIZE");
		System.out.println("Correct: " + (itemsIntersection.size() == bitsLeft.intersectionSize(bitsRight)));	
	}
	
	/**
	 * Simple test for the method {@link ExtendedSet#getUnion(ExtendedSet)}
	 * 
	 * @param c class to test
	 */
	@SuppressWarnings("unchecked")
	private static void testForUnionSimple(Class<? extends ExtendedSet> c) {
		ExtendedSet<Integer> bitsLeft;
		ExtendedSet<Integer> bitsRight;
		try {
			bitsLeft = c.newInstance();
			bitsRight = c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		TreeSet<Integer> itemsLeft = new TreeSet<Integer>();
		TreeSet<Integer> itemsRight = new TreeSet<Integer>();

		System.out.println("FIRST SET");
		itemsLeft.add(1);
		itemsLeft.add(2);
		itemsLeft.add(30000);
		for (Integer i : itemsLeft)
			bitsLeft.add(i);
		System.out.println("Correct: " + checkContent(bitsLeft, itemsLeft));
		System.out.println("Original items: " + itemsLeft);
		System.out.println(bitsLeft.debugInfo());

		System.out.println("SECOND SET");
		itemsRight.add(100);
		itemsRight.add(101);
		itemsRight.add(100000000);
		for (int i = 0; i < 62; i++)
			itemsRight.add(341 + i);
		for (Integer i : itemsRight) 
			bitsRight.add(i);
		System.out.println("Correct: " + checkContent(bitsRight, itemsRight));
		System.out.println("Original items: " + itemsRight);
		System.out.println(bitsRight.debugInfo());

		System.out.println("UNION SET");
		ExtendedSet<Integer> bitsUnion = bitsLeft.getUnion(bitsRight);
		TreeSet<Integer> itemsUnion = new TreeSet<Integer>(itemsLeft);
		itemsUnion.addAll(itemsRight);
		System.out.println("Correct: " + checkContent(bitsUnion, itemsUnion));
		System.out.println("Original items: " + itemsUnion);
		System.out.println(bitsUnion.debugInfo());
		
		System.out.println("UNION SIZE");
		System.out.println("Correct: " + (itemsUnion.size() == bitsLeft.unionSize(bitsRight)));
	}
	
	/**
	 * Simple test for the method {@link ExtendedSet#getComplement()}
	 * 
	 * @param c class to test
	 */
	@SuppressWarnings("unchecked")
	private static void testForComplement(Class<? extends ExtendedSet> c) {
		ExtendedSet<Integer> bits;
		try {
			bits = c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		System.out.println("Original");
		bits.add(1);
		bits.add(2);
		bits.add(30000);
		System.out.println(bits.debugInfo());

		System.out.format("Complement size: %d\n", bits.complementSize());
		System.out.println("Complement");
		bits = bits.getComplement();
		System.out.println(bits.debugInfo());

		System.out.format("Complement size: %d\n", bits.complementSize());
		System.out.println("Complement");
		bits = bits.getComplement();
		System.out.println(bits.debugInfo());

		System.out.format("Complement size: %d\n", bits.complementSize());
		System.out.println("Complement");
		bits = bits.getComplement();
		System.out.println(bits.debugInfo());

		System.out.format("Complement size: %d\n", bits.complementSize());
		System.out.println("Complement");
		bits = bits.getComplement();
		System.out.println(bits.debugInfo());
	}
	
	/**
	 * Simple test for the methods:
	 * <ul>
	 * <li> {@link ExtendedSet#add(Integer)}
	 * <li> {@link ExtendedSet#remove(Object)}
	 * <li> {@link ExtendedSet#addAll(Collection)}
	 * <li> {@link ExtendedSet#removeAll(Collection)}
	 * <li> {@link ExtendedSet#retainAll(Collection)}
	 * <li> {@link ExtendedSet#getSymmetricDifference(ExtendedSet)}
	 * <li> {@link ExtendedSet#getComplement()}
	 * </ul>
	 * 
	 * @param c class to test
	 */
	@SuppressWarnings("unchecked")
	private static void testForMixedStuff(Class<? extends ExtendedSet> c) {
		ExtendedSet<Integer> bitsLeft;
		ExtendedSet<Integer> bitsRight;
		try {
			bitsLeft = c.newInstance();
			bitsRight = c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		bitsLeft.add(1);
		bitsLeft.add(100);
		bitsLeft.add(2);
		bitsLeft.add(3);
		bitsLeft.add(2);
		bitsLeft.add(100);
		System.out.println("A: " + bitsLeft);
		System.out.println(bitsLeft.debugInfo());

		bitsRight.add(1);
		bitsRight.add(1000000);
		bitsRight.add(2);
		bitsRight.add(30000);
		bitsRight.add(1000000);
		System.out.println("B: " + bitsRight);
		System.out.println(bitsRight.debugInfo());

		System.out.println("A.getSymmetricDifference(B): " + bitsLeft.getSymmetricDifference(bitsRight));
		System.out.println(bitsLeft.getSymmetricDifference(bitsRight).debugInfo());

		System.out.println("A.getComplement(): " + bitsLeft.getComplement());
		System.out.println(bitsLeft.getComplement().debugInfo());

		bitsLeft.removeAll(bitsRight);
		System.out.println("A.removeAll(B): " + bitsLeft);
		System.out.println(bitsLeft.debugInfo());

		bitsLeft.addAll(bitsRight);
		System.out.println("A.addAll(B): " + bitsLeft);
		System.out.println(bitsLeft.debugInfo());

		bitsLeft.retainAll(bitsRight);
		System.out.println("A.retainAll(B): " + bitsLeft);
		System.out.println(bitsLeft.debugInfo());

		bitsLeft.remove(1);
		System.out.println("A.remove(1): " + bitsLeft);
		System.out.println(bitsLeft.debugInfo());
	}
	
	/**
	 * Stress test for {@link ConciseSet#add(Integer)}
	 * <p> 
	 * It starts from a very sparse set (most of the words will be 0's 
	 * sequences) and progressively become very dense (words first
	 * become 0's sequences with 1 set bit and there will be almost one 
	 * word per item, then words become literals, and finally they 
	 * become 1's sequences and drastically reduce in number)
	 */
	private static void testForAdditionStress() {
		ConciseSet previousBits = new ConciseSet();
		ConciseSet currentBits = new ConciseSet();
		TreeSet<Integer> currentItems = new TreeSet<Integer>();

		Random rnd = new Random(System.currentTimeMillis());

		// add 100000 random numbers
		for (int i = 0; i < 100000; i++) {
			// random number to add
			int item = rnd.nextInt(10000 + 1);

			// keep the previous results
			previousBits = currentBits;
			currentBits = currentBits.clone();

			// add the element
			System.out.format("Adding %d...\n", item);
			boolean itemExistsBefore = currentItems.contains(item);
			boolean itemAdded = currentItems.add(item);
			boolean itemExistsAfter = currentItems.contains(item);
			boolean bitExistsBefore = currentBits.contains(item);
			boolean bitAdded = currentBits.add(item);
			boolean bitExistsAfter = currentBits.contains(item);
			if (itemAdded ^ bitAdded) {
				System.out.println("wrong add() result");
				return;
			}
			if (itemExistsBefore ^ bitExistsBefore) {
				System.out.println("wrong contains() before");
				return;
			}
			if (itemExistsAfter ^ bitExistsAfter) {
				System.out.println("wrong contains() after");
				return;
			}

			// check the list of elements
			if (!checkContent(currentBits, currentItems)) {
				System.out.println("add() error");
				System.out.println("Same elements: " + (currentItems.toString().equals(currentBits.toString())));
				System.out.println("Original items: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
			
			// check the representation
			ConciseSet otherBits = ConciseSet.asConciseSet(currentItems);
			if (otherBits.hashCode() != currentBits.hashCode()) {
				System.out.println("Representation error");
				System.out.println(currentBits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}

			// check the union size
			ConciseSet singleBitSet = new ConciseSet();
			singleBitSet.add(item);
			if (currentItems.size() != currentBits.unionSize(singleBitSet)) {
				System.out.println("Size error");
				System.out.println("Original items: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
		}

		System.out.println("Final");
		System.out.println(currentBits.debugInfo());
		
		System.out.println();
		System.out.println(Statistics.getSummary());
	}
	
	/**
	 * Stress test for {@link ConciseSet#remove(Object)}
	 * <p> 
	 * It starts from a very dense set (most of the words will be 1's 
	 * sequences) and progressively become very sparse (words first
	 * become 1's sequences with 1 unset bit and there will be few 
	 * words per item, then words become literals, and finally they 
	 * become 0's sequences and drastically reduce in number)
	 */
	private static void testForRemovalStress() {
		ConciseSet previousBits = new ConciseSet();
		ConciseSet currentBits = new ConciseSet();
		TreeSet<Integer> currentItems = new TreeSet<Integer>();

		Random rnd = new Random(System.currentTimeMillis());

		// create a 1-filled bitset
		currentBits.add(10001);
		currentBits.complement();
		currentItems.addAll(currentBits);
		if (currentItems.size() != 10001) {
			System.out.println("Unexpected error!");
			return;
		}
		
		// remove 100000 random numbers
		for (int i = 0; i < 100000 & !currentBits.isEmpty(); i++) {
			// random number to remove
			int item = rnd.nextInt(10000 + 1);

			// keep the previous results
			previousBits = currentBits;
			currentBits = currentBits.clone();
			
			// remove the element
			System.out.format("Removing %d...\n", item);
			boolean itemExistsBefore = currentItems.contains(item);
			boolean itemRemoved = currentItems.remove(item);
			boolean itemExistsAfter = currentItems.contains(item);
			boolean bitExistsBefore = currentBits.contains(item);
			boolean bitRemoved = currentBits.remove(item);
			boolean bitExistsAfter = currentBits.contains(item);
			if (itemRemoved ^ bitRemoved) {
				System.out.println("wrong remove() result");
				return;
			}
			if (itemExistsBefore ^ bitExistsBefore) {
				System.out.println("wrong contains() before");
				return;
			}
			if (itemExistsAfter ^ bitExistsAfter) {
				System.out.println("wrong contains() after");
				return;
			}

			// check the list of elements
			if (!checkContent(currentBits, currentItems)) {
				System.out.println("remove() error");
				System.out.println("Same elements: " + (currentItems.toString().equals(currentBits.toString())));
				System.out.println("Original items: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				
				return;
			}
			
			// check the representation
			ConciseSet otherBits = ConciseSet.asConciseSet(currentItems);
			if (otherBits.hashCode() != currentBits.hashCode()) {
				System.out.println("Representation error");
				System.out.println(currentBits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(previousBits.debugInfo());

				return;
			}

			// check the union size
			ConciseSet singleBitSet = new ConciseSet();
			singleBitSet.add(item);
			if (currentItems.size() != currentBits.differenceSize(singleBitSet)) {
				System.out.println("Size error");
				System.out.println("Original items: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());

				return;
			}
		}

		System.out.println("Final");
		System.out.println(currentBits.debugInfo());

		System.out.println();
		System.out.println(Statistics.getSummary());
	}
	
	/**
	 * Random operations on random sets.
	 * <p>
	 * It randomly chooses among {@link ConciseSet#addAll(Collection)},
	 * {@link ConciseSet#removeAll(Collection)}, and
	 * {@link ConciseSet#retainAll(Collection)}, and perform the operation over
	 * random sets
	 */
	private static void testForRandomOperationsStress() {
		ConciseSet bitsLeft = new ConciseSet();
		ConciseSet bitsRight = new ConciseSet();
		TreeSet<Integer> itemsLeft = new TreeSet<Integer>();
		TreeSet<Integer> itemsRight = new TreeSet<Integer>();

		Random rnd = new Random(System.currentTimeMillis());

		// random operation loop
		for (int i = 0; i < 100000; i++) {
			System.out.print("Test " + i + ": ");
			
			// new set
			itemsRight.clear();
			bitsRight.clear();
			final int size = 1 + rnd.nextInt(10000);
			final int min = 1 + rnd.nextInt(10000 - 1);
			final int max = min + rnd.nextInt(10000 - min + 1);
			for (int j = 0; j < size; j++) {
				int item = min + rnd.nextInt(max - min + 1);
				itemsRight.add(item);
				bitsRight.add(item);
			}
			if (!checkContent(bitsRight, itemsRight)) {
				System.out.println("ERROR!");
				System.out.println("Same elements: " + (itemsRight.toString().equals(bitsRight.toString())));
				System.out.println("Original items: " + itemsRight);
				System.out.println(bitsRight.debugInfo());
				return;
			}
			
			// perform the random operation with the previous set
			int operationSize = 0;
			switch (1 + rnd.nextInt(3)) {
			case 1:
				System.out.format(" union of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
				operationSize = bitsLeft.unionSize(bitsRight);
				itemsLeft.addAll(itemsRight);
				bitsLeft.addAll(bitsRight);
				break;

			case 2:
				System.out.format(" difference of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
				operationSize = bitsLeft.differenceSize(bitsRight);
				itemsLeft.removeAll(itemsRight);
				bitsLeft.removeAll(bitsRight);
				break;

			case 3:
				System.out.format(" intersection of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
				operationSize = bitsLeft.intersectionSize(bitsRight);
				itemsLeft.retainAll(itemsRight);
				bitsLeft.retainAll(bitsRight);
				break;
			}
			
			// check the list of elements
			if (!checkContent(bitsLeft, itemsLeft)) {
				System.out.println("OPERATION ERROR!");
				System.out.println("Same elements: " + 
						(itemsLeft.toString().equals(bitsLeft.toString())));
				System.out.println("Original items: " + itemsLeft);
				System.out.println(bitsLeft.debugInfo());
				return;
			}
			
			// check the representation
			if (ConciseSet.asConciseSet(itemsLeft).hashCode() != bitsLeft.hashCode()) {
				System.out.println("REPRESENTATION ERROR!");
				System.out.println(bitsLeft.debugInfo());
				System.out.println(ConciseSet.asConciseSet(itemsLeft).debugInfo());
				return;
			}

			// check the union size
			if (itemsLeft.size() != operationSize) {
				System.out.println("SIZE ERROR");
				System.out.println("Wrong size: " + operationSize);
				System.out.println("Correct size: " + itemsRight.size());
				System.out.println(bitsLeft.debugInfo());
				return;
			}
			
			System.out.println("done.");
		}
	}
	
	/**
	 * Stress test (addition) for {@link #subSet(Integer, Integer)}
	 */
	private static void testForSubSetAdditionStress() {
		ConciseSet previousBits = new ConciseSet();
		ConciseSet currentBits = new ConciseSet();
		TreeSet<Integer> currentItems = new TreeSet<Integer>();

		Random rnd = new Random(System.currentTimeMillis());

		for (int j = 0; j < 100000; j++) {
			// keep the previous result
			previousBits = currentBits;
			currentBits = currentBits.clone();

			// generate a new subview
			int min = rnd.nextInt(10000);
			int max = min + 1 + rnd.nextInt(10000 - (min + 1) + 1);
			int item = min + rnd.nextInt((max - 1) - min + 1);
			System.out.println("Adding " + item + " to the subview from " + min + " to " + max + " - 1");
			SortedSet<Integer> subBits = currentBits.subSet(min, max);
			SortedSet<Integer> subItems = currentItems.subSet(min, max);
			boolean subBitsResult = subBits.add(item);
			boolean subItemsResult = subItems.add(item);

			if (subBitsResult != subItemsResult 
					|| subBits.size() != subItems.size() 
					|| !subBits.toString().equals(subItems.toString())) {
				System.out.println("Subset error!");
				return;
			}			
			
			if (!checkContent(currentBits, currentItems)) {
				System.out.println("Subview not correct!");
				System.out.println("Same elements: " + (currentItems.toString().equals(currentBits.toString())));
				System.out.println("Original items: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
			
			// check the representation
			ConciseSet otherBits = ConciseSet.asConciseSet(currentItems);
			if (otherBits.hashCode() != currentBits.hashCode()) {
				System.out.println("Representation not correct!");
				System.out.println(currentBits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
		}

		System.out.println(currentBits.debugInfo());
		System.out.println(Statistics.getSummary());
	}
	
	/**
	 * Stress test (addition) for {@link ConciseSet#subSet(Integer, Integer)}
	 */
	private static void testForSubSetRemovalStress() {
		ConciseSet previousBits = new ConciseSet();
		ConciseSet currentBits = new ConciseSet();
		TreeSet<Integer> currentItems = new TreeSet<Integer>();

		// create a 1-filled bitset
		currentBits.add(10001);
		currentBits.complement();
		currentItems.addAll(currentBits);
		if (currentItems.size() != 10001) {
			System.out.println("Unexpected error!");
			return;
		}

		Random rnd = new Random(System.currentTimeMillis());

		for (int j = 0; j < 100000; j++) {
			// keep the previous result
			previousBits = currentBits;
			currentBits = currentBits.clone();

			// generate a new subview
			int min = rnd.nextInt(10000);
			int max = min + 1 + rnd.nextInt(10000 - (min + 1) + 1);
			int item = rnd.nextInt(10000 + 1);
			System.out.println("Removing " + item + " from the subview from " + min + " to " + max + " - 1");
			SortedSet<Integer> subBits = currentBits.subSet(min, max);
			SortedSet<Integer> subItems = currentItems.subSet(min, max);
			boolean subBitsResult = subBits.remove(item);
			boolean subItemsResult = subItems.remove(item);

			if (subBitsResult != subItemsResult 
					|| subBits.size() != subItems.size() 
					|| !subBits.toString().equals(subItems.toString())) {
				System.out.println("Subset error!");
				return;
			}
			
			if (!checkContent(currentBits, currentItems)) {
				System.out.println("Subview not correct!");
				System.out.println("Same elements: " + (currentItems.toString().equals(currentBits.toString())));
				System.out.println("Original items: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
			
			// check the representation
			ConciseSet otherBits = ConciseSet.asConciseSet(currentItems);
			if (otherBits.hashCode() != currentBits.hashCode()) {
				System.out.println("Representation not correct!");
				System.out.println(currentBits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
		}

		System.out.println(currentBits.debugInfo());
		System.out.println(Statistics.getSummary());
	}

	/**
	 * Random operations on random sub sets.
	 * <p>
	 * It randomly chooses among all operations and performs the operation over
	 * random sets
	 */
	private static void testForSubSetRandomOperationsStress() {
		ConciseSet bits = new ConciseSet();
		ConciseSet bitsPrevious = new ConciseSet();
		TreeSet<Integer> items = new TreeSet<Integer>();

		Random rnd = new Random(System.currentTimeMillis());

		// random operation loop
		for (int i = 0; i < 100000; i++) {
			System.out.print("Test " + i + ": ");
			
			// new set
			bitsPrevious = bits.clone();
			if (!bitsPrevious.toString().equals(bits.toString()))
				throw new RuntimeException("clone() error!");
			bits.clear();
			items.clear();
			final int size = 1 + rnd.nextInt(10000);
			final int min = 1 + rnd.nextInt(10000 - 1);
			final int max = min + rnd.nextInt(10000 - min + 1);
			final int minSub = 1 + rnd.nextInt(10000 - 1);
			final int maxSub = minSub + rnd.nextInt(10000 - minSub + 1);
			for (int j = 0; j < size; j++) {
				int item = min + rnd.nextInt(max - min + 1);
				bits.add(item);
				items.add(item);
			}
			
			// perform base checks 
			SortedSet<Integer> bitsSubSet = bits.subSet(minSub, maxSub);
			SortedSet<Integer> itemsSubSet = items.subSet(minSub, maxSub);
			if (!bitsSubSet.toString().equals(itemsSubSet.toString())) {
				System.out.println("toString() difference!");
				System.out.println("value: " + bitsSubSet.toString());
				System.out.println("actual: " + itemsSubSet.toString());
				return;
			}
			if (bitsSubSet.size() != itemsSubSet.size()) {
				System.out.println("size() difference!");
				System.out.println("value: " + bitsSubSet.size());
				System.out.println("actual: " + itemsSubSet.size());
				System.out.println("bits: " + bits.toString());
				System.out.println("items: " + items.toString());
				System.out.println("bitsSubSet: " + bitsSubSet.toString());
				System.out.println("itemsSubSet: " + itemsSubSet.toString());
				return;
			}
			if (!itemsSubSet.isEmpty() && (!bitsSubSet.first().equals(itemsSubSet.first()))) {
				System.out.println("first() difference!");
				System.out.println("value: " + bitsSubSet.first());
				System.out.println("actual: " + itemsSubSet.first());
				System.out.println("bits: " + bits.toString());
				System.out.println("items: " + items.toString());
				System.out.println("bitsSubSet: " + bitsSubSet.toString());
				System.out.println("itemsSubSet: " + itemsSubSet.toString());
				return;
			}
			if (!itemsSubSet.isEmpty() && (!bitsSubSet.last().equals(itemsSubSet.last()))) {
				System.out.println("last() difference!");
				System.out.println("value: " + bitsSubSet.last());
				System.out.println("actual: " + itemsSubSet.last());
				System.out.println("bits: " + bits.toString());
				System.out.println("items: " + items.toString());
				System.out.println("bitsSubSet: " + bitsSubSet.toString());
				System.out.println("itemsSubSet: " + itemsSubSet.toString());
				return;
			}

			// perform the random operation 
			boolean resBits = false;
			boolean resItems = false;
			boolean exceptionBits = false;
			boolean exceptionItems = false;
			switch (1 + rnd.nextInt(4)) {
			case 1:
				System.out.format(" addAll() of %d elements on %d elements... ", bitsPrevious.size(), bits.size());
				try {
					resBits = bitsSubSet.addAll(bitsPrevious);
				} catch (Exception e) {
					bits.clear();
					System.out.print("\n\tEXCEPTION on bitsSubSet: " + e.getClass() + " ");
					exceptionBits = true;
				}
				try {
					resItems = itemsSubSet.addAll(bitsPrevious);
				} catch (Exception e) {
					items.clear();
					System.out.print("\n\tEXCEPTION on itemsSubSet: " + e.getClass() + " ");
					exceptionItems = true;
				}
				break;

			case 2:
				System.out.format(" removeAll() of %d elements on %d elements... ", bitsPrevious.size(), bits.size());
				try {
					resBits = bitsSubSet.removeAll(bitsPrevious);
				} catch (Exception e) {
					bits.clear();
					System.out.print("\n\tEXCEPTION on bitsSubSet: " + e.getClass() + " ");
					exceptionBits = true;
				}
				try {
					resItems = itemsSubSet.removeAll(bitsPrevious);
				} catch (Exception e) {
					items.clear();
					System.out.print("\n\tEXCEPTION on itemsSubSet: " + e.getClass() + " ");
					exceptionItems = true;
				}
				break;

			case 3:
				System.out.format(" retainAll() of %d elements on %d elements... ", bitsPrevious.size(), bits.size());
				try {
					resBits = bitsSubSet.retainAll(bitsPrevious);
				} catch (Exception e) {
					bits.clear();
					System.out.print("\n\tEXCEPTION on bitsSubSet: " + e.getClass() + " ");
					exceptionBits = true;
				}
				try {
					resItems = itemsSubSet.retainAll(bitsPrevious);
				} catch (Exception e) {
					items.clear();
					System.out.print("\n\tEXCEPTION on itemsSubSet: " + e.getClass() + " ");
					exceptionItems = true;
				}
				break;

			case 4:
				System.out.format(" clear() of %d elements on %d elements... ", bitsPrevious.size(), bits.size());
				try {
					bitsSubSet.clear();
				} catch (Exception e) {
					bits.clear();
					System.out.print("\n\tEXCEPTION on bitsSubSet: " + e.getClass() + " ");
					exceptionBits = true;
				}
				try {
					itemsSubSet.clear();
				} catch (Exception e) {
					items.clear();
					System.out.print("\n\tEXCEPTION on itemsSubSet: " + e.getClass() + " ");
					exceptionItems = true;
				}
				break;
			}			
			
			if (exceptionBits != exceptionItems) {
				System.out.println("Incorrect exception!");
				return;
			}

			if (resBits != resItems) {
				System.out.println("Incorrect results!");
				System.out.println("resBits: " + resBits);
				System.out.println("resItems: " + resItems);
				return;
			}
			
			if (!checkContent(bits, items)) {
				System.out.println("Subview not correct!");
				System.out.format("min: %d, max: %d, minSub: %d, maxSub: %d\n", min, max, minSub, maxSub);
				System.out.println("Same elements: " + (items.toString().equals(bits.toString())));
				System.out.println("Original items: " + items);
				System.out.println(bits.debugInfo());
				System.out.println(bitsPrevious.debugInfo());
				return;
			}
			
			// check the representation
			ConciseSet otherBits = ConciseSet.asConciseSet(items);
			if (otherBits.hashCode() != bits.hashCode()) {
				System.out.println("Representation not correct!");
				System.out.format("min: %d, max: %d, minSub: %d, maxSub: %d\n", min, max, minSub, maxSub);
				System.out.println(bits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(bitsPrevious.debugInfo());
				return;
			}
			
			System.out.println("done.");
		}
	}
	
	/**
	 * Simple test for {@link IndexedSet}
	 */
	@SuppressWarnings("unchecked")
	private static void testForIndexedSet() {
		Collection<String> allStrings = new ArrayList<String>();
		allStrings.add("One");
		allStrings.add("Two");
		allStrings.add("Three");
		allStrings.add("Four");

		ExtendedSet<String> empty = new IndexedSet<String>(allStrings, false);

		ExtendedSet<String> s1 = empty.clone();
		System.out.println(s1);

		s1.add("Two");
		System.out.println(s1);

		s1.add("One");
		System.out.println(s1);

		s1.add("Three");
		System.out.println(s1);

		s1.remove("One");
		System.out.println(s1);

		ExtendedSet<String> s2 = empty.clone();
		s2.add("Four");
		s2.add("Three");
		System.out.println(s2);

		s2.retainAll(s1);
		System.out.println(s2);

		s2.add("Four");
		System.out.println(s2);

		s1.addAll(s2);
		System.out.println(s1);
		
		/**
		 * Expected output:
		 * 
		 * []
		 * [Two]
		 * [One, Two]
		 * [One, Two, Three]
		 * [Two, Three]
		 * [Three, Four]
		 * [Three]
		 * [Three, Four]
		 * [Two, Three, Four]
		 */
	}
	
	/**
	 * Test the method {@link ExtendedSet#compareTo(ExtendedSet)}
	 * 
	 * @param c class to test
	 */
	@SuppressWarnings("unchecked")
	private static void testForComparator(Class<? extends ExtendedSet> c) {
		ExtendedSet<Integer> bitsLeft;
		ExtendedSet<Integer> bitsRight;
		try {
			bitsLeft = c.newInstance();
			bitsRight = c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		bitsLeft.add(1);
		bitsLeft.add(2);
		bitsLeft.add(3);
		bitsLeft.add(100);
		bitsRight.add(1000000);
		System.out.println("A: " + bitsLeft);
		System.out.println("B: " + bitsRight);
		System.out.println("A.compareTo(B): " + bitsLeft.compareTo(bitsRight));
		System.out.println();

		bitsLeft.add(1000000);
		bitsRight.add(1);
		bitsRight.add(2);
		bitsRight.add(3);
		System.out.println("A: " + bitsLeft);
		System.out.println("B: " + bitsRight);
		System.out.println("A.compareTo(B): " + bitsLeft.compareTo(bitsRight));
		System.out.println();

		bitsLeft.remove(100);
		System.out.println("A: " + bitsLeft);
		System.out.println("B: " + bitsRight);
		System.out.println("A.compareTo(B): " + bitsLeft.compareTo(bitsRight));
		System.out.println();

		bitsRight.remove(1);
		System.out.println("A: " + bitsLeft);
		System.out.println("B: " + bitsRight);
		System.out.println("A.compareTo(B): " + bitsLeft.compareTo(bitsRight));
		System.out.println();

		bitsLeft.remove(1);
		bitsLeft.remove(2);
		System.out.println("A: " + bitsLeft);
		System.out.println("B: " + bitsRight);
		System.out.println("A.compareTo(B): " + bitsLeft.compareTo(bitsRight));
		System.out.println();
	}
	
	/**
	 * Test launcher
	 * 
	 * @param args ID of the test to execute (from 1 to 24)
	 */
	public static void main(String[] args) {
		int testCase = 24;
		
		if (args != null && args.length == 1) {
			try {
				testCase = new Integer(args[0]);
			} catch (NumberFormatException ignore) {
				// nothing to do
			}
		}
		
		switch (testCase) {
		case 1:
			testForAppendSimple(ConciseSet.class);
			break;
		case 15:
			testForAppendSimple(FastSet.class);
			break;
		case 2:
			testForAppendComplex(ConciseSet.class);
			break;
		case 16:
			testForAppendComplex(FastSet.class);
			break;
		case 17:
			testForIndexedSet();
			break;
		case 3:
			testForAppendRandom();
			break;
		case 4:
			testForIntersectionSimple(ConciseSet.class);
			break;
		case 18:
			testForIntersectionSimple(FastSet.class);
			break;
		case 5:
			testForIntersectionComplex(ConciseSet.class);
			break;
		case 19:
			testForIntersectionComplex(FastSet.class);
			break;
		case 6:
			testForUnionSimple(ConciseSet.class);
			break;
		case 20:
			testForUnionSimple(FastSet.class);
			break;
		case 7:
			testForComplement(ConciseSet.class);
			break;
		case 21:
			testForComplement(FastSet.class);
			break;
		case 8:
			testForMixedStuff(ConciseSet.class);
			break;
		case 22:
			testForMixedStuff(FastSet.class);
			break;
		case 9:
			testForAdditionStress();
			break;
		case 10:
			testForRemovalStress();
			break;
		case 11:
			testForRandomOperationsStress();
			break;
		case 12:
			testForSubSetAdditionStress();
			break;
		case 13:
			testForSubSetRemovalStress();
			break;
		case 14:
			testForSubSetRandomOperationsStress();
			break;
		case 23:
			testForComparator(ConciseSet.class);
			break;
		case 24:
			testForComparator(FastSet.class);
			break;
		default:
			System.out.println("Unknown test case!");
		}
	}
}
