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

package it.uniroma3.mat.extendedset.test;

import it.uniroma3.mat.extendedset.ConcisePlusSet;
import it.uniroma3.mat.extendedset.ConciseSet;
import it.uniroma3.mat.extendedset.ExtendedSet;
import it.uniroma3.mat.extendedset.FastSet;
import it.uniroma3.mat.extendedset.ExtendedSet.ExtendedIterator;
import it.uniroma3.mat.extendedset.ExtendedSet.Statistics;
import it.uniroma3.mat.extendedset.utilities.MersenneTwister;
import it.uniroma3.mat.extendedset.wrappers.GenericExtendedSet;
import it.uniroma3.mat.extendedset.wrappers.IndexedSet;
import it.uniroma3.mat.extendedset.wrappers.IntegerSet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Test class for {@link ConciseSet}, {@link FastSet}, and {@link IndexedSet}.
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 */
public class Debug {
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
	private static <T> boolean checkContent(ExtendedSet<T> bits, SortedSet<T> items) {
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
		if (!bits.last().equals(items.last()))
			return false;
		if (!bits.first().equals(items.first()))
			return false;
		return true;
	}
	
	/**
	 * Populates a set with random values
	 * 
	 * @param set
	 *            the set to populate
	 * @param rnd
	 *            random number generator
	 * @param size
	 *            number of elements
	 * @param min
	 *            smallest element
	 * @param max
	 *            greatest element
	 */
	//TODO: cancellare
	private static void populate(ExtendedSet<Integer> set, Random rnd, int size, int min, int max) {
		if (min > max) 
			throw new IllegalArgumentException("min > max");
		if ((max - min + 1) < size) 
			throw new IllegalArgumentException("(max - min + 1) < size");
		
		// add elements
		while (set.size() < size) {
			if (rnd.nextDouble() < 0.8D) {
				set.complement();
				set.add(min + rnd.nextInt(max - min + 1));
			} else {
				if (rnd.nextDouble() < 0.2D) {
					// sequence
					int minSeq = min + rnd.nextInt(max - min + 1);
					int maxSeq = min + rnd.nextInt(max - min + 1);
					minSeq = Math.max(min, (minSeq / 31) * 31);
					maxSeq = Math.max(min, (maxSeq / 31) * 31);
					if (minSeq > maxSeq) {
						int tmp = maxSeq;
						maxSeq = minSeq;
						minSeq = tmp;
					}
					set.fill(minSeq, maxSeq);
				} else {
					// singleton
					set.add(min + rnd.nextInt(max - min + 1));
				}
			}
		}
		
		// remove elements when the set is too large
		while (set.size() > size) {
			int toClear = set.size() - size;
			int first;
			int last;
			if (rnd.nextBoolean()) {
				first = set.last() - toClear + 1;
				last = set.last();
			} else {
				first = set.first();
				last = set.first() + toClear - 1;
			}
			set.clear(first, last);
		}
	}
	
	/**
	 * Populates a set with random values, from 0 to the specified greatest element
	 * 
	 * @param set
	 *            the set to populate
	 * @param rnd
	 *            random number generator
	 * @param max
	 *            greatest elements
	 */
	//TODO: cancellare
	private static void populate(ExtendedSet<Integer> set, Random rnd, int max) {
		populate(set, rnd, (int) (rnd.nextDouble() * max), 0, max);
	}

