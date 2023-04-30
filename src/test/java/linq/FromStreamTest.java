package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.Test;

public class FromStreamTest {
	@Test
	public void testFrom() {
		var i = Linq.from(Stream.of("a", "b", "c")).iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var count = new AtomicInteger(0);
		Linq.from(Stream.empty().onClose(() -> count.incrementAndGet())).iterator().close();
		assertEquals(1, count.get());
	}

}
