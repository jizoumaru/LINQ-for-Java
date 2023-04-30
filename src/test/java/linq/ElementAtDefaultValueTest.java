package linq;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ElementAtDefaultValueTest {

	@Test
	public void testElementAtDefaultValue() {
		assertEquals("a", Linq.from("a", "b", "c").elementAtOrDefault(0, "d"));
		assertEquals("b", Linq.from("a", "b", "c").elementAtOrDefault(1, "d"));
		assertEquals("c", Linq.from("a", "b", "c").elementAtOrDefault(2, "d"));
		assertEquals("d", Linq.from("a", "b", "c").elementAtOrDefault(3, "d"));
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.elementAtOrDefault(0, 0);
		assertEquals(1, linq.getCloseCount());
	}
}
