package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ThenByTest {

	@Test
	public void testThenBy() {
		var i = Linq.from("Ca", "Cc", "Ba", "Aa", "Bb", "Cb")
				.orderBy(x -> x.charAt(1))
				.thenBy(x -> x.charAt(0))
				.iterator();
		assertEquals("Aa", i.next());
		assertEquals("Ba", i.next());
		assertEquals("Ca", i.next());
		assertEquals("Bb", i.next());
		assertEquals("Cb", i.next());
		assertEquals("Cc", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.orderBy(x -> x).thenBy(x -> x).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
