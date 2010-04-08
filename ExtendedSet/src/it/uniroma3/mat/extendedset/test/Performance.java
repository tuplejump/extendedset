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

package it.uniroma3.mat.extendedset.test;

import it.uniroma3.mat.extendedset.ConciseSet;
import it.uniroma3.mat.extendedset.FastSet;
import it.uniroma3.mat.extendedset.utilities.MersenneTwister;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * Class for performance evaluation.
 * <p>
 * This class has been used to produce the pictures in <a
 * href="http://arxiv.org/abs/1004.0403">http://arxiv.org/abs/1004.0403</a>.
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 */
public class Performance {
	/** class to test the WAH algorithm */
	private static class WAHSet extends ConciseSet {
		@SuppressWarnings("unused")
		public WAHSet() {super(true);}
		public WAHSet(Collection<? extends Integer> c) {super(true, c);}
	}

	/** number of times to repeat each test */
	private final static int REPETITIONS = 5;

	/** time measurement, in nanoseconds */
	private static long lastExecTime = -1;

	/** test results */
	private final static Map<String, Map<Class<?>, Double>> TIME_VALUES = new TreeMap<String, Map<Class<?>, Double>>(); 
	
	/**
	 * Start time measurement
	 */
	private static void startTimer() {
		lastExecTime = System.nanoTime();
	}
	
	/**
	 * Stop time measurement
	 * 
	 * @param c
	 *            class being tested
	 * @param name
	 *            method name
	 * @param div
	 *            division factor (elapsed time and allocated memory will be
	 *            divided by this number)
	 */
	private static void endTimer(Class<?> c, String name, long div) {
		// final time
		double t = ((double) (System.nanoTime() - lastExecTime)) / div;
		Map<Class<?>, Double> measure = TIME_VALUES.get(name);
		if (measure == null)
			TIME_VALUES.put(name, measure = new HashMap<Class<?>, Double>());
		measure.put(c, t);	
	}

