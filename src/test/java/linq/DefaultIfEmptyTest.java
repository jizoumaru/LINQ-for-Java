package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DefaultIfEmptyTest {

	@Test
	public void testDefaultIfEmpty() {
		var i = Linq.from("a", "b", "c").defaultIfEmpty("def").iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testDefaultIfEmptyEmpty() {
		var i = Linq.from().defaultIfEmpty("def").iterator();
		assertEquals("def", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.defaultIfEmpty(0).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}
}
