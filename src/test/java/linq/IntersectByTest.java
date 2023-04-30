package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class IntersectByTest {

	@Test
	public void testIntersectBy() {
		var a = Linq.from("Ac", "Aa", "Ab", "Ad", "Ae", "Af");
		var b = Linq.from("c", "b", "g", "h", "b", "c", "c", "i", "a");

		var i = a.intersectBy(b, x -> x.substring(1, 2)).iterator();
		assertEquals("Ac", i.next());
		assertEquals("Aa", i.next());
		assertEquals("Ab", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var left = CloseCountLinq.create();
		var right = CloseCountLinq.create();

		left.intersectBy(right, x -> x).iterator().close();

		assertEquals(1, left.getCloseCount());
		assertEquals(1, right.getCloseCount());
	}

}
