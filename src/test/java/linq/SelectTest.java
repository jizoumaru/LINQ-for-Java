package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class SelectTest {

	@Test
	public void testSelect() {
		var i = Linq.from("a", "b", "c")
				.select(x -> "S" + x).iterator();
		assertEquals("Sa", i.next());
		assertEquals("Sb", i.next());
		assertEquals("Sc", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.select(x -> x).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
