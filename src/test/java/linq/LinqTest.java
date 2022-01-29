package linq;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import linq.Linq.Tuple2;

public class LinqTest {
	@Test
	public void testFrom() {
		var i = Linq.from(Arrays.asList("a", "b", "c")).iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testRange() {
		var i = Linq.range(2, 3).iterator();
		assertTrue(2 == i.next());
		assertTrue(3 == i.next());
		assertTrue(4 == i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testRepeat() {
		var i = Linq.repeat("a", 3).iterator();
		assertEquals("a", i.next());
		assertEquals("a", i.next());
		assertEquals("a", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testAggregate() {
		var s = Linq.from(Arrays.asList(1, 2, 3)).aggregate((l, r) -> l + r);
		assertTrue(6 == s.value());
	}

	@Test
	public void testAggregateNone() {
		var s = Linq.from(Arrays.<Integer>asList()).aggregate((l, r) -> l + r);
		assertFalse(s.exists());
	}

	@Test
	public void testAggregateSeed() {
		var s = Linq.from(Arrays.asList(1, 2, 3)).aggregate(4, (l, r) -> l + r);
		assertTrue(10 == s);
	}

	@Test
	public void testAggregateSeedNone() {
		var s = Linq.from(Arrays.<Integer>asList()).aggregate(4, (l, r) -> l + r);
		assertTrue(4 == s);
	}

	@Test
	public void testAll() {
		assertTrue(Linq.from(Arrays.asList(2, 4, 6)).all(x -> x % 2 == 0));
		assertFalse(Linq.from(Arrays.asList(1, 4, 6)).all(x -> x % 2 == 0));
		assertFalse(Linq.from(Arrays.asList(2, 1, 6)).all(x -> x % 2 == 0));
		assertFalse(Linq.from(Arrays.asList(2, 4, 1)).all(x -> x % 2 == 0));
	}

	@Test
	public void testAny() {
		assertTrue(Linq.from(Arrays.asList(2, 3, 5)).any(x -> x == 2));
		assertTrue(Linq.from(Arrays.asList(1, 2, 5)).any(x -> x == 2));
		assertTrue(Linq.from(Arrays.asList(1, 3, 2)).any(x -> x == 2));
		assertFalse(Linq.from(Arrays.asList(1, 3, 5)).any(x -> x == 2));
	}

	@Test
	public void testAppend() {
		var i = Linq.from(Arrays.asList("a", "b")).append("c").iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testAverage() {
		var s = Linq.from(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)).average(x -> Long.valueOf(x));
		assertTrue(5 == s.value());
	}

	@Test
	public void testAverageNone() {
		var s = Linq.from(Arrays.<Integer>asList()).average(x -> Long.valueOf(x));
		assertFalse(s.exists());
	}

	@Test
	public void testCast() {
		var i = Linq.from(Arrays.<Object>asList("a", "b", "c", null)).<String>cast().iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertEquals(null, i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testCastNull() {
		var i = Linq.from(Arrays.<Object>asList(1)).<String>cast().iterator();

		ClassCastException ex = null;
		try {
			@SuppressWarnings("unused")
			var s = i.next();
		} catch (ClassCastException e) {
			ex = e;
		}
		assertTrue(ex != null);
	}

	@Test
	public void testChunk() {
		var i = Linq.range(0, 10).chunk(3).iterator();
		assertEquals(Arrays.asList(0, 1, 2), i.next());
		assertEquals(Arrays.asList(3, 4, 5), i.next());
		assertEquals(Arrays.asList(6, 7, 8), i.next());
		assertEquals(Arrays.asList(9), i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testConcat() {
		var i = Linq.from(Arrays.asList("a", "b"))
			.concat(Linq.from(Arrays.asList("c", "d", "e"))).iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertEquals("d", i.next());
		assertEquals("e", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testContains() {
		assertTrue(Linq.from(Arrays.asList("a", "b", "c")).contains("a"));
		assertTrue(Linq.from(Arrays.asList("a", "b", "c")).contains("b"));
		assertTrue(Linq.from(Arrays.asList("a", "b", "c")).contains("c"));
		assertFalse(Linq.from(Arrays.asList("a", "b", "c")).contains("d"));
	}

	@Test
	public void testCount() {
		assertEquals(3L, Linq.range(0, 3).count());
	}

	@Test
	public void testDefaultIfEmpty() {
		var i = Linq.from(Arrays.asList("a", "b", "c")).defaultIfEmpty("def").iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testDefaultIfEmptyEmpty() {
		var i = Linq.from(Arrays.asList()).defaultIfEmpty("def").iterator();
		assertEquals("def", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testDistinct() {
		var i = Linq.from(Arrays.asList("c", "b", "c", "b", "a", "c")).distinct().iterator();
		assertEquals("c", i.next());
		assertEquals("b", i.next());
		assertEquals("a", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testDistinctBy() {
		var i = Linq.from(Arrays.asList("1c", "2b", "3c", "4b", "5a", "6c"))
			.distinctBy(x -> x.charAt(1)).iterator();
		assertEquals("1c", i.next());
		assertEquals("2b", i.next());
		assertEquals("5a", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testElementAt() {
		assertEquals("a", Linq.from(Arrays.asList("a", "b", "c")).elementAt(0).value());
		assertEquals("b", Linq.from(Arrays.asList("a", "b", "c")).elementAt(1).value());
		assertEquals("c", Linq.from(Arrays.asList("a", "b", "c")).elementAt(2).value());
		assertFalse(Linq.from(Arrays.asList("a", "b", "c")).elementAt(3).exists());
	}

	@Test
	public void testElementAtDefaultValue() {
		assertEquals("a", Linq.from(Arrays.asList("a", "b", "c")).elementAt(0, "d"));
		assertEquals("b", Linq.from(Arrays.asList("a", "b", "c")).elementAt(1, "d"));
		assertEquals("c", Linq.from(Arrays.asList("a", "b", "c")).elementAt(2, "d"));
		assertEquals("d", Linq.from(Arrays.asList("a", "b", "c")).elementAt(3, "d"));
	}

	@Test
	public void testEmpty() {
		var i = Linq.empty().iterator();
		assertFalse(i.hasNext());
	}

	@Test
	public void testExcept() {
		var i = Linq.from(Arrays.asList("a", "b", "c", "d", "e", "f"))
			.except(Linq.from(Arrays.asList("d", "e", "f", "g", "h", "i")))
			.iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testExceptBy() {
		var i = Linq.from(Arrays.asList("1a", "2b", "3c", "4d", "5e", "6f"))
			.exceptBy(Linq.from(Arrays.asList("7d", "8e", "9f", "Ag", "Bh", "Ci")),
				x -> x.charAt(1))
			.iterator();
		assertEquals("1a", i.next());
		assertEquals("2b", i.next());
		assertEquals("3c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testFirst() {
		assertEquals("a", Linq.from(Arrays.asList("a", "b", "c")).first().value());
		assertFalse(Linq.from(Arrays.asList()).first().exists());
	}

	@Test
	public void testFirstOrDefault() {
		assertEquals("a", Linq.from(Arrays.asList("a", "b", "c")).firstOrDefault("def"));
		assertEquals("def", Linq.from(Arrays.asList()).firstOrDefault("def"));
	}

	@Test
	public void testGroupBy() {
		var i = Linq.from(Arrays.asList("d3", "f3", "c2", "e3", "b2", "a1"))
			.groupBy(x -> x.substring(1, 2)).iterator();

		var e = i.next();
		assertEquals("3", e.getKey());
		assertEquals(Arrays.asList("d3", "f3", "e3"), e.getValue());

		e = i.next();
		assertEquals("2", e.getKey());
		assertEquals(Arrays.asList("c2", "b2"), e.getValue());

		e = i.next();
		assertEquals("1", e.getKey());
		assertEquals(Arrays.asList("a1"), e.getValue());

		assertFalse(i.hasNext());
	}

	@Test
	public void testGroupJoin() {
		var i = Linq.from(Arrays.asList("La", "Lb", "Lc", "Ld", "Le", "Lf"))
			.groupJoin(
				Linq.from(Arrays.asList("dR", "eR", "fR", "gR", "hR", "iR", "eR2", "fR2", "fR3")),
				x -> x.charAt(1),
				x -> x.charAt(0),
				(x, y) -> x + ":" + y.toList())
			.iterator();

		assertEquals("La:[]", i.next());
		assertEquals("Lb:[]", i.next());
		assertEquals("Lc:[]", i.next());
		assertEquals("Ld:[dR]", i.next());
		assertEquals("Le:[eR, eR2]", i.next());
		assertEquals("Lf:[fR, fR2, fR3]", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testIntersect() {
		var a = Linq.from(Arrays.asList("b", "e", "f", "g", "c", "d", "a", "j", "h", "i"));
		var b = Linq.from(Arrays.asList("l", "j", "g", "h", "n", "o", "i", "m", "k", "f"));

		var i = a.intersect(b).iterator();
		assertEquals("f", i.next());
		assertEquals("g", i.next());
		assertEquals("j", i.next());
		assertEquals("h", i.next());
		assertEquals("i", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testIntersectBy() {
		var a = Linq.from(Arrays.asList("Ac", "Aa", "Ab", "Ad", "Ae", "Af"));
		var b = Linq.from(Arrays.asList("c", "b", "g", "h", "b", "c", "c", "i", "a"));

		var i = a.intersectBy(b, x -> x.substring(1, 2)).iterator();
		assertEquals("Ac", i.next());
		assertEquals("Aa", i.next());
		assertEquals("Ab", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testJoin() {
		var a = Linq.from(Arrays.asList("Ac", "Ab", "Af", "Ae", "Aa", "Ad"));
		var b = Linq.from(Arrays.asList("Be", "Ce", "Bf", "Bi", "Cd", "Cf", "Bd", "Bh", "Bg"));
		var i = a.join(b,
			x -> x.charAt(1),
			x -> x.charAt(1),
			(x, y) -> x + ":" + y.toList()).iterator();
		assertEquals("Af:[Bf, Cf]", i.next());
		assertEquals("Ae:[Be, Ce]", i.next());
		assertEquals("Ad:[Cd, Bd]", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testLast() {
		assertFalse(Linq.from(Arrays.asList()).last().exists());
		assertEquals("a", Linq.from(Arrays.asList("a")).last().value());
		assertEquals("c", Linq.from(Arrays.asList("a", "b", "c")).last().value());
	}

	@Test
	public void testMax() {
		assertFalse(Linq.from(Arrays.<String>asList()).max(String::compareTo).exists());
		assertEquals("a", Linq.from(Arrays.asList("a")).max(String::compareTo).value());
		assertEquals("e", Linq.from(Arrays.asList("d", "b", "e", "c", "a"))
			.max(String::compareTo).value());
	}

	@Test
	public void testMaxBy() {
		assertFalse(Linq.from(Arrays.<String>asList())
			.maxBy(String::compareTo, x -> x.substring(1, 2)).exists());

		assertEquals("Aa", Linq.from(Arrays.asList("Aa"))
			.maxBy(String::compareTo, x -> x.substring(1, 2)).value());

		assertEquals("Ce", Linq.from(Arrays.asList("Ad", "Bb", "Ce", "Dc", "Ea"))
			.maxBy(String::compareTo, x -> x.substring(1, 2)).value());
	}

	@Test
	public void testMin() {
		assertFalse(Linq.from(Arrays.<String>asList()).min(String::compareTo).exists());
		assertEquals("a", Linq.from(Arrays.asList("a")).min(String::compareTo).value());
		assertEquals("a", Linq.from(Arrays.asList("e", "b", "a", "d", "c"))
			.min(String::compareTo).value());
	}

	@Test
	public void testMinBy() {
		assertFalse(Linq.from(Arrays.<String>asList())
			.minBy(String::compareTo, x -> x.substring(1, 2)).exists());

		assertEquals("Aa", Linq.from(Arrays.asList("Aa"))
			.minBy(String::compareTo, x -> x.substring(1, 2)).value());

		assertEquals("Ea", Linq.from(Arrays.asList("Dc", "Bb", "Ce", "Ea", "Ad"))
			.minBy(String::compareTo, x -> x.substring(1, 2)).value());
	}

	@Test
	public void testOfType() {
		var i = Linq.from(Arrays.<Object>asList("a")).ofType(String.class).iterator();
		String s = i.next();
		assertEquals("a", s);
		assertFalse(i.hasNext());
	}

	@Test
	public void testOrderBy() {
		var i = Linq.from(Arrays.asList("b", "d", "a", "e", "c"))
			.orderBy(x -> x).iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertEquals("d", i.next());
		assertEquals("e", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testOrderByDescending() {
		var i = Linq.from(Arrays.asList("b", "d", "a", "e", "c"))
			.orderByDescending(x -> x).iterator();
		assertEquals("e", i.next());
		assertEquals("d", i.next());
		assertEquals("c", i.next());
		assertEquals("b", i.next());
		assertEquals("a", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testPrepend() {
		var i = Linq.from(Arrays.asList("a", "b", "c")).prepend("H").iterator();
		assertEquals("H", i.next());
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testReverse() {
		var i = Linq.from(Arrays.asList("b", "d", "a", "e", "c"))
			.reverse().iterator();
		assertEquals("c", i.next());
		assertEquals("e", i.next());
		assertEquals("a", i.next());
		assertEquals("d", i.next());
		assertEquals("b", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testSelect() {
		var i = Linq.from(Arrays.asList("a", "b", "c"))
			.select(x -> "S" + x).iterator();
		assertEquals("Sa", i.next());
		assertEquals("Sb", i.next());
		assertEquals("Sc", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testSelectMany() {
		var i = Linq.from(Arrays.asList(
			Arrays.asList("a"),
			Arrays.asList("b", "c"),
			Arrays.asList("d", "e", "f")))
			.selectMany(x -> Linq.from(x))
			.iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertEquals("d", i.next());
		assertEquals("e", i.next());
		assertEquals("f", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testSequenceEqual() {
		assertTrue(Linq.from(Arrays.asList("a", "b", "c"))
			.sequenceEqual(Linq.from(Arrays.asList("a", "b", "c"))));

		assertFalse(Linq.from(Arrays.asList("a", "b", "c"))
			.sequenceEqual(Linq.from(Arrays.asList("a", "b", "d"))));

		assertFalse(Linq.from(Arrays.asList("a", "b", "c"))
			.sequenceEqual(Linq.from(Arrays.asList("a", "b"))));

		assertFalse(Linq.from(Arrays.asList("a", "b", "c"))
			.sequenceEqual(Linq.from(Arrays.asList("a", "b", "c", "d"))));
	}

	@Test
	public void testSingle() {
		assertEquals("a", Linq.from(Arrays.asList("a")).single());

		try {
			Linq.from(Arrays.asList()).single();
			Assert.fail();
		} catch (IllegalStateException e) {
			assertEquals("値が存在しません", e.getMessage());
		}

		try {
			Linq.from(Arrays.asList("a", "b")).single();
			Assert.fail();
		} catch (IllegalStateException e) {
			assertEquals("値が複数存在します", e.getMessage());
		}

	}

	@Test
	public void testSingleDefault() {
		assertEquals("a", Linq.from(Arrays.asList("a")).single("d"));
		assertEquals("d", Linq.from(Arrays.asList()).single("d"));

		try {
			Linq.from(Arrays.asList("a", "b")).single("d");
			Assert.fail();
		} catch (IllegalStateException e) {
			assertEquals("値が複数存在します", e.getMessage());
		}
	}

	@Test
	public void testSkip() {
		{
			var i = Linq.from(Arrays.asList("a", "b", "c", "d", "e", "f")).skip(3).iterator();
			assertEquals("d", i.next());
			assertEquals("e", i.next());
			assertEquals("f", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("a", "b", "c", "d")).skip(3).iterator();
			assertEquals("d", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("a", "b", "c")).skip(3).iterator();
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("a", "b")).skip(3).iterator();
			assertFalse(i.hasNext());
		}
	}

	@Test
	public void testSkipLast() {
		{
			var i = Linq.from(Arrays.asList("a", "b", "c", "d", "e", "f")).skipLast(3).iterator();
			assertEquals("a", i.next());
			assertEquals("b", i.next());
			assertEquals("c", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("a", "b", "c", "d")).skipLast(3).iterator();
			assertEquals("a", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("a", "b", "c")).skipLast(3).iterator();
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("a", "b")).skipLast(3).iterator();
			assertFalse(i.hasNext());
		}
	}

	@Test
	public void testSkipWhile() {
		var i = Linq.from(Arrays.asList("Aa", "Ab", "Ac", "Ba", "Bb", "Bc"))
			.skipWhile(x -> x.charAt(0) == 'A')
			.iterator();
		assertEquals("Ba", i.next());
		assertEquals("Bb", i.next());
		assertEquals("Bc", i.next());
		assertFalse(i.hasNext());

		assertFalse(Linq.from(Arrays.asList("Aa", "Ab", "Ac"))
			.skipWhile(x -> x.charAt(0) == 'A')
			.iterator().hasNext());
	}

	@Test
	public void testSum() {
		assertEquals(60L, Linq.from(Arrays.asList("10", "20", "30"))
			.sum(x -> Long.valueOf(x)));
		assertEquals(0L, Linq.from(Arrays.<String>asList()).sum(x -> Long.valueOf(x)));
	}

	@Test
	public void testTake() {
		{
			var i = Linq.from(Arrays.asList("a", "b", "c", "d", "e", "f")).take(3).iterator();
			assertEquals("a", i.next());
			assertEquals("b", i.next());
			assertEquals("c", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("a", "b", "c")).take(3).iterator();
			assertEquals("a", i.next());
			assertEquals("b", i.next());
			assertEquals("c", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("a", "b")).take(3).iterator();
			assertEquals("a", i.next());
			assertEquals("b", i.next());
			assertFalse(i.hasNext());
		}
	}

	@Test
	public void testTakeLast() {
		{
			var i = Linq.from(Arrays.asList("a", "b", "c", "d", "e", "f")).takeLast(3).iterator();
			assertEquals("d", i.next());
			assertEquals("e", i.next());
			assertEquals("f", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("a", "b", "c")).takeLast(3).iterator();
			assertEquals("a", i.next());
			assertEquals("b", i.next());
			assertEquals("c", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("a", "b")).takeLast(3).iterator();
			assertEquals("a", i.next());
			assertEquals("b", i.next());
			assertFalse(i.hasNext());
		}
	}

	@Test
	public void testTakeWhile() {
		{
			var i = Linq.from(Arrays.asList("Aa", "Ab", "Ac", "Bd", "Be", "Bf"))
				.takeWhile(x -> x.startsWith("A")).iterator();
			assertEquals("Aa", i.next());
			assertEquals("Ab", i.next());
			assertEquals("Ac", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("Aa", "Ab", "Ac"))
				.takeWhile(x -> x.startsWith("A")).iterator();
			assertEquals("Aa", i.next());
			assertEquals("Ab", i.next());
			assertEquals("Ac", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("Aa", "Ab", "Ac", "Bd", "Be", "Bf"))
				.takeWhile(x -> x.startsWith("B")).iterator();
			assertFalse(i.hasNext());
		}
	}

	@Test
	public void testToArray() {
		var a = Linq.from(Arrays.asList("a", "b", "c")).toArray(new String[] {});
		assertArrayEquals(new String[] { "a", "b", "c" }, a);
	}

	@Test
	public void testToDictionary() {
		{
			var a = Linq.from(Arrays.asList("Aa", "Bb", "Cc")).toDictionary(x -> x.substring(0, 1));
			var e = new HashMap<String, String>();
			e.put("A", "Aa");
			e.put("B", "Bb");
			e.put("C", "Cc");
			assertEquals(e, a);
		}

		{
			try {
				Linq.from(Arrays.asList("a", "a")).toDictionary(x -> x);
				Assert.fail();
			} catch (IllegalArgumentException e) {
				assertEquals(e.getMessage(), "キーが重複しています: a");
			}
		}
	}

	@Test
	public void testToHashSet() {
		var a = Linq.from(Arrays.asList("a", "b", "b", "c", "c", "c")).toHashSet();
		var e = new HashSet<String>();
		e.add("a");
		e.add("b");
		e.add("c");
		assertEquals(e, a);
	}

	@Test
	public void testToList() {
		var a = Linq.from(Arrays.asList("a", "b", "c")).toList();
		var e = Arrays.asList("a", "b", "c");
		assertEquals(e, a);
	}

	@Test
	public void testToLookup() {
		var a = Linq.from(Arrays.asList("Aa", "Bb", "Bc", "Cd", "Ce", "Cf"))
			.toLookup(x -> x.substring(0, 1));
		var e = new HashMap<String, List<String>>();
		e.put("A", Arrays.asList("Aa"));
		e.put("B", Arrays.asList("Bb", "Bc"));
		e.put("C", Arrays.asList("Cd", "Ce", "Cf"));
		assertEquals(e, a);
	}

	@Test
	public void testUnion() {
		var i = Linq.from(Arrays.asList("a", "b", "c", "d"))
			.union(Linq.from(Arrays.asList("c", "d", "e", "f")))
			.iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertEquals("d", i.next());
		assertEquals("e", i.next());
		assertEquals("f", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testUnionBy() {
		var i = Linq.from(Arrays.asList("Aa", "Ab", "Ac", "Ad", "Ba", "Bb"))
			.unionBy(Linq.from(Arrays.asList("Bc", "Bd", "Be", "Bf", "Ce", "Cf")),
				x -> x.substring(1, 2))
			.iterator();
		assertEquals("Aa", i.next());
		assertEquals("Ab", i.next());
		assertEquals("Ac", i.next());
		assertEquals("Ad", i.next());
		assertEquals("Be", i.next());
		assertEquals("Bf", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testWhere() {
		var i = Linq.from(Arrays.asList("Aa", "Ab", "Ac", "Ba", "Bb", "Bc"))
			.where(x -> x.charAt(1) == 'b')
			.iterator();
		assertEquals("Ab", i.next());
		assertEquals("Bb", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testZip() {
		{
			var i = Linq.from(Arrays.asList("a", "b", "c"))
				.zip(Linq.from(Arrays.asList("d", "e", "f")))
				.iterator();
			assertEquals(new Tuple2<String, String>("a", "d"), i.next());
			assertEquals(new Tuple2<String, String>("b", "e"), i.next());
			assertEquals(new Tuple2<String, String>("c", "f"), i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("a", "b"))
				.zip(Linq.from(Arrays.asList("a'")))
				.iterator();
			assertEquals(new Tuple2<String, String>("a", "a'"), i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from(Arrays.asList("a"))
				.zip(Linq.from(Arrays.asList("a'", "b'")))
				.iterator();
			assertEquals(new Tuple2<String, String>("a", "a'"), i.next());
			assertFalse(i.hasNext());
		}
	}

	@Test
	public void testThenBy() {
		var i = Linq.from(Arrays.asList("Ca", "Cc", "Ba", "Aa", "Bb", "Cb"))
			.orderBy(x -> x.charAt(1))
			.thenBy(x -> x.charAt(0))
			.iterator();
		assertEquals("Aa", i.next());
		assertEquals("Ba", i.next());
		assertEquals("Ca", i.next());
		assertEquals("Bb", i.next());
		assertEquals("Cb", i.next());
		assertEquals("Cc", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testThenByDescending() {
		var i = Linq.from(Arrays.asList("Ca", "Cc", "Ba", "Aa", "Bb", "Cb"))
			.orderBy(x -> x.charAt(1))
			.thenByDescending(x -> x.charAt(0))
			.iterator();
		assertEquals("Ca", i.next());
		assertEquals("Ba", i.next());
		assertEquals("Aa", i.next());
		assertEquals("Cb", i.next());
		assertEquals("Bb", i.next());
		assertEquals("Cc", i.next());
		assertFalse(i.hasNext());
	}
}