	/**
	 * Generates an empty set of the specified class
	 * 
	 * @param c
	 *            the given class
	 * @return the empty set
	 */
	private static <X extends Collection<Integer>> X empty(Class<X> c) {
		try {
			return c.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
	private static void testForAdditionStress(Class<? extends ExtendedSet<Integer>> c) {
		ExtendedSet<Integer> previousBits = empty(c);
		ExtendedSet<Integer> currentBits = empty(c);
		TreeSet<Integer> currentItems = new TreeSet<Integer>();

		Random rnd = new MersenneTwister();

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
				System.out.println("Original: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
			
			// check the representation
			ExtendedSet<Integer> otherBits = previousBits.convert(currentItems);
			if (otherBits.hashCode() != currentBits.hashCode()) {
				System.out.println("Representation error");
				System.out.println(currentBits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}

			// check the union size
			ExtendedSet<Integer> singleBitSet = empty(c);
			singleBitSet.add(item);
			if (currentItems.size() != currentBits.unionSize(singleBitSet)) {
				System.out.println("Size error");
				System.out.println("Original: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
		}

		System.out.println("Final");
		System.out.println(currentBits.debugInfo());
		
		System.out.println();
		System.out.println(Statistics.summary());
	}
	
	/**
	 * Stress test for {@link ConciseSet#remove(Object)}
	 * <p> 
	 * It starts from a very dense set (most of the words will be 1's 
	 * sequences) and progressively become very sparse (words first
	 * become 1's sequences with 1 unset bit and there will be few 
	 * words per item, then words become literals, and finally they 
	 * become 0's sequences and drastically reduce in number)
	 * 
	 * @param c class to test
	 */
	private static void testForRemovalStress(Class<? extends ExtendedSet<Integer>> c) {
		ExtendedSet<Integer> previousBits = empty(c);
		ExtendedSet<Integer> currentBits = empty(c);
		TreeSet<Integer> currentItems = new TreeSet<Integer>();

		Random rnd = new MersenneTwister();

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
				System.out.println("Original: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				
				return;
			}
			
			// check the representation
			ExtendedSet<Integer> otherBits = empty(c);
			otherBits.addAll(currentItems);
			if (otherBits.hashCode() != currentBits.hashCode()) {
				System.out.println("Representation error");
				System.out.println(currentBits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(previousBits.debugInfo());

				return;
			}

			// check the union size
			ExtendedSet<Integer> singleBitSet = empty(c);
			singleBitSet.add(item);
			if (currentItems.size() != currentBits.differenceSize(singleBitSet)) {
				System.out.println("Size error");
				System.out.println("Original: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());

				return;
			}
		}

		System.out.println("Final");
		System.out.println(currentBits.debugInfo());

		System.out.println();
		System.out.println(Statistics.summary());
	}
	
	/**
	 * Random operations on random sets.
	 * <p>
	 * It randomly chooses among {@link ConciseSet#addAll(Collection)},
	 * {@link ConciseSet#removeAll(Collection)}, and
	 * {@link ConciseSet#retainAll(Collection)}, and perform the operation over
	 * random sets
	 * 
	 * @param c class to test
	 */
	private static void testForRandomOperationsStress(Class<? extends ExtendedSet<Integer>> c) {
		ExtendedSet<Integer> bitsLeft = empty(c);
		ExtendedSet<Integer> bitsRight = empty(c);
		SortedSet<Integer> itemsLeft = new TreeSet<Integer>();
		SortedSet<Integer> itemsRight = new TreeSet<Integer>();

		Random r = new MersenneTwister();
		final int maxCardinality = 1000;
		
		// random operation loop
		for (int i = 0; i < 1000000; i++) {
			System.out.print("Test " + i + ": ");
			
			//TODO:
			// clear(x,x)
			// fill(x,x)
			// equals()
			// unire gli altri test qui, in modo tale che ne faccio uno solo... Magari mantenere separato add e remove...
			
			RandomNumbers rn;
			switch (r.nextInt(3)) {
			case 0:
				rn = new RandomNumbers.Uniform(r.nextInt(maxCardinality), r.nextDouble() * 0.999, r.nextInt(maxCardinality / 10));
				break;
			case 1:
				rn = new RandomNumbers.Zipfian(r.nextInt(maxCardinality), r.nextDouble() * 0.9, r.nextInt(maxCardinality / 10), 2);
				break;
			case 2:
				rn = new RandomNumbers.Markovian(r.nextInt(maxCardinality), r.nextDouble() * 0.999, r.nextInt(maxCardinality / 10));
				break;
			default:
				throw new RuntimeException("unexpected");
			}
			
			/*
			 * contains(), add(), and remove()
			 */
			bitsRight.clear();
			itemsRight.clear();
			ExtendedSet<Integer> clone; 
			for (Integer e : rn.generate()) {
				if (itemsRight.contains(e) ^ bitsRight.contains(e)) {
					System.out.println("CONTAINS ERROR!");
					System.out.println("itemsRight.contains(" + e + "): " + itemsRight.contains(e));
					System.out.println("bitsRight.contains(" + e + "): " + bitsRight.contains(e));
					System.out.println("itemsRight:");
					System.out.println(itemsRight);
					System.out.println("bitsRight:");
					System.out.println(bitsRight.debugInfo());
					return;
				}
				clone = bitsRight.clone();
				boolean resItems = itemsRight.add(e);
				boolean resBits = bitsRight.add(e);
				ExtendedSet<Integer> app = empty(c);
				app.addAll(itemsRight);
				if (bitsRight.hashCode() != app.hashCode()) {
					System.out.println("ADD ERROR!");
					System.out.println("itemsRight.contains(" + e + "): " + itemsRight.contains(e));
					System.out.println("bitsRight.contains(" + e + "): " + bitsRight.contains(e));
					System.out.println("itemsRight:");
					System.out.println(itemsRight);
					System.out.println("bitsRight:");
					System.out.println(bitsRight.debugInfo());
					System.out.println("Append:");
					System.out.println(app.debugInfo());
					System.out.println("Clone:");
					System.out.println(clone.debugInfo());
					return;
				}
				if (resItems != resBits) {
					System.out.println("ADD BOOLEAN ERROR!");
					System.out.println("itemsRight.add(" + e + "): " + resItems);
					System.out.println("bitsRight.add(" + e + "): " + resBits);
					System.out.println("itemsRight:");
					System.out.println(itemsRight);
					System.out.println("bitsRight:");
					System.out.println(bitsRight.debugInfo());
					return;
				}
			}
			for (Integer e : rn.generate()) {
				clone = bitsRight.clone();
				boolean resItems = itemsRight.remove(e);
				boolean resBits = bitsRight.remove(e);
				ExtendedSet<Integer> app = empty(c);
				app.addAll(itemsRight);
				if (bitsRight.hashCode() != app.hashCode()) {
					System.out.println("REMOVE ERROR!");
					System.out.println("itemsRight.contains(" + e + "): " + itemsRight.contains(e));
					System.out.println("bitsRight.contains(" + e + "): " + bitsRight.contains(e));
					System.out.println("itemsRight:");
					System.out.println(itemsRight);
					System.out.println("bitsRight:");
					System.out.println(bitsRight.debugInfo());
					System.out.println("Append:");
					System.out.println(app.debugInfo());
					System.out.println("Clone:");
					System.out.println(clone.debugInfo());
					return;
				}
				if (resItems != resBits) {
					System.out.println("REMOVE BOOLEAN ERROR!");
					System.out.println("itemsRight.remove(" + e + "): " + resItems);
					System.out.println("bitsRight.remove(" + e + "): " + resBits);
					System.out.println("itemsRight:");
					System.out.println(itemsRight);
					System.out.println("bitsRight:");
					System.out.println(bitsRight.debugInfo());
					System.out.println("Clone:");
					System.out.println(clone.debugInfo());
					return;
				}
			}
			for (Integer e : rn.generate()) {
				clone = bitsRight.clone();
				if (!itemsRight.remove(e))
					itemsRight.add(e);
				bitsRight.flip(e);
				ExtendedSet<Integer> app = empty(c);
				app.addAll(itemsRight);
				if (bitsRight.hashCode() != app.hashCode()) {
					System.out.println("FLIP ERROR!");
					System.out.println("itemsRight.contains(" + e + "): " + itemsRight.contains(e));
					System.out.println("bitsRight.contains(" + e + "): " + bitsRight.contains(e));
					System.out.println("itemsRight:");
					System.out.println(itemsRight);
					System.out.println("bitsRight:");
					System.out.println(bitsRight.debugInfo());
					System.out.println("Append:");
					System.out.println(app.debugInfo());
					System.out.println("Clone:");
					System.out.println(clone.debugInfo());
					return;
				}
			}
			
			// new right operand
			itemsRight = rn.generate();
			bitsRight.clear();
			bitsRight.addAll(itemsRight);

			/*
			 * check for content correctness, first(), and last() 
			 */
			if (!checkContent(bitsRight, itemsRight)) {
				System.out.println("RIGHT OPERAND ERROR!");
				System.out.println("Same elements: " + (itemsRight.toString().equals(bitsRight.toString())));
				System.out.println("itemsRight:");
				System.out.println(itemsRight);
				System.out.println("bitsRight:");
				System.out.println(bitsRight.debugInfo());

				System.out.println("itemsRight.size(): "  + itemsRight.size() + " ?= bitsRight.size(): " + bitsRight.size());
				for (Integer x : bitsRight) 
					if (!itemsRight.contains(x)) 
						System.out.println("itemsRight does not contain " + x);
				for (Integer x : itemsRight) 
					if (!bitsRight.contains(x)) 
						System.out.println("itemsRight does not contain " + x);
				System.out.println("bitsRight.last(): " + bitsRight.last() + " ?= itemsRight.last(): " + itemsRight.last());
				System.out.println("bitsRight.first(): " + bitsRight.first() + " ?= itemsRight.first(): " + itemsRight.first());

				return;
			}
			
			/*
			 * containsAll()
			 */
			boolean bitsRes = bitsLeft.containsAll(bitsRight);
			boolean itemsRes = itemsLeft.containsAll(itemsRight);
			if (bitsRes != itemsRes) {
				System.out.println("CONTAINS_ALL ERROR!");
				System.out.println("bitsLeft.containsAll(bitsRight): " + bitsRes);
				System.out.println("itemsLeft.containsAll(itemsRight): " + itemsRes);
				System.out.println("bitsLeft:");
				System.out.println(bitsLeft.debugInfo());				
				System.out.println("bitsRight:");
				System.out.println(bitsRight.debugInfo());
				System.out.println("bitsLeft.intersection(bitsRight)");
				System.out.println(bitsLeft.intersection(bitsRight));
				System.out.println("itemsLeft.retainAll(itemsRight)");
				itemsLeft.retainAll(itemsRight);
				System.out.println(itemsLeft);
				return;
			}

			/*
			 * containsAny()
			 */
			bitsRes = bitsLeft.containsAny(bitsRight);
			itemsRes = true;
			for (Integer x : itemsRight) {
				itemsRes = itemsLeft.contains(x);
				if (itemsRes)
					break;
			}
			if (bitsRes != itemsRes) {
				System.out.println("bitsLeft.containsAny(bitsRight): " + bitsRes);
				System.out.println("itemsLeft.containsAny(itemsRight): " + itemsRes);
				System.out.println("bitsLeft:");
				System.out.println(bitsLeft.debugInfo());				
				System.out.println("bitsRight:");
				System.out.println(bitsRight.debugInfo());
				System.out.println("bitsLeft.intersection(bitsRight)");
				System.out.println(bitsLeft.intersection(bitsRight));
				System.out.println("itemsLeft.retainAll(itemsRight)");
				itemsLeft.retainAll(itemsRight);
				System.out.println(itemsLeft);
				return;
			}
			
			/*
			 * containsAtLeast()
			 */
			int l = 1 + r.nextInt(bitsRight.size() + 1);
			bitsRes = bitsLeft.containsAtLeast(bitsRight, l);
			int itemsResCnt = 0;
			for (Integer x : itemsRight) {
				if (itemsLeft.contains(x))
					itemsResCnt++;
				if (itemsResCnt >= l)
					break;
			}
			if (bitsRes != (itemsResCnt >= l)) {
				System.out.println("bitsLeft.containsAtLeast(bitsRight, " + l + "): " + bitsRes);
				System.out.println("itemsLeft.containsAtLeast(itemsRight, " + l + "): " + (itemsResCnt >= l));
				System.out.println("bitsLeft:");
				System.out.println(bitsLeft.debugInfo());				
				System.out.println("bitsRight:");
				System.out.println(bitsRight.debugInfo());
				System.out.println("bitsLeft.intersection(bitsRight)");
				System.out.println(bitsLeft.intersection(bitsRight));
				System.out.println("itemsLeft.retainAll(itemsRight)");
				itemsLeft.retainAll(itemsRight);
				System.out.println(itemsLeft);
				return;
			}

			/*
			 * Perform a random operation with the previous set:
			 * addAll() and unionSize()
			 * removeAll() and differenceSize()
			 * retainAll() and intersectionSize()
			 * symmetricDifference() and symmetricDifferenceSize()
			 * complement() and complementSize()
			 */
			int operationSize = 0;
			boolean resItems = true, resBits = true;
			switch (1 + r.nextInt(5)) {
			case 1:
				System.out.format(" union of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
				operationSize = bitsLeft.unionSize(bitsRight);
				resItems = itemsLeft.addAll(itemsRight);
				resBits = bitsLeft.addAll(bitsRight);
				break;

			case 2:
				System.out.format(" difference of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
				operationSize = bitsLeft.differenceSize(bitsRight);
				resItems = itemsLeft.removeAll(itemsRight);
				resBits = bitsLeft.removeAll(bitsRight);
				break;

			case 3:
				System.out.format(" intersection of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
				operationSize = bitsLeft.intersectionSize(bitsRight);
				resItems = itemsLeft.retainAll(itemsRight);
				resBits = bitsLeft.retainAll(bitsRight);
				break;

			case 4:
				System.out.format(" symmetric difference of %d elements with %d elements... ", itemsLeft.size(), itemsRight.size());
				operationSize = bitsLeft.symmetricDifferenceSize(bitsRight);
				TreeSet<Integer> temp = new TreeSet<Integer>(itemsRight);
				temp.removeAll(itemsLeft);
				itemsLeft.removeAll(itemsRight);
				itemsLeft.addAll(temp);
				bitsLeft = bitsLeft.symmetricDifference(bitsRight);
				break;

			case 5:
				System.out.format(" complement of %d elements... ", itemsLeft.size());
				operationSize = bitsLeft.complementSize();
				if (!itemsLeft.isEmpty())
					for (int j = itemsLeft.last(); j >= 0; j--)
						if (!itemsLeft.add(j))
							itemsLeft.remove(j);
				bitsLeft.complement();
				break;
			}
			
			// check the list of elements
			if (!checkContent(bitsLeft, itemsLeft)) {
				System.out.println("OPERATION ERROR!");
				System.out.println("Same elements: " + (itemsLeft.toString().equals(bitsLeft.toString())));
				System.out.println("itemsLeft:");
				System.out.println(itemsLeft);
				System.out.println("bitsLeft:");
				System.out.println(bitsLeft.debugInfo());

				System.out.println("itemsLeft.size(): "  + itemsLeft.size() + " ?= bitsLeft.size(): " + bitsLeft.size());
				for (Integer x : bitsLeft) 
					if (!itemsLeft.contains(x)) 
						System.out.println("itemsLeft does not contain " + x);
				for (Integer x : itemsLeft) 
					if (!bitsLeft.contains(x)) 
						System.out.println("itemsLeft does not contain " + x);
				System.out.println("bitsLeft.last(): " + bitsLeft.last() + " ?= itemsLeft.last(): " + itemsLeft.last());
				System.out.println("bitsLeft.first(): " + bitsLeft.first() + " ?= itemsLeft.first(): " + itemsLeft.first());
				
				return;
			}

			// check the size
			if (itemsLeft.size() != operationSize) {
				System.out.println("OPERATION SIZE ERROR");
				System.out.println("Wrong size: " + operationSize);
				System.out.println("Correct size: " + itemsLeft.size());
				System.out.println("bitsLeft:");
				System.out.println(bitsLeft.debugInfo());
				return;
			}

			// check the boolean result
			if (resItems != resBits) {
				System.out.println("OPERATION BOOLEAN ERROR!");
				System.out.println("resItems: " + resItems);
				System.out.println("resBits: " + resBits);
				System.out.println("bitsLeft:");
				System.out.println(bitsLeft.debugInfo());
				return;
			}

			// check the internal representation of the result
			ExtendedSet<Integer> x = bitsLeft.empty();
			x.addAll(itemsLeft);
			if (x.hashCode() != bitsLeft.hashCode()) {
				System.out.println("Internal representation error!");
				System.out.println("FROM APPEND:");
				System.out.println(x.debugInfo());
				System.out.println("FROM OPERATION:");
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
		IntegerSet previousBits = new IntegerSet(new ConciseSet());
		IntegerSet currentBits = new IntegerSet(new ConciseSet());
		TreeSet<Integer> currentItems = new TreeSet<Integer>();

		Random rnd = new MersenneTwister();

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
				System.out.println("Original: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
			
			// check the representation
			IntegerSet otherBits = new IntegerSet(new ConciseSet());
			otherBits.addAll(currentItems);
			if (otherBits.hashCode() != currentBits.hashCode()) {
				System.out.println("Representation not correct!");
				System.out.println(currentBits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
		}

		System.out.println(currentBits.debugInfo());
		System.out.println(Statistics.summary());
	}
	
	/**
	 * Stress test (addition) for {@link ConciseSet#subSet(Integer, Integer)}
	 */
	private static void testForSubSetRemovalStress() {
		IntegerSet previousBits = new IntegerSet(new ConciseSet());
		IntegerSet currentBits = new IntegerSet(new ConciseSet());
		TreeSet<Integer> currentItems = new TreeSet<Integer>();

		// create a 1-filled bitset
		currentBits.add(10001);
		currentBits.complement();
		currentItems.addAll(currentBits);
		if (currentItems.size() != 10001) {
			System.out.println("Unexpected error!");
			return;
		}

		Random rnd = new MersenneTwister();

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
				System.out.println("Original: " + currentItems);
				System.out.println(currentBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
			
			// check the representation
			IntegerSet otherBits = new IntegerSet(new ConciseSet());
			otherBits.addAll(currentItems);
			if (otherBits.hashCode() != currentBits.hashCode()) {
				System.out.println("Representation not correct!");
				System.out.println(currentBits.debugInfo());
				System.out.println(otherBits.debugInfo());
				System.out.println(previousBits.debugInfo());
				return;
			}
		}

		System.out.println(currentBits.debugInfo());
		System.out.println(Statistics.summary());
	}

	/**
	 * Random operations on random sub sets.
	 * <p>
	 * It randomly chooses among all operations and performs the operation over
	 * random sets
	 */
	private static void testForSubSetRandomOperationsStress() {
		IntegerSet bits = new IntegerSet(new ConciseSet());
		IntegerSet bitsPrevious = new IntegerSet(new ConciseSet());
		TreeSet<Integer> items = new TreeSet<Integer>();

		Random rnd = new MersenneTwister();

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
				System.out.println("Original: " + items);
				System.out.println(bits.debugInfo());
				System.out.println(bitsPrevious.debugInfo());
				return;
			}
			
			// check the representation
			IntegerSet otherBits = new IntegerSet(new ConciseSet());
			otherBits.addAll(items);
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
	 * Test the method {@link ExtendedSet#compareTo(ExtendedSet)}
	 * 
	 * @param c class to test
	 */
	private static void testForComparatorSimple(Class<? extends ExtendedSet<Integer>> c) {
		ExtendedSet<Integer> bitsLeft = empty(c);
		ExtendedSet<Integer> bitsRight = empty(c);

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
	 * Another test for {@link ExtendedSet#compareTo(ExtendedSet)}
	 * 
	 * @param c class to test
	 */
	private static void testForComparatorComplex(Class<? extends ExtendedSet<Integer>> c) {
		ExtendedSet<Integer> bitsLeft = empty(c);
		ExtendedSet<Integer> bitsRight = empty(c);

		Random rnd = new MersenneTwister(31);
		for (int i = 0; i < 10000; i++) {
			// empty numbers
			BigInteger correctLeft = BigInteger.ZERO;
			BigInteger correctRight = BigInteger.ZERO;
			bitsLeft.clear();
			bitsRight.clear();
			
			// generate two random numbers
			int size = 1 + rnd.nextInt(10000);
			int left = 0, right = 0;
			for (int j = 0; j < size; j++) {
				left = Math.abs(rnd.nextDouble() > 0.001D ? (left - 1) : rnd.nextInt(size));
				right = Math.abs(rnd.nextDouble() > 0.001D ? left : rnd.nextInt(size));
				bitsLeft.add(left);
				bitsRight.add(right);
				correctLeft = correctLeft.setBit(left);
				correctRight = correctRight.setBit(right);
			}
			
			// compare them!
			boolean correct = bitsLeft.compareTo(bitsRight) == correctLeft.compareTo(correctRight);
			System.out.println(i + ": " + correct);
			if (!correct) {
				System.out.println("ERROR!");
				System.out.println("bitsLeft:  " + bitsLeft);
				System.out.println("           " + bitsLeft.debugInfo());
				System.out.println("bitsRight: " + bitsRight);
				System.out.println("           " + bitsRight.debugInfo());
				int maxLength = Math.max(correctLeft.bitLength(), correctRight.bitLength());
				System.out.format("correctLeft.toString(2):  %" + maxLength + "s\n", correctLeft.toString(2));
				System.out.format("correctRight.toString(2): %" + maxLength + "s\n", correctRight.toString(2));
				System.out.println("correctLeft.compareTo(correctRight): " + correctLeft.compareTo(correctRight));
				System.out.println("bitsLeft.compareTo(bitsRight):  " + bitsLeft.compareTo(bitsRight));
				
				Iterator<Integer> itrLeft = bitsLeft.descendingIterator();
				Iterator<Integer> itrRight = bitsRight.descendingIterator();
				while (itrLeft.hasNext() && itrRight.hasNext()) {
					int l = itrLeft.next();
					int r = itrRight.next();
					if (l != r) {
						System.out.println("l != r --> " + l + ", " + r);
						break;
					}
				}
				return;
			}
		}
		System.out.println("Done!");
	}

	/**
	 * Stress test for {@link ExtendedSet#descendingIterator()}
	 * 
	 * @param c class to test
	 */
	private static void testForDescendingIterator(Class<? extends ExtendedSet<Integer>> c) {
		ExtendedSet<Integer> bits = empty(c);
		
		Random rnd = new MersenneTwister(31);
		for (int i = 0; i < 100000; i++) {
			HashSet<Integer> x = new HashSet<Integer>(bits);
			HashSet<Integer> y = new HashSet<Integer>();
			for (Integer e : bits.descending())
				y.add(e);
			
			boolean correct = x.equals(y);
			System.out.print(i + ": " + correct);
			if (!correct) {
				System.out.println("ERRORE!");
				System.out.println(bits.debugInfo());
				for (Integer e : bits.descending())
					System.out.print(e + ", ");
			}

			int n = rnd.nextInt(10000);
			System.out.println(" + " + n);
			bits.add(n);
		}
		
		System.out.println(bits.debugInfo());
		for (Integer e : bits.descending())
			System.out.print(e + ", ");
	}
	
	/**
	 * Stress test for {@link ConciseSet#get(int)}
	 * 
	 * @param c class to test
	 */
	private static void testForPosition(Class<? extends ExtendedSet<Integer>> c) {
		ExtendedSet<Integer> bits = empty(c);

		Random rnd = new MersenneTwister(31);
		for (int i = 0; i < 1000; i++) {
			// new set
			bits.clear();
			final int size = 1 + rnd.nextInt(10000);
			final int min = 1 + rnd.nextInt(10000 - 1);
			final int max = min + rnd.nextInt(10000 - min + 1);
			for (int j = 0; j < size; j++) {
				int item = min + rnd.nextInt(max - min + 1);
				bits.add(item);
			}
			
			// check correctness
			String good = bits.toString();
			StringBuilder other = new StringBuilder();
			int s = bits.size();
			other.append('[');
			for (int j = 0; j < s; j++) {
				other.append(bits.get(j));
				if (j < s - 1)
					other.append(", ");
			}
			other.append(']');
			
			if (good.equals(other.toString())) {
				System.out.println(i + ") OK");
			} else {
				System.out.println("ERROR");
				System.out.println(bits.debugInfo());
				System.out.println(bits);
				System.out.println(other);
				return;
			}
			
			int pos = 0;
			for (Integer x : bits) {
				if (bits.indexOf(x) != pos) {
					System.out.println("ERROR! " + pos + " != " + bits.indexOf(x) + " for element " + x);
					System.out.println(bits.debugInfo());
					return;
				}
				pos++;
			}
		}
	}
	
	/**
	 * Test for {@link ExtendedIterator#skipAllBefore(Object)}
	 * 
	 * @param c class to test
	 */
	private static void testForSkip(Class<? extends ExtendedSet<Integer>> c) {
		ExtendedSet<Integer> bits = empty(c);

		Random rnd = new MersenneTwister(31);
		for (int i = 0; i < 10000; i++) {
			int max = rnd.nextInt(10000);
			bits.clear();
			populate(bits, rnd, max);

			for (int j = 0; j < 100; j++) {
				int skip = rnd.nextInt(max + 1);
				boolean reverse = rnd.nextBoolean();
				System.out.format("%d) size=%d, skip=%d, reverse=%b ---> ", (i * 100) + j + 1, bits.size(), skip, reverse);

				ExtendedIterator<Integer> itr1, itr2;
				if (!reverse) {
					itr1 = bits.iterator();
					itr2 = bits.iterator();
					while (itr1.hasNext() && itr1.next() < skip) {/* nothing */}
				} else {
					itr1 = bits.descendingIterator();
					itr2 = bits.descendingIterator();
					while (itr1.hasNext() && itr1.next() > skip) {/* nothing */}
				}
				if (!itr1.hasNext()) {
					System.out.println("Skipped!");
					continue;
				}
				itr2.skipAllBefore(skip);
				itr2.next();
				Integer i1, i2;
				if (!(i1 = itr1.next()).equals(i2 = itr2.next())) {
					System.out.println("Error!");
					System.out.println("i1 = " + i1);
					System.out.println("i2 = " + i2);
					System.out.println(bits.debugInfo());
					return;
				}
				System.out.println("OK!");
			}
		}
		System.out.println("Done!");
	}

	private enum TestCase {
		ADDITION_STRESS,
		REMOVAL_STRESS,
		RANDOM_OPERATION_STRESS,
		SUBSET_ADDITION_STRESS_CONCISESET,
		SUBSET_REMOVAL_STRESS_CONCISESET,
		SUBSET_RANDOM_OPERATION_STRESS_CONCISESET,
		COMPARATOR_SIMPLE,
		COMPARATOR_COMPLEX,
		DESCENDING_ITERATOR,
		POSITION,
		SKIP,
		;
	}

	@SuppressWarnings("unused")
	private static class ListSet extends GenericExtendedSet<Integer> {
		ListSet() {
			super(ArrayList.class, GenericExtendedSet.ALL_POSITIVE_INTEGERS);
		}
	}

	@SuppressWarnings("unused")
	private static class LinkedSet extends GenericExtendedSet<Integer> {
		LinkedSet() {
			super(LinkedList.class, GenericExtendedSet.ALL_POSITIVE_INTEGERS);
		}
	}

	@SuppressWarnings("unused")
	private static class IntegerFastSet extends IntegerSet {IntegerFastSet() {super(new FastSet());}}
//	@SuppressWarnings("unused")
	private static class IntegerConciseSet extends IntegerSet {IntegerConciseSet() {super(new ConciseSet());}}
	@SuppressWarnings("unused")
	private static class IntegerWAHSet extends IntegerSet {IntegerWAHSet() {super(new ConciseSet(true));}}
	@SuppressWarnings("unused")
	private static class IntegerConcisePlusSet extends IntegerSet {IntegerConcisePlusSet() {super(new ConcisePlusSet());}}

	/**
	 * Test launcher
	 * 
	 * @param args ID of the test to execute
	 */
	public static void main(String[] args) {
		// NOTE: the most complete test is TestCase.RANDOM_OPERATION_STRESS
		TestCase testCase = TestCase.RANDOM_OPERATION_STRESS;
//		TestCase testCase = TestCase.SKIP;
//		Class<? extends ExtendedSet<Integer>> classToTest = IntegerFastSet.class;
		Class<? extends ExtendedSet<Integer>> classToTest = IntegerConciseSet.class;
//		Class<? extends ExtendedSet<Integer>> classToTest = IntegerWAHSet.class;
//		Class<? extends ExtendedSet<Integer>> classToTest = IntegerConcisePlusSet.class;
//		Class<? extends ExtendedSet<Integer>> classToTest = ListSet.class;
//		Class<? extends ExtendedSet<Integer>> classToTest = LinkedSet.class;
//		Class<? extends ExtendedSet<Integer>> classToTest = ArraySet.class;
		
		if (args != null && args.length > 0) {
			try {
				testCase = TestCase.values()[Integer.parseInt(args[0])];
			} catch (NumberFormatException ignore) {
				// nothing to do
			}
		}
		
		switch (testCase) {
		case ADDITION_STRESS:
			testForAdditionStress(classToTest);
			break;
		case REMOVAL_STRESS:
			testForRemovalStress(classToTest);
			break;
		case RANDOM_OPERATION_STRESS:
			testForRandomOperationsStress(classToTest);
			break;
		case SUBSET_ADDITION_STRESS_CONCISESET:
			testForSubSetAdditionStress();
			break;
		case SUBSET_REMOVAL_STRESS_CONCISESET:
			testForSubSetRemovalStress();
			break;
		case SUBSET_RANDOM_OPERATION_STRESS_CONCISESET:
			testForSubSetRandomOperationsStress();
			break;
		case COMPARATOR_SIMPLE:
			testForComparatorSimple(classToTest);
			break;
		case COMPARATOR_COMPLEX:
			testForComparatorComplex(classToTest);
			break;
		case DESCENDING_ITERATOR:
			testForDescendingIterator(classToTest);
			break;
		case POSITION:
			testForPosition(classToTest);
			break;
		case SKIP:
			testForSkip(classToTest);
		}
	}
}

