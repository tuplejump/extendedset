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

package it.uniroma3.mat.extendedset.transactions;

/**
 * A class for representing a single transaction-item relationship. This class
 * is mainly used in {@link PairSet} to iterate over the cells of a
 * binary matrix.
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @param <T>
 *            transaction type
 * @param <I>
 *            item type
 * @see PairSet
 */
public class Pair<T, I> {
	/** the transaction */
	public final T transaction;
	
	/** the item */
	public final I item;

	/**
	 * Creates a new transaction-item pair
	 * 
	 * @param transaction
	 * @param item
	 */
	public Pair(T transaction, I item) {
		this.transaction = transaction;
		this.item = item;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		// 524287 * i = (i << 19) - i, where 524287 is prime.
		// This hash function avoids transactions and items to overlap,
		// since "item" can often stay in 32 - 19 = 13 bits. Therefore, it is
		// better than multiplying by 31.
		final int hi = item.hashCode();
		final int ht = transaction.hashCode();
		return (hi << 19) - hi + ht;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (!(obj instanceof Pair<?, ?>))
			return false;
		@SuppressWarnings("unchecked")
		Pair<T, I> other = (Pair<T, I>) obj;
		return transaction.equals(other.transaction) && item.equals(other.item);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "(" + transaction + ", " + item + ")";
	}
}
