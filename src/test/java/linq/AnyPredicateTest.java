package linq;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AnyPredicateTest {

	@Test
	public void testAny() {
		assertTrue(Linq.from(2, 3, 5).any(x -> x == 2));
		assertTrue(Linq.from(1, 2, 5).any(x -> x == 2));
		assertTrue(Linq.from(1, 3, 2).any(x -> x == 2));
		assertFalse(Linq.from(1, 3, 5).any(x -> x == 2));
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.any(x -> true);
		assertEquals(1, linq.getCloseCount());
	}

}
