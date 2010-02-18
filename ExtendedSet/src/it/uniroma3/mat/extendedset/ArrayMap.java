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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A {@link Map} backed by an array, where keys are the indices of the array,
 * and values are the elements of the array.
 * <p>
 * Modifications to the map (i.e., through {@link #put(Integer, Object)} and
 * {@link java.util.Map.Entry#setValue(Object)}) are reflected to the original array.
 * However, the map has a fixed length, that is the length of the array.
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @param <T>
 *            the type of elements represented by columns
 */
public class ArrayMap<T> extends AbstractMap<Integer, T> {
	/** array backed by this map */
	private final T[] array;
	
	/** {@link Set} instance to iterate over #array */
	private Set<Entry<Integer, T>> entrySet;
	
	/** first index of the map */
	final int indexShift;
	
	/**
	 * Entry of the map
	 */
	private class SimpleEntry implements Entry<Integer, T> {
		/** index of {@link ArrayMap#array} */
		final Integer actualIndex; 
		
		/**
		 * Creates an entry
		 * 
		 * @param index
		 *            index of {@link ArrayMap#array}
		 */
		private SimpleEntry(Integer index) {
			this.actualIndex = index;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public Integer getKey() {
			return actualIndex + indexShift;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public T getValue() {
			return array[actualIndex];
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public T setValue(T value) {
			T old = array[actualIndex];
			array[actualIndex] = value;
			return old;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return (actualIndex + indexShift) + "=" + array[actualIndex];
		}
	}
	
	/**
	 * Initializes the map
	 * 
	 * @param array
	 *            array to manipulate
	 * @param indexShift
	 *            first index of the map
	 */
	ArrayMap(T[] array, int indexShift) {
		this.array = array;
		this.indexShift = indexShift;
		entrySet = null;
	}
	
	/**
	 * Initializes the map
	 * 
	 * @param array
	 *            array to manipulate
	 */
	ArrayMap(T[] array) {
		this(array, 0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Entry<Integer, T>> entrySet() {
		if (entrySet == null) {
			// create an entry for each element
			final List<SimpleEntry> entries = new ArrayList<SimpleEntry>(array.length);
			for (int i = 0; i < array.length; i++) 
				entries.add(new SimpleEntry(i));
			
			// create the Set instance
			entrySet = new AbstractSet<Entry<Integer, T>>() {
				@Override
				public Iterator<Entry<Integer, T>> iterator() {
					return new Iterator<Entry<Integer, T>>() {
						int curr = 0;
						
						@Override
						public boolean hasNext() {
							return curr < entries.size();
						}
						
						@Override
						public Entry<Integer, T> next() {
							if (!hasNext())
								throw new NoSuchElementException();
							return entries.get(curr++);
						}
						
						@Override
						public void remove() {
							throw new IllegalArgumentException();
						}
					};
				}
				
				@Override
				public int size() {
					return entries.size();
				}
			};
		}
		return entrySet;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return array.length;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsKey(Object key) {
		Integer index = (Integer) key - indexShift;
		return index.compareTo(0) >= 0 && index.compareTo(array.length) < 0;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public T get(Object key) {
		return array[(Integer) key - indexShift];
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public T put(Integer key, T value) {
		int actualIndex = key - indexShift;
		T old = array[actualIndex];
		array[actualIndex] = value;
		return old;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return Arrays.hashCode(array);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ArrayMap<?>))
			return false;
		return Arrays.equals(array, ((ArrayMap<?>) obj).array);
	}
	
	/**
	 * Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ArrayMap<String> am = new ArrayMap<String>(new String[] {"Three", "Four", "Five"}, 3);
		System.out.println(am);
		am.put(5, "FIVE");
		System.out.println(am);
		System.out.println(am.get(5));
		System.out.println(am.containsKey(2));
		System.out.println(am.containsKey(3));
		System.out.println(am.containsValue("THREE"));
		System.out.println(am.keySet());
		System.out.println(am.values());
	}
}
