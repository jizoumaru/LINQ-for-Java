package linq;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

public class ToHashSetTest {

	@Test
	public void testToHashSet() {
		var a = Linq.from("a", "b", "b", "c", "c", "c").toHashSet();
		var e = new HashSet<String>();
		e.add("a");
		e.add("b");
		e.add("c");
		assertEquals(e, a);
	}
	
	@Test
	public void testClose() {
		var linq = new CloseCountLinq();
		linq.toHashSet();
		assertEquals(1, linq.getCloseCount());
	}

}
