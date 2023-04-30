package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ConcatTest {

	@Test
	public void testConcat() {
		var i = Linq.from("a", "b")
				.concat(Linq.from("c", "d", "e")).iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertEquals("d", i.next());
		assertEquals("e", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var left = CloseCountLinq.create();
		var right = CloseCountLinq.create();

		left.concat(right).iterator().close();

		assertEquals(1, left.getCloseCount());
		assertEquals(1, right.getCloseCount());
	}
}
