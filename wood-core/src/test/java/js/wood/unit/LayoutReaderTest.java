package js.wood.unit;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.w3c.DocumentBuilderImpl;
import js.util.Files;
import js.wood.impl.LayoutReader;

public class LayoutReaderTest {
	@Test
	public void layoutReader() throws IOException {
		Reader reader = new StringReader("<h1>header 1</h1><h2>header 2</h2>");
		Reader layoutReader = new LayoutReader(reader);

		StringWriter stringWriter = new StringWriter();
		Files.copy(layoutReader, stringWriter);

		DocumentBuilder builder = new DocumentBuilderImpl();
		Document doc = builder.parseXML(stringWriter.toString());
		assertEquals("layout", doc.getRoot().getTag());
		assertEquals("header 1", doc.getRoot().getByTag("h1").getText());
		assertEquals("header 2", doc.getRoot().getByTag("h2").getText());
	}

}
