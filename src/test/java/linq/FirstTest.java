package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.Test;

public class FirstTest {

	@Test
	public void testFirst() {
		assertEquals("a", Linq.from("a", "b", "c").first());
		assertThrows(NoSuchElementException.class, () -> Linq.from().first());
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.first();
		assertEquals(1, linq.getCloseCount());
	}

}
