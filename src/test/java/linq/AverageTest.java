package linq;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class AverageTest {

	@Test
	public void testAverage() {
		var s = Linq.from(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).average(x -> Long.valueOf(x));
		assertTrue(5 == s);
	}

	@Test
	public void testAverageNone() {
		assertThrows(ArithmeticException.class, () -> Linq.<Long>from().average(x -> x));
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.average(x -> Long.valueOf(x));
		assertEquals(1, linq.getCloseCount());
	}
}
