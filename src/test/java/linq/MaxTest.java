package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.Test;

public class MaxTest {

	@Test
	public void testMax() {
		assertThrows(NoSuchElementException.class, () -> Linq.<String>from().max(String::compareTo));
		assertEquals("a", Linq.from("a").max(String::compareTo));
		assertEquals("e", Linq.from("d", "b", "e", "c", "a")
				.max(String::compareTo));
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.max(Integer::compare);
		assertEquals(1, linq.getCloseCount());
	}

}
