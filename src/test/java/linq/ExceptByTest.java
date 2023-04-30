package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ExceptByTest {

	@Test
	public void testExceptBy() {
		var i = Linq.from("1a", "2b", "3c", "4d", "5e", "6f")
				.exceptBy(Linq.from("7d", "8e", "9f", "Ag", "Bh", "Ci"),
						x -> x.charAt(1))
				.iterator();
		assertEquals("1a", i.next());
		assertEquals("2b", i.next());
		assertEquals("3c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var left = CloseCountLinq.create();
		var right = CloseCountLinq.create();

		left.exceptBy(right, x -> x).iterator().close();

		assertEquals(1, left.getCloseCount());
		assertEquals(1, right.getCloseCount());
	}
}
