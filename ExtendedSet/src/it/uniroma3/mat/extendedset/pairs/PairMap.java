package it.uniroma3.mat.extendedset.pairs;

import it.uniroma3.mat.extendedset.LongSet.ExtendedLongIterator;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author colantonio
 * 
 * @param <T>
 * @param <I>
 * @param <V>
 */
public class PairMap<T, I, V> extends AbstractMap<Pair<T, I>, V> implements Serializable {
	/** generated serial ID */
	private static final long serialVersionUID = 4699094886888004702L;

	private final PairSet<T, I> keys;
	private final Map<Long, V> values;

	/**
	 * Creates an empty map
	 * 
	 * @param keySet
	 *            {@link PairSet} instance internally used to store indices. If
	 *            not empty, {@link #get(Object)} will return <code>null</code>
	 *            for each existing pair if we do not also put a value.
	 */
	public PairMap(PairSet<T, I> keySet) {
		keys = keySet;
		values = new HashMap<Long, V>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() {
		keys.clear();
		values.clear();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsKey(Object key) {
		return keys.contains(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean containsValue(Object value) {
		return values.containsValue(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		if (key == null || !(key instanceof Pair<?, ?>))
			return null;
		return values.get(Long.valueOf(keys.pairToIndex((Pair<T, I>) key)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEmpty() {
		return keys.isEmpty();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public V put(Pair<T, I> key, V value) {
		keys.add(key);
		return values.put(Long.valueOf(keys.pairToIndex(key)), value);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
		if (key == null || !(key instanceof Pair<?, ?>))
			return null;
		keys.remove(key);
		return values.remove(Long.valueOf(keys.pairToIndex((Pair<T, I>) key)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int size() {
		return keys.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Pair<T, I>> keySet() {
		return new AbstractSet<Pair<T, I>>() {
			@Override
			public boolean add(Pair<T, I> e) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void clear() {
				PairMap.this.clear();
			}

			@Override
			public boolean contains(Object o) {
				return keys.contains(o);
			}

			@Override
			public boolean containsAll(Collection<?> c) {
				return keys.containsAll(c);
			}

			@Override
			public boolean isEmpty() {
				return keys.isEmpty();
			}

			@Override
			public Iterator<Pair<T, I>> iterator() {
				return new Iterator<Pair<T,I>>() {
					Iterator<Pair<T, I>> itr = keys.iterator();
					@Override
					public boolean hasNext() {
						return itr.hasNext();
					}

					@Override
					public Pair<T, I> next() {
						return itr.next();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public boolean remove(Object o) {
				if (o == null || !(o instanceof Pair<?, ?>))
					return false;
				@SuppressWarnings("unchecked")
				Pair<T, I> p = (Pair<T, I>) o;
				if (keys.remove(o)) {
					values.remove(Long.valueOf(keys.pairToIndex(p)));
					return true;
				}
				return false;
			}

			@Override
			public int size() {
				return keys.size();
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<V> values() {
		return new AbstractCollection<V>() {

			@Override
			public boolean add(V e) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void clear() {
				PairMap.this.clear();
			}

			@Override
			public boolean contains(Object o) {
				return values.containsValue(o);
			}

			@Override
			public boolean isEmpty() {
				return keys.isEmpty();
			}

			@Override
			public Iterator<V> iterator() {
				return new Iterator<V>() {
					Iterator<V> itr = values.values().iterator();

					@Override
					public boolean hasNext() {
						return itr.hasNext();
					}

					@Override
					public V next() {
						return itr.next();
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public boolean remove(Object o) {
				throw new UnsupportedOperationException("TODO");
			}

			@Override
			public int size() {
				return values.size();
			}
		};
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Entry<Pair<T, I>, V>> entrySet() {
		return new AbstractSet<Entry<Pair<T, I>, V>>() {
			@Override
			public boolean add(Entry<Pair<T, I>, V> e) {
				V res = PairMap.this.put(e.getKey(), e.getValue());
				return res != e.getValue();
			}

			@Override
			public void clear() {
				PairMap.this.clear();
			}

			@Override
			public boolean contains(Object o) {
				return o != null
						&& o instanceof Entry<?, ?>
						&& PairMap.this.containsKey(((Entry<?, ?>) o).getKey())
						&& PairMap.this.containsValue(((Entry<?, ?>) o).getValue());
			}

			@Override
			public boolean isEmpty() {
				return keys.isEmpty();
			}

			@Override
			public Iterator<Entry<Pair<T, I>, V>> iterator() {
				return new Iterator<Entry<Pair<T, I>, V>>() {
					final ExtendedLongIterator itr = keys.indices().longIterator();

					@Override
					public boolean hasNext() {
						return itr.hasNext();
					}

					@Override
					public Entry<Pair<T, I>, V> next() {
						final long index = itr.next();
						final Pair<T, I> key = keys.indexToPair(index);
						final V value = values.get(Long.valueOf(index));

						return new Entry<Pair<T, I>, V>() {
							@Override
							public Pair<T, I> getKey() {
								return key;
							}

							@Override
							public V getValue() {
								return value;
							}

							@Override
							public V setValue(@SuppressWarnings("hiding") V value) {
								return values.put(Long.valueOf(index), value);
							}
							
							@Override
							public String toString() {
								return "{" + key + "=" + value + "}";
							}
						};
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

			@Override
			public boolean remove(Object o) {
				if (o == null || !(o instanceof Entry<?, ?>))
					return false;
				@SuppressWarnings("unchecked")
				Entry<Pair<T, I>, V> e = (Entry<Pair<T, I>, V>) o;
				boolean res = keys.contains(e.getKey())
						&& values.values().contains(e.getValue());
				if (res) {
					keys.remove(e.getKey());
					values.remove(Long.valueOf(keys.pairToIndex(e.getKey())));
				}
				return res;
			}

			@Override
			public int size() {
				return keys.size();
			}
		};
	}
}