	/**
	 * Perform the time test
	 * @param classToTest
	 *            class of the {@link Collection} instance to test
	 * @param leftOperand
	 *            collection of integers representing the left operand
	 *            {@link Collection}
	 * @param rightOperand
	 *            collection of integers representing the right operand
	 *            {@link Collection}
	 */
	@SuppressWarnings("unchecked")
	private static void testClass(
			Class<?> classToTest, 
			Collection<Integer> leftOperand, 
			Collection<Integer> rightOperand) {
		// collections used for the test cases
		Collection<Integer>[] cAddAndRemove = new Collection[REPETITIONS];
		Collection<Integer>[] cAddAll = new Collection[REPETITIONS];
		Collection<Integer>[] cRemoveAll = new Collection[REPETITIONS];
		Collection<Integer>[] cRetainAll = new Collection[REPETITIONS];
		Collection<Integer>[] cRighOperand = new Collection[REPETITIONS];

		// CREATION
		for (int i = 0; i < REPETITIONS; i++) {
			try {
				cAddAndRemove[i] = (Collection) classToTest.newInstance();
				cAddAll[i] = (Collection) classToTest.newInstance();
				cRemoveAll[i] = (Collection) classToTest.newInstance();
				cRetainAll[i] = (Collection) classToTest.newInstance();
				cRighOperand[i] = (Collection) classToTest.newInstance(); 
			} catch (Exception e) {
				throw new RuntimeException(e);
			} 
		}
		
		// ADDITION
		startTimer();
		for (int i = 0; i < REPETITIONS; i++) {
			for (Integer x : leftOperand)
				cAddAndRemove[i].add(x);
			for (Integer x : rightOperand)
				cRighOperand[i].add(x);
			for (Integer x : leftOperand)
				cAddAll[i].add(x);
			for (Integer x : leftOperand)
				cRetainAll[i].add(x);
			for (Integer x : leftOperand)
				cRemoveAll[i].add(x);
		}
		endTimer(classToTest, "1) add()", REPETITIONS * (4 * leftOperand.size() + rightOperand.size()));
		
		// REMOVAL
		startTimer();
		for (int i = 0; i < REPETITIONS; i++) {
			for (Integer x : rightOperand)
				cAddAndRemove[i].remove(x);
		}
		endTimer(classToTest, "2) remove()", rightOperand.size() * REPETITIONS);
		
		// CONTAINS
		startTimer();
		for (int i = 0; i < REPETITIONS; i++) {
			for (Integer x : rightOperand)
				cAddAll[i].contains(x);
		}
		endTimer(classToTest, "3) contains()", rightOperand.size() * REPETITIONS);
		
		// CONTAINS for SORTED LISTS
		if (classToTest.getSimpleName().endsWith("List")) {
			startTimer();
			for (int i = 0; i < REPETITIONS; i++) {
				for (Integer x : rightOperand)
					Collections.binarySearch((List) (cAddAll[i]), x);
			}
			endTimer(classToTest, "3) contains() - bin", rightOperand.size() * REPETITIONS);
		}
		
		// AND SIZE
		startTimer();
		for (int i = 0; i < REPETITIONS; i++) {
			cAddAll[i].containsAll(cRighOperand[i]);
		}
		endTimer(classToTest, "4) containsAll()", REPETITIONS);
		
		// UNION
		startTimer();
		for (int i = 0; i < REPETITIONS; i++) {
			cAddAll[i].addAll(cRighOperand[i]);
		}
		endTimer(classToTest, "5) addAll()", REPETITIONS);
		
		// DIFFERENCE
		startTimer();
		for (int i = 0; i < REPETITIONS; i++) {
			cRemoveAll[i].removeAll(cRighOperand[i]);
		}
		endTimer(classToTest, "6) removeAll()", REPETITIONS);
		
		// INTERSECTION
		startTimer();
		for (int i = 0; i < REPETITIONS; i++) {
			cRetainAll[i].retainAll(cRighOperand[i]);
		}
		endTimer(classToTest, "7) retainAll()", REPETITIONS);
	}
	
	/**
	 * Summary information
	 */
	private static void printSummary(int cardinality, double density) {
		for (Entry<String, Map<Class<?>, Double>> e : TIME_VALUES.entrySet()) {
			// method name
			System.out.print(cardinality + "\t" + density + "\t");
			System.out.print(e.getKey());
			for (Entry<Class<?>, Double> m : e.getValue().entrySet()) {
				// class name
				System.out.print("\t" + m.getKey().getSimpleName() + "\t");
				
				// test values
				System.out.print(m.getValue().intValue());
			}
			System.out.println();
		}
	}

	/**
	 * Generates a set if integral numbers at random with uniform distribution
	 * 
	 * @param rnd
	 *            pseudo-random number generator
	 * @param cardinality
	 *            number of elements (i.e., the {@link Collection#size()}
	 *            result)
	 * @param density
	 *            ration between cardinality and max integer
	 * @return the set of integers
	 */
	private static Collection<Integer> uniform(Random rnd, int cardinality, double density) {
		// parameter check
		if (cardinality < 0)
			throw new IllegalArgumentException("cardinality < 0: " + cardinality);
		if (density < 0D)
			throw new IllegalArgumentException("density < 0: " + density);
		if (density > 1D)
			throw new IllegalArgumentException("density > 1: " + density);
		
		// maximum element
		int max = (int) (cardinality / density) + 1;
		
		// final set of random integers
		Set<Integer> integers = new HashSet<Integer>(Math.max(16, (int) (cardinality / .75f) + 1));
		while (integers.size() < cardinality)
			integers.add(rnd.nextInt(max));
		
		// sort integers
		List<Integer> res = new ArrayList<Integer>(integers);
		Collections.sort(res);
		
		return res;
	}

