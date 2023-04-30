package linq;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CountTest {

	@Test
	public void testCount() {
		assertEquals(3L, Linq.range(0, 3).count());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.count();
		assertEquals(1, linq.getCloseCount());
	}
}
