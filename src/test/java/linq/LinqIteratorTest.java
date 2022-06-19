package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

import org.junit.Test;

import linq.Linq.LinqIterator;
import linq.Linq.Holder;

public class LinqIteratorTest {
	@Test
	public void testNext() {
		var i = new LinqIterator<Integer>() {
			int i = 0;
			int n = 3;

			@Override
			protected Holder<Integer> get() {
				if (i < n) {
					return Holder.of(i++);
				} else {
					return Holder.none();
				}
			}
		};

		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.next() == 0);

		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.next() == 1);

		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.hasNext());
		assertTrue(i.next() == 2);

		assertFalse(i.hasNext());
		assertFalse(i.hasNext());
		assertFalse(i.hasNext());

		NoSuchElementException ex = null;
		try {
			i.next();
		} catch (NoSuchElementException e) {
			ex = e;
		}
		assertTrue(ex != null);
		i.close();
	}

	@Test
	public void testRemove() {
		var i = new LinqIterator<Integer>() {
			int i = 0;
			int n = 3;

			@Override
			protected Holder<Integer> get() {
				if (i < n) {
					return Holder.of(i++);
				} else {
					return Holder.none();
				}
			}
		};

		String msg = null;
		try {
			i.remove();
		} catch (RuntimeException e) {
			msg = e.getMessage();
		}
		assertEquals("not supported", msg);
		i.close();
	}
}
