package linq;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class SingleTest {

	@Test
	public void testSingle() {
		assertEquals("a", Linq.from("a").single());

		try {
			Linq.from().single();
			Assert.fail();
		} catch (IllegalStateException e) {
			assertEquals("値が存在しません", e.getMessage());
		}

		try {
			Linq.from("a", "b").single();
			Assert.fail();
		} catch (IllegalStateException e) {
			assertEquals("値が複数存在します", e.getMessage());
		}

	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.single();
		assertEquals(1, linq.getCloseCount());
	}

}
