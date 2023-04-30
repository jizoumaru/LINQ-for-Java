package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class PrependTest {

	@Test
	public void testPrepend() {
		var i = Linq.from("a", "b", "c").prepend("H").iterator();
		assertEquals("H", i.next());
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.prepend(2).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}

}
