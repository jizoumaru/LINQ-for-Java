package main;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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

public abstract class Linq<T> implements Iterable<T> {
	public interface BiFunction<T, U, R> {
		R apply(T left, U right);
	}

	public interface Function<T, U> {
		U apply(T value);
	}

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
				Nullable<T> ret = value;
				value = null;
				return ret.value();
			} else {
				throw new NoSuchElementException();
			}
		}

		@Override
		public void remove() {
			throw new RuntimeException("not suported");
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

	static class OrderingLinq<T> extends Linq<T> {
		private final Linq<T> linq;
		private final Comparator<T> cmp;

		public OrderingLinq(Linq<T> linq, Comparator<T> cmp) {
			this.linq = linq;
			this.cmp = cmp;
		}

		@Override
		public LinqIterator<T> iterator() {
			ArrayList<T> list = linq.toList();
			Collections.sort(list, cmp);
			return Linq.from(list).iterator();
		}

		public Linq<T> thenBy(final Comparator<T> cmp) {
			return new OrderingLinq<T>(this, new Comparator<T>() {
				@Override
				public int compare(T left, T right) {
					int c = OrderingLinq.this.cmp.compare(left, right);
					if (c == 0) {
						c = cmp.compare(left, right);
					}
					return c;
				}
			});
		}

		public Linq<T> thenByDescending(final Comparator<T> cmp) {
			return new OrderingLinq<T>(this, new Comparator<T>() {
				@Override
				public int compare(T left, T right) {
					int c = OrderingLinq.this.cmp.compare(left, right);
					if (c == 0) {
						c = cmp.compare(right, left);
					}
					return c;
				}
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
	}

	public static <T> Linq<T> from(final Iterable<T> iterable) {
		return new Linq<T>() {

			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					Iterator<T> iter = iterable.iterator();

					@Override
					protected Nullable<T> get() {
						if (iter.hasNext()) {
							return Nullable.of(iter.next());
						} else {
							return Nullable.none();
						}
					}
				};
			}
		};
	}

	public static void main(String[] args) {
		List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

		Linq<String> xs = Linq.from(list)
			.where(new Predicate<Integer>() {
				@Override
				public boolean test(Integer x) {
					return x % 2 == 0;
				}
			})
			.select(new Function<Integer, String>() {
				@Override
				public String apply(Integer x) {
					return "a" + x;
				}
			});

		for (String x : xs) {
			System.out.println(x);
		}
		for (String x : xs) {
			System.out.println(x);
		}
	}

	public static Linq<Integer> range(final int start, final int count) {
		return new Linq<Integer>() {
			@Override
			public LinqIterator<Integer> iterator() {
				return new LinqIterator<Integer>() {
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
		};
	}

	public static <T> Linq<T> repeat(final T value, final int count) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
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
		};
	}

	public Nullable<T> aggregate(BiFunction<T, T, T> func) {
		LinqIterator<T> it = iterator();
		if (it.hasNext()) {
			T l = it.next();
			while (it.hasNext()) {
				l = func.apply(l, it.next());
			}
			return Nullable.of(l);
		} else {
			return Nullable.none();
		}
	}

	public T aggregate(T seed, BiFunction<T, T, T> func) {
		LinqIterator<T> it = iterator();
		T l = seed;
		while (it.hasNext()) {
			l = func.apply(l, it.next());
		}
		return l;
	}

	public boolean all(Predicate<T> pred) {
		for (T v : this) {
			if (!pred.test(v)) {
				return false;
			}
		}
		return true;
	}

	public boolean any(Predicate<T> pred) {
		for (T v : this) {
			if (pred.test(v)) {
				return true;
			}
		}
		return false;
	}

	public Linq<T> append(final T value) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
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
		};
	}

	public Nullable<Long> average(Function<T, Long> func) {
		long sum = select(func).aggregate(0L, new BiFunction<Long, Long, Long>() {
			@Override
			public Long apply(Long l, Long r) {
				return l + r;
			}
		});
		long count = count();
		if (count == 0) {
			return Nullable.none();
		} else {
			return Nullable.of(sum / count);
		}
	}

	public <U> Linq<U> cast() {
		return new Linq<U>() {
			@Override
			public LinqIterator<U> iterator() {
				return new LinqIterator<U>() {
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
		};
	}

	public Linq<List<T>> chunk(final int size) {
		return new Linq<List<T>>() {
			@Override
			public LinqIterator<List<T>> iterator() {
				return new LinqIterator<List<T>>() {
					LinqIterator<T> it = Linq.this.iterator();

					@Override
					protected Nullable<List<T>> get() {
						if (it.hasNext()) {
							List<T> list = new ArrayList<T>();
							for (int i = 0; i < size; i++) {
								list.add(it.next());
								if (it.hasNext()) {
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
		};
	}

	public Linq<T> concat(final Linq<T> right) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
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
		};
	}

	public boolean contains(T target) {
		for (T v : this) {
			if (v.equals(target)) {
				return true;
			}
		}
		return false;
	}

	public long count() {
		long c = 0L;
		LinqIterator<T> it = iterator();
		while (it.hasNext()) {
			it.next();
			c++;
		}
		return c;
	}

	public Linq<T> defaultIfEmpty(final T defaultValue) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
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
		};
	}

	public Linq<T> distinct() {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					Iterator<T> it;

					@Override
					protected Nullable<T> get() {
						if (it == null) {
							Set<T> set = new LinkedHashSet<T>();
							for (T v : Linq.this) {
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
		};
	}

	public <TKey> Linq<T> distinctBy(final Function<T, TKey> func) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					Iterator<T> it;

					@Override
					protected Nullable<T> get() {
						if (it == null) {
							Map<TKey, T> map = new LinkedHashMap<TKey, T>();
							for (T v : Linq.this) {
								map.put(func.apply(v), v);
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
		};
	}

	public Nullable<T> elementAt(long index) {
		long c = 0L;
		for (T v : this) {
			if (c == index) {
				return Nullable.of(v);
			}
			c++;
		}
		return Nullable.none();
	}

	public T elementAt(long index, T defaultValue) {
		long c = 0L;
		for (T v : this) {
			if (c == index) {
				return v;
			}
			c++;
		}
		return defaultValue;
	}

	public Linq<T> empty() {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					@Override
					protected Nullable<T> get() {
						return Nullable.none();
					}
				};
			}
		};
	}

	public Linq<T> except(final Linq<T> right) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					Iterator<T> it = Linq.this.iterator();
					Set<T> set;

					@Override
					protected Nullable<T> get() {
						if (set == null) {
							set = new HashSet<>();
							for (T v : right) {
								set.add(v);
							}
						}
						while (it.hasNext()) {
							T v = it.next();
							if (!set.contains(v)) {
								return Nullable.of(v);
							}
						}
						return Nullable.none();
					}
				};
			}
		};
	}

	public <TKey> Linq<T> exceptBy(final Linq<T> right, final Function<T, TKey> keySelector) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					Iterator<T> it = Linq.this.iterator();
					Map<TKey, T> map;

					@Override
					protected Nullable<T> get() {
						if (map == null) {
							map = new HashMap<>();
							for (T v : right) {
								map.put(keySelector.apply(v), v);
							}
						}
						while (it.hasNext()) {
							T v = it.next();
							if (!map.containsKey(keySelector.apply(v))) {
								return Nullable.of(v);
							}
						}
						return Nullable.none();
					}
				};
			}
		};
	}

	public Nullable<T> first() {
		LinqIterator<T> it = iterator();
		if (it.hasNext()) {
			return Nullable.of(it.next());
		} else {
			return Nullable.none();
		}
	}

	public T firstOrDefault(T defaultValue) {
		LinqIterator<T> it = iterator();
		if (it.hasNext()) {
			return it.next();
		} else {
			return defaultValue;
		}
	}

	public <TKey> Linq<Entry<TKey, List<T>>> groupBy(final Function<T, TKey> keySelector) {
		return new Linq<Entry<TKey, List<T>>>() {
			@Override
			public LinqIterator<Entry<TKey, List<T>>> iterator() {
				return new LinqIterator<Entry<TKey, List<T>>>() {
					Iterator<Entry<TKey, List<T>>> it;

					@Override
					protected Nullable<Entry<TKey, List<T>>> get() {
						if (it == null) {
							LinkedHashMap<TKey, List<T>> map = new LinkedHashMap<TKey, List<T>>();
							for (T v : Linq.this) {
								TKey k = keySelector.apply(v);
								List<T> vs = map.get(k);
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
		};
	}

	public <TRight, TKey, TResult> Linq<TResult> groupJoin(
		final Linq<TRight> rightLinq,
		final Function<T, TKey> leftKeySelector,
		final Function<TRight, TKey> rightKeySelector,
		final BiFunction<T, Linq<TRight>, TResult> resultSelector) {
		return new Linq<TResult>() {
			@Override
			public LinqIterator<TResult> iterator() {
				return new LinqIterator<TResult>() {
					Iterator<T> it = Linq.this.iterator();
					Map<TKey, List<TRight>> map;

					@Override
					protected Nullable<TResult> get() {
						if (map == null) {
							map = new LinkedHashMap<>();
							for (TRight v : rightLinq) {
								TKey k = rightKeySelector.apply(v);
								List<TRight> vs = map.get(k);
								if (vs == null) {
									vs = new ArrayList<>();
									map.put(k, vs);
								}
								vs.add(v);
							}
						}

						if (it.hasNext()) {
							T l = it.next();
							List<TRight> list = map.get(leftKeySelector.apply(l));
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
		};
	}

	public Linq<T> intersect(final Linq<T> right) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					Iterator<T> it = Linq.this.iterator();
					Set<T> set;

					@Override
					protected Nullable<T> get() {
						if (set == null) {
							set = new HashSet<>();
							for (T v : right) {
								set.add(v);
							}
						}
						while (it.hasNext()) {
							T v = it.next();
							if (set.contains(v)) {
								return Nullable.of(v);
							}
						}
						return Nullable.none();
					}
				};
			}
		};
	}

	public <TKey> Linq<T> intersectBy(final Linq<T> right, final Function<T, TKey> keySelector) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					Iterator<T> it = Linq.this.iterator();
					Map<TKey, T> map;

					@Override
					protected Nullable<T> get() {
						if (map == null) {
							map = new HashMap<>();
							for (T v : right) {
								map.put(keySelector.apply(v), v);
							}
						}
						while (it.hasNext()) {
							T v = it.next();
							if (map.containsKey(keySelector.apply(v))) {
								return Nullable.of(v);
							}
						}
						return Nullable.none();
					}
				};
			}
		};
	}

	@Override
	public abstract LinqIterator<T> iterator();

	public <TRight, TKey, TResult> Linq<TResult> join(
		final Linq<TRight> rightLinq,
		final Function<T, TKey> leftKeySelector,
		final Function<TRight, TKey> rightKeySelector,
		final BiFunction<T, Linq<TRight>, TResult> resultSelector) {
		return new Linq<TResult>() {
			@Override
			public LinqIterator<TResult> iterator() {
				return new LinqIterator<TResult>() {
					Iterator<T> it = Linq.this.iterator();
					Map<TKey, List<TRight>> map;

					@Override
					protected Nullable<TResult> get() {
						if (map == null) {
							map = new LinkedHashMap<>();
							for (TRight v : rightLinq) {
								TKey k = rightKeySelector.apply(v);
								List<TRight> vs = map.get(k);
								if (vs == null) {
									vs = new ArrayList<>();
									map.put(k, vs);
								}
								vs.add(v);
							}
						}

						while (it.hasNext()) {
							T l = it.next();
							List<TRight> list = map.get(leftKeySelector.apply(l));
							if (list != null) {
								return Nullable.of(resultSelector.apply(l, Linq.from(list)));
							}
						}
						return Nullable.none();
					}
				};
			}
		};
	}

	public Nullable<T> last() {
		LinqIterator<T> it = iterator();
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

	public Nullable<T> max(Comparator<T> cmp) {
		LinqIterator<T> it = iterator();
		if (it.hasNext()) {
			T m = it.next();
			while (it.hasNext()) {
				T v = it.next();
				if (cmp.compare(m, v) < 0) {
					m = v;
				}
			}
			return Nullable.of(m);
		} else {
			return Nullable.none();
		}
	}

	public <TKey> Nullable<T> maxBy(Comparator<TKey> cmp, final Function<T, TKey> keySelector) {
		LinqIterator<T> it = iterator();
		if (it.hasNext()) {
			T m = it.next();
			TKey mk = keySelector.apply(m);

			while (it.hasNext()) {
				T v = it.next();
				TKey k = keySelector.apply(v);

				if (cmp.compare(mk, k) < 0) {
					m = v;
					mk = k;
				}
			}
			return Nullable.of(m);
		} else {
			return Nullable.none();
		}
	}

	public Nullable<T> min(Comparator<T> cmp) {
		LinqIterator<T> it = iterator();
		if (it.hasNext()) {
			T m = it.next();
			while (it.hasNext()) {
				T v = it.next();
				if (cmp.compare(v, m) < 0) {
					m = v;
				}
			}
			return Nullable.of(m);
		} else {
			return Nullable.none();
		}
	}

	public <TKey> Nullable<T> minBy(Comparator<TKey> cmp, final Function<T, TKey> keySelector) {
		LinqIterator<T> it = iterator();
		if (it.hasNext()) {
			T m = it.next();
			TKey mk = keySelector.apply(m);

			while (it.hasNext()) {
				T v = it.next();
				TKey k = keySelector.apply(v);

				if (cmp.compare(k, mk) < 0) {
					m = v;
					mk = k;
				}
			}
			return Nullable.of(m);
		} else {
			return Nullable.none();
		}
	}

	public <U> Linq<U> ofType(final Class<U> type) {
		return new Linq<U>() {
			@Override
			public LinqIterator<U> iterator() {
				return new LinqIterator<U>() {
					Iterator<T> it = Linq.this.iterator();

					@Override
					protected Nullable<U> get() {
						while (it.hasNext()) {
							T v = it.next();
							if (type.isInstance(v)) {
								return Nullable.of(type.cast(v));
							}
						}
						return Nullable.none();
					}
				};
			}
		};
	}

	public OrderingLinq<T> orderBy(final Comparator<T> cmp) {
		return new OrderingLinq<T>(this, cmp);
	}

	public OrderingLinq<T> OrderByDescending(final Comparator<T> cmp) {
		return new OrderingLinq<T>(this, new Comparator<T>() {
			@Override
			public int compare(T left, T right) {
				return cmp.compare(right, left);
			}
		});
	}

	public Linq<T> prepend(final T value) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
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
		};
	}

	public Linq<T> reverse() {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					Iterator<T> it;

					@Override
					protected Nullable<T> get() {
						if (it == null) {
							ArrayList<T> list = toList();
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
		};
	}

	public <R> Linq<R> select(final Function<T, R> func) {
		return new Linq<R>() {
			@Override
			public LinqIterator<R> iterator() {
				return new LinqIterator<R>() {
					LinqIterator<T> iter = Linq.this.iterator();

					@Override
					protected Nullable<R> get() {
						if (iter.hasNext()) {
							return Nullable.of(func.apply(iter.next()));
						}
						return Nullable.none();
					}
				};
			}
		};
	}

	public <U> Linq<U> selectMany(final Function<T, Linq<U>> func) {
		return new Linq<U>() {
			@Override
			public LinqIterator<U> iterator() {
				return new LinqIterator<U>() {
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
		};
	}

	public boolean sequenceEqual(Linq<T> rightLinq) {
		Iterator<T> left = iterator();
		Iterator<T> right = rightLinq.iterator();
		while (left.hasNext() && right.hasNext()) {
			if (!Objects.equals(left.next(), right.next())) {
				return false;
			}
		}
		return left.hasNext() == right.hasNext();
	}

	public T single() {
		Iterator<T> it = iterator();
		if (it.hasNext()) {
			T v = it.next();
			if (it.hasNext()) {
				throw new IllegalStateException("値が複数存在します");
			} else {
				return v;
			}
		} else {
			throw new IllegalStateException("値が存在しません");
		}
	}

	public T single(T defaultValue) {
		Iterator<T> it = iterator();
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

	public Linq<T> skip(final int n) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					LinqIterator<T> iter = Linq.this.iterator();
					long i = 0L;

					@Override
					protected Nullable<T> get() {
						while (i < n && iter.hasNext()) {
							i++;
							iter.next();
						}
						if (iter.hasNext()) {
							return Nullable.of(iter.next());
						} else {
							return Nullable.none();
						}
					}
				};
			}
		};
	}

	public Linq<T> skipLast(final long size) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					Iterator<T> it;
					long to;
					long i;

					@Override
					protected Nullable<T> get() {
						if (it == null) {
							long c = 0L;
							Iterator<T> it = Linq.this.iterator();
							while (it.hasNext()) {
								it.next();
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
		};
	}

	public Linq<T> skipWhile(final Predicate<T> p) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					LinqIterator<T> iter = Linq.this.iterator();
					boolean skipped = false;

					@Override
					protected Nullable<T> get() {
						if (!skipped) {
							while (iter.hasNext()) {
								T val = iter.next();
								if (!p.test(val)) {
									skipped = true;
									return Nullable.of(val);
								}
							}
							return Nullable.none();
						} else {
							if (iter.hasNext()) {
								return Nullable.of(iter.next());
							} else {
								return Nullable.none();
							}
						}
					}
				};
			}
		};
	}

	public long sum(Function<T, Long> func) {
		long s = 0L;
		for (T v : this) {
			s += func.apply(v);
		}
		return s;
	}

	public Linq<T> take(final int n) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					LinqIterator<T> iter = Linq.this.iterator();
					long i = 0L;

					@Override
					protected Nullable<T> get() {
						if (i < n && iter.hasNext()) {
							i++;
							return Nullable.of(iter.next());
						}
						return Nullable.none();
					}
				};
			}
		};
	}

	public Linq<T> takeLast(final int size) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					Iterator<T> it;

					@Override
					protected Nullable<T> get() {
						if (it == null) {
							ArrayDeque<T> q = new ArrayDeque<>(size);
							for (T v : Linq.this) {
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
		};
	}

	public Linq<T> takeWhile(final Predicate<T> p) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					LinqIterator<T> iter = Linq.this.iterator();

					@Override
					protected Nullable<T> get() {
						if (iter.hasNext()) {
							T val = iter.next();
							if (p.test(val)) {
								return Nullable.of(val);
							}
						}
						return Nullable.none();
					}
				};
			}
		};
	}

	public T[] toArray(T[] a) {
		List<T> list = new ArrayList<T>();
		for (T val : this) {
			list.add(val);
		}
		return list.toArray(a);
	}

	public <K> LinkedHashMap<K, T> toDictionary(Function<T, K> keySelector) {
		LinkedHashMap<K, T> map = new LinkedHashMap<K, T>();
		for (T val : this) {
			map.put(keySelector.apply(val), val);
		}
		return map;
	}

	public LinkedHashSet<T> toHashSet() {
		LinkedHashSet<T> set = new LinkedHashSet<T>();
		for (T val : this) {
			set.add(val);
		}
		return set;
	}

	public ArrayList<T> toList() {
		ArrayList<T> list = new ArrayList<T>();
		for (T val : this) {
			list.add(val);
		}
		return list;
	}

	public <K> LinkedHashMap<K, List<T>> toLookup(Function<T, K> keySelector) {
		LinkedHashMap<K, List<T>> map = new LinkedHashMap<K, List<T>>();
		for (T val : this) {
			K key = keySelector.apply(val);
			List<T> list = map.get(key);
			if (list == null) {
				list = new ArrayList<T>();
			}
			list.add(val);
		}
		return map;
	}

	public Linq<T> union(final Linq<T> right) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					Iterator<T> it;

					@Override
					protected Nullable<T> get() {
						if (it == null) {
							Set<T> set = new LinkedHashSet<T>();
							for (T v : Linq.this) {
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
		};
	}

	public <TKey> Linq<T> unionBy(final Linq<T> right, final Function<T, TKey> keySelector) {
		return new Linq<T>() {
			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					Iterator<T> it;

					@Override
					protected Nullable<T> get() {
						if (it == null) {
							Map<TKey, T> map = new LinkedHashMap<TKey, T>();
							for (T v : Linq.this) {
								map.put(keySelector.apply(v), v);
							}
							for (T v : right) {
								map.put(keySelector.apply(v), v);
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
		};
	}

	public Linq<T> where(final Predicate<T> pred) {
		return new Linq<T>() {

			@Override
			public LinqIterator<T> iterator() {
				return new LinqIterator<T>() {
					LinqIterator<T> iter = Linq.this.iterator();

					@Override
					protected Nullable<T> get() {
						while (iter.hasNext()) {
							T val = iter.next();

							if (pred.test(val)) {
								return Nullable.of(val);
							}
						}

						return Nullable.none();
					}
				};
			}
		};
	}

	public <T2> Linq<Tuple2<T, T2>> zip(final Linq<T2> right) {
		return new Linq<Tuple2<T, T2>>() {
			@Override
			public LinqIterator<Tuple2<T, T2>> iterator() {
				return new LinqIterator<Tuple2<T, T2>>() {
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
		};
	}

}
