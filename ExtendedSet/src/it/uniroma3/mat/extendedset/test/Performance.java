package it.uniroma3.mat.extendedset.test;

import it.uniroma3.mat.extendedset.ConciseSet;
import it.uniroma3.mat.extendedset.FastSet;
import it.uniroma3.mat.extendedset.utilities.MersenneTwister;

import java.text.DecimalFormat;
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
 * Class for performance evaluation
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 */
public class Performance {
	private static class WAHSet extends ConciseSet {
		@SuppressWarnings("unused")
		public WAHSet() {super(true);}
		public WAHSet(Collection<? extends Integer> c) {super(true, c);}
	}


	// number of times to repeat each test
	private final static int REPETITIONS = 5;

	// time and memory measurements
	private final static ResourceMonitor rm = new ResourceMonitor();
	
	// test results
	private final static Map<String, Map<Class<?>, Double>> timeValues = new TreeMap<String, Map<Class<?>, Double>>(); 
	private final static Map<String, Map<Class<?>, Double>> memoryValues = new TreeMap<String, Map<Class<?>, Double>>(); 
	
	/**
	 * Format the test results
	 */ 
	private static String format(double time, double memory) {
		StringBuilder s = new StringBuilder();

		// double value formatter
		DecimalFormat f = new DecimalFormat();
		f.setMinimumFractionDigits(6);
		f.setMaximumFractionDigits(6);
		f.setGroupingUsed(true);
		
		// append time result
		String n = f.format(time);
		
		s.append("Time = ");
		for (int i = 0; i < 22 - n.length(); i++) 
			s.append(' ');
		s.append(n);
		s.append(" ns,  ");

		// append memory result
		n = f.format(memory);

		s.append("Memory = ");
		for (int i = 0; i < 18 - n.length(); i++) 
			s.append(' ');
		s.append(n);
		s.append(" byte(s)");
		
		// final string
		return s.toString();
	}

	/**
	 * Compute final values
	 * 
	 * @param c
	 *            class being tested
	 * @param name
	 *            method name
	 * @param div
	 *            division factor (elapsed time and allocated memory will be
	 *            divided by this number)
	 */
	private static void endTimer(long[] timeAndMemory, Class<?> c, String name, long div) {
		// final time
		double t = ((double) timeAndMemory[0]) / div;
		Map<Class<?>, Double> measure = timeValues.get(name);
		if (measure == null) {
			measure = new HashMap<Class<?>, Double>();
			timeValues.put(name, measure);
		}
		measure.put(c, t);	
		
		// final memory
		double m = ((double) timeAndMemory[1]) / div;
		measure = memoryValues.get(name);
		if (measure == null) {
			measure = new HashMap<Class<?>, Double>();
			memoryValues.put(name, measure);
		}
		measure.put(c, m);	
		
		// results
//		System.out.print(name + "... ");
//		for (int i = 0; i < 30 - name.length(); i++) 
//			System.out.print(" ");
//		System.out.println(format(t, m));
	}

