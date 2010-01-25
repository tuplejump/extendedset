package it.uniroma3.mat.extendedset.test;

import it.uniroma3.mat.extendedset.ConciseSet;
import it.uniroma3.mat.extendedset.FastSet;

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
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * 
 * @author Alessandro Colantonio
 *
 */
public class Performance {
	// number of times to repeat each test
	private final static int REPETITIONS = 2;

	// time and memory measurements
	//TODO personalizzabile secondo le esigenze della classe!
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
	 * Set initial values
	 */
	private static void startTimer() {
		rm.startTimer();
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
	private static void endTimer(Class<?> c, String name, long div) {
		long[] timeAndMemory = rm.endTimer();
		
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
		System.out.print(name + "... ");
		for (int i = 0; i < 30 - name.length(); i++) 
			System.out.print(" ");
		System.out.println(format(t, m));
	}

	/**
	 * Perform the memory test
	 * 
	 * @param classToTest
	 *            class of the {@link Collection} instance to test
	 */
	@SuppressWarnings("unchecked")
	private static void testMemory(Class<?> classToTest, Collection<Integer> numbers) {
		// print the class being tested
		System.out.println(classToTest.getSimpleName());

		try {
			Collection<Integer> c;

			// new instance
			startTimer();
			c = (Collection)classToTest.newInstance();
			endTimer(classToTest, "a) creating empty instance", 1);
			rm.useObject(c);

			// elements
			startTimer();
			c.addAll(numbers);
			endTimer(classToTest, "b) adding " + numbers.size() + " elements", numbers.size());
			rm.useObject(c);

			/*
			 * NOTE: by repeating the same things we take into account caching...
			 */ 
			
			startTimer();
			c = (Collection)classToTest.newInstance();
			endTimer(classToTest, "c) creating empty instance", 1);
			rm.useObject(c);

			startTimer();
			c.addAll(numbers);
			endTimer(classToTest, "d) adding " + numbers.size() + " elements", numbers.size());
			rm.useObject(c);

			startTimer();
			c = (Collection)classToTest.newInstance();
			endTimer(classToTest, "e) creating empty instance", 1);
			rm.useObject(c);

			startTimer();
			c.addAll(numbers);
			endTimer(classToTest, "f) adding " + numbers.size() + " elements", numbers.size());
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
			List<Integer> numbersLeftOperand, 
			List<Integer> numbersRightOperand) {
		// collections used for the test cases
		Collection<Integer>[] cAddAndRemove = new Collection[REPETITIONS];
		Collection<Integer>[] cAddAll = new Collection[REPETITIONS];
		Collection<Integer>[] cRemoveAll = new Collection[REPETITIONS];
		Collection<Integer>[] cRetainAll = new Collection[REPETITIONS];
		Collection<Integer>[] cRighOperand = new Collection[REPETITIONS];

		// class name
		System.out.println();
		System.out.println("----------");
		System.out.println(classToTest.getSimpleName());
		System.out.println("----------");

		int i;
		int j;
		
		// CREATION
		startTimer();
		for (i = 0; i < REPETITIONS; i++) {
			try {
				cAddAndRemove[i] = (Collection)classToTest.newInstance();
				cAddAll[i] = (Collection)classToTest.newInstance();
				cRemoveAll[i] = (Collection)classToTest.newInstance();
				cRetainAll[i] = (Collection)classToTest.newInstance();
				cRighOperand[i] = (Collection)classToTest.newInstance(); 
			} catch (Exception e) {
				throw new RuntimeException(e);
			} 
		}
		endTimer(classToTest, "0) newInstance()", REPETITIONS * 5);
		for (i = 0; i < REPETITIONS; i++) {
			rm.useObject(cAddAndRemove[i]);
			rm.useObject(cAddAll[i]);
			rm.useObject(cRemoveAll[i]);
			rm.useObject(cRetainAll[i]);
			rm.useObject(cRighOperand[i]); 
		}
		
		// ADDITION
		startTimer();
		for (i = 0; i < REPETITIONS; i++) {
			for (j = 0; j < numbersLeftOperand.size(); j++)
				cAddAndRemove[i].add(numbersLeftOperand.get(j));
			for (j = 0; j < numbersRightOperand.size(); j++)
				cRighOperand[i].add(numbersRightOperand.get(j));
			for (j = 0; j < numbersLeftOperand.size(); j++)
				cAddAll[i].add(numbersLeftOperand.get(j));
			for (j = 0; j < numbersLeftOperand.size(); j++)
				cRetainAll[i].add(numbersLeftOperand.get(j));
			for (j = 0; j < numbersLeftOperand.size(); j++)
				cRemoveAll[i].add(numbersLeftOperand.get(j));
		}
		endTimer(classToTest, "1) add()", REPETITIONS * 5);
		System.out.println("Left operand elements: " + cAddAndRemove[0].size());
		System.out.println("Right operand elements: " + cRighOperand[0].size());
		for (i = 0; i < REPETITIONS; i++) {
			rm.useObject(cAddAndRemove[i]);
			rm.useObject(cRighOperand[i]);
			rm.useObject(cAddAll[i]);
			rm.useObject(cRetainAll[i]);
			rm.useObject(cRemoveAll[i]);
		}
		
		// REMOVAL
		startTimer();
		for (i = 0; i < REPETITIONS; i++) {
			for (j = 0; j < numbersRightOperand.size(); j++)
				cAddAndRemove[i].remove(numbersRightOperand.get(j));
		}
		endTimer(classToTest, "2) remove()", numbersRightOperand.size() * REPETITIONS);
		System.out.println("Left operand elements: " + cAddAndRemove[0].size());
		for (i = 0; i < REPETITIONS; i++) {
			rm.useObject(cAddAndRemove[i]);
		}
		
		// CONTAINS
		startTimer();
		for (i = 0; i < REPETITIONS; i++) {
			for (j = 0; j < numbersRightOperand.size(); j++)
				cAddAll[i].contains(numbersRightOperand.get(j));
		}
		endTimer(classToTest, "3) contains()", numbersRightOperand.size() * REPETITIONS);
		
		// AND SIZE
		startTimer();
		for (i = 0; i < REPETITIONS; i++) {
			cAddAll[i].containsAll(cRighOperand[i]);
		}
		endTimer(classToTest, "4) containsAll()", REPETITIONS);
		
		// UNION
		startTimer();
		for (i = 0; i < REPETITIONS; i++) {
			cAddAll[i].addAll(cRighOperand[i]);
		}
		endTimer(classToTest, "5) addAll()", REPETITIONS);
		System.out.println("Left operand elements: " + cAddAll[0].size());
		for (i = 0; i < REPETITIONS; i++) {
			rm.useObject(cAddAll[i]);
		}
		
		// DIFFERENCE
		startTimer();
		for (i = 0; i < REPETITIONS; i++) {
			cRemoveAll[i].retainAll(cRighOperand[i]);
		}
		endTimer(classToTest, "6) removeAll()", REPETITIONS);
		System.out.println("Left operand elements: " + cRemoveAll[0].size());
		for (i = 0; i < REPETITIONS; i++) {
			rm.useObject(cRemoveAll[i]);
		}
		
		// INTERSECTION
		startTimer();
		for (i = 0; i < REPETITIONS; i++) {
			cRetainAll[i].retainAll(cRighOperand[i]);
		}
		endTimer(classToTest, "7) retainAll()", REPETITIONS);
		System.out.println("Left operand elements: " + cRetainAll[0].size());
		for (i = 0; i < REPETITIONS; i++) {
			rm.useObject(cRetainAll[i]);
		}
	}
	/**
	 * Summary information
	 */
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
	 * TEST
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * TODO usare il seguente approccio per generare lunghe sequenze di bit
		 * tutti a 1 o a 0:
		 * 
		 * int size = 1 + rnd.nextInt(10000); int n = size; for (int j = 0; j <
		 * size; j++) { n = Math.abs(rnd.nextDouble() > 0.001D ? (n - 1) :
		 * rnd.nextInt(size)); collection.add(n); }
		 * 
		 * oppure, ancora meglio, usare il nuovo metodo ExtendedSet<T>.fill(T, T), 
		 * magari generando patterns secondo powerlaw, omogeneo, gaussiana, ecc.
		 */
		
		Random rnd = new Random();

		// random number range (from 0 to X)
		int[] maxRandomNumber = {
				10, 100, 1000, 10000, 100000, 1000000,
				1000, 1000, 1000, 1000, 1000, 
				10, 100, 10000, 100000, 1000000};
		
		// set size (number of random trials)
		int[] setSize = {
				10, 100, 1000, 10000, 100000, 1000000,
				10, 100, 10000, 100000, 1000000, 
				1000, 1000, 1000, 1000, 1000};

		/*
		 * Memory test
		 */ 
		final int NUM_COUNT = 100000;
		HashSet<Integer> numbers = new HashSet<Integer>();
		while (numbers.size() < NUM_COUNT) 
			numbers.add(new Integer(rnd.nextInt(NUM_COUNT * 1000 + 1)));
		rm.useObject(numbers);
		
		testMemory(ArrayList.class, numbers);
		testMemory(LinkedList.class, numbers);
		testMemory(TreeSet.class, numbers);
		testMemory(HashSet.class, numbers);
		testMemory(FastSet.class, numbers);
		testMemory(WAHSet.class, numbers);
		testMemory(ConciseSet.class, numbers);

		printSummary();

		
		/* 
		 * Timing test
		 */
		for (int x = 0; x < maxRandomNumber.length; x++) {
			System.out.println("\n\n**********");
			System.out.println("maxRandomNumber: " + maxRandomNumber[x]);
			System.out.println("approxSetSize: " + setSize[x]);
			System.out.println("**********\n");
		
			// generate random numbers
			// NOTE: we used Integer in order to avoid "new Integer()" at each
			// Collection.add() call
			ArrayList<Integer> randomNumbers1 = new ArrayList<Integer>(setSize[x]);
			ArrayList<Integer> randomNumbers2 = new ArrayList<Integer>(setSize[x]);
			for (int i = 0; i < setSize[x]; i++) 
				randomNumbers1.add(new Integer(rnd.nextInt(maxRandomNumber[x])));
			for (int i = 0; i < setSize[x]; i++) 
				randomNumbers2.add(new Integer(rnd.nextInt(maxRandomNumber[x])));
			rm.useObject(randomNumbers1);
			rm.useObject(randomNumbers2);
			
			// test all collections
			testTime(ArrayList.class, randomNumbers1, randomNumbers2);
			testTime(LinkedList.class, randomNumbers1, randomNumbers2);
			testTime(TreeSet.class, randomNumbers1, randomNumbers2);
			testTime(HashSet.class, randomNumbers1, randomNumbers2);
			testTime(FastSet.class, randomNumbers1, randomNumbers2);
			testTime(WAHSet.class, randomNumbers1, randomNumbers2);
			testTime(ConciseSet.class, randomNumbers1, randomNumbers2);
	
			printSummary();
			
			System.out.println("\n\n\n----------\nSORTED:\n----------\n\n");
			
			// test when numbers are sorted
			Collections.sort(randomNumbers1);
			Collections.sort(randomNumbers2);
			
			testTime(ArrayList.class, randomNumbers1, randomNumbers2);
			testTime(LinkedList.class, randomNumbers1, randomNumbers2);
			testTime(TreeSet.class, randomNumbers1, randomNumbers2);
			testTime(HashSet.class, randomNumbers1, randomNumbers2);
			testTime(FastSet.class, randomNumbers1, randomNumbers2);
			testTime(WAHSet.class, randomNumbers1, randomNumbers2);
			testTime(ConciseSet.class, randomNumbers1, randomNumbers2);

			printSummary();
		}
	}
}
