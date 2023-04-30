package linq;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class SingleDefaultTest {

	@Test
	public void testSingleDefault() {
		assertEquals("a", Linq.from("a").singleOrDefault("d"));
		assertEquals("d", Linq.from().singleOrDefault("d"));

		try {
			Linq.from("a", "b").singleOrDefault("d");
			Assert.fail();
		} catch (IllegalStateException e) {
			assertEquals("値が複数存在します", e.getMessage());
		}
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.singleOrDefault(0);
		assertEquals(1, linq.getCloseCount());
	}

}
