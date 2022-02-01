package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.util.Files;
import js.wood.impl.CustomElementsRegistry;
import js.wood.impl.OperatorsNaming;

@RunWith(MockitoJUnitRunner.class)
public class LayoutReaderTest {
	@Mock
	private Project project;
	@Mock
	private CustomElementsRegistry customElements;
	@Mock
	private ICustomElement customElement;
	@Mock
	private FilePath parentDir;
	@Mock
	private FilePath sourceFile;

	// use a small buffer to enforce multiple source file reads
	private char[] buffer = new char[7];
	private LayoutReader layoutReader;

	@Before
	public void beforeTest() {
		when(sourceFile.getProject()).thenReturn(project);
		when(project.getCustomElementsRegistry()).thenReturn(customElements);
		when(customElements.getByTagName("tab-view")).thenReturn(customElement);
		when(customElement.compoPath()).thenReturn("compo/tab-view");
		when(customElement.operator()).thenReturn("template");
	}

	@Test
	public void GivenMissingXmlnsOperators_WhenRead_ThenAddIt() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.XMLNS);

		Reader reader = new StringReader("<body xmlns:w=\"js-lib.com/wood\">\n\t<tab-view>\n\t</tab-view>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString("<tab-view w:template=\"compo/tab-view\">"));
	}

	@Test
	public void GivenMissingXmlnsOperatorsOnEmptyElement_WhenRead_ThenAddIt() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.XMLNS);

		Reader reader = new StringReader("<body xmlns:w=\"js-lib.com/wood\">\n\t<tab-view/>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString("<tab-view w:template=\"compo/tab-view\"/>"));
	}

	/**
	 * Operators naming strategy is XMLNS. Given empty element with space after tag name, inserted operator will have two
	 * leading spaces; this is a benign limitation not worth fixing.
	 */
	@Test
	public void GivenMissingXmlnsOperatorsOnEmptyElementAndNameTralingSpace_WhenRead_ThenAddIt() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.XMLNS);

		Reader reader = new StringReader("<body xmlns:w=\"js-lib.com/wood\">\n\t<tab-view />\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString("<tab-view  w:template=\"compo/tab-view\"/>"));
	}

	@Test
	public void GivenExistingXmlnsOperators_WhenRead_ThenDoNotAddIt() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.XMLNS);

		Reader reader = new StringReader("<body xmlns:w=\"js-lib.com/wood\">\n\t<tab-view w:template=\"compo/tab-view\">\n\t</tab-view>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString("<tab-view w:template=\"compo/tab-view\">"));
	}

	@Test
	public void GivenMisspelledXmlnsOperators_WhenRead_ThenAddTheCorrectOne() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.XMLNS);

		Reader reader = new StringReader("<body xmlns:w=\"js-lib.com/wood\">\n\t<tab-view w:templatex=\"compo/tab-view\">\n\t</tab-view>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString(" w:template=\"compo/tab-view\""));
	}

	@Test
	public void GivenWrongXmlnsOperators_WhenRead_ThenAddTheWrigthOne() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.XMLNS);

		Reader reader = new StringReader("<body xmlns:w=\"js-lib.com/wood\">\n\t<tab-view w:compo=\"compo/tab-view\">\n\t</tab-view>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString(" w:template=\"compo/tab-view\""));
	}

	@Test
	public void GivenMissingAttrOperators_WhenRead_ThenAddIt() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.ATTR);

		Reader reader = new StringReader("<body>\n\t<tab-view>\n\t</tab-view>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString(" template=\"compo/tab-view\""));
	}

	@Test
	public void GivenExistingAttrOperators_WhenRead_ThenDoNotAddIt() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.ATTR);

		Reader reader = new StringReader("<body>\n\t<tab-view template=\"compo/tab-view\">\n\t</tab-view>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString("<tab-view template=\"compo/tab-view\">"));
	}

	@Test
	public void GivenMisspelledAttrOperators_WhenRead_ThenAddTheCorrectOne() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.ATTR);

		Reader reader = new StringReader("<body>\n\t<tab-view templatex=\"compo/tab-view\">\n\t</tab-view>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString(" template=\"compo/tab-view\""));
	}

	@Test
	public void GivenWrongAttrOperators_WhenRead_ThenAddTheWrigthOne() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.ATTR);

		Reader reader = new StringReader("<body>\n\t<tab-view compo=\"compo/tab-view\">\n\t</tab-view>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString(" template=\"compo/tab-view\""));
	}

	@Test
	public void GivenMissingDataAttrOperators_WhenRead_ThenAddIt() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.DATA_ATTR);

		Reader reader = new StringReader("<body>\n\t<tab-view>\n\t</tab-view>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString(" data-template=\"compo/tab-view\""));
	}

	@Test
	public void GivenExistingDataAttrOperators_WhenRead_ThenDoNotAddIt() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.DATA_ATTR);

		Reader reader = new StringReader("<body>\n\t<tab-view data-template=\"compo/tab-view\">\n\t</tab-view>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString("<tab-view data-template=\"compo/tab-view\">"));
	}

	@Test
	public void GivenMisspelledDataAttrOperators_WhenRead_ThenAddTheCorrectOne() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.DATA_ATTR);

		Reader reader = new StringReader("<body>\n\t<tab-view data-templatex=\"compo/tab-view\">\n\t</tab-view>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString(" data-template=\"compo/tab-view\""));
	}

	@Test
	public void GivenWrongDataAttrOperators_WhenRead_ThenAddTheWrigthOne() throws IOException {
		// given
		when(project.getOperatorsNaming()).thenReturn(OperatorsNaming.DATA_ATTR);

		Reader reader = new StringReader("<body>\n\t<tab-view data-compo=\"compo/tab-view\">\n\t</tab-view>\n</body>\n");
		layoutReader = new LayoutReader(reader, sourceFile, buffer);
		Writer writer = new StringWriter();

		// when
		Files.copy(layoutReader, writer);

		// then
		assertThat(writer.toString(), containsString(" data-template=\"compo/tab-view\""));
	}
}
