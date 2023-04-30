package linq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class AppendTest {
	public static void main(String[] args) {
		try {
			Linq.from(Files.list(Path.of("C:\\Users\\a\\git\\LINQ-for-Java\\src\\test\\java\\linq")))
					.where(x -> x.toString().endsWith(".java"))
					.forEach(file -> {
						try {
							var content = Files.readString(file);
							var newContent = content.replace("CloseCountLinq.create()", "CloseCountLinq.create()");
							System.out.println(newContent);
							Files.writeString(file, newContent);
						} catch (IOException e) {
							throw new RuntimeException(file.toString(), e);
						}
					});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAppend() {
		var i = Linq.from("a", "b").append("c").iterator();
		assertEquals("a", i.next());
		assertEquals("b", i.next());
		assertEquals("c", i.next());
		assertFalse(i.hasNext());
	}

	@Test
	public void testClose() {
		var linq = CloseCountLinq.create();
		linq.append(2).iterator().close();
		assertEquals(1, linq.getCloseCount());
	}
}
