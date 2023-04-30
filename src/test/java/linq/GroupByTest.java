package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Test;

public class GroupByTest {

	@Test
	public void testGroupBy() {
		var i = Linq.from("d3", "f3", "c2", "e3", "b2", "a1")
				.groupBy(x -> x.substring(1, 2)).iterator();

		var e = i.next();
		assertEquals("3", e.getKey());
		assertEquals(Arrays.asList("d3", "f3", "e3"), e.getValue());

		e = i.next();
		assertEquals("2", e.getKey());
		assertEquals(Arrays.asList("c2", "b2"), e.getValue());

		e = i.next();
		assertEquals("1", e.getKey());
		assertEquals(Arrays.asList("a1"), e.getValue());

		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.groupBy(x -> x).iterator().close();

		assertEquals(1, linq.getCloseCount());
	}

}
