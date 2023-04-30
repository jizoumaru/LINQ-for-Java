package linq;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AllTest {

	@Test
	public void testAll() {
		assertTrue(Linq.from(2, 4, 6).all(x -> x % 2 == 0));
		assertFalse(Linq.from(1, 4, 6).all(x -> x % 2 == 0));
		assertFalse(Linq.from(2, 1, 6).all(x -> x % 2 == 0));
		assertFalse(Linq.from(2, 4, 1).all(x -> x % 2 == 0));
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.all(x -> true);
		assertEquals(1, linq.getCloseCount());
	}

}
