package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import linq.Linq.Tuple2;

public class ZipTest {

	@Test
	public void testZip() {
		{
			var i = Linq.from("a", "b", "c")
					.zip(Linq.from("d", "e", "f"))
					.iterator();
			assertEquals(new Tuple2<String, String>("a", "d"), i.next());
			assertEquals(new Tuple2<String, String>("b", "e"), i.next());
			assertEquals(new Tuple2<String, String>("c", "f"), i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("a", "b")
					.zip(Linq.from("a'"))
					.iterator();
			assertEquals(new Tuple2<String, String>("a", "a'"), i.next());
			assertFalse(i.hasNext());
		}

		{
			var i = Linq.from("a")
					.zip(Linq.from("a'", "b'"))
					.iterator();
			assertEquals(new Tuple2<String, String>("a", "a'"), i.next());
			assertFalse(i.hasNext());
		}
	}

	@Test
	public void testClose() {
		var left = CloseCountLinq.create();
		var right = CloseCountLinq.create();

		left.zip(right).iterator().close();

		assertEquals(1, left.getCloseCount());
		assertEquals(1, right.getCloseCount());
	}

}
