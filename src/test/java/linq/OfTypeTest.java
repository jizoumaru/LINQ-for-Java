package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Test;

public class OfTypeTest {

	@Test
	public void testOfType() {
		var i = Linq.from(Arrays.<Object>asList("a")).ofType(String.class).iterator();
		String s = i.next();
		assertEquals("a", s);
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.ofType(Integer.class).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}
}
