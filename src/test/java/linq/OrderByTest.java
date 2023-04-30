package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class OrderByTest {

	@Test
	public void testOrderBy() {
		var i = Linq.from("b", "d", "a", "e", "c")
				.orderBy(x -> x).iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertEquals("d", i.next());
		assertEquals("e", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.orderBy(x -> x).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
