package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ExceptTest {

	@Test
	public void testExcept() {
		var i = Linq.from("a", "b", "c", "d", "e", "f")
				.except(Linq.from("d", "e", "f", "g", "h", "i"))
				.iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var left = new CloseCountLinq();
		var right = new CloseCountLinq();

		left.except(right).iterator().close();

		assertEquals(1, left.getCloseCount());
		assertEquals(1, right.getCloseCount());
	}

}
