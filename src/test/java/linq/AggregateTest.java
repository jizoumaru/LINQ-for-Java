package linq;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.NoSuchElementException;

import org.junit.Test;

public class AggregateTest {

	@Test
	public void testAggregate() {
		var s = Linq.from(1, 2, 3).aggregate((l, r) -> l + r);
		assertTrue(6 == s);
	}

	@Test
	public void testAggregateNone() {
		assertThrows(NoSuchElementException.class, () -> Linq.<Integer>from().aggregate((l, r) -> l + r));
	}

	@Test
	public void testAggregateSeed() {
		var s = Linq.from(1, 2, 3).aggregate(4, (l, r) -> l + r);
		assertTrue(10 == s);
	}

	@Test
	public void testAggregateSeedNone() {
		var s = Linq.<Integer>from().aggregate(4, (l, r) -> l + r);
		assertTrue(4 == s);
	}
	
	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.aggregate((l, r) -> l);
		assertEquals(1, linq.getCloseCount());
	}
}
