package linq;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FirstOrDefaultTest {

	@Test
	public void testFirstOrDefault() {
		assertEquals("a", Linq.from("a", "b", "c").firstOrDefault("def"));
		assertEquals("def", Linq.from().firstOrDefault("def"));
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.firstOrDefault(0);
		assertEquals(1, linq.getCloseCount());
	}
}
