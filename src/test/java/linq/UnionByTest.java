package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class UnionByTest {

	@Test
	public void testUnionBy() {
		var i = Linq.from("Aa", "Ab", "Ac", "Ad", "Ba", "Bb")
				.unionBy(Linq.from("Bc", "Bd", "Be", "Bf", "Ce", "Cf"),
						x -> x.substring(1, 2))
				.iterator();
		assertEquals("Aa", i.next());
		assertEquals("Ab", i.next());
		assertEquals("Ac", i.next());
		assertEquals("Ad", i.next());
		assertEquals("Be", i.next());
		assertEquals("Bf", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var left = CloseCountLinq.create();
		var right = CloseCountLinq.create();

		left.unionBy(right, x -> x).iterator().close();

		assertEquals(1, left.getCloseCount());
		assertEquals(1, right.getCloseCount());
	}

}
