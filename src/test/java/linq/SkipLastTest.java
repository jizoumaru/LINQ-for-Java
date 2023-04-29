package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class SkipLastTest {

	@Test
	public void testSkipLast() {
		{
			var i = Linq.from("a", "b", "c", "d", "e", "f").skipLast(3).iterator();
			assertEquals("a", i.next());
			assertEquals("b", i.next());
			assertEquals("c", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("a", "b", "c", "d").skipLast(3).iterator();
			assertEquals("a", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("a", "b", "c").skipLast(3).iterator();
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("a", "b").skipLast(3).iterator();
			assertFalse(i.hasNext());
		}
	}
	
	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.skipLast(1).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
