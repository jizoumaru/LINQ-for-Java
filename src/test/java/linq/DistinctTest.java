package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DistinctTest {

	@Test
	public void testDistinct() {
		var i = Linq.from("c", "b", "c", "b", "a", "c").distinct().iterator();
		assertEquals("c", i.next());
		assertEquals("b", i.next());
		assertEquals("a", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.distinct().iterator().close();
		assertEquals(1, linq.getCloseCount());
	}
}
