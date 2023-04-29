package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;

import org.junit.Test;

public class MaxByTest {

	@Test
	public void testMaxBy() {
		assertThrows(NoSuchElementException.class, () -> Linq.<String>from()
				.maxBy(x -> x.substring(1, 2)));

		assertEquals("Aa", Linq.from("Aa")
				.maxBy(x -> x.substring(1, 2)));

		assertEquals("Ce", Linq.from("Ad", "Bb", "Ce", "Dc", "Ea")
				.maxBy(x -> x.substring(1, 2)));
	}

	@Test
	public void testMaxByComparator() {
		assertThrows(NoSuchElementException.class, () -> Linq.<String>from()
				.maxBy(x -> x, String::compareTo));

		assertEquals("Aa", Linq.from("Aa")
				.maxBy(x -> x, (x, y) -> x.substring(1).compareTo(y.substring(1))));

		assertEquals("Ce", Linq.from("Ad", "Bb", "Ce", "Dc", "Ea")
				.maxBy(x -> x, (x, y) -> x.substring(1).compareTo(y.substring(1))));

		assertEquals("Ce", Linq.from("Ad", "Bb", "Ce", "Dc", "Ea")
				.maxBy(x -> x.substring(1), String::compareTo));
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.maxBy(x -> x);
		assertEquals(1, linq.getCloseCount());
	}

	@Test
	public void testCloseComparator() {
		var linq = new CloseCountLinq();
		linq.maxBy(x -> x, Integer::compare);
		assertEquals(1, linq.getCloseCount());
	}

}
