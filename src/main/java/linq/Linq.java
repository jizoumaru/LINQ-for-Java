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
import java.util.function.Function;
import java.util.function.Predicate;

@FunctionalInterface
public interface Linq<T> extends Iterable<T> {
	public static class AppendIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private final T value;
		private boolean appended;

		public AppendIterator(LinqIterator<T> iterator, T value) {
			this.iterator = iterator;
			this.value = value;
			this.appended = false;
		}

		@Override
		protected Holder<T> get() {
			if (iterator.hasNext()) {
				return Holder.of(iterator.next());
			}

			if (!appended) {
				appended = true;
				return Holder.of(value);
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class ArrayIterator<T> extends LinqIterator<T> {
		private final T[] array;
		private int index;

		public ArrayIterator(T[] array) {
			this.array = array;
			this.index = 0;
		}

		@Override
		protected Holder<T> get() {
			if (index < array.length) {
				return Holder.of(array[index++]);
			}
			return Holder.none();
		}
	}

	public static class CastIterator<T, U> extends LinqIterator<U> {
		private final LinqIterator<T> iterator;

		public CastIterator(LinqIterator<T> iterator) {
			this.iterator = iterator;
		}

		@Override
		protected Holder<U> get() {
			if (iterator.hasNext()) {
				@SuppressWarnings("unchecked")
				var value = (U) iterator.next();
				return Holder.of(value);
			}
			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class ChunkIterator<T> extends LinqIterator<List<T>> {
		private final LinqIterator<T> iterator;
		private final int size;

		public ChunkIterator(LinqIterator<T> iterator, int size) {
			this.iterator = iterator;
			this.size = size;
		}

		@Override
		protected Holder<List<T>> get() {
			if (iterator.hasNext()) {
				var list = new ArrayList<T>();

				for (var i = 0; i < size; i++) {
					list.add(iterator.next());

					if (!iterator.hasNext()) {
						break;
					}
				}

				return Holder.of(list);
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class ConcatIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> left;
		private final LinqIterator<T> right;

		public ConcatIterator(LinqIterator<T> left, LinqIterator<T> right) {
			this.left = left;
			this.right = right;
		}

		@Override
		protected Holder<T> get() {
			if (left.hasNext()) {
				return Holder.of(left.next());
			}

			if (right.hasNext()) {
				return Holder.of(right.next());
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			try {
				left.close();
			} finally {
				right.close();
			}
		}

	}

	public static class DefaultIfEmptyIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private final T defaultValue;
		private Boolean isEmpty;
		private boolean ended;

		public DefaultIfEmptyIterator(LinqIterator<T> iterator, T defaultValue) {
			this.iterator = iterator;
			this.defaultValue = defaultValue;
			this.isEmpty = null;
			this.ended = false;
		}

		@Override
		protected Holder<T> get() {
			if (isEmpty == null) {
				isEmpty = !iterator.hasNext();
			}

			if (isEmpty) {
				if (!ended) {
					ended = true;
					return Holder.of(defaultValue);
				}
			}

			if (iterator.hasNext()) {
				return Holder.of(iterator.next());
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class DistinctByIterator<T, K> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private final Function<T, K> keyFactory;
		private Iterator<T> values;

		public DistinctByIterator(LinqIterator<T> iterator, Function<T, K> keyFactory) {
			this.iterator = iterator;
			this.keyFactory = keyFactory;
			this.values = null;
		}

		@Override
		protected Holder<T> get() {
			if (values == null) {
				var map = new LinkedHashMap<K, T>();

				while (iterator.hasNext()) {
					var value = iterator.next();
					var key = keyFactory.apply(value);
					map.putIfAbsent(key, value);
				}

				values = map.values().iterator();
			}

			if (values.hasNext()) {
				return Holder.of(values.next());
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class DistinctIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private Iterator<T> values;

		public DistinctIterator(LinqIterator<T> iterator) {
			this.iterator = iterator;
			this.values = null;
		}

		@Override
		protected Holder<T> get() {
			if (values == null) {
				var set = new LinkedHashSet<T>();

				while (iterator.hasNext()) {
					var value = iterator.next();

					if (!set.contains(value)) {
						set.add(value);
					}
				}

				values = set.iterator();
			}

			if (values.hasNext()) {
				return Holder.of(values.next());
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class EmptyIterator<T> extends LinqIterator<T> {
		@Override
		protected Holder<T> get() {
			return Holder.none();
		}
	}

	public static class ExceptByIterator<T, K> extends LinqIterator<T> {
		private final LinqIterator<T> left;
		private final LinqIterator<T> right;
		private final Function<T, K> keyFactory;
		private Map<K, T> map;

		public ExceptByIterator(LinqIterator<T> left, LinqIterator<T> right, Function<T, K> keyFactory) {
			this.left = left;
			this.right = right;
			this.keyFactory = keyFactory;
			this.map = null;
		}

		@Override
		protected Holder<T> get() {
			if (map == null) {
				map = new HashMap<K, T>();

				while (right.hasNext()) {
					var value = right.next();
					var key = keyFactory.apply(value);
					map.put(key, value);
				}
			}

			while (left.hasNext()) {
				var value = left.next();
				var key = keyFactory.apply(value);

				if (!map.containsKey(key)) {
					return Holder.of(value);
				}
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			try {
				left.close();
			} finally {
				right.close();
			}
		}
	}

	public static class ExceptIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> left;
		private final LinqIterator<T> right;
		private Set<T> set;

		public ExceptIterator(LinqIterator<T> left, LinqIterator<T> right) {
			this.left = left;
			this.right = right;
			this.set = null;
		}

		@Override
		protected Holder<T> get() {
			if (set == null) {
				set = new HashSet<T>();

				while (right.hasNext()) {
					set.add(right.next());
				}
			}

			while (left.hasNext()) {
				var value = left.next();

				if (!set.contains(value)) {
					return Holder.of(value);
				}
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			try {
				left.close();
			} finally {
				right.close();
			}
		}
	}

	public static class GroupByIterator<T, K> extends LinqIterator<Entry<K, List<T>>> {
		private final LinqIterator<T> iterator;
		private final Function<T, K> keyFactory;
		private Iterator<Entry<K, List<T>>> entries;

		public GroupByIterator(LinqIterator<T> iterator, Function<T, K> keyFactory) {
			this.iterator = iterator;
			this.keyFactory = keyFactory;
			this.entries = null;
		}

		@Override
		protected Holder<Entry<K, List<T>>> get() {
			if (entries == null) {
				var map = new LinkedHashMap<K, List<T>>();

				while (iterator.hasNext()) {
					var value = iterator.next();
					var key = keyFactory.apply(value);
					var values = map.get(key);

					if (values == null) {
						values = new ArrayList<T>();
						map.put(key, values);
					}

					values.add(value);
				}

				entries = map.entrySet().iterator();
			}

			if (entries.hasNext()) {
				return Holder.of(entries.next());
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class GroupJoinIterator<TLeft, TRight, TKey, TResult> extends LinqIterator<TResult> {
		private final LinqIterator<TLeft> left;
		private final LinqIterator<TRight> right;
		private final Function<TLeft, TKey> leftKeyFactory;
		private final Function<TRight, TKey> rightKeyFactory;
		private final BiFunction<TLeft, Linq<TRight>, TResult> resultFactory;
		private Map<TKey, List<TRight>> map;

		public GroupJoinIterator(
				LinqIterator<TLeft> left,
				LinqIterator<TRight> right,
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
		protected Holder<TResult> get() {
			if (map == null) {
				map = new LinkedHashMap<TKey, List<TRight>>();

				while (right.hasNext()) {
					var value = right.next();
					var key = rightKeyFactory.apply(value);
					var values = map.get(key);

					if (values == null) {
						values = new ArrayList<TRight>();
						map.put(key, values);
					}

					values.add(value);
				}
			}

			if (left.hasNext()) {
				var value = left.next();
				var key = leftKeyFactory.apply(value);
				var values = map.get(key);

				if (values == null) {
					values = new ArrayList<TRight>();
				}

				return Holder.of(resultFactory.apply(value, Linq.from(values)));
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			try {
				left.close();
			} finally {
				right.close();
			}
		}
	}

	public static class Holder<T> {
		public static <T> Holder<T> none() {
			return new Holder<T>(null, false);
		}

		public static <T> Holder<T> of(T value) {
			return new Holder<T>(value, true);
		}

		private final T value;
		private final boolean exist;

		private Holder(T value, boolean exist) {
			this.value = value;
			this.exist = exist;
		}

		public boolean exists() {
			return exist;
		}

		public T value() {
			if (exist) {
				return value;
			}
			throw new NoSuchElementException();
		}
	}

	public static class IntersectByIterator<TLeft, TKey> extends LinqIterator<TLeft> {
		private final LinqIterator<TLeft> left;
		private final LinqIterator<TKey> right;
		private final Function<TLeft, TKey> keyFactory;
		private Set<TKey> set;

		public IntersectByIterator(
				LinqIterator<TLeft> left,
				LinqIterator<TKey> right,
				Function<TLeft, TKey> keyFactory) {
			this.left = left;
			this.right = right;
			this.keyFactory = keyFactory;
			this.set = null;
		}

		@Override
		protected Holder<TLeft> get() {
			if (set == null) {
				set = new HashSet<TKey>();

				while (right.hasNext()) {
					set.add(right.next());
				}
			}

			while (left.hasNext()) {
				var value = left.next();
				var key = keyFactory.apply(value);

				if (set.contains(key)) {
					return Holder.of(value);
				}
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			try {
				left.close();
			} finally {
				right.close();
			}
		}
	}

	public static class IntersectIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> left;
		private final LinqIterator<T> right;
		private Set<T> set;

		public IntersectIterator(LinqIterator<T> left, LinqIterator<T> right) {
			this.left = left;
			this.right = right;
			this.set = null;
		}

		@Override
		protected Holder<T> get() {
			if (set == null) {
				set = new HashSet<T>();

				while (right.hasNext()) {
					set.add(right.next());
				}
			}

			while (left.hasNext()) {
				var value = left.next();

				if (set.contains(value)) {
					return Holder.of(value);
				}
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			try {
				left.close();
			} finally {
				right.close();
			}
		}
	}

	public static class IterableIterator<T> extends LinqIterator<T> {
		private final Iterable<T> iterable;
		private Iterator<T> iterator;

		public IterableIterator(Iterable<T> iterable) {
			this.iterable = iterable;
			this.iterator = null;
		}

		@Override
		protected Holder<T> get() {
			if (iterator == null) {
				iterator = iterable.iterator();
			}

			if (iterator.hasNext()) {
				return Holder.of(iterator.next());
			}

			return Holder.none();
		}
	}

	public static class JoinIterator<TLeft, TRight, TKey, TResult> extends LinqIterator<TResult> {
		private final LinqIterator<TLeft> left;
		private final LinqIterator<TRight> right;
		private final Function<TLeft, TKey> leftKeyFactory;
		private final Function<TRight, TKey> rightKeyFactory;
		private final BiFunction<TLeft, Linq<TRight>, TResult> resultFactory;
		private Map<TKey, List<TRight>> map;

		public JoinIterator(
				LinqIterator<TLeft> left,
				LinqIterator<TRight> right,
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
		protected Holder<TResult> get() {
			if (map == null) {
				map = new LinkedHashMap<>();

				while (right.hasNext()) {
					var value = right.next();
					var key = rightKeyFactory.apply(value);
					var values = map.get(key);

					if (values == null) {
						values = new ArrayList<TRight>();
						map.put(key, values);
					}

					values.add(value);
				}
			}

			while (left.hasNext()) {
				var value = left.next();
				var key = leftKeyFactory.apply(value);
				var values = map.get(key);

				if (values != null) {
					return Holder.of(resultFactory.apply(value, Linq.from(values)));
				}
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			try {
				left.close();
			} finally {
				right.close();
			}
		}
	}

	public abstract static class LinqIterator<T> implements Iterator<T>, AutoCloseable {
		private Holder<T> value = null;
		private boolean closed = false;

		@Override
		public void close() {
			if (!closed) {
				internalClose();
				closed = true;
			}
		}

		protected abstract Holder<T> get();

		@Override
		public boolean hasNext() {
			if (value == null) {
				value = get();
				if (!value.exists()) {
					close();
				}
			}
			return value.exists();
		}

		protected void internalClose() {
		}

		@Override
		public T next() {
			if (hasNext()) {
				var ret = value;
				value = null;
				return ret.value();
			}
			throw new NoSuchElementException();
		}

		@Override
		public void remove() {
			throw new RuntimeException("not supported");
		}
	}

	public static class OrderingLinq<T> implements Linq<T> {
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

		public <U extends Comparable<U>> OrderingLinq<T> thenBy(final Function<T, U> keySelector) {
			return new OrderingLinq<T>(this, (l, r) -> {
				var c = OrderingLinq.this.cmp.compare(l, r);
				if (c == 0) {
					c = keySelector.apply(l).compareTo(keySelector.apply(r));
				}
				return c;
			});
		}

		public <U extends Comparable<U>> OrderingLinq<T> thenByDescending(final Function<T, U> keySelector) {
			return new OrderingLinq<T>(this, (l, r) -> {
				var c = OrderingLinq.this.cmp.compare(l, r);
				if (c == 0) {
					c = keySelector.apply(r).compareTo(keySelector.apply(l));
				}
				return c;
			});
		}

	}

	public static class PrependIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private final T value;
		private boolean prepended;

		public PrependIterator(LinqIterator<T> iterator, T value) {
			this.iterator = iterator;
			this.value = value;
			this.prepended = false;
		}

		@Override
		protected Holder<T> get() {
			if (!prepended) {
				prepended = true;
				return Holder.of(value);
			}

			if (iterator.hasNext()) {
				return Holder.of(iterator.next());
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class RangeIterator extends LinqIterator<Integer> {
		private final int start;
		private final int count;
		private int index;

		public RangeIterator(int start, int count) {
			this.start = start;
			this.count = count;
			this.index = 0;
		}

		@Override
		protected Holder<Integer> get() {
			if (index < count) {
				return Holder.of(start + (index++));
			}
			return Holder.none();
		}
	}

	public static class RepeatIterator<T> extends LinqIterator<T> {
		private final T value;
		private final int count;
		private int index;

		public RepeatIterator(T value, int count) {
			this.value = value;
			this.count = count;
			this.index = 0;
		}

		@Override
		protected Holder<T> get() {
			if (index < count) {
				index++;
				return Holder.of(value);
			}
			return Holder.none();
		}
	}

	public static class ReverseIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private Iterator<T> values;

		public ReverseIterator(LinqIterator<T> iterator) {
			this.iterator = iterator;
			this.values = null;
		}

		@Override
		protected Holder<T> get() {
			if (values == null) {
				var list = new ArrayList<T>();
				while (iterator.hasNext()) {
					list.add(iterator.next());
				}
				Collections.reverse(list);
				values = list.iterator();
			}

			if (values.hasNext()) {
				return Holder.of(values.next());
			}

			return Holder.none();
		}
	}

	public static class SelectIterator<T, U> extends LinqIterator<U> {
		private final LinqIterator<T> iterator;
		private final Function<T, U> mapper;

		public SelectIterator(LinqIterator<T> iterator, Function<T, U> mapper) {
			this.iterator = iterator;
			this.mapper = mapper;
		}

		@Override
		protected Holder<U> get() {
			if (iterator.hasNext()) {
				return Holder.of(mapper.apply(iterator.next()));
			}
			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class SelectMany<T, U> extends LinqIterator<U> {
		private final LinqIterator<T> iterator;
		private final Function<T, Linq<U>> mapper;
		private LinqIterator<U> inner;

		public SelectMany(LinqIterator<T> iterator, Function<T, Linq<U>> mapper) {
			this.iterator = iterator;
			this.mapper = mapper;
			this.inner = new EmptyIterator<U>();
		}

		@Override
		public void close() {
			try {
				if (inner != null) {
					inner.close();
				}
			} finally {
				iterator.close();
			}
		}

		@Override
		protected Holder<U> get() {
			while (!inner.hasNext() && iterator.hasNext()) {
				inner = mapper.apply(iterator.next()).iterator();
			}

			if (inner.hasNext()) {
				return Holder.of(inner.next());
			}

			return Holder.none();
		}
	}

	public static class SkipIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private final int count;
		private int index;

		public SkipIterator(LinqIterator<T> iterator, int count) {
			this.iterator = iterator;
			this.count = count;
			this.index = 0;
		}

		@Override
		protected Holder<T> get() {
			while (index < count && iterator.hasNext()) {
				index++;
				iterator.next();
			}

			if (iterator.hasNext()) {
				return Holder.of(iterator.next());
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class SkipLastIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private final int count;
		private Queue<T> queue;

		public SkipLastIterator(LinqIterator<T> iterator, int count) {
			this.iterator = iterator;
			this.count = count;
			this.queue = null;
		}

		@Override
		protected Holder<T> get() {
			if (queue == null) {
				queue = new ArrayDeque<T>(count);
				for (var i = 0; i < count && iterator.hasNext(); i++) {
					queue.add(iterator.next());
				}
			}

			if (iterator.hasNext()) {
				var value = queue.remove();
				queue.add(iterator.next());
				return Holder.of(value);
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class SkipWhileIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private final Predicate<T> predicate;
		private boolean skipped;

		public SkipWhileIterator(LinqIterator<T> iterator, Predicate<T> predicate) {
			this.iterator = iterator;
			this.predicate = predicate;
			this.skipped = false;
		}

		@Override
		protected Holder<T> get() {
			if (!skipped) {
				skipped = true;

				while (iterator.hasNext()) {
					var value = iterator.next();

					if (!predicate.test(value)) {
						return Holder.of(value);
					}
				}

				return Holder.none();
			}

			if (iterator.hasNext()) {
				return Holder.of(iterator.next());
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class TakeIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private final int count;
		private int index;

		public TakeIterator(LinqIterator<T> iterator, int count) {
			this.iterator = iterator;
			this.count = count;
			this.index = 0;
		}

		@Override
		protected Holder<T> get() {
			if (index < count && iterator.hasNext()) {
				index++;
				return Holder.of(iterator.next());
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class TakeLastIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private final int count;
		private Queue<T> queue;

		public TakeLastIterator(LinqIterator<T> iterator, int count) {
			this.iterator = iterator;
			this.count = count;
			this.queue = null;
		}

		@Override
		protected Holder<T> get() {
			if (queue == null) {
				queue = new ArrayDeque<T>(count);

				while (iterator.hasNext()) {
					if (queue.size() == count) {
						queue.remove();
					}
					queue.add(iterator.next());
				}
			}

			if (queue.size() > 0) {
				return Holder.of(queue.remove());
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class TakeWhileIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private final Predicate<T> predicate;

		public TakeWhileIterator(LinqIterator<T> iterator, Predicate<T> predicate) {
			this.iterator = iterator;
			this.predicate = predicate;
		}

		@Override
		protected Holder<T> get() {
			if (iterator.hasNext()) {
				var value = iterator.next();

				if (predicate.test(value)) {
					return Holder.of(value);
				}
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class Tuple2<T1, T2> {
		public final T1 value1;
		public final T2 value2;

		public Tuple2(T1 value1, T2 value2) {
			this.value1 = value1;
			this.value2 = value2;
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
		public int hashCode() {
			return Objects.hash(value1, value2);
		}

		@Override
		public String toString() {
			return "Tuple2 [value1=" + value1 + ", value2=" + value2 + "]";
		}
	}

	public static class TypeIterator<T, U> extends LinqIterator<U> {
		private final LinqIterator<T> iterator;
		private final Class<U> type;

		public TypeIterator(LinqIterator<T> iterator, Class<U> type) {
			this.iterator = iterator;
			this.type = type;
		}

		@Override
		protected Holder<U> get() {
			while (iterator.hasNext()) {
				var value = iterator.next();

				if (type.isInstance(value)) {
					return Holder.of(type.cast(value));
				}
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class UnionByIterator<T, K> extends LinqIterator<T> {
		private final LinqIterator<T> left;
		private final LinqIterator<T> right;
		private final Function<T, K> keyFactory;
		private Iterator<T> values;

		public UnionByIterator(LinqIterator<T> left, LinqIterator<T> right, Function<T, K> keyFactory) {
			this.left = left;
			this.right = right;
			this.keyFactory = keyFactory;
			this.values = null;
		}

		@Override
		protected Holder<T> get() {
			if (values == null) {
				var map = new LinkedHashMap<K, T>();

				while (left.hasNext()) {
					var value = left.next();
					map.putIfAbsent(keyFactory.apply(value), value);
				}

				while (right.hasNext()) {
					var value = right.next();
					map.putIfAbsent(keyFactory.apply(value), value);
				}

				values = map.values().iterator();
			}

			if (values.hasNext()) {
				return Holder.of(values.next());
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			try {
				left.close();
			} finally {
				right.close();
			}
		}
	}

	public static class UnionIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> left;
		private final LinqIterator<T> right;
		private Iterator<T> values;

		public UnionIterator(LinqIterator<T> left, LinqIterator<T> right) {
			this.left = left;
			this.right = right;
			this.values = null;
		}

		@Override
		protected Holder<T> get() {
			if (values == null) {
				var set = new LinkedHashSet<T>();

				while (left.hasNext()) {
					set.add(left.next());
				}

				while (right.hasNext()) {
					set.add(right.next());
				}

				values = set.iterator();
			}

			if (values.hasNext()) {
				return Holder.of(values.next());
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			try {
				left.close();
			} finally {
				right.close();
			}
		}
	}

	public static class WhereIterator<T> extends LinqIterator<T> {
		private final LinqIterator<T> iterator;
		private final Predicate<T> predicate;

		public WhereIterator(LinqIterator<T> iterator, Predicate<T> predicate) {
			this.iterator = iterator;
			this.predicate = predicate;
		}

		@Override
		protected Holder<T> get() {
			while (iterator.hasNext()) {
				var value = iterator.next();

				if (predicate.test(value)) {
					return Holder.of(value);
				}
			}

			return Holder.none();
		}

		@Override
		protected void internalClose() {
			iterator.close();
		}
	}

	public static class ZipIterator<TLeft, TRight> extends LinqIterator<Tuple2<TLeft, TRight>> {
		private final LinqIterator<TLeft> left;
		private final LinqIterator<TRight> right;

		public ZipIterator(LinqIterator<TLeft> left, LinqIterator<TRight> right) {
			this.left = left;
			this.right = right;
		}

		@Override
		protected Holder<Tuple2<TLeft, TRight>> get() {
			if (left.hasNext() && right.hasNext()) {
				return Holder.of(new Tuple2<TLeft, TRight>(left.next(), right.next()));
			}
			return Holder.none();
		}

		@Override
		protected void internalClose() {
			try {
				left.close();
			} finally {
				right.close();
			}
		}
	}

	public static <T> Linq<T> empty() {
		return () -> new EmptyIterator<T>();
	}

	public static <T> Linq<T> from(final Iterable<T> iterable) {
		return () -> new IterableIterator<T>(iterable);
	}

	@SafeVarargs
	public static <T> Linq<T> from(final T... xs) {
		return () -> new ArrayIterator<T>(xs);
	}

	public static Linq<Integer> range(final int start, final int count) {
		return () -> new RangeIterator(start, count);
	}

	public static <T> Linq<T> repeat(final T value, final int count) {
		return () -> new RepeatIterator<T>(value, count);
	}

	public default Holder<T> aggregate(BiFunction<T, T, T> func) {
		try (var iterator = iterator()) {
			if (iterator.hasNext()) {
				var result = iterator.next();
				while (iterator.hasNext()) {
					result = func.apply(result, iterator.next());
				}
				return Holder.of(result);
			}
			return Holder.none();
		}
	}

	public default T aggregate(T seed, BiFunction<T, T, T> func) {
		try (var iterator = iterator()) {
			var result = seed;
			while (iterator.hasNext()) {
				result = func.apply(result, iterator.next());
			}
			return result;
		}
	}

	public default boolean all(Predicate<T> predicate) {
		try (var iterator = iterator()) {
			while (iterator.hasNext()) {
				if (!predicate.test(iterator.next())) {
					return false;
				}
			}
			return true;
		}
	}

	public default boolean any(Predicate<T> predicate) {
		try (var iterator = iterator()) {
			while (iterator.hasNext()) {
				if (predicate.test(iterator.next())) {
					return true;
				}
			}
			return false;
		}
	}

	public default Linq<T> append(final T value) {
		return () -> new AppendIterator<T>(iterator(), value);
	}

	public default Holder<Long> average(Function<T, Long> func) {
		var sum = 0L;

		try (var iterator = iterator()) {
			while (iterator.hasNext()) {
				sum += func.apply(iterator.next());
			}
		}

		var count = 0L;

		try (var iterator = iterator()) {
			while (iterator.hasNext()) {
				iterator.next();
				count++;
			}
		}

		if (count != 0L) {
			return Holder.of(sum / count);
		}

		return Holder.none();
	}

	public default <U> Linq<U> cast() {
		return () -> new CastIterator<T, U>(iterator());
	}

	public default Linq<List<T>> chunk(final int size) {
		return () -> new ChunkIterator<T>(iterator(), size);
	}

	public default Linq<T> concat(final Linq<T> right) {
		return () -> new ConcatIterator<T>(iterator(), right.iterator());
	}

	public default boolean contains(T target) {
		try (var iterator = iterator()) {
			while (iterator.hasNext()) {
				if (Objects.equals(iterator.next(), target)) {
					return true;
				}
			}
			return false;
		}
	}

	public default long count() {
		try (var iterator = iterator()) {
			var count = 0L;
			while (iterator.hasNext()) {
				iterator.next();
				count++;
			}
			return count;
		}
	}

	public default Linq<T> defaultIfEmpty(final T defaultValue) {
		return () -> new DefaultIfEmptyIterator<T>(iterator(), defaultValue);
	}

	public default Linq<T> distinct() {
		return () -> new DistinctIterator<T>(iterator());
	}

	public default <K> Linq<T> distinctBy(final Function<T, K> keyFactory) {
		return () -> new DistinctByIterator<T, K>(iterator(), keyFactory);
	}

	public default Holder<T> elementAt(long index) {
		try (var iterator = iterator()) {
			var count = 0L;

			while (iterator.hasNext()) {
				var value = iterator.next();

				if (count == index) {
					return Holder.of(value);
				}

				count++;
			}

			return Holder.none();
		}
	}

	public default T elementAt(long index, T defaultValue) {
		try (var iterator = iterator()) {
			var count = 0L;

			while (iterator.hasNext()) {
				var value = iterator.next();

				if (count == index) {
					return value;
				}

				count++;
			}

			return defaultValue;
		}
	}

	public default Linq<T> except(final Linq<T> right) {
		return () -> new ExceptIterator<T>(iterator(), right.iterator());
	}

	public default <K> Linq<T> exceptBy(final Linq<T> right, final Function<T, K> keyFactory) {
		return () -> new ExceptByIterator<T, K>(iterator(), right.iterator(), keyFactory);
	}

	public default Holder<T> first() {
		try (var iterator = iterator()) {
			if (iterator.hasNext()) {
				return Holder.of(iterator.next());
			}
			return Holder.none();
		}
	}

	public default T firstOrDefault(T defaultValue) {
		try (var iterator = iterator()) {
			if (iterator.hasNext()) {
				return iterator.next();
			}
			return defaultValue;
		}
	}

	public default <K> Linq<Entry<K, List<T>>> groupBy(final Function<T, K> keySelector) {
		return () -> new GroupByIterator<T, K>(iterator(), keySelector);
	}

	public default <TRight, TKey, TResult> Linq<TResult> groupJoin(
			Linq<TRight> right,
			Function<T, TKey> leftKeyFactory,
			Function<TRight, TKey> rightKeyFactory,
			BiFunction<T, Linq<TRight>, TResult> resultFactory) {

		return () -> new GroupJoinIterator<T, TRight, TKey, TResult>(
				iterator(),
				right.iterator(),
				leftKeyFactory,
				rightKeyFactory,
				resultFactory);
	}

	public default Linq<T> intersect(final Linq<T> right) {
		return () -> new IntersectIterator<T>(iterator(), right.iterator());
	}

	public default <TKey> Linq<T> intersectBy(final Linq<TKey> right, final Function<T, TKey> keyFactory) {
		return () -> new IntersectByIterator<T, TKey>(iterator(), right.iterator(), keyFactory);
	}

	@Override
	public abstract LinqIterator<T> iterator();

	public default <TRight, TKey, TResult> Linq<TResult> join(
			Linq<TRight> right,
			Function<T, TKey> leftKeyFactory,
			Function<TRight, TKey> rightKeyFactory,
			BiFunction<T, Linq<TRight>, TResult> resultFactory) {

		return () -> new JoinIterator<T, TRight, TKey, TResult>(
				iterator(),
				right.iterator(),
				leftKeyFactory,
				rightKeyFactory,
				resultFactory);
	}

	public default Holder<T> last() {
		try (var iterator = iterator()) {
			if (iterator.hasNext()) {
				T value;
				do {
					value = iterator.next();
				} while (iterator.hasNext());

				return Holder.of(value);
			}

			return Holder.none();
		}
	}

	public default Holder<T> max(Comparator<T> comparator) {
		try (var iterator = iterator()) {
			if (iterator.hasNext()) {
				var result = iterator.next();

				while (iterator.hasNext()) {
					var value = iterator.next();

					if (comparator.compare(result, value) < 0) {
						result = value;
					}
				}

				return Holder.of(result);
			}

			return Holder.none();
		}
	}

	public default <TKey> Holder<T> maxBy(
			Comparator<TKey> keyComparator,
			Function<T, TKey> keyFactory) {

		try (var iterator = iterator()) {
			if (iterator.hasNext()) {
				var resultValue = iterator.next();
				var resultKey = keyFactory.apply(resultValue);

				while (iterator.hasNext()) {
					var value = iterator.next();
					var key = keyFactory.apply(value);

					if (keyComparator.compare(resultKey, key) < 0) {
						resultValue = value;
						resultKey = key;
					}
				}

				return Holder.of(resultValue);
			}

			return Holder.none();
		}
	}

	public default Holder<T> min(Comparator<T> comparator) {
		try (var iterator = iterator()) {
			if (iterator.hasNext()) {
				var result = iterator.next();

				while (iterator.hasNext()) {
					var value = iterator.next();

					if (comparator.compare(value, result) < 0) {
						result = value;
					}
				}

				return Holder.of(result);
			}

			return Holder.none();
		}
	}

	public default <TKey> Holder<T> minBy(
			Comparator<TKey> keyComparator,
			Function<T, TKey> keyFactory) {

		try (var iterator = iterator()) {
			if (iterator.hasNext()) {
				var resultValue = iterator.next();
				var resultKey = keyFactory.apply(resultValue);

				while (iterator.hasNext()) {
					var value = iterator.next();
					var key = keyFactory.apply(value);

					if (keyComparator.compare(key, resultKey) < 0) {
						resultValue = value;
						resultKey = key;
					}
				}

				return Holder.of(resultValue);
			}

			return Holder.none();
		}
	}

	public default <U> Linq<U> ofType(final Class<U> type) {
		return () -> new TypeIterator<T, U>(iterator(), type);
	}

	public default <U extends Comparable<U>> OrderingLinq<T> orderBy(final Function<T, U> keySelector) {
		return new OrderingLinq<T>(this, (x, y) -> keySelector.apply(x).compareTo(keySelector.apply(y)));
	}

	public default <U extends Comparable<U>> OrderingLinq<T> orderByDescending(final Function<T, U> keySelector) {
		return new OrderingLinq<T>(this, (l, r) -> keySelector.apply(r).compareTo(keySelector.apply(l)));
	}

	public default Linq<T> prepend(T value) {
		return () -> new PrependIterator<T>(iterator(), value);
	}

	public default Linq<T> reverse() {
		return () -> new ReverseIterator<T>(iterator());
	}

	public default <U> Linq<U> select(final Function<T, U> mapper) {
		return () -> new SelectIterator<T, U>(iterator(), mapper);
	}

	public default <U> Linq<U> selectMany(final Function<T, Linq<U>> mapper) {
		return () -> new SelectMany<T, U>(iterator(), mapper);
	}

	public default boolean sequenceEqual(Linq<T> rightLinq) {
		try (var left = iterator(); var right = rightLinq.iterator()) {

			while (left.hasNext() && right.hasNext()) {
				if (!Objects.equals(left.next(), right.next())) {
					return false;
				}
			}

			return left.hasNext() == right.hasNext();
		}
	}

	public default T single() {
		try (var iterator = iterator()) {
			if (iterator.hasNext()) {
				var value = iterator.next();

				if (iterator.hasNext()) {
					throw new IllegalStateException("値が複数存在します");
				}

				return value;
			}

			throw new IllegalStateException("値が存在しません");
		}
	}

	public default T single(T defaultValue) {
		try (var iterator = iterator()) {
			if (iterator.hasNext()) {
				var value = iterator.next();

				if (iterator.hasNext()) {
					throw new IllegalStateException("値が複数存在します");
				}

				return value;
			}

			return defaultValue;
		}
	}

	public default Linq<T> skip(final int count) {
		return () -> new SkipIterator<T>(iterator(), count);
	}

	public default Linq<T> skipLast(final int size) {
		return () -> new SkipLastIterator<T>(iterator(), size);
	}

	public default Linq<T> skipWhile(Predicate<T> predicate) {
		return () -> new SkipWhileIterator<T>(iterator(), predicate);
	}

	public default long sum(Function<T, Long> func) {
		try (var iterator = iterator()) {
			var result = 0L;

			while (iterator.hasNext()) {
				result += func.apply(iterator.next());
			}

			return result;
		}
	}

	public default Linq<T> take(int count) {
		return () -> new TakeIterator<T>(iterator(), count);
	}

	public default Linq<T> takeLast(final int size) {
		return () -> new TakeLastIterator<T>(iterator(), size);
	}

	public default Linq<T> takeWhile(Predicate<T> predicate) {
		return () -> new TakeWhileIterator<T>(iterator(), predicate);
	}

	public default T[] toArray(T[] array) {
		try (var iterator = iterator()) {
			var list = new ArrayList<T>();

			while (iterator.hasNext()) {
				list.add(iterator.next());
			}

			return list.toArray(array);
		}
	}

	public default <K> LinkedHashMap<K, T> toDictionary(Function<T, K> keyFactory) {
		try (var iterator = iterator()) {
			var map = new LinkedHashMap<K, T>();

			while (iterator.hasNext()) {
				var value = iterator.next();
				var key = keyFactory.apply(value);

				if (map.containsKey(key)) {
					throw new IllegalArgumentException("キーが重複しています: " + key);
				}

				map.put(key, value);
			}

			return map;
		}
	}

	public default LinkedHashSet<T> toHashSet() {
		try (var iterator = iterator()) {
			var set = new LinkedHashSet<T>();

			while (iterator.hasNext()) {
				set.add(iterator.next());
			}

			return set;
		}
	}

	public default ArrayList<T> toList() {
		try (var iterator = iterator()) {
			var list = new ArrayList<T>();

			while (iterator.hasNext()) {
				list.add(iterator.next());
			}

			return list;
		}
	}

	public default <K> LinkedHashMap<K, List<T>> toLookup(Function<T, K> keyFactory) {
		try (var iterator = iterator()) {
			var map = new LinkedHashMap<K, List<T>>();

			while (iterator.hasNext()) {
				var value = iterator.next();
				var key = keyFactory.apply(value);
				var list = map.get(key);

				if (list == null) {
					list = new ArrayList<T>();
					map.put(key, list);
				}

				list.add(value);
			}

			return map;
		}
	}

	public default Linq<T> union(final Linq<T> right) {
		return () -> new UnionIterator<T>(iterator(), right.iterator());
	}

	public default <TKey> Linq<T> unionBy(final Linq<T> right, final Function<T, TKey> keyFactory) {
		return () -> new UnionByIterator<T, TKey>(iterator(), right.iterator(), keyFactory);
	}

	public default Linq<T> where(final Predicate<T> predicatge) {
		return () -> new WhereIterator<T>(iterator(), predicatge);
	}

	public default <TRight> Linq<Tuple2<T, TRight>> zip(final Linq<TRight> right) {
		return () -> new ZipIterator<T, TRight>(iterator(), right.iterator());
	}

}
