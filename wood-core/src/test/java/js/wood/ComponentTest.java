package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import js.dom.EList;
import js.dom.Element;
import js.dom.NamespaceContext;
import js.util.Classes;
import js.wood.CompoPath;
import js.wood.Component;
import js.wood.FilePath;
import js.wood.IReferenceHandler;
import js.wood.Project;
import js.wood.Reference;
import js.wood.Variables;
import js.wood.WOOD;
import js.wood.WoodException;

public class ComponentTest {
	@Test
	public void simpleLayout() {
		Component compo = getCompo("res/simple/layout");

		assertThat(field(compo, "baseLayoutPath").toString(), equalTo("res/simple/layout/layout.htm"));
		assertThat(compo.getName(), equalTo("layout"));
		assertThat(compo.getDisplay(), equalTo("Components / Layout"));
		assertThat(compo.getDescription(), equalTo("Components / Layout"));
		assertThat(compo.getLayoutFileName(), equalTo("layout.htm"));

		Element layout = compo.getLayout();
		assertThat(layout, notNullValue());
		assertThat(layout.getTag(), equalTo("body"));
		assertThat(layout.getByTag("h1").getText(), equalTo("Simple Layout"));

		assertTrue(compo.getStyleFiles().isEmpty());
	}

	@Test
	public void simpleTemplate() {
		Component compo = getCompo("res/simple/template/compo");
		Element layout = compo.getLayout();

		Element editable = layout.getByTag("section");
		assertThat(layout.getTag(), equalTo("body"));

		EList headings = layout.findByTag("h1");
		assertThat(headings.size(), equalTo(2));
		assertThat(headings.item(0).getText(), equalTo("Template"));
		assertThat(headings.item(1).getText(), equalTo("Content"));

		editable = layout.getByTag("section");
		assertThat(editable, notNullValue());
		assertFalse(editable.hasAttrNS(WOOD.NS, "editable"));
	}

	@Test
	public void variableTemplate() {
		Component compo = getCompo("res/variable-template/compo");
		Element layout = compo.getLayout();

		EList inputs = layout.findByTag("input");
		assertThat(inputs.size(), equalTo(3));
		assertThat(inputs.item(0).getAttr("name"), equalTo("user-name"));
		assertThat(inputs.item(1).getAttr("name"), equalTo("address"));
		assertThat(inputs.item(2).getAttr("name"), equalTo("password"));
	}

	@Test
	public void templatesHierarchy() {
		Component compo = getCompo("res/templates-hierarchy/compo");
		Element layout = compo.getLayout();

		assertThat(layout.getTag(), equalTo("body"));

		EList headings = layout.findByTag("h1");
		assertThat(headings.size(), equalTo(3));
		assertThat(headings.item(0).getText(), equalTo("Grand Parent"));
		assertThat(headings.item(1).getText(), equalTo("Parent"));
		assertThat(headings.item(2).getText(), equalTo("Child"));
	}

	@Test
	public void simpleWidget() {
		Component compo = getCompo("res/simple/widget/compo");
		Element layout = compo.getLayout();

		EList headings = layout.findByTag("h1");
		assertThat(headings.size(), equalTo(2));
		assertThat(headings.item(0).getText(), equalTo("Component"));
		assertThat(headings.item(1).getText(), equalTo("Widget"));
	}

	@Test
	public void widgetsTree() {
		Component compo = getCompo("res/widgets-tree/compo");
		Element layout = compo.getLayout();

		EList headings = layout.findByTag("h1");
		assertThat(headings.size(), equalTo(5));
		assertThat(headings.item(0).getText(), equalTo("Component"));
		assertThat(headings.item(1).getText(), equalTo("Child One"));
		assertThat(headings.item(2).getText(), equalTo("Nephew One"));
		assertThat(headings.item(3).getText(), equalTo("Child Two"));
		assertThat(headings.item(4).getText(), equalTo("Nephew Two"));
	}

	@Test
	public void attributes() {
		Component compo = getCompo("res/attributes/compo");
		Element layout = compo.getLayout();

		Element section = layout.getByTag("section");
		assertThat(section.getAttr("class"), equalTo("template compo"));
		assertThat(section.getAttr("data-format"), equalTo("js.wood.Title"));
		assertThat(section.getAttr("id"), equalTo("section-id"));

		Element div = layout.getByTag("div");
		assertThat(div.getAttr("class"), equalTo("widget compo"));
		assertThat(div.getAttr("data-class"), equalTo("js.wood.Widget"));
		assertThat(div.getAttr("id"), equalTo("compo-id"));

		// template and widget does not overwrite compo attributes; names are not changed
		assertThat(section.getAttr("name"), equalTo("section"));
		assertThat(div.getAttr("name"), equalTo("div"));
	}

