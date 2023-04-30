package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.Test;

public class ChunkTest {
	@Test
	public void testEmpty() {
		var i = Linq.empty().chunk(1).iterator();
		assertFalse(i.hasNext());
	}

	@Test
	public void testChunk() {
		var i = Linq.range(0, 10).chunk(3).iterator();
		assertEquals(Arrays.asList(0, 1, 2), i.next());
		assertEquals(Arrays.asList(3, 4, 5), i.next());
		assertEquals(Arrays.asList(6, 7, 8), i.next());
		assertEquals(Arrays.asList(9), i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.chunk(1).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}
}
