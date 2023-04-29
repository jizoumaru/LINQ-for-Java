package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.Test;

public class MinTest {

	@Test
	public void testMin() {
		assertThrows(NoSuchElementException.class, () -> Linq.<String>from().min(String::compareTo));
		assertEquals("a", Linq.from("a").min(String::compareTo));
		assertEquals("a", Linq.from("e", "b", "a", "d", "c").min(String::compareTo));
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.min(Integer::compare);
		assertEquals(1, linq.getCloseCount());
	}

}