	@Test
	public void attributesWithEntity() {
		Component compo = getCompo("res/attributes-entity/compo");
		Element layout = compo.getLayout();
		Element div = layout.getByTag("div");
		assertThat(div.getAttr("name"), equalTo("P&G"));
	}

	@Test
	public void widgetParameter() {
		Component compo = getCompo("res/parameter/compo");
		Element layout = compo.getLayout();
		Element div = layout.getByTag("h1");
		assertThat(div.getText(), equalTo("Widget Title"));
	}

	@Test
	public void widgetParameterWithEntity() {
		Component compo = getCompo("res/parameter-entity/compo");
		Element layout = compo.getLayout();
		Element div = layout.getByTag("h1");
		assertThat(div.getAttr("data-name"), equalTo("P&G"));
		assertThat(div.getText(), equalTo("P&G"));
	}

	@Test
	public void stylesInclusion() {
		Component compo = getCompo("res/styles/compo");
		List<FilePath> styles = compo.getStyleFiles();

		assertThat(styles.size(), equalTo(3));
		assertThat(styles.get(0).toString(), equalTo("res/styles/widget/widget.css"));
		assertThat(styles.get(1).toString(), equalTo("res/styles/template/template.css"));
		assertThat(styles.get(2).toString(), equalTo("res/styles/compo/compo.css"));
	}

	/**
	 * Test if template, widgets and script references are removed from component aggregated layout.
	 */
	@Test
	public void objectReferencesErasure() {
		for (String compoPath : new String[] { "res/simple/widget/compo", "res/simple/template/compo", "res/widgets-tree/compo", "res/templates-hierarchy/compo", "res/scripts/page-script" }) {
			Component compo = getCompo(compoPath);
			Element layout = compo.getLayout();
			layout.getDocument().dump();
			assertTrue(layout.findByAttr("w:template").isEmpty());
			assertTrue(layout.findByXPathNS(new NamespaceContext() {
				@Override
				public String getNamespaceURI(String prefix) {
					return "js-lib.com/wood";
				}
			}, "/*[@w:editable]").isEmpty());
			assertTrue(layout.findByXPath("/*[@w:editable]").isEmpty());
			assertTrue(layout.findByXPath("/*[@editable]").isEmpty());
			assertTrue(layout.findByAttr("w:param").isEmpty());
			assertTrue(layout.findByAttr("w:widget").isEmpty());
			assertTrue(layout.findByAttr("w:script").isEmpty());
		}
	}

	@Test
	public void referenceHandler() {
		Project project = new Project(new File("src/test/resources/components"));
		CompoPath path = new CompoPath(project, "res/references");
		final List<Reference> references = new ArrayList<>();

		Component compo = new Component(path, new IReferenceHandler() {
			@Override
			public String onResourceReference(Reference reference, FilePath sourcePath) {
				references.add(reference);
				return reference.toString();
			}
		});
		compo.scan();

		assertThat(references.size(), equalTo(3));
		assertThat(references.get(0).toString(), equalTo("@string/title"));
		assertThat(references.get(1).toString(), equalTo("@image/logo"));
		assertThat(references.get(2).toString(), equalTo("@text/message"));
	}

	@Test
	public void inline() {
		Component compo = getCompo("res/inline");

		Element layout = compo.getLayout();
		assertNotNull(layout);
	}

