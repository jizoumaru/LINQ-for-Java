package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class JoinTest {

	@Test
	public void testJoin() {
		var a = Linq.from("Ac", "Ab", "Af", "Ae", "Aa", "Ad");
		var b = Linq.from("Be", "Ce", "Bf", "Bi", "Cd", "Cf", "Bd", "Bh", "Bg");
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
	public void testClose() {
		var left = CloseCountLinq.create();
		var right = CloseCountLinq.create();

		left.join(right, x -> x, x -> x, (x, y) -> 0).iterator().close();

		assertEquals(1, left.getCloseCount());
		assertEquals(1, right.getCloseCount());
	}

}
