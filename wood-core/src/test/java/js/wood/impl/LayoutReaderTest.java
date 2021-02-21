package js.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
	public void content() throws IOException {
		Reader reader = new StringReader("<h1>header 1</h1><h2>header 2</h2>");
		Reader layoutReader = new LayoutReader(reader);

		StringWriter stringWriter = new StringWriter();
		Files.copy(layoutReader, stringWriter);

		String expected = "<?xml version='1.0' encoding='UTF-8'?>\r\n" + //
				"<!DOCTYPE layout PUBLIC '-//W3C//DTD XHTML 1.0 Transitional//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd'>\r\n" + //
				"<layout>\r\n" + //
				"<h1>header 1</h1><h2>header 2</h2>\r\n" + //
				"</layout>";

		assertThat(stringWriter.toString(), equalTo(expected));
	}

	@Test
	public void document() throws IOException {
		Reader reader = new StringReader("<h1>header 1</h1><h2>header 2</h2>");
		Reader layoutReader = new LayoutReader(reader);

		StringWriter stringWriter = new StringWriter();
		Files.copy(layoutReader, stringWriter);

		DocumentBuilder builder = new DocumentBuilderImpl();
		Document doc = builder.parseXML(stringWriter.toString());
		assertThat(doc.getRoot().getTag(), equalTo("layout"));
		assertThat(doc.getRoot().getByTag("h1").getText(), equalTo("header 1"));
		assertThat(doc.getRoot().getByTag("h2").getText(), equalTo("header 2"));
	}
}
