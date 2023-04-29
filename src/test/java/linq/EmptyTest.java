package linq;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class EmptyTest {

	@Test
	public void testEmpty() {
		var i = Linq.empty().iterator();
		assertFalse(i.hasNext());
	}
}
