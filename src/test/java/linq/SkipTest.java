package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class SkipTest {

	@Test
	public void testSkip() {
		{
			var i = Linq.from("a", "b", "c", "d", "e", "f").skip(3).iterator();
			assertEquals("d", i.next());
			assertEquals("e", i.next());
			assertEquals("f", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("a", "b", "c", "d").skip(3).iterator();
			assertEquals("d", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("a", "b", "c").skip(3).iterator();
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("a", "b").skip(3).iterator();
			assertFalse(i.hasNext());
		}
	}
	
	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.skip(1).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}
}
