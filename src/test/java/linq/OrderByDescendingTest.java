package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class OrderByDescendingTest {

	@Test
	public void testOrderByDescending() {
		var i = Linq.from("b", "d", "a", "e", "c")
				.orderByDescending(x -> x).iterator();
		assertEquals("e", i.next());
		assertEquals("d", i.next());
		assertEquals("c", i.next());
		assertEquals("b", i.next());
		assertEquals("a", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.orderByDescending(x -> x).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
