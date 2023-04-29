package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class FromArrayTest {

	@Test
	public void testFromArray() {
		var i = Linq.from("a", "b", "c").iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

}
