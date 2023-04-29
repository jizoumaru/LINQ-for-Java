package linq;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AnyTest {

	@Test
	public void testAny() {
		assertFalse(Linq.from().any());
		assertTrue(Linq.from(1).any());
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.any();
		assertEquals(1, linq.getCloseCount());
	}

}
