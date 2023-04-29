package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class SkipWhileTest {

	@Test
	public void testSkipWhile() {
		var i = Linq.from("Aa", "Ab", "Ac", "Ba", "Bb", "Bc")
				.skipWhile(x -> x.charAt(0) == 'A')
				.iterator();
		assertEquals("Ba", i.next());
		assertEquals("Bb", i.next());
		assertEquals("Bc", i.next());
		assertFalse(i.hasNext());

		assertFalse(Linq.from("Aa", "Ab", "Ac")
				.skipWhile(x -> x.charAt(0) == 'A')
				.iterator().hasNext());
	}
	
	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.skipWhile(x -> false).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
