package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class UnionTest {

	@Test
	public void testUnion() {
		var i = Linq.from("a", "b", "c", "d")
				.union(Linq.from("c", "d", "e", "f"))
				.iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertEquals("d", i.next());
		assertEquals("e", i.next());
		assertEquals("f", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var left = new CloseCountLinq();
		var right = new CloseCountLinq();

		left.union(right).iterator().close();

		assertEquals(1, left.getCloseCount());
		assertEquals(1, right.getCloseCount());
	}

}
