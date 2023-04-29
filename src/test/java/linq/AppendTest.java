package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class AppendTest {

	@Test
	public void testAppend() {
		var i = Linq.from("a", "b").append("c").iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.append(2).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}
}
