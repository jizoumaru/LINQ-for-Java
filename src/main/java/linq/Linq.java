package linq;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface Linq<T> extends Iterable<T> {
	public abstract static class LinqIterator<T> implements Iterator<T> {
		private Nullable<T> value = null;

		protected abstract Nullable<T> get();

		@Override
		public boolean hasNext() {
			if (value == null) {
				value = get();
			}
			return value.exists();
		}

		@Override
		public T next() {
			if (hasNext()) {
				var ret = value;
				value = null;
				return ret.value();
			} else {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			throw new RuntimeException("not supported");
		}
	}

	public static class Nullable<T> {
		public static <T> Nullable<T> none() {
			return new Nullable<>(null, false);
		}

		public static <T> Nullable<T> of(T value) {
			return new Nullable<>(value, true);
		}

		private final T value;
		private final boolean exist;

		private Nullable(T value, boolean exist) {
			this.value = value;
			this.exist = exist;
		}

		public boolean exists() {
			return exist;
		}

		public T value() {
			if (exist) {
				return value;
			} else {
				throw new NoSuchElementException();
			}
		}
	}

	static class OrderingLinq<T> implements Linq<T> {
		private final Linq<T> linq;
		private final Comparator<T> cmp;

		public OrderingLinq(Linq<T> linq, Comparator<T> cmp) {
			this.linq = linq;
			this.cmp = cmp;
		}

		@Override
		public LinqIterator<T> iterator() {
			var list = linq.toList();
			Collections.sort(list, cmp);
			return Linq.from(list).iterator();
		}

		public <U extends Comparable<U>> Linq<T> thenBy(final Function<T, U> keySelector) {
			return new OrderingLinq<T>(this, (l, r) -> {
				var c = OrderingLinq.this.cmp.compare(l, r);
				if (c == 0) {
					c = keySelector.apply(l).compareTo(keySelector.apply(r));
				}
				return c;
			});
		}

		public <U extends Comparable<U>> Linq<T> thenByDescending(final Function<T, U> keySelector) {
			return new OrderingLinq<T>(this, (l, r) -> {
				var c = OrderingLinq.this.cmp.compare(l, r);
				if (c == 0) {
					c = keySelector.apply(r).compareTo(keySelector.apply(l));
				}
				return c;
			});
		}

	}

	public interface Predicate<T> {
		boolean test(T value);
	}

	public static class Tuple2<T1, T2> {
		public final T1 value1;
		public final T2 value2;

		public Tuple2(T1 value1, T2 value2) {
			this.value1 = value1;
			this.value2 = value2;
		}

		@Override
		public int hashCode() {
			return Objects.hash(value1, value2);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			Tuple2<T1, T2> other = (Tuple2<T1, T2>) obj;
			return Objects.equals(value1, other.value1) && Objects.equals(value2, other.value2);
		}

		@Override
		public String toString() {
			return "Tuple2 [value1=" + value1 + ", value2=" + value2 + "]";
		}
	}

	public static <T> Linq<T> empty() {
		return () -> new LinqIterator<T>() {
			@Override
			protected Nullable<T> get() {
				return Nullable.none();
			}
		};
	}

	public static <T> Linq<T> from(final Iterable<T> iterable) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it = iterable.iterator();

			@Override
			protected Nullable<T> get() {
				if (it.hasNext()) {
					return Nullable.of(it.next());
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public static Linq<Integer> range(final int start, final int count) {
		return () -> new LinqIterator<Integer>() {
			int i = 0;

			@Override
			protected Nullable<Integer> get() {
				if (i < count) {
					return Nullable.of(start + i++);
				}
				return Nullable.none();
			}
		};
	}

	public static <T> Linq<T> repeat(final T value, final int count) {
		return () -> new LinqIterator<T>() {
			int i = 0;

			@Override
			protected Nullable<T> get() {
				if (i < count) {
					i++;
					return Nullable.of(value);
				}
				return Nullable.none();
			}
		};
	}

	public default Nullable<T> aggregate(BiFunction<T, T, T> func) {
		var it = iterator();
		if (it.hasNext()) {
			var l = it.next();
			while (it.hasNext()) {
				l = func.apply(l, it.next());
			}
			return Nullable.of(l);
		} else {
			return Nullable.none();
		}
	}

	public default T aggregate(T seed, BiFunction<T, T, T> func) {
		var it = iterator();
		var l = seed;
		while (it.hasNext()) {
			l = func.apply(l, it.next());
		}
		return l;
	}

	public default boolean all(Predicate<T> pred) {
		for (var v : this) {
			if (!pred.test(v)) {
				return false;
			}
		}
		return true;
	}

	public default boolean any(Predicate<T> pred) {
		for (var v : this) {
			if (pred.test(v)) {
				return true;
			}
		}
		return false;
	}

	public default Linq<T> append(final T value) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it = Linq.this.iterator();
			boolean end = false;

			@Override
			protected Nullable<T> get() {
				if (it.hasNext()) {
					return Nullable.of(it.next());
				} else {
					if (!end) {
						end = true;
						return Nullable.of(value);
					} else {
						return Nullable.none();
					}
				}
			}
		};
	}

	public default Nullable<Long> average(Function<T, Long> func) {
		var s = select(func).aggregate(0L, (l, r) -> l + r);
		var c = count();

		if (c == 0) {
			return Nullable.none();
		} else {
			return Nullable.of(s / c);
		}
	}

	public default <U> Linq<U> cast() {
		return () -> new LinqIterator<U>() {
			Iterator<T> it = Linq.this.iterator();

			@SuppressWarnings("unchecked")
			@Override
			protected Nullable<U> get() {
				if (it.hasNext()) {
					return Nullable.of((U) it.next());
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public default Linq<List<T>> chunk(final int size) {
		return () -> new LinqIterator<List<T>>() {
			LinqIterator<T> it = Linq.this.iterator();

			@Override
			protected Nullable<List<T>> get() {
				if (it.hasNext()) {
					var list = new ArrayList<T>();
					for (var i = 0; i < size; i++) {
						list.add(it.next());
						if (!it.hasNext()) {
							break;
						}
					}
					return Nullable.of(list);
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public default Linq<T> concat(final Linq<T> right) {
		return () -> new LinqIterator<T>() {
			Iterator<T> l = Linq.this.iterator();
			Iterator<T> r = right.iterator();

			@Override
			protected Nullable<T> get() {
				if (l.hasNext()) {
					return Nullable.of(l.next());
				} else {
					if (r.hasNext()) {
						return Nullable.of(r.next());
					} else {
						return Nullable.none();
					}
				}
			}
		};
	}

	public default boolean contains(T target) {
		for (var v : this) {
			if (v.equals(target)) {
				return true;
			}
		}
		return false;
	}

	public default long count() {
		var c = 0L;
		var it = iterator();
		while (it.hasNext()) {
			it.next();
			c++;
		}
		return c;
	}

	public default Linq<T> defaultIfEmpty(final T defaultValue) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it = Linq.this.iterator();
			Boolean isIt;
			boolean end;

			@Override
			protected Nullable<T> get() {
				if (isIt == null) {
					isIt = it.hasNext();
				}
				if (isIt) {
					if (it.hasNext()) {
						return Nullable.of(it.next());
					} else {
						return Nullable.none();
					}
				} else {
					if (!end) {
						end = true;
						return Nullable.of(defaultValue);
					} else {
						return Nullable.none();
					}
				}
			}
		};
	}

	public default Linq<T> distinct() {
		return () -> new LinqIterator<T>() {
			Iterator<T> it;

			@Override
			protected Nullable<T> get() {
				if (it == null) {
					var set = new LinkedHashSet<T>();
					for (var v : Linq.this) {
						if (!set.contains(v)) {
							set.add(v);
						}
					}
					it = set.iterator();
				}
				if (it.hasNext()) {
					return Nullable.of(it.next());
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public default <TKey> Linq<T> distinctBy(final Function<T, TKey> func) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it;

			@Override
			protected Nullable<T> get() {
				if (it == null) {
					var map = new LinkedHashMap<TKey, T>();
					for (var v : Linq.this) {
						var k = func.apply(v);
						if (!map.containsKey(k)) {
							map.put(k, v);
						}
					}
					it = map.values().iterator();
				}
				if (it.hasNext()) {
					return Nullable.of(it.next());
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public default Nullable<T> elementAt(long index) {
		var c = 0L;
		for (var v : this) {
			if (c == index) {
				return Nullable.of(v);
			}
			c++;
		}
		return Nullable.none();
	}

	public default T elementAt(long index, T defaultValue) {
		var c = 0L;
		for (var v : this) {
			if (c == index) {
				return v;
			}
			c++;
		}
		return defaultValue;
	}

	public default Linq<T> except(final Linq<T> right) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it = Linq.this.iterator();
			Set<T> set;

			@Override
			protected Nullable<T> get() {
				if (set == null) {
					set = new HashSet<>();
					for (var v : right) {
						set.add(v);
					}
				}
				while (it.hasNext()) {
					var v = it.next();
					if (!set.contains(v)) {
						return Nullable.of(v);
					}
				}
				return Nullable.none();
			}
		};
	}

	public default <TKey> Linq<T> exceptBy(final Linq<T> right, final Function<T, TKey> keySelector) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it = Linq.this.iterator();
			Map<TKey, T> map;

			@Override
			protected Nullable<T> get() {
				if (map == null) {
					map = new HashMap<>();
					for (var v : right) {
						map.put(keySelector.apply(v), v);
					}
				}
				while (it.hasNext()) {
					var v = it.next();
					if (!map.containsKey(keySelector.apply(v))) {
						return Nullable.of(v);
					}
				}
				return Nullable.none();
			}
		};
	}

	public default Nullable<T> first() {
		var it = iterator();
		if (it.hasNext()) {
			return Nullable.of(it.next());
		} else {
			return Nullable.none();
		}
	}

	public default T firstOrDefault(T defaultValue) {
		var it = iterator();
		if (it.hasNext()) {
			return it.next();
		} else {
			return defaultValue;
		}
	}

	public default <TKey> Linq<Entry<TKey, List<T>>> groupBy(final Function<T, TKey> keySelector) {
		return () -> new LinqIterator<Entry<TKey, List<T>>>() {
			Iterator<Entry<TKey, List<T>>> it;

			@Override
			protected Nullable<Entry<TKey, List<T>>> get() {
				if (it == null) {
					var map = new LinkedHashMap<TKey, List<T>>();
					for (var v : Linq.this) {
						var k = keySelector.apply(v);
						var vs = map.get(k);
						if (vs == null) {
							vs = new ArrayList<T>();
							map.put(k, vs);
						}
						vs.add(v);
					}
					it = map.entrySet().iterator();
				}
				if (it.hasNext()) {
					return Nullable.of(it.next());
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public default <TRight, TKey, TResult> Linq<TResult> groupJoin(
		final Linq<TRight> rightLinq,
		final Function<T, TKey> leftKeySelector,
		final Function<TRight, TKey> rightKeySelector,
		final BiFunction<T, Linq<TRight>, TResult> resultSelector) {

		return () -> new LinqIterator<TResult>() {
			Iterator<T> it = Linq.this.iterator();
			Map<TKey, List<TRight>> map;

			@Override
			protected Nullable<TResult> get() {
				if (map == null) {
					map = new LinkedHashMap<>();
					for (var v : rightLinq) {
						var k = rightKeySelector.apply(v);
						var vs = map.get(k);
						if (vs == null) {
							vs = new ArrayList<>();
							map.put(k, vs);
						}
						vs.add(v);
					}
				}

				if (it.hasNext()) {
					var l = it.next();
					var list = map.get(leftKeySelector.apply(l));
					if (list == null) {
						list = new ArrayList<>();
					}
					return Nullable.of(resultSelector.apply(l, Linq.from(list)));
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public default Linq<T> intersect(final Linq<T> right) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it = Linq.this.iterator();
			Set<T> set;

			@Override
			protected Nullable<T> get() {
				if (set == null) {
					set = new HashSet<>();
					for (var v : right) {
						set.add(v);
					}
				}
				while (it.hasNext()) {
					var v = it.next();
					if (set.contains(v)) {
						return Nullable.of(v);
					}
				}
				return Nullable.none();
			}
		};
	}

	public default <U> Linq<T> intersectBy(final Linq<U> right, final Function<T, U> keySelector) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it = Linq.this.iterator();
			Set<U> set;

			@Override
			protected Nullable<T> get() {
				if (set == null) {
					set = new HashSet<>();
					for (var v : right) {
						set.add(v);
					}
				}
				while (it.hasNext()) {
					var v = it.next();
					if (set.contains(keySelector.apply(v))) {
						return Nullable.of(v);
					}
				}
				return Nullable.none();
			}
		};
	}

	@Override
	public abstract LinqIterator<T> iterator();

	public default <TRight, TKey, TResult> Linq<TResult> join(
		final Linq<TRight> rightLinq,
		final Function<T, TKey> leftKeySelector,
		final Function<TRight, TKey> rightKeySelector,
		final BiFunction<T, Linq<TRight>, TResult> resultSelector) {

		return () -> new LinqIterator<TResult>() {
			Iterator<T> it = Linq.this.iterator();
			Map<TKey, List<TRight>> map;

			@Override
			protected Nullable<TResult> get() {
				if (map == null) {
					map = new LinkedHashMap<>();
					for (var v : rightLinq) {
						var k = rightKeySelector.apply(v);
						var vs = map.get(k);
						if (vs == null) {
							vs = new ArrayList<>();
							map.put(k, vs);
						}
						vs.add(v);
					}
				}

				while (it.hasNext()) {
					var l = it.next();
					var list = map.get(leftKeySelector.apply(l));
					if (list != null) {
						return Nullable.of(resultSelector.apply(l, Linq.from(list)));
					}
				}
				return Nullable.none();
			}
		};
	}

	public default Nullable<T> last() {
		var it = iterator();
		if (it.hasNext()) {
			T value;
			do {
				value = it.next();
			} while (it.hasNext());
			return Nullable.of(value);
		} else {
			return Nullable.none();
		}
	}

	public default Nullable<T> max(Comparator<T> cmp) {
		var it = iterator();
		if (it.hasNext()) {
			var m = it.next();
			while (it.hasNext()) {
				var v = it.next();
				if (cmp.compare(m, v) < 0) {
					m = v;
				}
			}
			return Nullable.of(m);
		} else {
			return Nullable.none();
		}
	}

	public default <TKey> Nullable<T> maxBy(Comparator<TKey> cmp, final Function<T, TKey> keySelector) {
		var it = iterator();
		if (it.hasNext()) {
			var mv = it.next();
			var mk = keySelector.apply(mv);

			while (it.hasNext()) {
				var v = it.next();
				var k = keySelector.apply(v);

				if (cmp.compare(mk, k) < 0) {
					mv = v;
					mk = k;
				}
			}
			return Nullable.of(mv);
		} else {
			return Nullable.none();
		}
	}

	public default Nullable<T> min(Comparator<T> cmp) {
		var it = iterator();
		if (it.hasNext()) {
			var m = it.next();
			while (it.hasNext()) {
				var v = it.next();
				if (cmp.compare(v, m) < 0) {
					m = v;
				}
			}
			return Nullable.of(m);
		} else {
			return Nullable.none();
		}
	}

	public default <TKey> Nullable<T> minBy(Comparator<TKey> cmp, final Function<T, TKey> keySelector) {
		var it = iterator();
		if (it.hasNext()) {
			var mv = it.next();
			var mk = keySelector.apply(mv);

			while (it.hasNext()) {
				var v = it.next();
				var k = keySelector.apply(v);

				if (cmp.compare(k, mk) < 0) {
					mv = v;
					mk = k;
				}
			}
			return Nullable.of(mv);
		} else {
			return Nullable.none();
		}
	}

	public default <U> Linq<U> ofType(final Class<U> type) {
		return () -> new LinqIterator<U>() {
			Iterator<T> it = Linq.this.iterator();

			@Override
			protected Nullable<U> get() {
				while (it.hasNext()) {
					var v = it.next();
					if (type.isInstance(v)) {
						return Nullable.of(type.cast(v));
					}
				}
				return Nullable.none();
			}
		};
	}

	public default <U extends Comparable<U>> OrderingLinq<T> orderBy(final Function<T, U> keySelector) {
		return new OrderingLinq<T>(this, (x, y) -> keySelector.apply(x)
				.compareTo(keySelector.apply(y)));
	}

	public default <U extends Comparable<U>> OrderingLinq<T> orderByDescending(final Function<T, U> keySelector) {
		return new OrderingLinq<T>(this, (l, r) -> keySelector.apply(r).compareTo(keySelector.apply(l)));
	}

	public default Linq<T> prepend(final T value) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it = Linq.this.iterator();
			boolean isIt = false;

			@Override
			protected Nullable<T> get() {
				if (!isIt) {
					isIt = true;
					return Nullable.of(value);
				} else {
					if (it.hasNext()) {
						return Nullable.of(it.next());
					} else {
						return Nullable.none();
					}
				}
			}
		};
	}

	public default Linq<T> reverse() {
		return () -> new LinqIterator<T>() {
			Iterator<T> it;

			@Override
			protected Nullable<T> get() {
				if (it == null) {
					var list = toList();
					Collections.reverse(list);
					it = list.iterator();
				}
				if (it.hasNext()) {
					return Nullable.of(it.next());
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public default <R> Linq<R> select(final Function<T, R> func) {
		return () -> new LinqIterator<R>() {
			LinqIterator<T> it = Linq.this.iterator();

			@Override
			protected Nullable<R> get() {
				if (it.hasNext()) {
					return Nullable.of(func.apply(it.next()));
				}
				return Nullable.none();
			}
		};
	}

	public default <U> Linq<U> selectMany(final Function<T, Linq<U>> func) {
		return () -> new LinqIterator<U>() {
			Iterator<T> it = Linq.this.iterator();
			Iterator<U> inner = null;

			@Override
			protected Nullable<U> get() {
				while (inner == null || !inner.hasNext()) {
					if (!it.hasNext()) {
						return Nullable.none();
					}
					inner = func.apply(it.next()).iterator();
				}
				return Nullable.of(inner.next());
			}
		};
	}

	public default boolean sequenceEqual(Linq<T> rightLinq) {
		var left = iterator();
		var right = rightLinq.iterator();
		while (left.hasNext() && right.hasNext()) {
			if (!Objects.equals(left.next(), right.next())) {
				return false;
			}
		}
		return left.hasNext() == right.hasNext();
	}

	public default T single() {
		var it = iterator();
		if (it.hasNext()) {
			var v = it.next();
			if (it.hasNext()) {
				throw new IllegalStateException("値が複数存在します");
			} else {
				return v;
			}
		} else {
			throw new IllegalStateException("値が存在しません");
		}
	}

	public default T single(T defaultValue) {
		var it = iterator();
		if (it.hasNext()) {
			T v = it.next();
			if (it.hasNext()) {
				throw new IllegalStateException("値が複数存在します");
			} else {
				return v;
			}
		} else {
			return defaultValue;
		}
	}

	public default Linq<T> skip(final int n) {
		return () -> new LinqIterator<T>() {
			LinqIterator<T> it = Linq.this.iterator();
			long i = 0L;

			@Override
			protected Nullable<T> get() {
				while (i < n && it.hasNext()) {
					i++;
					it.next();
				}
				if (it.hasNext()) {
					return Nullable.of(it.next());
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public default Linq<T> skipLast(final long size) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it;
			long to;
			long i;

			@Override
			protected Nullable<T> get() {
				if (it == null) {
					var c = 0L;
					var cit = Linq.this.iterator();
					while (cit.hasNext()) {
						cit.next();
						c++;
					}
					it = Linq.this.iterator();
					to = c - size;
					i = 0L;
				}

				if (i < to && it.hasNext()) {
					i++;
					return Nullable.of(it.next());
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public default Linq<T> skipWhile(final Predicate<T> p) {
		return () -> new LinqIterator<T>() {
			LinqIterator<T> it = Linq.this.iterator();
			boolean skipped = false;

			@Override
			protected Nullable<T> get() {
				if (!skipped) {
					while (it.hasNext()) {
						var val = it.next();
						if (!p.test(val)) {
							skipped = true;
							return Nullable.of(val);
						}
					}
					return Nullable.none();
				} else {
					if (it.hasNext()) {
						return Nullable.of(it.next());
					} else {
						return Nullable.none();
					}
				}
			}
		};
	}

	public default long sum(Function<T, Long> func) {
		var s = 0L;
		for (var v : this) {
			s += func.apply(v);
		}
		return s;
	}

	public default Linq<T> take(final int n) {
		return () -> new LinqIterator<T>() {
			LinqIterator<T> it = Linq.this.iterator();
			long i = 0L;

			@Override
			protected Nullable<T> get() {
				if (i < n && it.hasNext()) {
					i++;
					return Nullable.of(it.next());
				}
				return Nullable.none();
			}
		};
	}

	public default Linq<T> takeLast(final int size) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it;

			@Override
			protected Nullable<T> get() {
				if (it == null) {
					var q = new ArrayDeque<T>(size);
					for (var v : Linq.this) {
						if (q.size() == size) {
							q.remove();
						}
						q.add(v);
					}
					it = q.iterator();
				}
				if (it.hasNext()) {
					return Nullable.of(it.next());
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public default Linq<T> takeWhile(final Predicate<T> p) {
		return () -> new LinqIterator<T>() {
			LinqIterator<T> it = Linq.this.iterator();

			@Override
			protected Nullable<T> get() {
				if (it.hasNext()) {
					var val = it.next();
					if (p.test(val)) {
						return Nullable.of(val);
					}
				}
				return Nullable.none();
			}
		};
	}

	public default T[] toArray(T[] a) {
		var list = new ArrayList<T>();
		for (var val : this) {
			list.add(val);
		}
		return list.toArray(a);
	}

	public default <K> LinkedHashMap<K, T> toDictionary(Function<T, K> keySelector) {
		var map = new LinkedHashMap<K, T>();
		for (var val : this) {
			var key = keySelector.apply(val);
			if (map.containsKey(key)) {
				throw new IllegalArgumentException("キーが重複しています: " + key);
			}
			map.put(key, val);
		}
		return map;
	}

	public default LinkedHashSet<T> toHashSet() {
		var set = new LinkedHashSet<T>();
		for (var val : this) {
			set.add(val);
		}
		return set;
	}

	public default ArrayList<T> toList() {
		var list = new ArrayList<T>();
		for (T val : this) {
			list.add(val);
		}
		return list;
	}

	public default <K> LinkedHashMap<K, List<T>> toLookup(Function<T, K> keySelector) {
		var map = new LinkedHashMap<K, List<T>>();
		for (var val : this) {
			var key = keySelector.apply(val);
			var list = map.get(key);
			if (list == null) {
				list = new ArrayList<T>();
				map.put(key, list);
			}
			list.add(val);
		}
		return map;
	}

	public default Linq<T> union(final Linq<T> right) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it;

			@Override
			protected Nullable<T> get() {
				if (it == null) {
					var set = new LinkedHashSet<T>();
					for (var v : Linq.this) {
						set.add(v);
					}
					for (T v : right) {
						set.add(v);
					}
					it = set.iterator();
				}
				if (it.hasNext()) {
					return Nullable.of(it.next());
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public default <TKey> Linq<T> unionBy(final Linq<T> right, final Function<T, TKey> keySelector) {
		return () -> new LinqIterator<T>() {
			Iterator<T> it;

			@Override
			protected Nullable<T> get() {
				if (it == null) {
					var map = new LinkedHashMap<TKey, T>();
					for (var v : Linq.this) {
						map.putIfAbsent(keySelector.apply(v), v);
					}
					for (T v : right) {
						map.putIfAbsent(keySelector.apply(v), v);
					}
					it = map.values().iterator();
				}
				if (it.hasNext()) {
					return Nullable.of(it.next());
				} else {
					return Nullable.none();
				}
			}
		};
	}

	public default Linq<T> where(final Predicate<T> pred) {
		return () -> new LinqIterator<T>() {
			LinqIterator<T> it = Linq.this.iterator();

			@Override
			protected Nullable<T> get() {
				while (it.hasNext()) {
					var val = it.next();

					if (pred.test(val)) {
						return Nullable.of(val);
					}
				}

				return Nullable.none();
			}
		};
	}

	public default <T2> Linq<Tuple2<T, T2>> zip(final Linq<T2> right) {
		return () -> new LinqIterator<Tuple2<T, T2>>() {
			Iterator<T> l = Linq.this.iterator();
			Iterator<T2> r = right.iterator();

			@Override
			protected Nullable<Tuple2<T, T2>> get() {
				if (l.hasNext() && r.hasNext()) {
					return Nullable.of(new Tuple2<T, T2>(l.next(), r.next()));
				}
				return Nullable.none();
			}
		};
	}

}
