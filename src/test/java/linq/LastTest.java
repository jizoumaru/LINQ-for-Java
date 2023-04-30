package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.NoSuchElementException;

import org.junit.Test;

public class LastTest {

	@Test
	public void testLast() {
		assertThrows(NoSuchElementException.class, () -> Linq.from().last());
		assertEquals("a", Linq.from("a").last());
		assertEquals("c", Linq.from("a", "b", "c").last());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.last();
		assertEquals(1, linq.getCloseCount());
	}

}
