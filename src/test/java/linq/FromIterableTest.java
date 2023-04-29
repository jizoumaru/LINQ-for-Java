package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Test;

public class FromIterableTest {
	@Test
	public void testFrom() {
		var i = Linq.from(Arrays.asList("a", "b", "c")).iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

}
