package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class TakeWhileTest {

	@Test
	public void testTakeWhile() {
		{
			var i = Linq.from("Aa", "Ab", "Ac", "Bd", "Be", "Bf")
					.takeWhile(x -> x.startsWith("A")).iterator();
			assertEquals("Aa", i.next());
			assertEquals("Ab", i.next());
			assertEquals("Ac", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("Aa", "Ab", "Ac")
					.takeWhile(x -> x.startsWith("A")).iterator();
			assertEquals("Aa", i.next());
			assertEquals("Ab", i.next());
			assertEquals("Ac", i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("Aa", "Ab", "Ac", "Bd", "Be", "Bf")
					.takeWhile(x -> x.startsWith("B")).iterator();
			assertFalse(i.hasNext());
		}
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.takeWhile(x -> false).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
