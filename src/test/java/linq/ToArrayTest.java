package linq;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ToArrayTest {

	@Test
	public void testToArray() {
		var a = Linq.from("a", "b", "c").toArray(new String[] {});
		assertArrayEquals(new String[] { "a", "b", "c" }, a);
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.toArray(new Integer[] {});
		assertEquals(1, linq.getCloseCount());
	}

}