	/**
	 * Generates a set if integral numbers at random with Markovian distribution
	 * 
	 * @param rnd
	 *            pseudo-random number generator
	 * @param cardinality
	 *            number of elements (i.e., the {@link Collection#size()}
	 *            result)
	 * @param switchProb
	 *            the probability of switching from 0 to 1 (and viceversa) in
	 *            the sequence
	 * @return the set of integers
	 */
	private static Collection<Integer> markovian(Random rnd, int cardinality, double switchProb) {
		// parameter check
		if (cardinality < 0)
			throw new IllegalArgumentException("cardinality < 0: " + cardinality);
		if (switchProb < 0D)
			throw new IllegalArgumentException("switchProb < 0: " + switchProb);
		if (switchProb > 1D)
			throw new IllegalArgumentException("switchProb > 1: " + switchProb);
		
		// final set of random integers
		List<Integer> res = new ArrayList<Integer>(cardinality);
		int i = 0;
		boolean add = true;
		while(res.size() < cardinality) {
			if (add)
				res.add(i);
			add ^= rnd.nextDouble() < switchProb;
			i++;
		}
		
		return res;
	}

	/**
	 * Generates a set if integral numbers at random with Zipfian distribution
	 * 
	 * @param rnd
	 *            pseudo-random number generator
	 * @param cardinality
	 *            number of elements (i.e., the {@link Collection#size()}
	 *            result)
	 * @param max
	 *            maximal integer
	 * @return the set of integers
	 */
	private static Collection<Integer> zipfian(Random rnd, int cardinality, int max) {
		int k = 4;
		
		// parameter check
		if (cardinality < 0)
			throw new IllegalArgumentException("cardinality < 0: " + cardinality);
		if (max <= cardinality)
			throw new IllegalArgumentException("max <= cardinality: " + max + " <= " + cardinality);
		
		// final set of random integers
		Set<Integer> integers = new HashSet<Integer>(Math.max(16, (int) (cardinality / .75f) + 1));
		while (integers.size() < cardinality)
			integers.add((int) (max * Math.pow(rnd.nextDouble(), k)));
		
		// sort integers
		List<Integer> res = new ArrayList<Integer>(integers);
		Collections.sort(res);
		
		return res;
	}
	
	/**
	 * TEST
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Random rnd = new MersenneTwister(31);
		
		boolean calcUniform = true;
		boolean calcMarkovian = true;
		boolean calcZipfian = true;
		
		int maxCardinality = 100000;
		
		
		/*
		 * MEMORY
		 */
		
		if (calcUniform) {
			for (int cardinality = 1000; cardinality <= maxCardinality; cardinality *= 10) {
				for (double density = .01; density < 1D; density += .01) {
					System.out.format("MEMORY UNIFORM --> cardinality: %7d, density: %.2f, words: ", cardinality, density);
					
					Collection<Integer> integers = uniform(rnd, cardinality, density);
					
					FastSet s0 = new FastSet(integers);
					System.out.format("FastSet=%7d", (int) (s0.collectionCompressionRatio() * cardinality));
					
					ConciseSet s1 = new ConciseSet(integers);
					System.out.format(", ConciseSet=%7d", (int) (s1.collectionCompressionRatio() * cardinality));
					
					WAHSet s2 = new WAHSet(integers);
					System.out.format(", WAHSet=%7d\n", (int) (s2.collectionCompressionRatio() * cardinality));
				}
			}
		}

		if (calcMarkovian) {
			for (int cardinality = 1000; cardinality <= maxCardinality; cardinality *= 10) {
				double delta = .00001;
				for (double density = delta; density < 1D; density += delta, delta *= 1.1) {
					System.out.format("MEMORY MARKOVIAN --> cardinality: %7d, density: %.5f, words: ", cardinality, density);
					
					Collection<Integer> integers = markovian(rnd, cardinality, density);
					
					FastSet s0 = new FastSet(integers);
					System.out.format("FastSet=%7d", (int) (s0.collectionCompressionRatio() * cardinality));
					
					ConciseSet s1 = new ConciseSet(integers);
					System.out.format(", ConciseSet=%7d", (int) (s1.collectionCompressionRatio() * cardinality));
					
					WAHSet s2 = new WAHSet(integers);
					System.out.format(", WAHSet=%7d\n", (int) (s2.collectionCompressionRatio() * cardinality));
				}
			}
		}

