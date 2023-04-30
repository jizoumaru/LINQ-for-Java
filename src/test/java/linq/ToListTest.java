package linq;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class ToListTest {

	@Test
	public void testToList() {
		var a = Linq.from("a", "b", "c").toList();
		var e = Arrays.asList("a", "b", "c");
		assertEquals(e, a);
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.toList();
		assertEquals(1, linq.getCloseCount());
	}

}
