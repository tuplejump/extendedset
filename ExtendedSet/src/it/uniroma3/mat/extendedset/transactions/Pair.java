package it.uniroma3.mat.extendedset.transactions;

/**
 * A class for representing a single transaction-item relationship. This class
 * is mainly used in {@link TransactionSet} to iterate over the cells of a
 * binary matrix.
 * 
 * @author Alessandro Colantonio
 * @version $Id$
 * 
 * @param <T>
 *            transaction type
 * @param <I>
 *            item type
 * @see TransactionSet
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
