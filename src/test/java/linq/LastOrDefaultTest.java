package linq;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LastOrDefaultTest {

	@Test
	public void testLast() {
		assertEquals("X", Linq.from().lastOrDefault("X"));
		assertEquals("a", Linq.from("a").lastOrDefault("X"));
		assertEquals("c", Linq.from("a", "b", "c").lastOrDefault("X"));
	}
	
	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.lastOrDefault(0);
		assertEquals(1, linq.getCloseCount());
	}

}
