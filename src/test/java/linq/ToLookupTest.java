package linq;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

public class ToLookupTest {

	@Test
	public void testToLookup() {
		var a = Linq.from("Aa", "Bb", "Bc", "Cd", "Ce", "Cf")
				.toLookup(x -> x.substring(0, 1));
		var e = new HashMap<String, List<String>>();
		e.put("A", Arrays.asList("Aa"));
		e.put("B", Arrays.asList("Bb", "Bc"));
		e.put("C", Arrays.asList("Cd", "Ce", "Cf"));
		assertEquals(e, a);
	}

	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.toLookup(x -> x);
		assertEquals(1, linq.getCloseCount());
	}

}
