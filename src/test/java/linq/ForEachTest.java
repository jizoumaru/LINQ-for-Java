package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class ForEachTest {

	@Test
	public void testEmpty() {
		var count = new AtomicInteger(0);
		Linq.empty().forEach(x -> {
			count.incrementAndGet();
		});
		assertEquals(0, count.get());
	}

	@Test
	public void testSome() {
		var expectets = new ArrayDeque<Integer>(Arrays.asList(1, 2, 3));
		Linq.from(1, 2, 3).forEach(x -> {
			assertEquals(expectets.removeFirst(), x);
		});
		assertEquals(0, expectets.size());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.forEach(x -> {
		});
		assertEquals(1, linq.getCloseCount());
	}

}
