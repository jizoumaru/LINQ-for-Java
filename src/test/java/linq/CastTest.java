package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CastTest {

	@Test
	public void testCast() {
		var i = Linq.from("a", "b", "c", null).<String>cast().iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertEquals(null, i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testCastNull() {
		var i = Linq.<Object>from(1).<String>cast().iterator();

		ClassCastException ex = null;
		try {
			@SuppressWarnings("unused")
			var s = i.next();
		} catch (ClassCastException e) {
			ex = e;
		}
		assertTrue(ex != null);
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.cast().iterator().close();
		assertEquals(1, linq.getCloseCount());
	}
}
