package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class WhereTest {

	@Test
	public void testWhere() {
		var i = Linq.from("Aa", "Ab", "Ac", "Ba", "Bb", "Bc")
				.where(x -> x.charAt(1) == 'b')
				.iterator();
		assertEquals("Ab", i.next());
		assertEquals("Bb", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.where(x -> true).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