		if (calcZipfian) {
			for (int cardinality = 1000; cardinality <= maxCardinality; cardinality *= 10) {
				for (int max = (int) (cardinality * 1.2); max < (cardinality << 17); max *= 1.2) {
					System.out.format("MEMORY ZIPFIAN --> cardinality: %7d, max: %10d, words: ", cardinality, max);
					
					Collection<Integer> integers = zipfian(rnd, cardinality, max);
					
					FastSet s0 = new FastSet(integers);
					System.out.format("FastSet=%10d", (int) (s0.collectionCompressionRatio() * cardinality));
					
					ConciseSet s1 = new ConciseSet(integers);
					System.out.format(", ConciseSet=%10d", (int) (s1.collectionCompressionRatio() * cardinality));
					
					WAHSet s2 = new WAHSet(integers);
					System.out.format(", WAHSet=%10d\n", (int) (s2.collectionCompressionRatio() * cardinality));
				}
			}
		}

		
		
		/*
		 * TIME
		 */
		
		if (calcUniform) {
			System.out.println("\nTIME UNIFORM\n----------");
			for (int cardinality = 1000; cardinality <= maxCardinality; cardinality *= 10) {
				for (double density : new double[] {.00625, .00625, .0125, .025, .05, .1, .2, .4, .8, .999}) {
					Collection<Integer> left = uniform(rnd, cardinality, density);
					Collection<Integer> right = uniform(rnd, cardinality, density);
					
					testClass(ArrayList.class, left, right);
					testClass(LinkedList.class, left, right);
					testClass(TreeSet.class, left, right);
					testClass(HashSet.class, left, right);
					testClass(FastSet.class, left, right);
					testClass(ConciseSet.class, left, right);
					testClass(WAHSet.class, left, right);
	
					printSummary(cardinality, density);
				}
			}
		}

		if (calcMarkovian) {
			System.out.println("\nTIME MARKOVIAN\n----------");
			for (int cardinality = 1000; cardinality <= maxCardinality; cardinality *= 10) {
				for (double switchProb : new double[] {.00625, .00625, .0125, .025, .05, .1, .2, .4, .8, .999}) {
					Collection<Integer> left = markovian(rnd, cardinality, switchProb);
					Collection<Integer> right = markovian(rnd, cardinality, switchProb);
					
					testClass(ArrayList.class, left, right);
					testClass(LinkedList.class, left, right);
					testClass(TreeSet.class, left, right);
					testClass(HashSet.class, left, right);
					testClass(FastSet.class, left, right);
					testClass(ConciseSet.class, left, right);
					testClass(WAHSet.class, left, right);
	
					printSummary(cardinality, switchProb);
				}
			}
		}

		if (calcZipfian) {
			System.out.println("\nTIME ZIPFIAN\n----------");
			for (int cardinality = 1000; cardinality <= maxCardinality; cardinality *= 10) {
				for (double max : new double[] {1.2, 1.2, 10, 100, 1000, 10000, 100000}) {
					Collection<Integer> left = zipfian(rnd, cardinality, (int) (cardinality * max));
					Collection<Integer> right = zipfian(rnd, cardinality, (int) (cardinality * max));
					
					testClass(ArrayList.class, left, right);
					testClass(LinkedList.class, left, right);
					testClass(TreeSet.class, left, right);
					testClass(HashSet.class, left, right);
					testClass(FastSet.class, left, right);
					testClass(ConciseSet.class, left, right);
					testClass(WAHSet.class, left, right);

					printSummary(cardinality, max);
				}
			}
		}
		
		System.out.println("\nDone!");
	}
}
