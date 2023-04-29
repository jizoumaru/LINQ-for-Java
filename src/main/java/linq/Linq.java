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
import java.util.stream.Stream;

@FunctionalInterface
public interface Linq<T> {
	public static final class AppendFetch<T> extends Fetch<T> {
		private final FetchBase<T> fetch;
		private final T value;
		private boolean appended;

		public AppendFetch(FetchBase<T> fetch, T value) {
			this.fetch = fetch;
			this.value = value;
			this.appended = false;
		}

		@Override
		protected final Holder<T> internalNext() {
			var holder = fetch.next();

			if (holder.exists()) {
				return holder;
			}

			if (!appended) {
				appended = true;
				return Holder.of(value);
			}

			return Holder.none();
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

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
		private final FetchBase<T> fetch;

		public CastFetch(FetchBase<T> fetch) {
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
		private final FetchBase<T> fetch;
		private final int size;

		public ChunkFetch(FetchBase<T> fetch, int size) {
			this.fetch = fetch;
			this.size = size;
		}

		@Override
		protected final Holder<List<T>> internalNext() {
			var list = new ArrayList<T>();

			for (var i = 0; i < size; i++) {
				var holder = fetch.next();

				if (!holder.exists()) {
					break;
				}

				list.add(holder.value());
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
		private final FetchBase<T> left;
		private final FetchBase<T> right;
		private FetchBase<T> fetch;

		public ConcatFetch(FetchBase<T> left, FetchBase<T> right) {
			this.left = left;
			this.right = right;
			this.fetch = left;
		}

		@Override
		protected final Holder<T> internalNext() {
			var holder = fetch.next();

			if (holder.exists()) {
				return holder;
			} else {
				if (fetch == left) {
					fetch.close();
					fetch = right;
					return fetch.next();
				}
				return holder;
			}
		}

		@Override
		protected final void internalClose() {
			try (var _right = right;
					var _left = left) {
			}
		}

	}

	public static final class DefaultIfEmptyFetch<T> extends Fetch<T> {
		private final FetchBase<T> fetch;
		private final T defaultValue;
		private State state;

		enum State {
			DEFAULT, EMPTY, FETCH
		}

		public DefaultIfEmptyFetch(FetchBase<T> fetch, T defaultValue) {
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
		private final FetchBase<T> fetch;
		private final Function<T, K> keyFactory;
		private Iterator<Holder<T>> iterator;

		public DistinctByFetch(FetchBase<T> fetch, Function<T, K> keyFactory) {
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
		private final FetchBase<T> fetch;
		private Iterator<Holder<T>> iterator;

		public DistinctFetch(FetchBase<T> fetch) {
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
		private final FetchBase<T> left;
		private final FetchBase<T> right;
		private final Function<T, K> keyFactory;
		private Map<K, Holder<T>> map;

		public ExceptByFetch(FetchBase<T> left, FetchBase<T> right, Function<T, K> keyFactory) {
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
		private final FetchBase<T> left;
		private final FetchBase<T> right;
		private Set<Holder<T>> set;

		public ExceptFetch(FetchBase<T> left, FetchBase<T> right) {
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

	public static abstract class Fetch<T> extends FetchBase<T> {

		public static final <T> EmptyFetch<T> empty() {
			return new EmptyFetch<T>();
		}

		public static final <T> IterableFetch<T> from(final Iterable<T> iterable) {
			return new IterableFetch<T>(iterable);
		}

		public static final <T> StreamFetch<T> from(final Stream<T> stream) {
			return new StreamFetch<T>(stream);
		}

		@SafeVarargs
		public static final <T> ArrayFetch<T> from(final T... xs) {
			return new ArrayFetch<T>(xs);
		}

		public static final RangeFetch range(final int start, final int count) {
			return new RangeFetch(start, count);
		}

		public static final <T> RepeatFetch<T> repeat(final T value, final int count) {
			return new RepeatFetch<T>(value, count);
		}

		public final FetchIterator<T> iterator() {
			return new FetchIterator<T>(this);
		}

		public final <TRight, TKey, TResult> GroupJoinFetch<T, TRight, TKey, TResult> groupJoin(
				FetchBase<TRight> right,
				Function<T, TKey> leftKeyFactory,
				Function<TRight, TKey> rightKeyFactory,
				BiFunction<T, Fetch<TRight>, TResult> resultFactory) {

			return new GroupJoinFetch<T, TRight, TKey, TResult>(
					this,
					right,
					leftKeyFactory,
					rightKeyFactory,
					resultFactory);
		}

		public final <TRight, TKey, TResult> JoinFetch<T, TRight, TKey, TResult> join(
				FetchBase<TRight> right,
				Function<T, TKey> leftKeyFactory,
				Function<TRight, TKey> rightKeyFactory,
				BiFunction<T, Fetch<TRight>, TResult> resultFactory) {

			return new JoinFetch<T, TRight, TKey, TResult>(
					this,
					right,
					leftKeyFactory,
					rightKeyFactory,
					resultFactory);
		}

		public final <U> CastFetch<T, U> cast() {
			return new CastFetch<T, U>(this);
		}

		public final ChunkFetch<T> chunk(final int size) {
			return new ChunkFetch<T>(this, size);
		}

		public final ConcatFetch<T> concat(final Fetch<T> right) {
			return new ConcatFetch<T>(this, right);
		}

		public final AppendFetch<T> append(final T value) {
			return new AppendFetch<T>(this, value);
		}

		public final DefaultIfEmptyFetch<T> defaultIfEmpty(final T defaultValue) {
			return new DefaultIfEmptyFetch<T>(this, defaultValue);
		}

		public final DistinctFetch<T> distinct() {
			return new DistinctFetch<T>(this);
		}

		public final <K> DistinctByFetch<T, K> distinctBy(final Function<T, K> keyFactory) {
			return new DistinctByFetch<T, K>(this, keyFactory);
		}

		public final ExceptFetch<T> except(final Fetch<T> right) {
			return new ExceptFetch<T>(this, right);
		}

		public final <K> ExceptByFetch<T, K> exceptBy(final Fetch<T> right, final Function<T, K> keyFactory) {
			return new ExceptByFetch<T, K>(this, right, keyFactory);
		}

		public final <K> GroupByFetch<T, K> groupBy(final Function<T, K> keySelector) {
			return new GroupByFetch<T, K>(this, keySelector);
		}

		public final IntersectFetch<T> intersect(final Fetch<T> right) {
			return new IntersectFetch<T>(this, right);
		}

		public final <TKey> IntersectByFetch<T, TKey> intersectBy(final Fetch<TKey> right, final Function<T, TKey> keyFactory) {
			return new IntersectByFetch<T, TKey>(this, right, keyFactory);
		}

		public final <U> TypeFetch<T, U> ofType(final Class<U> type) {
			return new TypeFetch<T, U>(this, type);
		}

		public final <U extends Comparable<U>> OrderFetch<T> orderBy(final Function<T, U> keySelector) {
			return new OrderFetch<T>(this, Comparator.comparing(keySelector));
		}

		public final <U extends Comparable<U>> OrderFetch<T> orderByDescending(final Function<T, U> keySelector) {
			return new OrderFetch<T>(this, Comparator.comparing(keySelector, Comparator.reverseOrder()));
		}

		public final PrependFetch<T> prepend(T value) {
			return new PrependFetch<T>(this, value);
		}

		public final ReverseFetch<T> reverse() {
			return new ReverseFetch<T>(this);
		}

		public final <U> SelectFetch<T, U> select(final Function<T, U> mapper) {
			return new SelectFetch<T, U>(this, mapper);
		}

		public final <U> SelectManyFetch<T, U> selectMany(final Function<T, FetchBase<U>> mapper) {
			return new SelectManyFetch<T, U>(this, mapper);
		}

		public final TakeFetch<T> take(int count) {
			return new TakeFetch<T>(this, count);
		}

		public final TakeLastFetch<T> takeLast(final int size) {
			return new TakeLastFetch<T>(this, size);
		}

		public final TakeWhileFetch<T> takeWhile(Predicate<T> predicate) {
			return new TakeWhileFetch<T>(this, predicate);
		}

		public final UnionFetch<T> union(final Linq<T> right) {
			return new UnionFetch<T>(this, right.fetch());
		}

		public final <TKey> UnionByFetch<T, TKey> unionBy(final Linq<T> right, final Function<T, TKey> keyFactory) {
			return new UnionByFetch<T, TKey>(this, right.fetch(), keyFactory);
		}

		public final WhereFetch<T> where(final Predicate<T> predicatge) {
			return new WhereFetch<T>(this, predicatge);
		}

		public final <TRight> ZipFetch<T, TRight> zip(final Linq<TRight> right) {
			return new ZipFetch<T, TRight>(this, right.fetch());
		}

		public final SkipFetch<T> skip(final long count) {
			return new SkipFetch<T>(this, count);
		}

		public final SkipLastFetch<T> skipLast(final int size) {
			return new SkipLastFetch<T>(this, size);
		}

		public final SkipWhileFetch<T> skipWhile(Predicate<T> predicate) {
			return new SkipWhileFetch<T>(this, predicate);
		}

	}

	public static abstract class FetchBase<T> implements AutoCloseable {
		private boolean closed;
		private Holder<T> peek;

		public final Holder<T> peek() {
			if (peek == null) {
				peek = internalNext();
			}
			return peek;
		}

		public final Holder<T> next() {
			if (peek == null) {
				return internalNext();
			} else {
				var current = peek;
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

		public final T aggregate(BiFunction<T, T, T> func) {
			try (var _this = this) {
				var result = next().value();

				while (true) {
					var current = next();

					if (!current.exists()) {
						return result;
					}

					result = func.apply(result, current.value());
				}
			}
		}

		public final T aggregate(T seed, BiFunction<T, T, T> func) {
			try (var _this = this) {
				var result = seed;

				while (true) {
					var current = next();

					if (!current.exists()) {
						return result;
					}

					result = func.apply(result, current.value());
				}
			}
		}

		public final boolean all(Predicate<T> predicate) {
			try (var _this = this) {
				while (true) {
					var current = next();

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
			try (var _this = this) {
				return next().exists();
			}
		}

		public final boolean any(Predicate<T> predicate) {
			try (var _this = this) {
				while (true) {
					var current = next();

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
			try (var _this = this) {
				var sum = 0L;
				var count = 0L;

				while (true) {
					var current = next();

					if (!current.exists()) {
						return sum / count;
					}

					sum += func.apply(current.value());
					count++;
				}
			}
		}

		public final boolean contains(T target) {
			try (var _this = this) {
				while (true) {
					var current = next();

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
			try (var _this = this) {
				var count = 0L;

				while (next().exists()) {
					count++;
				}

				return count;
			}
		}

		public final T elementAt(long index) {
			try (var _this = this) {
				var count = 0L;

				while (true) {
					var current = next();

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
			try (var _this = this) {
				var count = 0L;

				while (true) {
					var current = next();

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
			try (var _this = this) {
				return next().value();
			}
		}

		public final T firstOrDefault(T defaultValue) {
			try (var _this = this) {
				var first = next();

				if (first.exists()) {
					return first.value();
				} else {
					return defaultValue;
				}
			}
		}

		public final T last() {
			try (var _this = this) {
				var last = next();

				if (last.exists()) {
					while (true) {
						var current = next();

						if (!current.exists()) {
							return last.value();
						}

						last = current;
					}
				} else {
					return last.value();
				}
			}
		}

		public final T lastOrDefault(T defaultValue) {
			try (var _this = this) {
				var last = next();

				if (last.exists()) {
					while (true) {
						var current = next();

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
			try (var _this = this) {
				var max = next();

				while (true) {
					var current = next();

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
			try (var _this = this) {
				var max = next();
				var maxKey = keyFactory.apply(max.value());

				while (true) {
					var current = next();

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
			try (var _this = this) {
				var max = next();
				var maxKey = keyFactory.apply(max.value());

				while (true) {
					var current = next();

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
			try (var _this = this) {
				var min = next();

				while (true) {
					var current = next();

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
			try (var _this = this) {
				var min = next();
				var minKey = keyFactory.apply(min.value());

				while (true) {
					var current = next();

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
			try (var _this = this) {
				var min = next();
				var minKey = keyFactory.apply(min.value());

				while (true) {
					var current = next();

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

		public final boolean sequenceEqual(Fetch<T> right) {
			try (var _right = right;
					var _left = this) {

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
			try (var _this = this) {
				var current = next();

				if (!current.exists()) {
					throw new IllegalStateException("値が存在しません");
				}

				if (next().exists()) {
					throw new IllegalStateException("値が複数存在します");
				}

				return current.value();
			}
		}

		public final T singleOrDefault(T defaultValue) {
			try (var _this = this) {
				var current = next();

				if (!current.exists()) {
					return defaultValue;
				}

				if (next().exists()) {
					throw new IllegalStateException("値が複数存在します");
				}

				return current.value();
			}
		}

		public final long sum(Function<T, Long> func) {
			try (var _this = this) {
				var result = 0L;

				while (true) {
					var current = next();

					if (!current.exists()) {
						return result;
					}

					result += func.apply(current.value());
				}
			}
		}

		public final void forEach(Consumer<? super T> consumer) {
			try (var _this = this) {
				while (true) {
					var current = next();

					if (!current.exists()) {
						return;
					}

					consumer.accept(current.value());
				}
			}
		}

		public final T[] toArray(T[] array) {
			try (var _this = this) {
				var list = new ArrayList<T>();

				while (true) {
					var current = next();

					if (!current.exists()) {
						return list.toArray(array);
					}

					list.add(current.value());
				}
			}
		}

		public final <K> LinkedHashMap<K, T> toDictionary(Function<T, K> keyFactory) {
			try (var _this = this) {
				var map = new LinkedHashMap<K, T>();

				while (true) {
					var current = next();

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
			try (var _this = this) {
				var set = new LinkedHashSet<T>();

				while (true) {
					var current = next();

					if (!current.exists()) {
						return set;
					}

					set.add(current.value());
				}
			}
		}

		public final ArrayList<T> toList() {
			try (var _this = this) {
				var list = new ArrayList<T>();

				while (true) {
					var current = next();

					if (!current.exists()) {
						return list;
					}

					list.add(current.value());
				}
			}
		}

		public final <K> LinkedHashMap<K, List<T>> toLookup(Function<T, K> keyFactory) {
			try (var _this = this) {
				var map = new LinkedHashMap<K, List<T>>();

				while (true) {
					var current = next();

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

	public static final class FetchIterator<T> implements Iterator<T>, AutoCloseable {
		private final FetchBase<T> fetch;

		public FetchIterator(FetchBase<T> fetch) {
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
		private final FetchBase<T> fetch;
		private final Function<T, K> keyFactory;
		private Iterator<Entry<K, List<T>>> entries;

		public GroupByFetch(FetchBase<T> fetch, Function<T, K> keyFactory) {
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
		private final FetchBase<TLeft> left;
		private final FetchBase<TRight> right;
		private final Function<TLeft, TKey> leftKeyFactory;
		private final Function<TRight, TKey> rightKeyFactory;
		private final BiFunction<TLeft, Fetch<TRight>, TResult> resultFactory;
		private Map<TKey, List<TRight>> map;

		public GroupJoinFetch(
				FetchBase<TLeft> left,
				FetchBase<TRight> right,
				Function<TLeft, TKey> leftKeyFactory,
				Function<TRight, TKey> rightKeyFactory,
				BiFunction<TLeft, Fetch<TRight>, TResult> resultFactory) {
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

				return Holder.of(resultFactory.apply(holder.value(), Fetch.from(values)));
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
		private final FetchBase<TLeft> left;
		private final FetchBase<TKey> right;
		private final Function<TLeft, TKey> keyFactory;
		private Set<TKey> set;

		public IntersectByFetch(
				FetchBase<TLeft> left,
				FetchBase<TKey> right,
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
		private final FetchBase<T> left;
		private final FetchBase<T> right;
		private Set<Holder<T>> set;

		public IntersectFetch(FetchBase<T> left, FetchBase<T> right) {
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
		private final FetchBase<TLeft> left;
		private final FetchBase<TRight> right;
		private final Function<TLeft, TKey> leftKeyFactory;
		private final Function<TRight, TKey> rightKeyFactory;
		private final BiFunction<TLeft, Fetch<TRight>, TResult> resultFactory;
		private Map<TKey, List<TRight>> map;

		public JoinFetch(
				FetchBase<TLeft> left,
				FetchBase<TRight> right,
				Function<TLeft, TKey> leftKeyFactory,
				Function<TRight, TKey> rightKeyFactory,
				BiFunction<TLeft, Fetch<TRight>, TResult> resultFactory) {
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
					return Holder.of(resultFactory.apply(holder.value(), Fetch.from(values)));
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
		private final FetchBase<T> fetch;
		private final Comparator<T> comparator;
		private Iterator<T> iterator;

		public OrderFetch(FetchBase<T> fetch, Comparator<T> comparator) {
			this.fetch = fetch;
			this.comparator = comparator;
			this.iterator = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (iterator == null) {
				var list = fetch.toList();
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

	public static final class PrependFetch<T> extends Fetch<T> {
		private final FetchBase<T> fetch;
		private final T value;
		private boolean prepended;

		public PrependFetch(FetchBase<T> fetch, T value) {
			this.fetch = fetch;
			this.value = value;
			this.prepended = false;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (!prepended) {
				prepended = true;
				return Holder.of(value);
			}
			return fetch.next();
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
		private final FetchBase<T> fetch;
		private Iterator<Holder<T>> iterator;

		public ReverseFetch(FetchBase<T> fetch) {
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
		private final FetchBase<T> fetch;
		private final Function<T, U> function;

		public SelectFetch(FetchBase<T> iterator, Function<T, U> function) {
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
		private final FetchBase<T> fetch;
		private final Function<T, FetchBase<U>> function;
		private FetchBase<U> inner;

		public SelectManyFetch(FetchBase<T> fetch, Function<T, FetchBase<U>> function) {
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

				inner.close();
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
		private final FetchBase<T> fetch;
		private final long count;
		private long index;

		public SkipFetch(FetchBase<T> fetch, long count) {
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
		private final FetchBase<T> fetch;
		private final int count;
		private Queue<Holder<T>> queue;

		public SkipLastFetch(FetchBase<T> fetch, int count) {
			this.fetch = fetch;
			this.count = count;
			this.queue = null;
		}

		@Override
		protected final Holder<T> internalNext() {
			if (queue == null) {
				queue = new ArrayDeque<Holder<T>>(count);

				for (var i = 0; i < count; i++) {
					var holder = fetch.next();

					if (!holder.exists()) {
						break;
					}

					queue.add(holder);
				}
			}

			var holder = fetch.next();

			if (holder.exists()) {
				var value = queue.remove();
				queue.add(holder);
				return value;
			}

			return holder;
		}

		@Override
		protected final void internalClose() {
			fetch.close();
		}
	}

	public static final class SkipWhileFetch<T> extends Fetch<T> {
		private final FetchBase<T> fetch;
		private final Predicate<T> predicate;
		private boolean skipped;

		public SkipWhileFetch(FetchBase<T> fetch, Predicate<T> predicate) {
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
		private final FetchBase<T> fetch;
		private final int count;
		private int index;

		public TakeFetch(FetchBase<T> fetch, int count) {
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
		private final FetchBase<T> fetch;
		private final int count;
		private Queue<Holder<T>> queue;

		public TakeLastFetch(FetchBase<T> fetch, int count) {
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
		private final FetchBase<T> fetch;
		private final Predicate<T> predicate;
		private boolean terminated;

		public TakeWhileFetch(FetchBase<T> fetch, Predicate<T> predicate) {
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
		private final FetchBase<T> fetch;
		private final Class<U> type;

		public TypeFetch(FetchBase<T> fetch, Class<U> type) {
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
		private final FetchBase<T> left;
		private final FetchBase<T> right;
		private final Function<T, K> keyFactory;
		private Iterator<Holder<T>> iterator;

		public UnionByFetch(FetchBase<T> left, FetchBase<T> right, Function<T, K> keyFactory) {
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
		private final FetchBase<T> left;
		private final FetchBase<T> right;
		private Iterator<Holder<T>> iterator;

		public UnionFetch(FetchBase<T> left, FetchBase<T> right) {
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
		private final FetchBase<T> fetch;
		private final Predicate<T> predicate;

		public WhereFetch(FetchBase<T> fetch, Predicate<T> predicate) {
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
		private final FetchBase<TLeft> left;
		private final FetchBase<TRight> right;

		public ZipFetch(FetchBase<TLeft> left, FetchBase<TRight> right) {
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

	@FunctionalInterface
	public interface OrderLinq<T> extends Linq<T> {
		@Override
		OrderFetch<T> fetch();

		public default <U extends Comparable<U>> OrderLinq<T> thenBy(final Function<T, U> keySelector) {
			return () -> fetch().thenBy(keySelector);
		}

		public default <U extends Comparable<U>> OrderLinq<T> thenByDescending(final Function<T, U> keySelector) {
			return () -> fetch().thenByDescending(keySelector);
		}
	}

	public static <T> Linq<T> empty() {
		return () -> Fetch.empty();
	}

	public static <T> Linq<T> from(final Iterable<T> iterable) {
		return () -> Fetch.from(iterable);
	}

	public static <T> Linq<T> from(final Stream<T> stream) {
		return () -> Fetch.from(stream);
	}

	@SafeVarargs
	public static <T> Linq<T> from(final T... xs) {
		return () -> Fetch.from(xs);
	}

	public static Linq<Integer> range(final int start, final int count) {
		return () -> Fetch.range(start, count);
	}

	public static <T> Linq<T> repeat(final T value, final int count) {
		return () -> Fetch.repeat(value, count);
	}

	Fetch<T> fetch();

	public default <TRight, TKey, TResult> Linq<TResult> groupJoin(Linq<TRight> right, Function<T, TKey> leftKeyFactory, Function<TRight, TKey> rightKeyFactory, BiFunction<T, Linq<TRight>, TResult> resultFactory) {
		return () -> fetch().groupJoin(right.fetch(), leftKeyFactory, rightKeyFactory, (l, r) -> resultFactory.apply(l, () -> r));
	}

	public default T aggregate(BiFunction<T, T, T> func) {
		return fetch().aggregate(func);
	}

	public default T aggregate(T seed, BiFunction<T, T, T> func) {
		return fetch().aggregate(seed, func);
	}

	public default <TRight, TKey, TResult> Linq<TResult> join(Linq<TRight> right, Function<T, TKey> leftKeyFactory, Function<TRight, TKey> rightKeyFactory, BiFunction<T, Linq<TRight>, TResult> resultFactory) {
		return () -> fetch().join(right.fetch(), leftKeyFactory, rightKeyFactory, (l, r) -> resultFactory.apply(l, () -> r));
	}

	public default boolean all(Predicate<T> predicate) {
		return fetch().all(predicate);
	}

	public default <U> Linq<U> cast() {
		return () -> fetch().cast();
	}

	public default boolean any() {
		return fetch().any();
	}

	public default boolean any(Predicate<T> predicate) {
		return fetch().any(predicate);
	}

	public default Linq<List<T>> chunk(int size) {
		return () -> fetch().chunk(size);
	}

	public default Linq<T> concat(Linq<T> right) {
		return () -> fetch().concat(right.fetch());
	}

	public default Linq<T> append(T value) {
		return () -> fetch().append(value);
	}

	public default Long average(Function<T, Long> func) {
		return fetch().average(func);
	}

	public default Linq<T> defaultIfEmpty(T defaultValue) {
		return () -> fetch().defaultIfEmpty(defaultValue);
	}

	public default Linq<T> distinct() {
		return () -> fetch().distinct();
	}

	public default <K> Linq<T> distinctBy(Function<T, K> keyFactory) {
		return () -> fetch().distinctBy(keyFactory);
	}

	public default boolean contains(T target) {
		return fetch().contains(target);
	}

	public default Linq<T> except(Linq<T> right) {
		return () -> fetch().except(right.fetch());
	}

	public default <K> Linq<T> exceptBy(Linq<T> right, Function<T, K> keyFactory) {
		return () -> fetch().exceptBy(right.fetch(), keyFactory);
	}

	public default long count() {
		return fetch().count();
	}

	public default <K> Linq<Entry<K, List<T>>> groupBy(Function<T, K> keySelector) {
		return () -> fetch().groupBy(keySelector);
	}

	public default T elementAt(long index) {
		return fetch().elementAt(index);
	}

	public default Linq<T> intersect(Linq<T> right) {
		return () -> fetch().intersect(right.fetch());
	}

	public default <TKey> Linq<T> intersectBy(Linq<TKey> right, Function<T, TKey> keyFactory) {
		return () -> fetch().intersectBy(right.fetch(), keyFactory);
	}

	public default T elementAtOrDefault(long index, T defaultValue) {
		return fetch().elementAtOrDefault(index, defaultValue);
	}

	public default <U> Linq<U> ofType(Class<U> type) {
		return () -> fetch().ofType(type);
	}

	public default <U extends Comparable<U>> OrderLinq<T> orderBy(Function<T, U> keySelector) {
		return () -> fetch().orderBy(keySelector);
	}

	public default T first() {
		return fetch().first();
	}

	public default <U extends Comparable<U>> OrderLinq<T> orderByDescending(Function<T, U> keySelector) {
		return () -> fetch().orderByDescending(keySelector);
	}

	public default T firstOrDefault(T defaultValue) {
		return fetch().firstOrDefault(defaultValue);
	}

	public default Linq<T> prepend(T value) {
		return () -> fetch().prepend(value);
	}

	public default T last() {
		return fetch().last();
	}

	public default Linq<T> reverse() {
		return () -> fetch().reverse();
	}

	public default <U> Linq<U> select(Function<T, U> mapper) {
		return () -> fetch().select(mapper);
	}

	public default <U> Linq<U> selectMany(Function<T, Linq<U>> mapper) {
		return () -> fetch().selectMany(x -> mapper.apply(x).fetch());
	}

	public default T lastOrDefault(T defaultValue) {
		return fetch().lastOrDefault(defaultValue);
	}

	public default Linq<T> take(int count) {
		return () -> fetch().take(count);
	}

	public default Linq<T> takeLast(int size) {
		return () -> fetch().takeLast(size);
	}

	public default T max(Comparator<T> comparator) {
		return fetch().max(comparator);
	}

	public default Linq<T> takeWhile(Predicate<T> predicate) {
		return () -> fetch().takeWhile(predicate);
	}

	public default Linq<T> union(Linq<T> right) {
		return () -> fetch().union(right);
	}

	public default <TKey> Linq<T> unionBy(Linq<T> right, Function<T, TKey> keyFactory) {
		return () -> fetch().unionBy(right, keyFactory);
	}

	public default <TKey extends Comparable<TKey>> T maxBy(Function<T, TKey> keyFactory) {
		return fetch().maxBy(keyFactory);
	}

	public default <TKey> T maxBy(Function<T, TKey> keyFactory, Comparator<TKey> keyComparator) {
		return fetch().maxBy(keyFactory, keyComparator);
	}

	public default Linq<T> where(Predicate<T> predicatge) {
		return () -> fetch().where(predicatge);
	}

	public default <TRight> Linq<Tuple2<T, TRight>> zip(Linq<TRight> right) {
		return () -> fetch().zip(right);
	}

	public default Linq<T> skip(long count) {
		return () -> fetch().skip(count);
	}

	public default Linq<T> skipLast(int size) {
		return () -> fetch().skipLast(size);
	}

	public default T min(Comparator<T> comparator) {
		return fetch().min(comparator);
	}

	public default Linq<T> skipWhile(Predicate<T> predicate) {
		return () -> fetch().skipWhile(predicate);
	}

	public default <TKey extends Comparable<TKey>> T minBy(Function<T, TKey> keyFactory) {
		return fetch().minBy(keyFactory);
	}

	public default <TKey> T minBy(Function<T, TKey> keyFactory, Comparator<TKey> keyComparator) {
		return fetch().minBy(keyFactory, keyComparator);
	}

	public default boolean sequenceEqual(Linq<T> right) {
		return fetch().sequenceEqual(right.fetch());
	}

	public default T single() {
		return fetch().single();
	}

	public default T singleOrDefault(T defaultValue) {
		return fetch().singleOrDefault(defaultValue);
	}

	public default long sum(Function<T, Long> func) {
		return fetch().sum(func);
	}

	public default T[] toArray(T[] array) {
		return fetch().toArray(array);
	}

	public default <K> LinkedHashMap<K, T> toDictionary(Function<T, K> keyFactory) {
		return fetch().toDictionary(keyFactory);
	}

	public default LinkedHashSet<T> toHashSet() {
		return fetch().toHashSet();
	}

	public default ArrayList<T> toList() {
		return fetch().toList();
	}

	public default <K> LinkedHashMap<K, List<T>> toLookup(Function<T, K> keyFactory) {
		return fetch().toLookup(keyFactory);
	}

	public default FetchIterator<T> iterator() {
		return fetch().iterator();
	}

	public default void forEach(Consumer<? super T> consumer) {
		fetch().forEach(consumer);
	}
}
