package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class IntersectTest {

	@Test
	public void testIntersect() {
		var a = Linq.from("b", "e", "f", "g", "c", "d", "a", "j", "h", "i");
		var b = Linq.from("l", "j", "g", "h", "n", "o", "i", "m", "k", "f");

		var i = a.intersect(b).iterator();
		assertEquals("f", i.next());
		assertEquals("g", i.next());
		assertEquals("j", i.next());
		assertEquals("h", i.next());
		assertEquals("i", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var left = new CloseCountLinq();
		var right = new CloseCountLinq();

		left.intersect(right).iterator().close();

		assertEquals(1, left.getCloseCount());
		assertEquals(1, right.getCloseCount());
	}

}