	@Test
	public void fullComponent() {
		Component compo = getCompo("res/full/compo");
		Element layout = Classes.getFieldValue(compo, "layout");

		assertThat(layout, notNullValue());
		assertThat(layout.getTag(), equalTo("body"));
		EList children = layout.getChildren();
		assertThat(children.size(), equalTo(4));
		assertThat(children.item(0).getTag(), equalTo("header"));
		assertThat(children.item(1).getTag(), equalTo("nav"));
		assertThat(children.item(2).getTag(), equalTo("section"));
		assertThat(children.item(3).getTag(), equalTo("footer"));

		Element mainSection = children.item(2);
		String mainSectionCssClass = mainSection.getAttr("class");
		assertTrue(mainSectionCssClass.contains("main"));
		assertTrue(mainSectionCssClass.contains("rounded-box"));
		assertThat(mainSection.getAttr("data-editable"), nullValue());

		children = mainSection.getChildren();
		assertThat(children.size(), equalTo(3));
		assertThat(children.item(0).getTag(), equalTo("section"));
		assertThat(children.item(1).getTag(), equalTo("h1"));
		assertThat(children.item(2).getTag(), equalTo("section"));

		Element sideBar = children.item(0);
		assertThat(sideBar.getAttr("class"), equalTo("side-bar"));

		Element heading = children.item(1);
		assertThat(heading.getAttr("class"), equalTo("child-caption"));
		assertThat(heading.getAttr("data-class"), equalTo("js.wood.Title"));
		assertThat(heading.getAttr("data-editable"), nullValue());
		assertThat(heading.getAttr("id"), equalTo("caption-id"));
		assertThat(heading.getText(), equalTo("Child Caption"));

		Element content = children.item(2);
		String contentCssClass = content.getAttr("class");
		assertTrue(contentCssClass.contains("child-content"));
		assertTrue(contentCssClass.contains("content"));
		assertThat(content.getAttr("data-editable"), nullValue());
		assertThat(content.getAttr("id"), equalTo("content-id"));

		children = content.getChildren();
		assertThat(children.size(), equalTo(2));
		assertThat(children.item(0).getTag(), equalTo("h2"));
		assertThat(children.item(1).getTag(), equalTo("section"));

		heading = children.item(0);
		assertThat(heading.getText(), equalTo("Child Content"));

		Element section = children.item(1);
		String sectionCssClass = section.getAttr("class");
		assertTrue(sectionCssClass.contains("child"));
		assertTrue(sectionCssClass.contains("rounded-box"));
		assertThat(section.getAttr("data-widget"), nullValue());
		assertThat(section.getAttr("id"), equalTo("child-id"));

		children = section.getChildren();
		assertThat(children.size(), equalTo(4));
		assertThat(children.item(0).getTag(), equalTo("h3"));
		assertThat(children.item(3).getTag(), equalTo("ul"));

		Element ul = children.item(3);
		String ulCssClass = ul.getAttr("class");
		assertTrue(ulCssClass.contains("nephew"));
		assertTrue(ulCssClass.contains("menu"));
		assertThat(ul.getAttr("data-class"), equalTo("js.wood.Menu"));
		assertThat(ul.getAttr("data-widget"), nullValue());
		assertThat(ul.getAttr("id"), equalTo("nephew-id"));

		children = ul.getChildren();
		assertThat(children.size(), equalTo(4));
		assertThat(children.item(0).getText(), equalTo("Create"));
		assertThat(children.item(1).getText(), equalTo("Read"));
		assertThat(children.item(2).getText(), equalTo("Update"));
		assertThat(children.item(3).getText(), equalTo("Delete"));
	}

	// ------------------------------------------------------
	// Exceptional conditions

	@Test(expected = WoodException.class)
	public void badLayoutName() {
		getCompo("exception/bad-layout-name");
	}

	@Test(expected = WoodException.class)
	public void missingTemplateCompo() {
		getCompo("exception/missing-template-compo");
	}

	@Test(expected = WoodException.class)
	public void missingWidgetCompo() {
		getCompo("exception/missing-widget-compo");
	}

	@Test(expected = WoodException.class)
	public void circularTemplateReference() {
		getCompo("exception/circular-template-reference");
	}

	@Test(expected = WoodException.class)
	public void circularWidgetReference() {
		getCompo("exception/circular-widget-reference");
	}

	@Test(expected = WoodException.class)
	public void emptyLayout() {
		getCompo("exception/empty-layout");
	}

	@Test(expected = WoodException.class)
	public void missingEditable() {
		getCompo("exception/missing-editable/content");
	}

	// ------------------------------------------------------
	// Helper methods

	private static Component getCompo(String path) {
		Project project = new Project(new File("src/test/resources/components"));
		return createCompo(project, new CompoPath(project, path));
	}

	private static Component createCompo(final Project project, final CompoPath path) {
		Component compo = new Component(path, new IReferenceHandler() {
			@Override
			public String onResourceReference(Reference reference, FilePath sourcePath) {
				Variables variables = new Variables(sourcePath.getParentDirPath());
				if (path.getProject().getAssetsDir().exists()) {
					try {
						Classes.invoke(variables, "load", path.getProject().getAssetsDir());
					} catch (Exception e) {
						throw new IllegalStateException(e);
					}
				}
				return variables.get(new Locale("en"), reference, sourcePath, this);
			}
		});

		compo.scan();
		return compo;
	}

	private static <T> T field(Object object, String field) {
		return Classes.getFieldValue(object, field);
	}
}
