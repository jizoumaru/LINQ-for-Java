package linq;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

public class ToDictionaryTest {

	@Test
	public void testToDictionary() {
		{
			var a = Linq.from("Aa", "Bb", "Cc").toDictionary(x -> x.substring(0, 1));
			var e = new HashMap<String, String>();
			e.put("A", "Aa");
			e.put("B", "Bb");
			e.put("C", "Cc");
			assertEquals(e, a);
		}

		{
			try {
				Linq.from("a", "a").toDictionary(x -> x);
				Assert.fail();
			} catch (IllegalArgumentException e) {
				assertEquals(e.getMessage(), "キーが重複しています: a");
			}
		}
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.toDictionary(x -> x);
		assertEquals(1, linq.getCloseCount());
	}

}
