package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;

public class ElementAtTest {

	@Test
	public void testElementAt() {
		assertEquals("a", Linq.from("a", "b", "c").elementAt(0));
		assertEquals("b", Linq.from("a", "b", "c").elementAt(1));
		assertEquals("c", Linq.from("a", "b", "c").elementAt(2));
		assertThrows(IndexOutOfBoundsException.class, () -> Linq.from("a", "b", "c").elementAt(3));
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.elementAt(0);
		assertEquals(1, linq.getCloseCount());
	}
}
