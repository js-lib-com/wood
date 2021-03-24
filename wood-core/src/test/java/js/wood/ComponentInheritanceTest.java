package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.dom.EList;
import js.dom.Element;
import js.wood.impl.FileType;
import js.wood.impl.IOperatorsHandler;
import js.wood.impl.Operator;
import js.wood.impl.XmlnsOperatorsHandler;

/**
 * Components inheritance via templates. Fixture contains mocks for a component and a hierarchy of two templates:
 * <code>page</code> and </code>template</code>.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class ComponentInheritanceTest {
	@Mock
	private Project project;
	@Mock
	private Factory factory;

	@Mock
	private FilePath pageLayout; // layout file for page template
	@Mock
	private FilePath pageDescriptor;
	@Mock
	private FilePath pageStyle; // style file for page template
	@Mock
	private CompoPath pageCompo; // path to page editable element, declared on template

	@Mock
	private FilePath templateLayout; // layout file for template component
	@Mock
	private FilePath templateDescriptor;
	@Mock
	private FilePath templateStyle; // style file for template component
	@Mock
	private CompoPath templateCompo; // path to template editable element, declared on component
	@Mock
	private CompoPath templateCompo2; // path to second template editable element, declared on component

	@Mock
	private CompoPath compoPath;
	@Mock
	private FilePath compoLayout; // layout file for component
	@Mock
	private FilePath compoDescriptor;
	@Mock
	private FilePath compoStyle; // style file for component

	@Mock
	private IReferenceHandler referenceHandler;

	private IOperatorsHandler operatorsHandler;

	@Before
	public void beforeTest() {
		operatorsHandler = new XmlnsOperatorsHandler();

		when(project.getFactory()).thenReturn(factory);
		when(project.getDisplay()).thenReturn("Components");
		when(project.hasNamespace()).thenReturn(true);
		when(project.getOperatorsHandler()).thenReturn(operatorsHandler);

		when(pageLayout.exists()).thenReturn(true);
		when(pageLayout.isLayout()).thenReturn(true);
		when(pageLayout.cloneTo(FileType.XML)).thenReturn(pageDescriptor);
		when(pageLayout.cloneTo(FileType.STYLE)).thenReturn(pageStyle);
		when(pageCompo.getLayoutPath()).thenReturn(pageLayout);

		when(templateLayout.exists()).thenReturn(true);
		when(templateLayout.isLayout()).thenReturn(true);
		when(templateLayout.cloneTo(FileType.XML)).thenReturn(templateDescriptor);
		when(templateLayout.cloneTo(FileType.STYLE)).thenReturn(templateStyle);
		when(templateCompo.getLayoutPath()).thenReturn(templateLayout);

		when(compoPath.getLayoutPathEx()).thenReturn(compoLayout);

		when(compoLayout.getProject()).thenReturn(project);
		when(compoLayout.exists()).thenReturn(true);
		when(compoLayout.isLayout()).thenReturn(true);
		when(compoLayout.cloneTo(FileType.XML)).thenReturn(compoDescriptor);
		when(compoLayout.cloneTo(FileType.STYLE)).thenReturn(compoStyle);
	}

	/** Simple inheritance: component inherits 'section' editable element from template. */
	@Test
	public void simple() {
		String templateHTML = "" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Template</h1>" + //
				"	<section w:editable='section'></section>" + //
				"</body>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

		String compoHTML = "" + //
				"<section w:template='res/template#section' xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Content</h1>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();
		assertThat(layout.getTag(), equalTo("body"));

		EList headings = layout.findByTag("h1");
		assertThat(headings.size(), equalTo(2));
		assertThat(headings.item(0).getText(), equalTo("Template"));
		assertThat(headings.item(1).getText(), equalTo("Content"));

		Element section = layout.getByTag("section");
		assertThat(section, notNullValue());
		assertFalse(section.hasAttrNS(WOOD.NS, "editable"));
		assertFalse(section.hasAttrNS(WOOD.NS, "template"));
		assertTrue(section.hasAttr("xmlns:w"));
	}

	@Test
	public void parameter() {
		String templateHTML = "" + //
				"<body title='@param/caption' xmlns:w='js-lib.com/wood'>" + //
				"	<h1>@param/title</h1>" + //
				"	<section w:editable='section'></section>" + //
				"</body>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

		String compoHTML = "" + //
				"<section w:template='res/template#section' w:param='caption:Compo Caption;title:Compo Title' xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Content</h1>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();

		assertThat(layout.getAttr("title"), equalTo("Compo Caption"));
		assertThat(layout.getByTag("h1").getText(), equalTo("Compo Title"));
	}

	@Test(expected = WoodException.class)
	public void parameter_Missing() {
		String templateHTML = "" + //
				"<body title='@param/caption' xmlns:w='js-lib.com/wood'>" + //
				"	<section w:editable='section'></section>" + //
				"</body>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

		String compoHTML = "" + //
				"<section w:template='res/template#section' w:param='title:Compo Title' xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Content</h1>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		new Component(compoPath, referenceHandler);
	}

	/** Component with template and multiple implementations for the same editable element. */
	@Test
	public void multipleRealizations() {
		String templateHTML = "" + //
				"<form xmlns:w='js-lib.com/wood'>" + //
				"	<fieldset w:editable='fieldset'></fieldset>" + //
				"	<button>Submit</button>" + //
				"</form>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

		String compoHTML = "" + //
				"<embed type='text/html' w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
				"	<fieldset w:content='fieldset'>" + //
				"		<input name='user-name' />" + //
				"	</fieldset>" + //
				"" + //
				"	<fieldset w:content='fieldset'>" + //
				"		<input name='address' />" + //
				"	</fieldset>" + //
				"" + //
				"	<fieldset w:content='fieldset'>" + //
				"		<input name='password' />" + //
				"	</fieldset>" + //
				"</embed>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();
		assertThat(layout.getTag(), equalTo("form"));

		EList inputs = layout.findByTag("input");
		assertThat(inputs.size(), equalTo(3));
		assertThat(inputs.item(0).getAttr("name"), equalTo("user-name"));
		assertThat(inputs.item(1).getAttr("name"), equalTo("address"));
		assertThat(inputs.item(2).getAttr("name"), equalTo("password"));
	}

	/** Template with two editable sections. Component inherits from template and implements both editables. */
	@Test
	public void multipleEditables() {
		String templateHTML = "" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Template</h1>" + //
				"	<section w:editable='section-1'></section>" + //
				"	<section w:editable='section-2'></section>" + //
				"</body>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

		String compoHTML = "" + //
				"<embed type='text/html' w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
				"	<section w:content='section-1'>" + //
				"		<h1>Content One</h1>" + //
				"	</section>" + //
				"" + //
				"	<section w:content='section-2'>" + //
				"		<h1>Content Two</h1>" + //
				"	</section>" + //
				"</embed>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();
		assertThat(layout.getTag(), equalTo("body"));

		EList headings = layout.findByTag("h1");
		assertThat(headings.size(), equalTo(3));
		assertThat(headings.item(0).getText(), equalTo("Template"));
		assertThat(headings.item(1).getText(), equalTo("Content One"));
		assertThat(headings.item(2).getText(), equalTo("Content Two"));

		EList sections = layout.findByTag("section");
		assertThat(sections.size(), equalTo(2));
		assertFalse(sections.item(0).hasAttrNS(WOOD.NS, "editable"));
		assertFalse(sections.item(1).hasAttrNS(WOOD.NS, "editable"));
	}

	/** Component inherits 'paragraph' from template that inherits 'section' from page. */
	@Test
	public void hierarchy() {
		String pageHTML = "" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Page</h1>" + //
				"	<section w:editable='section'></section>" + //
				"</body>";
		when(pageLayout.getReader()).thenReturn(new StringReader(pageHTML));

		String templateHTML = "" + //
				"<section w:template='res/page#section' xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Template</h1>" + //
				"	<div w:editable='paragraph'></div>" + //
				"</section>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

		String compoHTML = "" + //
				"<div w:template='res/template#paragraph' xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Component</h1>" + //
				"</div>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(factory.createCompoPath("res/page")).thenReturn(pageCompo);
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();
		assertThat(layout.getTag(), equalTo("body"));

		EList headings = layout.findByTag("h1");
		assertThat(headings.size(), equalTo(3));
		assertThat(headings.item(0).getText(), equalTo("Page"));
		assertThat(headings.item(1).getText(), equalTo("Template"));
		assertThat(headings.item(2).getText(), equalTo("Component"));
	}

	/**
	 * Test attributes merging for editable element on simple inheritance: template has an editable section and component
	 * implements content for that editable. Both template and component have attributes. Component attributes takes precedence
	 * over template attributes.
	 */
	@Test
	public void attributesMerging() {
		String templateHTML = "" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Template</h1>" + //
				"	<section id='template-id' name='template' class='template-class' w:editable='section'></section>" + //
				"</body>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

		String compoHTML = "" + //
				"<section name='component' class='component-class' disabled='true' w:template='res/template#section' xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Content</h1>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		Component compo = new Component(compoPath, referenceHandler);
		Element section = compo.getLayout().getByTag("section");

		assertThat(section.getAttr("id"), equalTo("template-id"));
		assertThat(section.getAttr("name"), equalTo("component"));
		assertThat(section.getAttr("class"), equalTo("component-class template-class"));
		assertThat(section.getAttr("disabled"), equalTo("true"));
	}

	/**
	 * Fixture similar to {@link #attributesMerging()} but with attributes with third party namespace. Namespace should be
	 * preserved but editable attribute with wood namepsace should not be copied.
	 */
	@Test
	public void attributesMerging_ThirdPartyNamespace() {
		String templateHTML = "" + //
				"<body xmlns:w='js-lib.com/wood' xmlns:t='js-lib.com/test'>" + //
				"	<h1>Template</h1>" + //
				"	<section t:name='template' w:editable='section'></section>" + //
				"</body>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

		String compoHTML = "" + //
				"<section name='component' w:template='res/template#section' xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Content</h1>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		Component compo = new Component(compoPath, referenceHandler);
		Element section = compo.getLayout().getByTag("section");

		assertFalse(section.hasAttrNS(WOOD.NS, "editable"));
		assertThat(section.getAttrNS("js-lib.com/test", "name"), equalTo("template"));
		assertThat(section.getAttr("name"), equalTo("component"));
	}

	@Test
	public void styles() {
		when(templateStyle.exists()).thenReturn(true);
		when(templateStyle.value()).thenReturn("template.css");
		when(compoStyle.exists()).thenReturn(true);
		when(compoStyle.value()).thenReturn("compo.css");

		String templateLayoutHTML = "" + //
				"<div>" + //
				"	<h1 w:editable='h1' xmlns:w='js-lib.com/wood'></h1>" + //
				"</div>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateLayoutHTML));

		String compoLayoutHTML = "<h1 w:template='res/template#h1' xmlns:w='js-lib.com/wood'>Content</h1>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoLayoutHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		Component compo = new Component(compoPath, referenceHandler);

		List<FilePath> styles = compo.getStyleFiles();
		assertThat(styles, hasSize(2));
		assertThat(styles.get(0).value(), equalTo("template.css"));
		assertThat(styles.get(1).value(), equalTo("compo.css"));
	}

	@Test
	public void scriptDescriptors() {
		String templateDescriptorXML = "" + //
				"<compo>" + //
				"	<script src='libs/js-lib.js'></script>" + //
				"	<script src='scripts/Template.js'></script>" + //
				"</compo>";
		when(templateDescriptor.exists()).thenReturn(true);
		when(templateDescriptor.getReader()).thenReturn(new StringReader(templateDescriptorXML));

		String templateLayoutHTML = "" + //
				"<div>" + //
				"	<h1 w:editable='h1' xmlns:w='js-lib.com/wood'></h1>" + //
				"</div>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateLayoutHTML));

		String compoDescriptorXML = "" + //
				"<compo>" + //
				"	<script src='libs/js-lib.js'></script>" + //
				"	<script src='scripts/Compo.js'></script>" + //
				"</compo>";
		when(compoDescriptor.exists()).thenReturn(true);
		when(compoDescriptor.getReader()).thenReturn(new StringReader(compoDescriptorXML));

		String compoLayoutHTML = "<h1 w:template='res/template#h1' xmlns:w='js-lib.com/wood'>Content</h1>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoLayoutHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		Component compo = new Component(compoPath, referenceHandler);
		List<IScriptDescriptor> scripts = compo.getScriptDescriptors();
		assertThat(scripts, hasSize(3));
		assertThat(scripts.get(0).getSource(), equalTo("libs/js-lib.js"));
		assertThat(scripts.get(1).getSource(), equalTo("scripts/Template.js"));
		assertThat(scripts.get(2).getSource(), equalTo("scripts/Compo.js"));
	}

	/** {@link Operator#TEMPLATE} and {@link Operator#EDITABLE} should be erased from layout. */
	@Test
	public void operatorsErasure() {
		String templateHTML = "" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Template</h1>" + //
				"	<section w:editable='section'></section>" + //
				"</body>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

		String compoHTML = "" + //
				"<section w:template='res/template#section' xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Content</h1>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		Component compo = new Component(compoPath, referenceHandler);
		Element layout = compo.getLayout();
		assertThat(layout.getByXPathNS(WOOD.NS, "//*[@w:template]"), nullValue());
		assertThat(layout.getByXPathNS(WOOD.NS, "//*[@w:editable]"), nullValue());
	}

	@Test
	public void clean() {
		String templateHTML = "" + //
				"<body xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Template</h1>" + //
				"	<section w:editable='section'></section>" + //
				"	<div w:editable='empty'></div>" + //
				"</body>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

		String compoHTML = "" + //
				"<section w:template='res/template#section' xmlns:w='js-lib.com/wood'>" + //
				"	<h1>Content</h1>" + //
				"</section>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		Component compo = new Component(compoPath, referenceHandler);
		compo.clean();

		Element layout = compo.getLayout();
		assertFalse(layout.hasAttr("xmlns:w"));
		assertThat(layout.getByTag("section"), notNullValue());
		assertFalse(layout.getByTag("section").hasAttr("xmlns:w"));
		assertThat(layout.getByTag("div"), nullValue());
	}

	/** Template editable element is named 'h1' but component fragment references it as 'fake'. */
	@Test(expected = WoodException.class)
	public void missingEditable() {
		String templateLayoutHTML = "<h1 w:editable='h1' xmlns:w='js-lib.com/wood'></h1>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateLayoutHTML));

		String compoLayoutHTML = "<h1 w:template='res/template#fake' xmlns:w='js-lib.com/wood'></h1>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoLayoutHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		new Component(compoPath, referenceHandler);
	}

	/** Template editable element has children. */
	@Test(expected = WoodException.class)
	public void editableWithChildren() {
		String templateLayoutHTML = "<h1 w:editable='h1' xmlns:w='js-lib.com/wood'><b>Title</b></h1>";
		when(templateLayout.getReader()).thenReturn(new StringReader(templateLayoutHTML));

		String compoLayoutHTML = "<h1 w:template='res/template#h1' xmlns:w='js-lib.com/wood'></h1>";
		when(compoLayout.getReader()).thenReturn(new StringReader(compoLayoutHTML));
		when(factory.createCompoPath("res/template")).thenReturn(templateCompo);

		new Component(compoPath, referenceHandler);
	}
}
