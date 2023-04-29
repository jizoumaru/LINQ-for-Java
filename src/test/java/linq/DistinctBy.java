package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DistinctBy {

	@Test
	public void testDistinct() {
		var i = Linq.from("c", "b", "c", "b", "a", "c").distinct().iterator();
		assertEquals("c", i.next());
		assertEquals("b", i.next());
		assertEquals("a", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testDistinctBy() {
		var i = Linq.from("1c", "2b", "3c", "4b", "5a", "6c")
				.distinctBy(x -> x.charAt(1)).iterator();
		assertEquals("1c", i.next());
		assertEquals("2b", i.next());
		assertEquals("5a", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.distinctBy(x -> x).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}
}
