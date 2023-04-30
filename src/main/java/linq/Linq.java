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
import java.util.Queue;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Linq<T> {
	public static final class ArrayFetch<T> extends Fetch<T> {
		private final T[] array;
		private int index;

		public ArrayFetch(T[] array) {
			this.array = array;
			this.index = 0;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (index < array.length) {
				return Holder.of(array[index++]);
			}
			return Holder.none();
		}

		@Override
		protected final void internalClose() {
		}
	}

	public static final class CastFetch<T, U> extends Fetch<U> {
		private final Fetch<T> fetch;

		public CastFetch(Fetch<T> fetch) {
			this.fetch = fetch;
		}

		@Override
		protected final Holder<U> internalNext() {
			var holder = fetch.next();

			if (holder.exists()) {
				@SuppressWarnings("unchecked")
				var value = (U) holder.value();
				return Holder.of(value);
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class ChunkFetch<T> extends Fetch<List<T>> {
		private final Fetch<T> fetch;
		private final int size;
		private boolean terminated;

		public ChunkFetch(Fetch<T> fetch, int size) {
			this.fetch = fetch;
			this.size = size;
			this.terminated = false;
		}

		@Override
		protected final Holder<List<T>> internalNext() {
			if (terminated) {
				return Holder.none();
			}

			var list = new ArrayList<T>();

			for (var i = 0; i < size; i++) {
				var current = fetch.next();

				if (!current.exists()) {
					terminated = true;
					break;
				}

				list.add(current.value());
			}

			if (list.isEmpty()) {
				return Holder.none();
			}

			return Holder.of(list);
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class ConcatFetch<T> extends Fetch<T> {
		private final Fetch<T> left;
		private final Fetch<T> right;
		private Fetch<T> fetch;

		public ConcatFetch(Fetch<T> left, Fetch<T> right) {
			this.left = left;
			this.right = right;
			this.fetch = left;
		}

		@Override
		protected final Holder<T> internalNext() {
			var current = fetch.next();

			if (current.exists()) {
				return current;
			}

			if (fetch == left) {
				fetch = right;
				return fetch.next();
			}

			return current;
		}

		@Override
		protected final void internalClose() {
			try (var _right = right;
					var _left = left) {
			}
		}

	}

	public static final class DefaultIfEmptyFetch<T> extends Fetch<T> {
		private final Fetch<T> fetch;
		private final T defaultValue;
		private State state;

		enum State {
			DEFAULT, EMPTY, FETCH
		}

		public DefaultIfEmptyFetch(Fetch<T> fetch, T defaultValue) {
			this.fetch = fetch;
			this.defaultValue = defaultValue;
			this.state = State.DEFAULT;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (state == State.FETCH) {
				return fetch.next();
			}

			if (state == State.DEFAULT) {
				var holder = fetch.next();

				if (holder.exists()) {
					state = State.FETCH;
					return holder;
				} else {
					state = State.EMPTY;
					return Holder.of(defaultValue);
				}
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class DistinctByFetch<T, K> extends Fetch<T> {
		private final Fetch<T> fetch;
		private final Function<T, K> keyFactory;
		private Iterator<Holder<T>> iterator;

		public DistinctByFetch(Fetch<T> fetch, Function<T, K> keyFactory) {
			this.fetch = fetch;
			this.keyFactory = keyFactory;
			this.iterator = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (iterator == null) {
				var map = new LinkedHashMap<K, Holder<T>>();

				while (true) {
					var holder = fetch.next();

					if (!holder.exists()) {
						break;
					}

					var key = keyFactory.apply(holder.value());
					map.putIfAbsent(key, holder);
				}

				iterator = map.values().iterator();
			}

			if (iterator.hasNext()) {
				return iterator.next();
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class DistinctFetch<T> extends Fetch<T> {
		private final Fetch<T> fetch;
		private Iterator<Holder<T>> iterator;

		public DistinctFetch(Fetch<T> fetch) {
			this.fetch = fetch;
			this.iterator = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (iterator == null) {
				var set = new LinkedHashSet<Holder<T>>();

				while (true) {
					var holder = fetch.next();

					if (!holder.exists()) {
						break;
					}

					if (!set.contains(holder)) {
						set.add(holder);
					}
				}

				iterator = set.iterator();
			}

			if (iterator.hasNext()) {
				return iterator.next();
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class EmptyFetch<T> extends Fetch<T> {
		@Override
		protected final Holder<T> internalNext() {
			return Holder.none();
		}

		@Override
		protected final void internalClose() {
		}
	}

	public static final class ExceptByFetch<T, K> extends Fetch<T> {
		private final Fetch<T> left;
		private final Fetch<T> right;
		private final Function<T, K> keyFactory;
		private Map<K, Holder<T>> map;

		public ExceptByFetch(Fetch<T> left, Fetch<T> right, Function<T, K> keyFactory) {
			this.left = left;
			this.right = right;
			this.keyFactory = keyFactory;
			this.map = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (map == null) {
				map = new HashMap<K, Holder<T>>();

				while (true) {
					var holder = right.next();

					if (!holder.exists()) {
						break;
					}

					var key = keyFactory.apply(holder.value());
					map.put(key, holder);
				}
			}

			while (true) {
				var holder = left.next();

				if (!holder.exists()) {
					return holder;
				}

				var key = keyFactory.apply(holder.value());

				if (!map.containsKey(key)) {
					return holder;
				}
			}
		}

		@Override
		protected final void internalClose() {
			try (var _left = left;
					var _right = right) {
			}
		}
	}

	public static final class ExceptFetch<T> extends Fetch<T> {
		private final Fetch<T> left;
		private final Fetch<T> right;
		private Set<Holder<T>> set;

		public ExceptFetch(Fetch<T> left, Fetch<T> right) {
			this.left = left;
			this.right = right;
			this.set = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (set == null) {
				set = new HashSet<Holder<T>>();

				while (true) {
					var holder = right.next();

					if (!holder.exists()) {
						break;
					}

					set.add(holder);
				}
			}

			while (true) {
				var holder = left.next();

				if (!holder.exists()) {
					return holder;
				}

				if (!set.contains(holder)) {
					return holder;
				}
			}
		}

		@Override
		protected final void internalClose() {
			try (var _left = left;
					var _right = right) {
			}
		}
	}

	public static abstract class Fetch<T> implements AutoCloseable {
		private boolean closed;
		private Holder<T> peek;

		public final Holder<T> peek() {
			if (closed) {
				throw new IllegalStateException("already closed");
			}

			if (peek == null) {
				peek = internalNext();
			}

			return peek;
		}

		public final Holder<T> next() {
			if (closed) {
				throw new IllegalStateException("already closed");
			}

			if (peek == null) {
				var current = internalNext();
				if (!current.exists()) {
					close();
				}
				return current;
			} else {
				var current = peek;
				if (!current.exists()) {
					close();
				}
				peek = null;
				return current;
			}
		}

		protected abstract Holder<T> internalNext();

		@Override
		public final void close() {
			if (!closed) {
				closed = true;
				internalClose();
			}
		}

		protected abstract void internalClose();

	}

	public static final class FetchIterator<T> implements Iterator<T>, AutoCloseable {
		private final Fetch<T> fetch;

		public FetchIterator(Fetch<T> fetch) {
			this.fetch = fetch;
		}

		@Override
		public final boolean hasNext() {
			return fetch.peek().exists();
		}

		@Override
		public final T next() {
			return fetch.next().value();
		}

		@Override
		public void close() {
			fetch.close();
		}
	}

	public static final class GroupByFetch<T, K> extends Fetch<Entry<K, List<T>>> {
		private final Fetch<T> fetch;
		private final Function<T, K> keyFactory;
		private Iterator<Entry<K, List<T>>> entries;

		public GroupByFetch(Fetch<T> fetch, Function<T, K> keyFactory) {
			this.fetch = fetch;
			this.keyFactory = keyFactory;
			this.entries = null;
		}

		@Override
		protected final Holder<Entry<K, List<T>>> internalNext() {
			if (entries == null) {
				var map = new LinkedHashMap<K, List<T>>();

				while (true) {
					var holder = fetch.next();

					if (!holder.exists()) {
						break;
					}

					var key = keyFactory.apply(holder.value());
					var values = map.get(key);

					if (values == null) {
						values = new ArrayList<T>();
						map.put(key, values);
					}

					values.add(holder.value());
				}

				entries = map.entrySet().iterator();
			}

			if (entries.hasNext()) {
				return Holder.of(entries.next());
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class GroupJoinFetch<TLeft, TRight, TKey, TResult> extends Fetch<TResult> {
		private final Fetch<TLeft> left;
		private final Fetch<TRight> right;
		private final Function<TLeft, TKey> leftKeyFactory;
		private final Function<TRight, TKey> rightKeyFactory;
		private final BiFunction<TLeft, Linq<TRight>, TResult> resultFactory;
		private Map<TKey, List<TRight>> map;

		public GroupJoinFetch(
				Fetch<TLeft> left,
				Fetch<TRight> right,
				Function<TLeft, TKey> leftKeyFactory,
				Function<TRight, TKey> rightKeyFactory,
				BiFunction<TLeft, Linq<TRight>, TResult> resultFactory) {
			this.left = left;
			this.right = right;
			this.leftKeyFactory = leftKeyFactory;
			this.rightKeyFactory = rightKeyFactory;
			this.resultFactory = resultFactory;
			this.map = null;
		}

		@Override
		protected final Holder<TResult> internalNext() {
			if (map == null) {
				map = new LinkedHashMap<TKey, List<TRight>>();

				while (true) {
					var holder = right.next();

					if (!holder.exists()) {
						break;
					}

					var key = rightKeyFactory.apply(holder.value());
					var values = map.get(key);

					if (values == null) {
						values = new ArrayList<TRight>();
						map.put(key, values);
					}

					values.add(holder.value());
				}
			}

			var holder = left.next();

			if (holder.exists()) {
				var key = leftKeyFactory.apply(holder.value());
				var values = map.get(key);

				if (values == null) {
					values = new ArrayList<TRight>();
				}

				return Holder.of(resultFactory.apply(holder.value(), Linq.from(values)));
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			try (var _left = left;
					var _right = right) {
			}
		}
	}

	public static final class Holder<T> {
		public static final <T> Holder<T> none() {
			return new Holder<T>(null, false);
		}

		public static final <T> Holder<T> of(T value) {
			return new Holder<T>(value, true);
		}

		private final T value;
		private final boolean exist;

		private Holder(T value, boolean exist) {
			this.value = value;
			this.exist = exist;
		}

		public final boolean exists() {
			return exist;
		}

		public final T value() {
			if (exist) {
				return value;
			}
			throw new NoSuchElementException();
		}

		@Override
		public final int hashCode() {
			return Objects.hash(exist, value);
		}

		@Override
		public final boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof Holder)) {
				return false;
			}
			@SuppressWarnings("unchecked")
			var other = (Holder<T>) obj;
			return exist == other.exist && Objects.equals(value, other.value);
		}

	}

	public static final class IntersectByFetch<TLeft, TKey> extends Fetch<TLeft> {
		private final Fetch<TLeft> left;
		private final Fetch<TKey> right;
		private final Function<TLeft, TKey> keyFactory;
		private Set<TKey> set;

		public IntersectByFetch(
				Fetch<TLeft> left,
				Fetch<TKey> right,
				Function<TLeft, TKey> keyFactory) {
			this.left = left;
			this.right = right;
			this.keyFactory = keyFactory;
			this.set = null;
		}

		@Override
		protected final Holder<TLeft> internalNext() {
			if (set == null) {
				set = new HashSet<TKey>();

				while (true) {
					var holder = right.next();

					if (!holder.exists()) {
						break;
					}

					set.add(holder.value());
				}
			}

			while (true) {
				var holder = left.next();

				if (!holder.exists()) {
					return holder;
				}

				var key = keyFactory.apply(holder.value());

				if (set.contains(key)) {
					return holder;
				}
			}
		}

		@Override
		protected final void internalClose() {
			try (var _left = left;
					var _right = right) {
			}
		}
	}

	public static final class IntersectFetch<T> extends Fetch<T> {
		private final Fetch<T> left;
		private final Fetch<T> right;
		private Set<Holder<T>> set;

		public IntersectFetch(Fetch<T> left, Fetch<T> right) {
			this.left = left;
			this.right = right;
			this.set = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (set == null) {
				set = new HashSet<Holder<T>>();

				while (true) {
					var holder = right.next();

					if (!holder.exists()) {
						break;
					}

					set.add(holder);
				}
			}

			while (true) {
				var holder = left.next();

				if (!holder.exists()) {
					return holder;
				}

				if (set.contains(holder)) {
					return holder;
				}
			}
		}

		@Override
		protected final void internalClose() {
			try (var _left = left;
					var _right = right) {
			}
		}
	}

	public static final class IterableFetch<T> extends Fetch<T> {
		private final Iterable<T> iterable;
		private Iterator<T> iterator;

		public IterableFetch(Iterable<T> iterable) {
			this.iterable = iterable;
			this.iterator = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (iterator == null) {
				iterator = iterable.iterator();
			}

			if (iterator.hasNext()) {
				return Holder.of(iterator.next());
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
		}
	}

	public static final class StreamFetch<T> extends Fetch<T> {
		private final Stream<T> stream;
		private Iterator<T> iterator;

		public StreamFetch(Stream<T> stream) {
			this.stream = stream;
			this.iterator = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (iterator == null) {
				iterator = stream.iterator();
			}

			if (iterator.hasNext()) {
				return Holder.of(iterator.next());
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			stream.close();
		}
	}

	public static final class JoinFetch<TLeft, TRight, TKey, TResult> extends Fetch<TResult> {
		private final Fetch<TLeft> left;
		private final Fetch<TRight> right;
		private final Function<TLeft, TKey> leftKeyFactory;
		private final Function<TRight, TKey> rightKeyFactory;
		private final BiFunction<TLeft, Linq<TRight>, TResult> resultFactory;
		private Map<TKey, List<TRight>> map;

		public JoinFetch(
				Fetch<TLeft> left,
				Fetch<TRight> right,
				Function<TLeft, TKey> leftKeyFactory,
				Function<TRight, TKey> rightKeyFactory,
				BiFunction<TLeft, Linq<TRight>, TResult> resultFactory) {
			this.left = left;
			this.right = right;
			this.leftKeyFactory = leftKeyFactory;
			this.rightKeyFactory = rightKeyFactory;
			this.resultFactory = resultFactory;
			this.map = null;
		}

		@Override
		protected final Holder<TResult> internalNext() {
			if (map == null) {
				map = new LinkedHashMap<>();

				while (true) {
					var holder = right.next();

					if (!holder.exists()) {
						break;
					}

					var key = rightKeyFactory.apply(holder.value());
					var values = map.get(key);

					if (values == null) {
						values = new ArrayList<TRight>();
						map.put(key, values);
					}

					values.add(holder.value());
				}
			}

			while (true) {
				var holder = left.next();

				if (!holder.exists()) {
					return Holder.none();
				}

				var key = leftKeyFactory.apply(holder.value());
				var values = map.get(key);

				if (values != null) {
					return Holder.of(resultFactory.apply(holder.value(), Linq.from(values)));
				}
			}
		}

		@Override
		protected final void internalClose() {
			try (var _left = left;
					var _right = right) {
			}
		}
	}

	public static final class OrderFetch<T> extends Fetch<T> {
		private final Fetch<T> fetch;
		private final Comparator<T> comparator;
		private Iterator<T> iterator;

		public OrderFetch(Fetch<T> fetch, Comparator<T> comparator) {
			this.fetch = fetch;
			this.comparator = comparator;
			this.iterator = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (iterator == null) {
				var list = new ArrayList<T>();
				
				while (true) {
					var current = fetch.next();
					
					if (!current.exists()) {
						break;
					}
					
					list.add(current.value());
				}
				
				Collections.sort(list, comparator);
				iterator = list.iterator();
			}

			if (iterator.hasNext()) {
				return Holder.of(iterator.next());
			}

			return Holder.none();
		}

		public final <U extends Comparable<U>> OrderFetch<T> thenBy(final Function<T, U> keySelector) {
			return new OrderFetch<T>(fetch, comparator.thenComparing(keySelector));
		}

		public final <U extends Comparable<U>> OrderFetch<T> thenByDescending(final Function<T, U> keySelector) {
			return new OrderFetch<T>(fetch, comparator.thenComparing(keySelector, Comparator.reverseOrder()));
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class RangeFetch extends Fetch<Integer> {
		private final int start;
		private final int count;
		private int index;

		public RangeFetch(int start, int count) {
			this.start = start;
			this.count = count;
			this.index = 0;
		}

		@Override
		protected final Holder<Integer> internalNext() {
			if (index < count) {
				return Holder.of(start + (index++));
			}
			return Holder.none();
		}

		@Override
		protected final void internalClose() {
		}
	}

	public static final class RepeatFetch<T> extends Fetch<T> {
		private final T value;
		private final int count;
		private int index;

		public RepeatFetch(T value, int count) {
			this.value = value;
			this.count = count;
			this.index = 0;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (index < count) {
				index++;
				return Holder.of(value);
			}
			return Holder.none();
		}

		@Override
		protected final void internalClose() {
		}
	}

	public static final class ReverseFetch<T> extends Fetch<T> {
		private final Fetch<T> fetch;
		private Iterator<Holder<T>> iterator;

		public ReverseFetch(Fetch<T> fetch) {
			this.fetch = fetch;
			this.iterator = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (iterator == null) {
				var list = new ArrayList<Holder<T>>();

				while (true) {
					var holder = fetch.next();

					if (!holder.exists()) {
						break;
					}

					list.add(holder);
				}

				Collections.reverse(list);
				iterator = list.iterator();
			}

			if (iterator.hasNext()) {
				return iterator.next();
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class SelectFetch<T, U> extends Fetch<U> {
		private final Fetch<T> fetch;
		private final Function<T, U> function;

		public SelectFetch(Fetch<T> iterator, Function<T, U> function) {
			this.fetch = iterator;
			this.function = function;
		}

		@Override
		protected final Holder<U> internalNext() {
			var holder = fetch.next();

			if (holder.exists()) {
				return Holder.of(function.apply(holder.value()));
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class SelectManyFetch<T, U> extends Fetch<U> {
		private final Fetch<T> fetch;
		private final Function<T, Fetch<U>> function;
		private Fetch<U> inner;

		public SelectManyFetch(Fetch<T> fetch, Function<T, Fetch<U>> function) {
			this.fetch = fetch;
			this.function = function;
			this.inner = new EmptyFetch<U>();
		}

		@Override
		protected final Holder<U> internalNext() {
			while (true) {
				var holder = inner.next();

				if (holder.exists()) {
					return holder;
				}

				var innerHolder = fetch.next();

				if (!innerHolder.exists()) {
					return holder;
				}

				inner = function.apply(innerHolder.value());
			}
		}

		@Override
		protected final void internalClose() {
			try (var _iterator = fetch;
					var _inner = inner) {
			}
		}
	}

	public static final class SkipFetch<T> extends Fetch<T> {
		private final Fetch<T> fetch;
		private final long count;
		private long index;

		public SkipFetch(Fetch<T> fetch, long count) {
			this.fetch = fetch;
			this.count = count;
			this.index = 0L;
		}

		@Override
		protected final Holder<T> internalNext() {
			while (index < count) {
				var holder = fetch.next();

				if (!holder.exists()) {
					return holder;
				}

				index++;
			}

			return fetch.next();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class SkipLastFetch<T> extends Fetch<T> {
		private final Fetch<T> fetch;
		private final int count;
		private ArrayDeque<Holder<T>> queue;

		public SkipLastFetch(Fetch<T> fetch, int count) {
			this.fetch = fetch;
			this.count = count;
			this.queue = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (queue == null) {
				queue = new ArrayDeque<Holder<T>>(count);

				for (var i = 0; i < count; i++) {
					var current = fetch.next();

					if (!current.exists()) {
						return current;
					}

					queue.addLast(current);
				}
			}

			var current = fetch.next();

			if (current.exists()) {
				var first = queue.removeFirst();
				queue.addLast(current);
				return first;
			}

			return current;
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class SkipWhileFetch<T> extends Fetch<T> {
		private final Fetch<T> fetch;
		private final Predicate<T> predicate;
		private boolean skipped;

		public SkipWhileFetch(Fetch<T> fetch, Predicate<T> predicate) {
			this.fetch = fetch;
			this.predicate = predicate;
			this.skipped = false;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (!skipped) {
				skipped = true;

				while (true) {
					var holder = fetch.next();

					if (!holder.exists()) {
						return holder;
					}

					if (!predicate.test(holder.value())) {
						return holder;
					}
				}
			}

			return fetch.next();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class TakeFetch<T> extends Fetch<T> {
		private final Fetch<T> fetch;
		private final int count;
		private int index;

		public TakeFetch(Fetch<T> fetch, int count) {
			this.fetch = fetch;
			this.count = count;
			this.index = 0;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (index < count) {
				var holder = fetch.next();

				if (!holder.exists()) {
					return holder;
				}

				index++;
				return holder;
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class TakeLastFetch<T> extends Fetch<T> {
		private final Fetch<T> fetch;
		private final int count;
		private Queue<Holder<T>> queue;

		public TakeLastFetch(Fetch<T> fetch, int count) {
			this.fetch = fetch;
			this.count = count;
			this.queue = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (queue == null) {
				queue = new ArrayDeque<Holder<T>>(count);

				while (true) {
					var holder = fetch.next();

					if (!holder.exists()) {
						break;
					}

					if (queue.size() == count) {
						queue.remove();
					}

					queue.add(holder);
				}
			}

			if (queue.size() > 0) {
				return queue.remove();
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class TakeWhileFetch<T> extends Fetch<T> {
		private final Fetch<T> fetch;
		private final Predicate<T> predicate;
		private boolean terminated;

		public TakeWhileFetch(Fetch<T> fetch, Predicate<T> predicate) {
			this.fetch = fetch;
			this.predicate = predicate;
			this.terminated = false;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (terminated) {
				return Holder.none();
			}

			var holder = fetch.next();

			if (holder.exists()) {
				if (predicate.test(holder.value())) {
					return holder;
				} else {
					terminated = true;
					return Holder.none();
				}
			}

			return holder;
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class Tuple2<T1, T2> {
		public final T1 value1;
		public final T2 value2;

		public Tuple2(T1 value1, T2 value2) {
			this.value1 = value1;
			this.value2 = value2;
		}

		@Override
		public final boolean equals(Object obj) {
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
		public final int hashCode() {
			return Objects.hash(value1, value2);
		}

		@Override
		public final String toString() {
			return "Tuple2 [value1=" + value1 + ", value2=" + value2 + "]";
		}
	}

	public static final class TypeFetch<T, U> extends Fetch<U> {
		private final Fetch<T> fetch;
		private final Class<U> type;

		public TypeFetch(Fetch<T> fetch, Class<U> type) {
			this.fetch = fetch;
			this.type = type;
		}

		@Override
		protected final Holder<U> internalNext() {
			while (true) {
				var holder = fetch.next();

				if (!holder.exists()) {
					break;
				}

				if (type.isInstance(holder.value())) {
					return Holder.of(type.cast(holder.value()));
				}
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class UnionByFetch<T, K> extends Fetch<T> {
		private final Fetch<T> left;
		private final Fetch<T> right;
		private final Function<T, K> keyFactory;
		private Iterator<Holder<T>> iterator;

		public UnionByFetch(Fetch<T> left, Fetch<T> right, Function<T, K> keyFactory) {
			this.left = left;
			this.right = right;
			this.keyFactory = keyFactory;
			this.iterator = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (iterator == null) {
				var map = new LinkedHashMap<K, Holder<T>>();

				while (true) {
					var holder = left.next();

					if (!holder.exists()) {
						break;
					}

					map.putIfAbsent(keyFactory.apply(holder.value()), holder);
				}

				while (true) {
					var holder = right.next();

					if (!holder.exists()) {
						break;
					}

					map.putIfAbsent(keyFactory.apply(holder.value()), holder);
				}

				iterator = map.values().iterator();
			}

			if (iterator.hasNext()) {
				return iterator.next();
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			try (var _right = right;
					var _left = left) {
			}
		}
	}

	public static final class UnionFetch<T> extends Fetch<T> {
		private final Fetch<T> left;
		private final Fetch<T> right;
		private Iterator<Holder<T>> iterator;

		public UnionFetch(Fetch<T> left, Fetch<T> right) {
			this.left = left;
			this.right = right;
			this.iterator = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (iterator == null) {
				var set = new LinkedHashSet<Holder<T>>();

				while (true) {
					var holder = left.next();

					if (!holder.exists()) {
						break;
					}

					set.add(holder);
				}

				while (true) {
					var holder = right.next();

					if (!holder.exists()) {
						break;
					}

					set.add(holder);
				}

				iterator = set.iterator();
			}

			if (iterator.hasNext()) {
				return iterator.next();
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			try (var _right = right;
					var _left = left) {
			}
		}
	}

	public static final class WhereFetch<T> extends Fetch<T> {
		private final Fetch<T> fetch;
		private final Predicate<T> predicate;

		public WhereFetch(Fetch<T> fetch, Predicate<T> predicate) {
			this.fetch = fetch;
			this.predicate = predicate;
		}

		@Override
		protected final Holder<T> internalNext() {
			while (true) {
				var holder = fetch.next();

				if (!holder.exists()) {
					return holder;
				}

				if (predicate.test(holder.value())) {
					return holder;
				}
			}
		}

		@Override
		protected void internalClose() {
			fetch.close();
		}
	}

	public static final class ZipFetch<TLeft, TRight> extends Fetch<Tuple2<TLeft, TRight>> {
		private final Fetch<TLeft> left;
		private final Fetch<TRight> right;

		public ZipFetch(Fetch<TLeft> left, Fetch<TRight> right) {
			this.left = left;
			this.right = right;
		}

		@Override
		protected final Holder<Tuple2<TLeft, TRight>> internalNext() {
			var leftHolder = left.next();

			if (!leftHolder.exists()) {
				return Holder.none();
			}

			var rightHolder = right.next();

			if (!rightHolder.exists()) {
				return Holder.none();
			}

			return Holder.of(new Tuple2<TLeft, TRight>(leftHolder.value(), rightHolder.value()));
		}

		@Override
		protected final void internalClose() {
			try (var _right = right;
					var _left = left) {
			}
		}
	}

	public static final class OrderLinq<T> extends Linq<T> {
		private final Supplier<OrderFetch<T>> supplier;

		public OrderLinq(Supplier<OrderFetch<T>> supplier) {
			super(supplier);
			this.supplier = supplier;
		}

		@Override
		public OrderFetch<T> fetch() {
			return supplier.get();
		}

		public final <U extends Comparable<U>> OrderLinq<T> thenBy(final Function<T, U> keySelector) {
			return new OrderLinq<T>(() -> fetch().thenBy(keySelector));
		}

		public final <U extends Comparable<U>> OrderLinq<T> thenByDescending(final Function<T, U> keySelector) {
			return new OrderLinq<T>(() -> fetch().thenByDescending(keySelector));
		}
	}

	public static final <T> Linq<T> empty() {
		return of(() -> new EmptyFetch<T>());
	}

	public static final <T> Linq<T> from(final Iterable<T> iterable) {
		return of(() -> new IterableFetch<T>(iterable));
	}

	public static final <T> Linq<T> from(final Stream<T> stream) {
		return of(() -> new StreamFetch<T>(stream));
	}

	@SafeVarargs
	public static final <T> Linq<T> from(final T... xs) {
		return of(() -> new ArrayFetch<T>(xs));
	}

	public static final Linq<Integer> range(final int start, final int count) {
		return of(() -> new RangeFetch(start, count));
	}

	public static final <T> Linq<T> repeat(final T value, final int count) {
		return of(() -> new RepeatFetch<T>(value, count));
	}

	public final FetchIterator<T> iterator() {
		return new FetchIterator<T>(fetch());
	}

	public final <TRight, TKey, TResult> Linq<TResult> groupJoin(
			Linq<TRight> right,
			Function<T, TKey> leftKeyFactory,
			Function<TRight, TKey> rightKeyFactory,
			BiFunction<T, Linq<TRight>, TResult> resultFactory) {

		return of(() -> new GroupJoinFetch<T, TRight, TKey, TResult>(
				fetch(),
				right.fetch(),
				leftKeyFactory,
				rightKeyFactory,
				resultFactory));
	}

	public final <TRight, TKey, TResult> Linq<TResult> join(
			Linq<TRight> right,
			Function<T, TKey> leftKeyFactory,
			Function<TRight, TKey> rightKeyFactory,
			BiFunction<T, Linq<TRight>, TResult> resultFactory) {

		return of(() -> new JoinFetch<T, TRight, TKey, TResult>(
				fetch(),
				right.fetch(),
				leftKeyFactory,
				rightKeyFactory,
				resultFactory));
	}

	private final Supplier<? extends Fetch<T>> supplier;

	public Linq(Supplier<? extends Fetch<T>> supplier) {
		this.supplier = supplier;
	}

	public Fetch<T> fetch() {
		return supplier.get();
	}

	public static <T> Linq<T> of(Supplier<Fetch<T>> supplier) {
		return new Linq<T>(supplier);
	}

	public final <U> Linq<U> cast() {
		return of(() -> new CastFetch<T, U>(fetch()));
	}

	public final Linq<List<T>> chunk(final int size) {
		return of(() -> new ChunkFetch<T>(fetch(), size));
	}

	public final Linq<T> concat(final Linq<T> right) {
		return of(() -> new ConcatFetch<T>(fetch(), right.fetch()));
	}

	public final Linq<T> append(final T value) {
		return of(() -> new ConcatFetch<T>(fetch(), Linq.from(value).fetch()));
	}

	public final Linq<T> defaultIfEmpty(final T defaultValue) {
		return of(() -> new DefaultIfEmptyFetch<T>(fetch(), defaultValue));
	}

	public final Linq<T> distinct() {
		return of(() -> new DistinctFetch<T>(fetch()));
	}

	public final <K> Linq<T> distinctBy(final Function<T, K> keyFactory) {
		return of(() -> new DistinctByFetch<T, K>(fetch(), keyFactory));
	}

	public final Linq<T> except(final Linq<T> right) {
		return of(() -> new ExceptFetch<T>(fetch(), right.fetch()));
	}

	public final <K> Linq<T> exceptBy(final Linq<T> right, final Function<T, K> keyFactory) {
		return of(() -> new ExceptByFetch<T, K>(fetch(), right.fetch(), keyFactory));
	}

	public final <K> Linq<Entry<K, List<T>>> groupBy(final Function<T, K> keySelector) {
		return of(() -> new GroupByFetch<T, K>(fetch(), keySelector));
	}

	public final Linq<T> intersect(final Linq<T> right) {
		return of(() -> new IntersectFetch<T>(fetch(), right.fetch()));
	}

	public final <TKey> Linq<T> intersectBy(final Linq<TKey> right, final Function<T, TKey> keyFactory) {
		return of(() -> new IntersectByFetch<T, TKey>(fetch(), right.fetch(), keyFactory));
	}

	public final <U> Linq<U> ofType(final Class<U> type) {
		return of(() -> new TypeFetch<T, U>(fetch(), type));
	}

	public final <U extends Comparable<U>> OrderLinq<T> orderBy(final Function<T, U> keySelector) {
		return new OrderLinq<T>(() -> new OrderFetch<T>(fetch(), Comparator.comparing(keySelector)));
	}

	public final <U extends Comparable<U>> OrderLinq<T> orderByDescending(final Function<T, U> keySelector) {
		return new OrderLinq<T>(() -> new OrderFetch<T>(fetch(), Comparator.comparing(keySelector, Comparator.reverseOrder())));
	}

	public final Linq<T> prepend(T value) {
		return of(() -> new ConcatFetch<T>(Linq.from(value).fetch(), fetch()));
	}

	public final Linq<T> reverse() {
		return of(() -> new ReverseFetch<T>(fetch()));
	}

	public final <U> Linq<U> select(final Function<T, U> mapper) {
		return of(() -> new SelectFetch<T, U>(fetch(), mapper));
	}

	public final <U> Linq<U> selectMany(final Function<T, Linq<U>> mapper) {
		return of(() -> new SelectManyFetch<T, U>(fetch(), x -> mapper.apply(x).fetch()));
	}

	public final Linq<T> take(int count) {
		return of(() -> new TakeFetch<T>(fetch(), count));
	}

	public final Linq<T> takeLast(final int size) {
		return of(() -> new TakeLastFetch<T>(fetch(), size));
	}

	public final Linq<T> takeWhile(Predicate<T> predicate) {
		return of(() -> new TakeWhileFetch<T>(fetch(), predicate));
	}

	public final Linq<T> union(final Linq<T> right) {
		return of(() -> new UnionFetch<T>(fetch(), right.fetch()));
	}

	public final <TKey> Linq<T> unionBy(final Linq<T> right, final Function<T, TKey> keyFactory) {
		return of(() -> new UnionByFetch<T, TKey>(fetch(), right.fetch(), keyFactory));
	}

	public final Linq<T> where(final Predicate<T> predicatge) {
		return of(() -> new WhereFetch<T>(fetch(), predicatge));
	}

	public final <TRight> Linq<Tuple2<T, TRight>> zip(final Linq<TRight> right) {
		return of(() -> new ZipFetch<T, TRight>(fetch(), right.fetch()));
	}

	public final Linq<T> skip(final long count) {
		return of(() -> new SkipFetch<T>(fetch(), count));
	}

	public final Linq<T> skipLast(final int size) {
		return of(() -> new SkipLastFetch<T>(fetch(), size));
	}

	public final Linq<T> skipWhile(Predicate<T> predicate) {
		return of(() -> new SkipWhileFetch<T>(fetch(), predicate));
	}

	public final T aggregate(BiFunction<T, T, T> func) {
		try (var fetch = fetch()) {
			var first = fetch.next();

			if (!first.exists()) {
				throw new NoSuchElementException();
			}

			var result = first.value();

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return result;
				}

				result = func.apply(result, current.value());
			}
		}
	}

	public final T aggregate(T seed, BiFunction<T, T, T> func) {
		try (var fetch = fetch()) {
			var result = seed;

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return result;
				}

				result = func.apply(result, current.value());
			}
		}
	}

	public final boolean all(Predicate<T> predicate) {
		try (var fetch = fetch()) {
			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return true;
				}

				if (!predicate.test(current.value())) {
					return false;
				}
			}
		}
	}

	public final boolean any() {
		try (var fetch = fetch()) {
			return fetch.next().exists();
		}
	}

	public final boolean any(Predicate<T> predicate) {
		try (var fetch = fetch()) {
			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return false;
				}

				if (predicate.test(current.value())) {
					return true;
				}
			}
		}
	}

	public final Long average(Function<T, Long> func) {
		try (var fetch = fetch()) {
			var sum = 0L;
			var count = 0L;

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return sum / count;
				}

				sum += func.apply(current.value());
				count++;
			}
		}
	}

	public final boolean contains(T target) {
		try (var fetch = fetch()) {
			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return false;
				}

				if (Objects.equals(target, current.value())) {
					return true;
				}
			}
		}
	}

	public final long count() {
		try (var fetch = fetch()) {
			var count = 0L;

			while (fetch.next().exists()) {
				count++;
			}

			return count;
		}
	}

	public final T elementAt(long index) {
		try (var fetch = fetch()) {
			var count = 0L;

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					throw new IndexOutOfBoundsException();
				}

				if (count == index) {
					return current.value();
				}

				count++;
			}
		}
	}

	public final T elementAtOrDefault(long index, T defaultValue) {
		try (var fetch = fetch()) {
			var count = 0L;

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return defaultValue;
				}

				if (count == index) {
					return current.value();
				}

				count++;
			}
		}
	}

	public final T first() {
		try (var fetch = fetch()) {
			var first = fetch.next();

			if (!first.exists()) {
				throw new NoSuchElementException();
			}

			return first.value();
		}
	}

	public final T firstOrDefault(T defaultValue) {
		try (var fetch = fetch()) {
			var first = fetch.next();

			if (first.exists()) {
				return first.value();
			} else {
				return defaultValue;
			}
		}
	}

	public final T last() {
		try (var fetch = fetch()) {
			var last = fetch.next();

			if (!last.exists()) {
				throw new NoSuchElementException();
			}

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return last.value();
				}

				last = current;
			}
		}
	}

	public final T lastOrDefault(T defaultValue) {
		try (var fetch = fetch()) {
			var last = fetch.next();

			if (last.exists()) {
				while (true) {
					var current = fetch.next();

					if (!current.exists()) {
						return last.value();
					}

					last = current;
				}
			} else {
				return defaultValue;
			}
		}
	}

	public final T max(Comparator<T> comparator) {
		try (var fetch = fetch()) {
			var max = fetch.next();

			if (!max.exists()) {
				throw new NoSuchElementException();
			}

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return max.value();
				}

				if (comparator.compare(max.value(), current.value()) < 0) {
					max = current;
				}
			}
		}
	}

	public final <TKey extends Comparable<TKey>> T maxBy(Function<T, TKey> keyFactory) {
		try (var fetch = fetch()) {
			var max = fetch.next();

			if (!max.exists()) {
				throw new NoSuchElementException();
			}

			var maxKey = keyFactory.apply(max.value());

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return max.value();
				}

				var currentKey = keyFactory.apply(current.value());

				if (maxKey.compareTo(currentKey) < 0) {
					max = current;
					maxKey = currentKey;
				}
			}
		}
	}

	public final <TKey> T maxBy(Function<T, TKey> keyFactory, Comparator<TKey> keyComparator) {
		try (var fetch = fetch()) {
			var max = fetch.next();

			if (!max.exists()) {
				throw new NoSuchElementException();
			}

			var maxKey = keyFactory.apply(max.value());

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return max.value();
				}

				var currentKey = keyFactory.apply(current.value());

				if (keyComparator.compare(maxKey, currentKey) < 0) {
					max = current;
					maxKey = currentKey;
				}
			}
		}
	}

	public final T min(Comparator<T> comparator) {
		try (var fetch = fetch()) {
			var min = fetch.next();

			if (!min.exists()) {
				throw new NoSuchElementException();
			}

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return min.value();
				}

				if (comparator.compare(min.value(), current.value()) > 0) {
					min = current;
				}
			}
		}
	}

	public final <TKey extends Comparable<TKey>> T minBy(Function<T, TKey> keyFactory) {
		try (var fetch = fetch()) {
			var min = fetch.next();

			if (!min.exists()) {
				throw new NoSuchElementException();
			}

			var minKey = keyFactory.apply(min.value());

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return min.value();
				}

				var currentKey = keyFactory.apply(current.value());

				if (minKey.compareTo(currentKey) > 0) {
					min = current;
					minKey = currentKey;
				}
			}
		}
	}

	public final <TKey> T minBy(Function<T, TKey> keyFactory, Comparator<TKey> keyComparator) {
		try (var fetch = fetch()) {
			var min = fetch.next();

			if (!min.exists()) {
				throw new NoSuchElementException();
			}

			var minKey = keyFactory.apply(min.value());

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return min.value();
				}

				var currentKey = keyFactory.apply(current.value());

				if (keyComparator.compare(minKey, currentKey) > 0) {
					min = current;
					minKey = currentKey;
				}
			}
		}
	}

	public final boolean sequenceEqual(Linq<T> right) {
		try (var _right = right.fetch();
				var _left = fetch()) {

			while (true) {
				var l = _left.next();
				var r = _right.next();

				if (!l.exists() && !r.exists()) {
					return true;
				}

				if (!Objects.equals(l, r)) {
					return false;
				}
			}
		}
	}

	public final T single() {
		try (var fetch = fetch()) {
			var current = fetch.next();

			if (!current.exists()) {
				throw new IllegalStateException("値が存在しません");
			}

			if (fetch.next().exists()) {
				throw new IllegalStateException("値が複数存在します");
			}

			return current.value();
		}
	}

	public final T singleOrDefault(T defaultValue) {
		try (var fetch = fetch()) {
			var current = fetch.next();

			if (!current.exists()) {
				return defaultValue;
			}

			if (fetch.next().exists()) {
				throw new IllegalStateException("値が複数存在します");
			}

			return current.value();
		}
	}

	public final long sum(Function<T, Long> func) {
		try (var fetch = fetch()) {
			var result = 0L;

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return result;
				}

				result += func.apply(current.value());
			}
		}
	}

	public final void forEach(Consumer<? super T> consumer) {
		try (var fetch = fetch()) {
			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return;
				}

				consumer.accept(current.value());
			}
		}
	}

	public final T[] toArray(T[] array) {
		try (var fetch = fetch()) {
			var list = new ArrayList<T>();

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return list.toArray(array);
				}

				list.add(current.value());
			}
		}
	}

	public final <K> LinkedHashMap<K, T> toDictionary(Function<T, K> keyFactory) {
		try (var fetch = fetch()) {
			var map = new LinkedHashMap<K, T>();

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return map;
				}

				var key = keyFactory.apply(current.value());

				if (map.containsKey(key)) {
					throw new IllegalArgumentException("キーが重複しています: " + key);
				}

				map.put(key, current.value());
			}
		}
	}

	public final LinkedHashSet<T> toHashSet() {
		try (var fetch = fetch()) {
			var set = new LinkedHashSet<T>();

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return set;
				}

				set.add(current.value());
			}
		}
	}

	public final ArrayList<T> toList() {
		try (var fetch = fetch()) {
			var list = new ArrayList<T>();

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return list;
				}

				list.add(current.value());
			}
		}
	}

	public final <K> LinkedHashMap<K, List<T>> toLookup(Function<T, K> keyFactory) {
		try (var fetch = fetch()) {
			var map = new LinkedHashMap<K, List<T>>();

			while (true) {
				var current = fetch.next();

				if (!current.exists()) {
					return map;
				}

				var key = keyFactory.apply(current.value());
				var list = map.get(key);

				if (list == null) {
					list = new ArrayList<T>();
					map.put(key, list);
				}

				list.add(current.value());
			}
		}
	}

}
