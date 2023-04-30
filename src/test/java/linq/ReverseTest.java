package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ReverseTest {

	@Test
	public void testReverse() {
		var i = Linq.from("b", "d", "a", "e", "c")
				.reverse().iterator();
		assertEquals("c", i.next());
		assertEquals("e", i.next());
		assertEquals("a", i.next());
		assertEquals("d", i.next());
		assertEquals("b", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.reverse().iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
