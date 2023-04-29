package linq;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RangeTest {

	@Test
	public void testRange() {
		var i = Linq.range(2, 3).iterator();
		assertTrue(2 == i.next());
		assertTrue(3 == i.next());
		assertTrue(4 == i.next());
		assertFalse(i.hasNext());
	}

}
