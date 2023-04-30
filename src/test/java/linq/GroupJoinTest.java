package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class GroupJoinTest {

	@Test
	public void testGroupJoin() {
		var i = Linq.from("La", "Lb", "Lc", "Ld", "Le", "Lf")
				.groupJoin(
						Linq.from("dR", "eR", "fR", "gR", "hR", "iR", "eR2", "fR2", "fR3"),
						x -> x.charAt(1),
						x -> x.charAt(0),
						(x, y) -> x + ":" + y.toList())
				.iterator();

		assertEquals("La:[]", i.next());
		assertEquals("Lb:[]", i.next());
		assertEquals("Lc:[]", i.next());
		assertEquals("Ld:[dR]", i.next());
		assertEquals("Le:[eR, eR2]", i.next());
		assertEquals("Lf:[fR, fR2, fR3]", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var left = CloseCountLinq.create();
		var right = CloseCountLinq.create();

		left.groupJoin(right, x -> x, x -> x, (x, y) -> 0).iterator().close();

		assertEquals(1, left.getCloseCount());
		assertEquals(1, right.getCloseCount());
	}
}
