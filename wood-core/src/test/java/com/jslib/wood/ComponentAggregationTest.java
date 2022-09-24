package com.jslib.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.io.StringReader;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.api.dom.EList;
import com.jslib.api.dom.Element;
import com.jslib.wood.impl.FileType;
import com.jslib.wood.impl.IOperatorsHandler;
import com.jslib.wood.impl.Operator;
import com.jslib.wood.impl.XmlnsOperatorsHandler;

@RunWith(MockitoJUnitRunner.class)
public class ComponentAggregationTest {
	@Mock
	private Project project;

	@Mock
	private CompoPath compoPath; // component path
	@Mock
	private FilePath compoLayout; // layout file for component
	@Mock
	private FilePath compoDescriptor; // descriptor file for component
	@Mock
	private FilePath compoStyle; // style file for component
	@Mock
	private FilePath compoScript; // script file for component

	@Mock
	private CompoPath childPath; // child component path
	@Mock
	private FilePath childLayout; // layout file for child component
	@Mock
	private FilePath childDescriptor; // descriptor file for child component
	@Mock
	private FilePath childStyle; // style file for child component
	@Mock
	private FilePath childScript; // script file for child component

	@Mock
	private IReferenceHandler referenceHandler;

	private IOperatorsHandler operatorsHandler;

	@Before
	public void beforeTest() {
		operatorsHandler = new XmlnsOperatorsHandler();

		when(project.getTitle()).thenReturn("Components");
		when(project.hasNamespace()).thenReturn(true);
		when(project.getOperatorsHandler()).thenReturn(operatorsHandler);
		
		when(compoPath.getLayoutPath()).thenReturn(compoLayout);
		when(compoLayout.getProject()).thenReturn(project);
		when(compoLayout.exists()).thenReturn(true);
		when(compoLayout.isLayout()).thenReturn(true);
		when(compoLayout.cloneTo(FileType.XML)).thenReturn(compoDescriptor);
		when(compoLayout.cloneTo(FileType.STYLE)).thenReturn(compoStyle);
		when(compoDescriptor.cloneTo(FileType.SCRIPT)).thenReturn(compoScript);

		when(childPath.getLayoutPath()).thenReturn(childLayout);
		when(childLayout.exists()).thenReturn(true);
		when(childLayout.isLayout()).thenReturn(true);
		when(childLayout.cloneTo(FileType.XML)).thenReturn(childDescriptor);
		when(childLayout.cloneTo(FileType.STYLE)).thenReturn(childStyle);
		when(childDescriptor.cloneTo(FileType.SCRIPT)).thenReturn(childScript);
	}

	/** Component without template but with child component. */
	@Test
	public void GivenSimpleAggregation_ThenIncludeChildLayout() {
		// given
		String childHTML = "" + //
				"<div>" + //
				"	<h1>Child</h1>" + //
				"</div>";
		when(childLayout.getReader()).thenReturn(new StringReader(childHTML));

		String compoHTML = "" + //
				"<section xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Component</h1>" + //
				"	<div w:compo='res/child'></div>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(project.createCompoPath("res/child")).thenReturn(childPath);

		// when
		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();

		// then
		EList headings = layout.findByTag("h1");
		assertThat(headings.size(), equalTo(2));
		assertThat(headings.item(0).getText(), equalTo("Component"));
		assertThat(headings.item(1).getText(), equalTo("Child"));
	}

	@Test
	public void GivenSimpleAggregationAndAttributeCollision_ThenParentTakesPrecedence() {
		// given
		String childHTML = "<div id='child'></div>";
		when(childLayout.getReader()).thenReturn(new StringReader(childHTML));

		String compoHTML = "" + //
				"<section xmlns:w='js-lib.com/wood'>" + //
				"	<div id='parent' w:compo='res/child'></div>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(project.createCompoPath("res/child")).thenReturn(childPath);

		// when
		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();

		// then
		assertThat(layout.getByTag("div").getAttr("id"), equalTo("parent"));
	}

	@Test
	public void GivenSimpleAggregationAndChildTagNameWithDash_ThenIncludeChildLayout() {
		// given
		String childHTML = "" + //
				"<x-div>" + //
				"	<h1>Child</h1>" + //
				"</x-div>";
		when(childLayout.getReader()).thenReturn(new StringReader(childHTML));

		String compoHTML = "" + //
				"<section xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Component</h1>" + //
				"	<x-div w:compo='res/child'></x-div>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(project.createCompoPath("res/child")).thenReturn(childPath);

		// when
		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();

		// then
		EList headings = layout.findByTag("h1");
		assertThat(headings.size(), equalTo(2));
		assertThat(headings.item(0).getText(), equalTo("Component"));
		assertThat(headings.item(1).getText(), equalTo("Child"));
	}

	/** {@link Operator#COMPO} and {@link Operator#PARAM} should be erased from layout. */
	@Test
	public void GivenSimpleAggregation_ThenEraseOperators() throws XPathExpressionException {
		// given
		String childHTML = "" + //
				"<div>" + //
				"	<h1>@param/title</h1>" + //
				"</div>";
		when(childLayout.getReader()).thenReturn(new StringReader(childHTML));

		String compoHTML = "" + //
				"<section xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Component</h1>" + //
				"	<div w:compo='res/child' w:param='title:Child'></div>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(project.createCompoPath("res/child")).thenReturn(childPath);

		// when
		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();

		// then
		assertThat(layout.getByXPathNS(WOOD.NS, "//*[@w:compo]"), nullValue());
		assertThat(layout.getByXPathNS(WOOD.NS, "//*[@w:param]"), nullValue());
	}

