package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ThenByDescendingTest {

	@Test
	public void testThenByDescending() {
		var i = Linq.from("Ca", "Cc", "Ba", "Aa", "Bb", "Cb")
				.orderBy(x -> x.charAt(1))
				.thenByDescending(x -> x.charAt(0))
				.iterator();
		assertEquals("Ca", i.next());
		assertEquals("Ba", i.next());
		assertEquals("Aa", i.next());
		assertEquals("Cb", i.next());
		assertEquals("Bb", i.next());
		assertEquals("Cc", i.next());
		assertFalse(i.hasNext());
	}
	
	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.orderBy(x -> x).thenByDescending(x -> x).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
