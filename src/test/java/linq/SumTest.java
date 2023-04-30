package linq;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class SumTest {

	@Test
	public void testSum() {
		assertEquals(60L, Linq.from("10", "20", "30")
				.sum(x -> Long.valueOf(x)));
		assertEquals(0L, Linq.from(Arrays.<String>asList()).sum(x -> Long.valueOf(x)));
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.sum(x -> Long.valueOf(x));
		assertEquals(1, linq.getCloseCount());
	}

}
