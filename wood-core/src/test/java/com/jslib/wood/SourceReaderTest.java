package com.jslib.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
	public void GivenStringReference_WhenSourceRead_ThenReferenceHandlerParameters() throws IOException {
		// given
		String source = "" + //
				"<body>" + //
				"	<h1>@string/title</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));
		when(sourceFile.value()).thenReturn("res/compo/compo.htm");

		ArgumentCaptor<Reference> referenceCaptor = ArgumentCaptor.forClass(Reference.class);
		ArgumentCaptor<FilePath> sourceFileCaptor = ArgumentCaptor.forClass(FilePath.class);
		when(handler.onResourceReference(referenceCaptor.capture(), sourceFileCaptor.capture())).thenReturn("Component Title");

		// when
		SourceReader reader = new SourceReader(sourceFile, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);

		// then
		assertThat(referenceCaptor.getValue().getType(), equalTo(Reference.Type.STRING));
		assertThat(referenceCaptor.getValue().getName(), equalTo("title"));
		assertThat(sourceFileCaptor.getValue().value(), equalTo("res/compo/compo.htm"));
	}

	@Test
	public void GivenLiteralAtMetaReference_WhenSourceRead_ThenPreserveAtChar() throws IOException {
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
	public void GivenStringReference_WhenSourceRead_ThenStringResolve() throws IOException {
		// given
		String source = "" + //
				"<body>" + //
				"	<h1>@string/title</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));

		when(handler.onResourceReference(new Reference(Reference.Type.STRING, "title"), sourceFile)).thenReturn("Component Title");

		// when
		SourceReader reader = new SourceReader(sourceFile, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);

		// then
		assertTrue(writer.toString().contains("<h1>Component Title</h1>"));
	}

	@Test
	public void GivenMediaQuery_WhenSourceRead_ThenPreserveMediaQuery() throws IOException {
		// given
		String source = "" + //
				"@media all" + //
				"body {" + //
				"	width: 800px" + //
				"}";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));

		// when
		SourceReader reader = new SourceReader(sourceFile, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);

		// then
		assertTrue(writer.toString().contains("@media all"));
	}

	@Test
	public void GivenParamReference_WhenSourceRead_ThenParamResolve() throws IOException {
		// given
		String source = "" + //
				"<body>" + //
				"	<h1>@param/title</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));
		when(sourceFile.isLayout()).thenReturn(true);

		LayoutParameters parameters = new LayoutParameters();
		parameters.reload("title:Component Parameter;");

		// when
		SourceReader reader = new SourceReader(sourceFile, parameters, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);

		// then
		assertTrue(writer.toString().contains("<h1>Component Parameter</h1>"));
	}

	@Test(expected = WoodException.class)
	public void GiveParamReferenceOnMissingParameter_WhenSourceRead_ThenException() throws IOException {
		// given
		String source = "" + //
				"<body>" + //
				"	<h1>@param/title</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));
		when(sourceFile.isLayout()).thenReturn(true);

		LayoutParameters parameters = new LayoutParameters(); // empty parameters

		// when
		SourceReader reader = new SourceReader(sourceFile, parameters, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);

		// then
	}

	@Test(expected = WoodException.class)
	public void GivenParamReferenceAndNullLayoutParameters_WhenSourceRead_ThenException() throws IOException {
		// given
		String source = "" + //
				"<body>" + //
				"	<h1>@param/title</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));
		when(sourceFile.isLayout()).thenReturn(true);

		// when
		SourceReader reader = new SourceReader(sourceFile, null, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);

		// then
	}

	@Test
	public void GivenUnknownReference_WhenSourceRead_ThenPreserveText() throws IOException {
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

	@Test
	public void GivenProjectReference_WhenSourceRead_ThenProjectDescriptorValue() throws IOException {
		// given
		String source = "" + //
				"<body>" + //
				"	<h1>@project/authors</h1>" + //
				"</body>";
		when(sourceFile.getReader()).thenReturn(new StringReader(source));

		when(handler.onResourceReference(new Reference(Reference.Type.PROJECT, "authors"), sourceFile)).thenReturn("Iulian Rotaru&lt;mr.iulianrotaru@gmail.com&gt;");

		// when
		SourceReader reader = new SourceReader(sourceFile, handler);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);

		// then
		assertTrue(writer.toString().contains("Iulian Rotaru&lt;mr.iulianrotaru@gmail.com&gt;"));
	}
}
