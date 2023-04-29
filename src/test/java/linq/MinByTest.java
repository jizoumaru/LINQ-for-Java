package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.Test;

public class MinByTest {
	@Test
	public void testMinBy() {
		assertThrows(NoSuchElementException.class, () -> Linq.<String>from()
				.minBy(x -> x.substring(1, 2)));

		assertEquals("Aa", Linq.from("Aa")
				.minBy(x -> x.substring(1, 2)));

		assertEquals("Ea", Linq.from("Dc", "Bb", "Ce", "Ea", "Ad")
				.minBy(x -> x.substring(1, 2)));
	}

	@Test
	public void testMinByComparator() {
		assertThrows(NoSuchElementException.class, () -> Linq.<String>from()
				.minBy(x -> x.substring(1, 2), String::compareTo));

		assertEquals("Aa", Linq.from("Aa")
				.minBy(x -> x.substring(1, 2), String::compareTo));

		assertEquals("Ea", Linq.from("Dc", "Bb", "Ce", "Ea", "Ad")
				.minBy(x -> x.substring(1, 2), String::compareTo));

		assertEquals("Ea", Linq.from("Dc", "Bb", "Ce", "Ea", "Ad")
				.minBy(x -> x, (x, y) -> x.substring(1, 2).compareTo(y.substring(1, 2))));
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.minBy(x -> x);
		assertEquals(1, linq.getCloseCount());
	}

	@Test
	public void testCloseComparator() {
		var linq = new CloseCountLinq();
		linq.minBy(x -> x, Integer::compare);
		assertEquals(1, linq.getCloseCount());
	}

}
