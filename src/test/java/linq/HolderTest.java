package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.Test;

import linq.Linq.Holder;

public class HolderTest {
	@Test
	public void testOf() {
		assertTrue(Holder.of("a").exists());
		assertEquals("a", Holder.of("a").value());
	}

	@Test
	public void testNull() {
		assertTrue(Holder.of(null).exists());
		assertEquals(null, Holder.of(null).value());
	}

	@Test
	public void testNone() {
		assertFalse(Holder.none().exists());
		assertThrows(NoSuchElementException.class, () -> Holder.none().value());
	}
}
