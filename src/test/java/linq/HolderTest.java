package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

import org.junit.Test;

import linq.Linq.Holder;

public class HolderTest {
	@Test
	public void testOf() {
		var n = Holder.of("a");
		assertTrue(n.exists());
		assertEquals("a", n.value());
	}

	@Test
	public void testNull() {
		var n = Holder.of(null);
		assertTrue(n.exists());
		assertEquals(null, n.value());
	}

	@Test
	public void testNone() {
		var n = Holder.none();
		assertFalse(n.exists());

		NoSuchElementException ex = null;
		try {
			n.value();
		} catch (NoSuchElementException e) {
			ex = e;
		}
		assertTrue(ex != null);
	}
}
