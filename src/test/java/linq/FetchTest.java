package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class FetchTest {
	@Test
	public void testClosedPeek() {
		var fetch = Linq.empty().fetch();
		fetch.close();
		assertThrows(IllegalStateException.class, () -> fetch.peek());
	}

	@Test
	public void testClosedNext() {
		var fetch = Linq.empty().fetch();
		fetch.close();
		assertThrows(IllegalStateException.class, () -> fetch.next());
	}

	@Test
	public void testPeekNext() {
		var fetch = Linq.from("A", "B", "C").fetch();
		assertEquals("A", fetch.peek().value());
		assertEquals("A", fetch.peek().value());
		assertEquals("A", fetch.next().value());
		assertEquals("B", fetch.peek().value());
		assertEquals("B", fetch.peek().value());
		assertEquals("B", fetch.next().value());
		assertEquals("C", fetch.peek().value());
		assertEquals("C", fetch.peek().value());
		assertEquals("C", fetch.next().value());
		assertFalse(fetch.next().exists());
	}
	
	@Test
	public void testNextNext() {
		var fetch = Linq.from("A", "B", "C").fetch();
		assertEquals("A", fetch.next().value());
		assertEquals("B", fetch.next().value());
		assertEquals("C", fetch.next().value());
		assertFalse(fetch.next().exists());
	}
}
