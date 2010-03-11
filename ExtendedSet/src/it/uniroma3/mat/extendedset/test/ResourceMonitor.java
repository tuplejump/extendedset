package it.uniroma3.mat.extendedset.test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Class for time and memory measurement
 * <p>
 * There are several problems with this approach, which results in very
 * unpredictable results:
 * <ul>
 * <li> Warm-up the JVM:
 * <ol>
 * <li> Execute the task once to load all classes.
 * <li> Execute the task enough times to ensure that its steady-state execution
 * profile has emerged.
 * <li> Execute the task some more times to obtain an estimate of its execution
 * time. Note that methods should be called 1,000,000 times before they get
 * fully JIT compiled into optimal form.
 * <li> Use the previous step to calculate <code>n</code>, the number of task
 * executions whose cumulative execution time is sufficiently large.
 * <li> Measure the overall execution time <code>t</code> of <code>n</code>
 * more calls of task.
 * <li> Estimate the execution time as <code>t/n</code>.
 * </ol>
 * <li> Run every micro-benchmark in a new JVM-instance (call for every
 * benchmark new Java) otherwise optimization effects of the JVM can influence
 * later running tests.
 * <li> Don't execute things that aren't executed in the warm-up phase (as this
 * could trigger class-load and recompilation).
 * <li> For a better comparison with other algorithms, you should measure the
 * RMS for each run.
 * <li> You should start with enough memory, so specify e.g.
 * <tt>-Xms1024m -Xmx1024m</tt>. Because JVM memory allocation could get time
 * consuming.
 * </ul>
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 */
//TODO: differenziare rispetto alla versione di algorithms
public class ResourceMonitor {
	/*
	 * This method offers the tantalizing possibility of measuring not the
	 * elapsed ("wall clock") time, but the actual CPU time used by the current
	 * thread, which is less than or equal to elapsed time.
	 * 
	 * NOTE: this doesn't include Thread.sleep(), and it doesn't include the
	 * time used by threads that were created by the measured code!!!
	 */
	private final ThreadMXBean mx;
	
	// clock and memory allocation of the last "startTimer" call, in nanoseconds
	private long lastExecTime = -1;
	private long lastMemSize = -1;

	// says if messages should be printed
	private boolean printMessages;

	// very useless result value used by "useCollection" in order to use in some
	// way the collection's elements
	private static int res = 0;

	// this collection avoids that objects are garbage collected
	private final static Collection<Object> garbage = new ArrayList<Object>();
	
	/**
	 * Call garbage collector and select the simplest time measurement method
	 */
	public ResourceMonitor() {
		this(false);
	}

	/**
	 * Call garbage collector and specify the time measurement method
	 * @param threadMeasurement 
	 */
	public ResourceMonitor(boolean threadMeasurement) {
		collectGarbage();
		if (threadMeasurement) {
			mx = ManagementFactory.getThreadMXBean();
			if (!mx.isCurrentThreadCpuTimeSupported()) 
				throw new RuntimeException("Time measurement not supported!!!");
			mx.setThreadCpuTimeEnabled(true);
		} else {
			mx = null;
		}
	}
	/**
	 * Force garbage collection
	 */
	public void collectGarbage() {
		try {
			for (int i = 0; i < 2; i++) {
				System.gc();
				TimeUnit.MILLISECONDS.sleep(50);
				System.runFinalization();
				TimeUnit.MILLISECONDS.sleep(50);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Start memory and time measurement
	 */
	public void startTimer() {
		startTimer(null);
	}

	/**
	 * Start memory and time measurement
	 * 
	 * @param msg
	 *            message to print
	 */
	public void startTimer(String msg) {
		collectGarbage();
		lastMemSize = Runtime.getRuntime().totalMemory()
				- Runtime.getRuntime().freeMemory();
		
		printMessages = msg != null;

		if (printMessages) 
			System.out.format("*** Timer started!  %s...\n", msg);

		lastExecTime = mx == null ? System.nanoTime() : mx.getCurrentThreadCpuTime();
	}

	/**
	 * Stop memory and time measurement
	 * 
	 * @param msg
	 *            message to print (i.e., results)
	 * @return an array of two doubles, where the first value is the time
	 *         measurement (nanoseconds) while the second value is the memory
	 *         (bytes) measurement
	 */
	public long[] endTimer(String msg) {
		final long t = (mx == null ? System.nanoTime() : mx.getCurrentThreadCpuTime()) - lastExecTime;
		 
		collectGarbage();
		long m = Runtime.getRuntime().totalMemory()
				- Runtime.getRuntime().freeMemory() - lastMemSize;

		if (printMessages) {
			System.out.print("*** Timer stopped!  ");
			System.out.println(msg);
			System.out.format("*** Time:   %d s and %d ns\n", t / 1000000000L, t % 1000000000L);
			System.out.format(Locale.ENGLISH, "*** Memory: %f Mbytes\n\n", (double) m / (1024 * 1024));
		}

		return new long[] { t, m };
	}

	/**
	 * Stop memory and time measurement, with default message
	 * @return memory and time measurement
	 */
	public long[] endTimer() {
		return endTimer("Done");
	}

	/**
	 * Avoid that the compiler remove unused objects
	 * 
	 * @param obj
	 *            object that should not be garbage collected
	 */
	public void useObject(Object obj) {
		if (obj == null)
			return;

		garbage.add(obj);
		
		if (obj instanceof Collection<?>) {
			for (Object item : (Collection<?>) obj)
				useObject(item);
		} else if (obj instanceof Object[]) {
			for (Object item : (Object[]) obj)
				useObject(item);
		} else {
			res += obj.hashCode();
		}
	}
}
