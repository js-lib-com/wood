package com.jslib.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.util.Classes;
import com.jslib.util.Files;
import com.jslib.wood.impl.LayoutParameters;

@RunWith(MockitoJUnitRunner.class)
public class SourceReaderTest {
	@Mock
	private Project project;
	@Mock
	private FilePath sourceFile;
	@Mock
	private IReferenceHandler handler;

	@Before
	public void beforeTest() throws Exception {
		when(sourceFile.exists()).thenReturn(true);
	}

	@Test
	public void fileConstructor() {
		String source = "" + //
				"<body>" + //
				"	<h1>Title</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));
		when(sourceFile.toString()).thenReturn("res/compo/compo.htm");

		SourceReader reader = new SourceReader(sourceFile, handler);
		assertReader(reader);
	}

	@Test
	public void readerConstructor() throws FileNotFoundException {
		when(sourceFile.toString()).thenReturn("res/compo/compo.htm");

		Reader fileReader = new StringReader("");
		SourceReader reader = new SourceReader(fileReader, sourceFile, handler);
		assertReader(reader);
	}

	private void assertReader(SourceReader reader) {
		assertThat(field(reader, "sourceFile").toString(), equalTo("res/compo/compo.htm"));
		assertThat(field(reader, "referenceHandler"), equalTo(handler));
		assertThat(field(reader, "reader"), notNullValue());
		assertThat(field(reader, "metaBuilder"), notNullValue());
		assertThat(field(reader, "state").toString(), equalTo("TEXT"));
		assertThat(field(reader, "value"), nullValue());
		assertThat((int) field(reader, "valueIndex"), equalTo(0));
		assertThat((int) field(reader, "charAfterMeta"), equalTo(0));
	}

	@Test
	public void referenceHandlerParameter() throws IOException {
		String source = "" + //
				"<body>" + //
				"	<h1>@string/title</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));
		when(sourceFile.value()).thenReturn("res/compo/compo.htm");

		ArgumentCaptor<Reference> referenceCaptor = ArgumentCaptor.forClass(Reference.class);
		ArgumentCaptor<FilePath> sourceFileCaptor = ArgumentCaptor.forClass(FilePath.class);
		when(handler.onResourceReference(referenceCaptor.capture(), sourceFileCaptor.capture())).thenReturn("Component Title");

		SourceReader reader = new SourceReader(sourceFile, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);

		assertThat(referenceCaptor.getValue().getType(), equalTo(Reference.Type.STRING));
		assertThat(referenceCaptor.getValue().getName(), equalTo("title"));
		assertThat(sourceFileCaptor.getValue().value(), equalTo("res/compo/compo.htm"));
	}

	@Test
	public void GivenLiteralAtMetaReference_WhenCopySource_ThenPreserveAtChar() throws IOException {
		// given
		String source = "" + //
				"<body>" + //
				"	<h1>@@string/title</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));

		// when
		SourceReader reader = new SourceReader(sourceFile, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
		
		// then
		assertTrue(writer.toString().contains("<h1>@string/title</h1>"));
	}

	@Test
	public void copy_StringResolve() throws IOException {
		String source = "" + //
				"<body>" + //
				"	<h1>@string/title</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));

		when(handler.onResourceReference(new Reference(Reference.Type.STRING, "title"), sourceFile)).thenReturn("Component Title");

		SourceReader reader = new SourceReader(sourceFile, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
		assertTrue(writer.toString().contains("<h1>Component Title</h1>"));
	}

	@Test
	public void copy_StyleMedia() throws IOException {
		String source = "" + //
				"@media all" + //
				"body {" + //
				"	width: 800px" + //
				"}";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));

		SourceReader reader = new SourceReader(sourceFile, handler);

		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
		assertTrue(writer.toString().contains("@media all"));
	}

	@Test
	public void copy_ParamResolve() throws IOException {
		String source = "" + //
				"<body>" + //
				"	<h1>@param/title</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));
		when(sourceFile.isLayout()).thenReturn(true);

		LayoutParameters parameters = new LayoutParameters();
		parameters.reload("title:Component Parameter;");

		SourceReader reader = new SourceReader(sourceFile, parameters, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
		assertTrue(writer.toString().contains("<h1>Component Parameter</h1>"));
	}

	@Test(expected = WoodException.class)
	public void copy_ParamResolve_MissingParameter() throws IOException {
		String source = "" + //
				"<body>" + //
				"	<h1>@param/title</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));
		when(sourceFile.isLayout()).thenReturn(true);

		LayoutParameters parameters = new LayoutParameters();

		SourceReader reader = new SourceReader(sourceFile, parameters, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
	}

	@Test(expected = WoodException.class)
	public void copy_ParamResolve_NullLayoutParameters() throws IOException {
		String source = "" + //
				"<body>" + //
				"	<h1>@param/title</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));
		when(sourceFile.isLayout()).thenReturn(true);

		SourceReader reader = new SourceReader(sourceFile, null, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
	}

	@Test
	public void GivenUnknownReferenceType_WhenSourceRead_ThenPreserveText() throws IOException {
		// given
		String source = "" + //
				"<body>" + //
				"	<h1>@strings/value</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));

		// when
		SourceReader reader = new SourceReader(sourceFile, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);

		// then
		assertTrue(writer.toString().contains("@strings/value"));
	}

	// --------------------------------------------------------------------------------------------
	// Internal helpers

	private static <T> T field(Object object, String field) {
		return Classes.getFieldValue(object, field);
	}
}
