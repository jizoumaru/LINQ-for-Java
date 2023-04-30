package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SequenceEqualTest {

	@Test
	public void testSequenceEqual() {
		assertTrue(Linq.from("a", "b", "c")
				.sequenceEqual(Linq.from("a", "b", "c")));

		assertFalse(Linq.from("a", "b", "c")
				.sequenceEqual(Linq.from("a", "b", "d")));

		assertFalse(Linq.from("a", "b", "c")
				.sequenceEqual(Linq.from("a", "b")));

		assertFalse(Linq.from("a", "b", "c")
				.sequenceEqual(Linq.from("a", "b", "c", "d")));
	}

	@Test
	public void testClose() {
		var left = CloseCountLinq.create();
		var right = CloseCountLinq.create();

		left.sequenceEqual(right);

		assertEquals(1, left.getCloseCount());
		assertEquals(1, right.getCloseCount());
	}

}
