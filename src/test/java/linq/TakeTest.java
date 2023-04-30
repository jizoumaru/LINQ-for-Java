package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class TakeTest {

	@Test
	public void testTake() {
		{
			var i = Linq.from("a", "b", "c", "d", "e", "f").take(3).iterator();
			assertEquals("a", i.next());
			assertEquals("b", i.next());
			assertEquals("c", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("a", "b", "c").take(3).iterator();
			assertEquals("a", i.next());
			assertEquals("b", i.next());
			assertEquals("c", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("a", "b").take(3).iterator();
			assertEquals("a", i.next());
			assertEquals("b", i.next());
			assertFalse(i.hasNext());
		}
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.take(1).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