	/**
	 * Perform the memory test
	 * 
	 * @param classToTest
	 *            class of the {@link Collection} instance to test
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	private static void testMemory(Class<?> classToTest, Collection<Integer> integers) {
		// print the class being tested
		System.out.println("class: " + classToTest.getSimpleName());

		try {
			// load classes
			Collection<Integer> c = (Collection) classToTest.newInstance();
			rm.useObject(c);
			
			// new instance
			rm.startTimer();
			c = (Collection) classToTest.newInstance();
			endTimer(rm.endTimer(), classToTest, "a) creating empty instance", 1);
			rm.useObject(c);
			System.gc();
			System.runFinalization();
			System.gc();
			System.runFinalization();

			// add integers
			rm.startTimer();
			c.addAll(integers);
			endTimer(rm.endTimer(), classToTest, "b) adding " + integers.size() + " elements", integers.size());
			rm.useObject(c);
			// new instance

			//TWICE
			
			// new instance
			rm.startTimer();
			c = (Collection) classToTest.newInstance();
			endTimer(rm.endTimer(), classToTest, "a) creating empty instance", 1);
			rm.useObject(c);

			// add integers
			rm.startTimer();
			c.addAll(integers);
			endTimer(rm.endTimer(), classToTest, "b) adding " + integers.size() + " elements", integers.size());
			rm.useObject(c);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		System.out.println();
	}

	/**
	 * Perform the time test
	 * @param classToTest
	 *            class of the {@link Collection} instance to test
	 * @param numbersLeftOperand
	 *            array of integers representing the left operand
	 *            {@link Collection}
	 * @param numbersRightOperand
	 *            array of integers representing the right operand
	 *            {@link Collection}
	 */
	@SuppressWarnings("unchecked")
	private static void testTime(
			Class<?> classToTest, 
			Collection<Integer> numbersLeftOperand, 
			Collection<Integer> numbersRightOperand) {
		// collections used for the test cases
		Collection<Integer>[] cAddAndRemove = new Collection[REPETITIONS];
		Collection<Integer>[] cAddAll = new Collection[REPETITIONS];
		Collection<Integer>[] cRemoveAll = new Collection[REPETITIONS];
		Collection<Integer>[] cRetainAll = new Collection[REPETITIONS];
		Collection<Integer>[] cRighOperand = new Collection[REPETITIONS];

		// class name
//		System.out.println();
//		System.out.println("----------");
//		System.out.println(classToTest.getSimpleName());
//		System.out.println("----------");

		int i;
		
		// CREATION
//		rm.fastStartTimer(null);
		for (i = 0; i < REPETITIONS; i++) {
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
//		endTimer(rm.fastEndTimer("Done"), classToTest, "0) newInstance()", REPETITIONS * 5);
//		for (i = 0; i < REPETITIONS; i++) {
//			rm.useObject(cAddAndRemove[i]);
//			rm.useObject(cAddAll[i]);
//			rm.useObject(cRemoveAll[i]);
//			rm.useObject(cRetainAll[i]);
//			rm.useObject(cRighOperand[i]); 
//		}
		
		// ADDITION
		rm.fastStartTimer(null);
		for (i = 0; i < REPETITIONS; i++) {
			for (Integer x : numbersLeftOperand)
				cAddAndRemove[i].add(x);
			for (Integer x : numbersRightOperand)
				cRighOperand[i].add(x);
			for (Integer x : numbersLeftOperand)
				cAddAll[i].add(x);
			for (Integer x : numbersLeftOperand)
				cRetainAll[i].add(x);
			for (Integer x : numbersLeftOperand)
				cRemoveAll[i].add(x);
		}
		endTimer(rm.fastEndTimer("Done"), classToTest, "1) add()", REPETITIONS * (4 * numbersLeftOperand.size() + numbersRightOperand.size()));
//		System.out.println("Left operand elements: " + cAddAndRemove[0].size());
//		System.out.println("Right operand elements: " + cRighOperand[0].size());
//		for (i = 0; i < REPETITIONS; i++) {
//			rm.useObject(cAddAndRemove[i]);
//			rm.useObject(cRighOperand[i]);
//			rm.useObject(cAddAll[i]);
//			rm.useObject(cRetainAll[i]);
//			rm.useObject(cRemoveAll[i]);
//		}
		
		// REMOVAL
		rm.fastStartTimer(null);
		for (i = 0; i < REPETITIONS; i++) {
			for (Integer x : numbersRightOperand)
				cAddAndRemove[i].remove(x);
		}
		endTimer(rm.fastEndTimer("Done"), classToTest, "2) remove()", numbersRightOperand.size() * REPETITIONS);
//		System.out.println("Left operand elements: " + cAddAndRemove[0].size());
//		for (i = 0; i < REPETITIONS; i++) {
//			rm.useObject(cAddAndRemove[i]);
//		}
		
		// CONTAINS
		rm.fastStartTimer(null);
		for (i = 0; i < REPETITIONS; i++) {
			for (Integer x : numbersRightOperand)
				cAddAll[i].contains(x);
		}
		endTimer(rm.fastEndTimer("Done"), classToTest, "3) contains()", numbersRightOperand.size() * REPETITIONS);
		
		// CONTAINS x LISTS
		if (classToTest.getSimpleName().endsWith("List")) {
			rm.fastStartTimer(null);
			for (i = 0; i < REPETITIONS; i++) {
				for (Integer x : numbersRightOperand)
					Collections.binarySearch((List) (cAddAll[i]), x);
			}
			endTimer(rm.fastEndTimer("Done"), classToTest, "3) contains() - bin", numbersRightOperand.size() * REPETITIONS);
		}
		
		// AND SIZE
		rm.fastStartTimer(null);
		for (i = 0; i < REPETITIONS; i++) {
			cAddAll[i].containsAll(cRighOperand[i]);
		}
		endTimer(rm.fastEndTimer("Done"), classToTest, "4) containsAll()", REPETITIONS);
		
		// UNION
		rm.fastStartTimer(null);
		for (i = 0; i < REPETITIONS; i++) {
			cAddAll[i].addAll(cRighOperand[i]);
		}
		endTimer(rm.fastEndTimer("Done"), classToTest, "5) addAll()", REPETITIONS);
//		System.out.println("Left operand elements: " + cAddAll[0].size());
//		for (i = 0; i < REPETITIONS; i++) {
//			rm.useObject(cAddAll[i]);
//		}
		
		// DIFFERENCE
		rm.fastStartTimer(null);
		for (i = 0; i < REPETITIONS; i++) {
			cRemoveAll[i].removeAll(cRighOperand[i]);
		}
		endTimer(rm.fastEndTimer("Done"), classToTest, "6) removeAll()", REPETITIONS);
//		System.out.println("Left operand elements: " + cRemoveAll[0].size());
//		for (i = 0; i < REPETITIONS; i++) {
//			rm.useObject(cRemoveAll[i]);
//		}
		
		// INTERSECTION
		rm.fastStartTimer(null);
		for (i = 0; i < REPETITIONS; i++) {
			cRetainAll[i].retainAll(cRighOperand[i]);
		}
		endTimer(rm.fastEndTimer("Done"), classToTest, "7) retainAll()", REPETITIONS);
//		System.out.println("Left operand elements: " + cRetainAll[0].size());
//		for (i = 0; i < REPETITIONS; i++) {
//			rm.useObject(cRetainAll[i]);
//		}
	}
	
	/**
	 * Summary information
	 */
	@SuppressWarnings("unused")
	private static void printSummary() {
		System.out.println("\n\n----------\nSUMMARY:\n----------\n");
		for (Entry<String, Map<Class<?>, Double>> e : timeValues.entrySet()) {
			// method name
			System.out.println(e.getKey() + "\n----------");
			for (Entry<Class<?>, Double> m : e.getValue().entrySet()) {
				// class name
				System.out.print(m.getKey().getSimpleName() + ": ");
				for (int i = 0; i < 25 - m.getKey().getSimpleName().length(); i++) 
					System.out.print(" ");
				
				// test values
				System.out.println(format(
						m.getValue(), 
						memoryValues.get(e.getKey()).get(m.getKey())));
			}
			System.out.println();
		}
		System.out.println("\n\n\n");
	}
	
	/**
	 * Summary information
	 */
	private static void printSummary2(int cardinality, double density) {
		for (Entry<String, Map<Class<?>, Double>> e : timeValues.entrySet()) {
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

	private static Collection<Integer> powerLaw(Random rnd, int cardinality, int max) {
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
		
		boolean calcUniform = false;
		boolean calcMarkovian = false;
		boolean calcPowerLaw = false;
		
		if (calcUniform) {
			for (int cardinality : new int[] {10, 10, 10, 100, 1000, 10000, 100000, 1000000}) {
				for (double density = .01; density < 1D; density += .01) {
					System.out.format("UNIFORM --> cardinality: %7d, density: %.2f, words: ", cardinality, density);
					
					Collection<Integer> integers = uniform(rnd, cardinality, density);
					
					FastSet s0 = new FastSet(integers);
					System.out.format("FastSet=%7d", (int) (s0.collectionCompressionRatio() * cardinality));
					
					ConciseSet s1 = new ConciseSet(integers);
					System.out.format(", ConciseSet=%7d", (int) (s1.collectionCompressionRatio() * cardinality));
					
					WAHSet s2 = new WAHSet(integers);
					System.out.format(", WAHSet2=%7d\n", (int) (s2.collectionCompressionRatio() * cardinality));
				}
			}
		}

		if (calcMarkovian) {
			for (int cardinality : new int[] {10, 10, 10, 100, 1000, 10000, 100000, 1000000}) {
				double delta = .00001;
				for (double density = delta; density < 1D; density += delta, delta *= 1.1) {
					System.out.format("MARKOVIAN --> cardinality: %7d, density: %.5f, words: ", cardinality, density);
					
					Collection<Integer> integers = markovian(rnd, cardinality, density);
					
					FastSet s0 = new FastSet(integers);
					System.out.format("FastSet=%7d", (int) (s0.collectionCompressionRatio() * cardinality));
					
					ConciseSet s1 = new ConciseSet(integers);
					System.out.format(", ConciseSet=%7d", (int) (s1.collectionCompressionRatio() * cardinality));
					
					WAHSet s2 = new WAHSet(integers);
					System.out.format(", WAHSet2=%7d\n", (int) (s2.collectionCompressionRatio() * cardinality));
				}
			}
		}

		if (calcPowerLaw) {
			for (int cardinality : new int[] {10, 10, 10, 100, 1000, 10000, 100000, 1000000}) {
				for (int max = (int) (cardinality * 1.2); max < (cardinality << 17); max *= 1.2) {
					System.out.format("POWERLAW --> cardinality: %7d, max: %10d, words: ", cardinality, max);
					
					Collection<Integer> integers = powerLaw(rnd, cardinality, max);
					
					FastSet s0 = new FastSet(integers);
					System.out.format("FastSet=%10d", (int) (s0.collectionCompressionRatio() * cardinality));
					
					ConciseSet s1 = new ConciseSet(integers);
					System.out.format(", ConciseSet=%10d", (int) (s1.collectionCompressionRatio() * cardinality));
					
					WAHSet s2 = new WAHSet(integers);
					System.out.format(", WAHSet2=%10d\n", (int) (s2.collectionCompressionRatio() * cardinality));
				}
			}
		}

		if (calcUniform) {
			for (int cardinality : new int[] {1000, 10000, 100000, 1000000, 10000000}) {
				for (double density : new double[] {.00625, .00625, .0125, .025, .05, .1, .2, .4, .8, .999}) {
					Collection<Integer> integers = uniform(rnd, cardinality, density);
					Collection<Integer> integers2 = uniform(rnd, cardinality, density);
					
					testTime(ArrayList.class, integers, integers2);
					testTime(LinkedList.class, integers, integers2);
					testTime(TreeSet.class, integers, integers2);
					testTime(HashSet.class, integers, integers2);
					testTime(FastSet.class, integers, integers2);
					testTime(ConciseSet.class, integers, integers2);
					testTime(WAHSet.class, integers, integers2);
	
					printSummary2(cardinality, density);
				}
			}
		}

		if (calcMarkovian) {
			for (int cardinality : new int[] {1000, 10000, 100000, 1000000, 10000000}) {
				for (double switchProb : new double[] {.00625, .00625, .0125, .025, .05, .1, .2, .4, .8, .999}) {
					Collection<Integer> integers = markovian(rnd, cardinality, switchProb);
					Collection<Integer> integers2 = markovian(rnd, cardinality, switchProb);
					
					testTime(ArrayList.class, integers, integers2);
					testTime(LinkedList.class, integers, integers2);
					testTime(TreeSet.class, integers, integers2);
					testTime(HashSet.class, integers, integers2);
					testTime(FastSet.class, integers, integers2);
					testTime(ConciseSet.class, integers, integers2);
					testTime(WAHSet.class, integers, integers2);
	
					printSummary2(cardinality, switchProb);
				}
			}
		}

		calcPowerLaw = true;
		if (calcPowerLaw) {
//			for (int cardinality : new int[] {1000, 10000, 100000, 1000000, 10000000}) {
			for (int cardinality : new int[] {1000}) {
				for (double max : new double[] {1.2, 1.2, 10, 100, 1000, 10000, 100000}) {
					Collection<Integer> integers = powerLaw(rnd, cardinality, (int) (cardinality * max));
					Collection<Integer> integers2 = powerLaw(rnd, cardinality, (int) (cardinality * max));
					
//					testTime(ArrayList.class, integers, integers2);
//					testTime(LinkedList.class, integers, integers2);
//					testTime(TreeSet.class, integers, integers2);
//					testTime(HashSet.class, integers, integers2);
					testTime(FastSet.class, integers, integers2);
//					testTime(ConciseSet.class, integers, integers2);
//					testTime(WAHSet.class, integers, integers2);

					printSummary2(cardinality, max);
				}
			}
		}
	}
}
