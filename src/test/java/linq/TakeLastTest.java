package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class TakeLastTest {

	@Test
	public void testTakeLast() {
		{
			var i = Linq.from("a", "b", "c", "d", "e", "f").takeLast(3).iterator();
			assertEquals("d", i.next());
			assertEquals("e", i.next());
			assertEquals("f", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("a", "b", "c").takeLast(3).iterator();
			assertEquals("a", i.next());
			assertEquals("b", i.next());
			assertEquals("c", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("a", "b").takeLast(3).iterator();
			assertEquals("a", i.next());
			assertEquals("b", i.next());
			assertFalse(i.hasNext());
		}
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.takeLast(1).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
