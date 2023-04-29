package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class RepeatTest {

	@Test
	public void testRepeat() {
		var i = Linq.repeat("a", 3).iterator();
		assertEquals("a", i.next());
		assertEquals("a", i.next());
		assertEquals("a", i.next());
		assertFalse(i.hasNext());
	}

}
