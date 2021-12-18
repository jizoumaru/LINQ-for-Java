package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Test;

public class FromTest {
	@Test
	public void testFrom() {
		var list = Arrays.asList("a", "b", "c");
		var iter = Linq.from(list).iterator();
		assertEquals(iter.next(), "a");
		assertEquals(iter.next(), "b");
		assertEquals(iter.next(), "c");
		assertFalse(iter.hasNext());
	}
}
