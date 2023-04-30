package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ContainsTest {

	@Test
	public void testContains() {
		assertTrue(Linq.from("a", "b", "c").contains("a"));
		assertTrue(Linq.from("a", "b", "c").contains("b"));
		assertTrue(Linq.from("a", "b", "c").contains("c"));
		assertFalse(Linq.from("a", "b", "c").contains("d"));
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.contains(1);
		assertEquals(1, linq.getCloseCount());
	}
}