	@Test
	public void GivenAggregationOnStandaloneTemplate_ThenIncludeResolvedTemplate() {
		// given
		FilePath templateLayout = Mockito.mock(FilePath.class);
		when(templateLayout.exists()).thenReturn(true);
		when(templateLayout.isLayout()).thenReturn(true);

		FilePath templateDescriptor = Mockito.mock(FilePath.class);
		when(templateLayout.cloneTo(FileType.XML)).thenReturn(templateDescriptor);
		when(templateLayout.cloneTo(FileType.STYLE)).thenReturn(Mockito.mock(FilePath.class));
		when(templateDescriptor.cloneTo(FileType.SCRIPT)).thenReturn(Mockito.mock(FilePath.class));

		CompoPath templatePath = Mockito.mock(CompoPath.class);
		when(templatePath.getLayoutPath()).thenReturn(templateLayout);

		String templateHTML = "" + //
				"<article xmlns:w='js-lib.com/wood'>" + //
				"	<h2>Article</h2>" + //
				"	<section w:editable='section'></section>" + //
				"</article>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

		String childHTML = "" + //
				"<div w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
				"	<section w:content='section'>" + //
				"		<h3>Section</h3>" + //
				"		<p>Paragraph.</p>" + //
				"	</section>" + //
				"</div>";
		when(childLayout.getReader()).thenReturn(new StringReader(childHTML));
		when(project.createCompoPath("res/template")).thenReturn(templatePath);

		String compoHTML = "" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Page</h1>" + //
				"	<article w:compo='res/child'></article>" + //
				"</body>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(project.createCompoPath("res/child")).thenReturn(childPath);

		// when
		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();

		// then
		assertThat(layout.getTag(), equalTo("body"));

		assertThat(layout.getByTag("article"), notNullValue());
		assertThat(layout.getByTag("section"), notNullValue());
		assertThat(layout.getByTag("h1"), notNullValue());
		assertThat(layout.getByTag("h2"), notNullValue());
		assertThat(layout.getByTag("h3"), notNullValue());
		assertThat(layout.getByTag("p"), notNullValue());

		assertThat(layout.getByTag("h1").getText(), equalTo("Page"));
		assertThat(layout.getByTag("h2").getText(), equalTo("Article"));
		assertThat(layout.getByTag("h3").getText(), equalTo("Section"));
		assertThat(layout.getByTag("p").getText(), equalTo("Paragraph."));
	}

	@Test
	public void GivenAggregationOnStandaloneTemplate_ThenMergeAttributes() {
		// given
		FilePath templateLayout = Mockito.mock(FilePath.class);
		when(templateLayout.exists()).thenReturn(true);
		when(templateLayout.isLayout()).thenReturn(true);

		FilePath templateDescriptor = Mockito.mock(FilePath.class);
		when(templateLayout.cloneTo(FileType.XML)).thenReturn(templateDescriptor);
		when(templateLayout.cloneTo(FileType.STYLE)).thenReturn(Mockito.mock(FilePath.class));
		when(templateDescriptor.cloneTo(FileType.SCRIPT)).thenReturn(Mockito.mock(FilePath.class));

		CompoPath templatePath = Mockito.mock(CompoPath.class);
		when(templatePath.getLayoutPath()).thenReturn(templateLayout);

		String templateHTML = "" + //
				"<article id='template' name='template' class='template' xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section'></section>" + //
				"</article>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

		String childHTML = "" + //
				"<div data-cfg='multi-select:true' name='child' class='container' w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
				"	<section id='child' class='child' w:content='section'></section>" + //
				"</div>";
		when(childLayout.getReader()).thenReturn(new StringReader(childHTML));
		when(project.createCompoPath("res/template")).thenReturn(templatePath);

		String compoHTML = "" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<article id='component' class='component' title='component' w:compo='res/child'></article>" + //
				"</body>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(project.createCompoPath("res/child")).thenReturn(childPath);

		// when
		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();

		// then
		Element article = layout.getByTag("article");
		assertThat(article.getAttr("id"), equalTo("component"));
		assertThat(article.getAttr("name"), equalTo("child"));
		assertThat(article.getAttr("title"), equalTo("component"));
		assertThat(article.getAttr("data-cfg"), equalTo("multi-select:true"));
		assertThat(article.hasCssClass("component"), equalTo(true));
		assertThat(article.hasCssClass("container"), equalTo(true));
		assertThat(article.hasCssClass("template"), equalTo(true));

		// do not alter parent body attributes
		assertThat(layout.getAttr("id"), nullValue());
		assertThat(layout.getAttr("name"), nullValue());
		assertThat(layout.getAttr("title"), nullValue());
		assertThat(layout.getAttr("data-cfg"), nullValue());
		assertThat(layout.hasCssClass("component"), equalTo(false));
		assertThat(layout.hasCssClass("container"), equalTo(false));
		assertThat(layout.hasCssClass("template"), equalTo(false));
	}
}
