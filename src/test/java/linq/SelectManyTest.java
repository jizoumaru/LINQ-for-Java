package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Test;

public class SelectManyTest {

	@Test
	public void testSelectMany() {
		var i = Linq.from(Arrays.asList(
				Arrays.asList("a"),
				Arrays.asList("b", "c"),
				Arrays.asList("d", "e", "f")))
				.selectMany(x -> Linq.from(x))
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
		var base = CloseCountLinq.create();
		var sub1 = CloseCountLinq.create();

		var iter = base.selectMany(x -> sub1).iterator();
		iter.next();
		iter.close();
		assertEquals(1, sub1.getCloseCount());
		assertEquals(1, base.getCloseCount());
	}

}
