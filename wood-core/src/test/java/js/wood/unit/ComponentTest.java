package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import js.wood.IReference;
import js.wood.IReferenceHandler;
import js.wood.Project;
import js.wood.WOOD;
import js.wood.WoodException;
import js.wood.impl.Variables;

public class ComponentTest {
	@Test
	public void simpleLayout() {
		Component compo = getCompo("simple/layout");

		assertEquals("res/simple/layout/layout.htm", field(compo, "baseLayoutPath").toString());
		assertEquals("layout", compo.getName());
		assertEquals("Components / Layout", compo.getDisplay());
		assertEquals("Components / Layout", compo.getDescription());
		assertEquals("layout.htm", compo.getLayoutFileName());

		Element layout = compo.getLayout();
		assertNotNull(layout);
		assertEquals("body", layout.getTag());
		assertEquals("Simple Layout", layout.getByTag("h1").getText());

		assertTrue(compo.getStyleFiles().isEmpty());
	}

	@Test
	public void simpleTemplate() {
		Component compo = getCompo("simple/template/compo");
		Element layout = compo.getLayout();

		Element editable = layout.getByTag("section");
		assertEquals("body", layout.getTag());

		EList headings = layout.findByTag("h1");
		assertEquals(2, headings.size());
		assertEquals("Template", headings.item(0).getText());
		assertEquals("Content", headings.item(1).getText());

		editable = layout.getByTag("section");
		assertNotNull(editable);
		assertFalse(editable.hasAttrNS(WOOD.NS, "editable"));
	}

	@Test
	public void variableTemplate() {
		Component compo = getCompo("variable-template/compo");
		Element layout = compo.getLayout();

		EList inputs = layout.findByTag("input");
		assertEquals(3, inputs.size());
		assertEquals("user-name", inputs.item(0).getAttr("name"));
		assertEquals("address", inputs.item(1).getAttr("name"));
		assertEquals("password", inputs.item(2).getAttr("name"));
	}

	@Test
	public void templatesHierarchy() {
		Component compo = getCompo("templates-hierarchy/compo");
		Element layout = compo.getLayout();

		assertEquals("body", layout.getTag());

		EList headings = layout.findByTag("h1");
		assertEquals(3, headings.size());
		assertEquals("Grand Parent", headings.item(0).getText());
		assertEquals("Parent", headings.item(1).getText());
		assertEquals("Child", headings.item(2).getText());
	}

	@Test
	public void simpleWidget() {
		Component compo = getCompo("simple/widget/compo");
		Element layout = compo.getLayout();

		EList headings = layout.findByTag("h1");
		assertEquals(2, headings.size());
		assertEquals("Component", headings.item(0).getText());
		assertEquals("Widget", headings.item(1).getText());
	}

	@Test
	public void widgetsTree() {
		Component compo = getCompo("widgets-tree/compo");
		Element layout = compo.getLayout();

		EList headings = layout.findByTag("h1");
		assertEquals(5, headings.size());
		assertEquals("Component", headings.item(0).getText());
		assertEquals("Child One", headings.item(1).getText());
		assertEquals("Nephew One", headings.item(2).getText());
		assertEquals("Child Two", headings.item(3).getText());
		assertEquals("Nephew Two", headings.item(4).getText());
	}

	@Test
	public void attributes() {
		Component compo = getCompo("attributes/compo");
		Element layout = compo.getLayout();

		Element section = layout.getByTag("section");
		assertEquals("template compo", section.getAttr("class"));
		assertEquals("js.wood.Title", section.getAttr("data-format"));
		assertEquals("section-id", section.getAttr("id"));

		Element div = layout.getByTag("div");
		assertEquals("widget compo", div.getAttr("class"));
		assertEquals("js.wood.Widget", div.getAttr("data-class"));
		assertEquals("compo-id", div.getAttr("id"));

		// template and widget does not overwrite compo attributes; names are not changed
		assertEquals("section", section.getAttr("name"));
		assertEquals("div", div.getAttr("name"));
	}

	@Test
	public void attributesWithEntity() {
		Component compo = getCompo("attributes-entity/compo");
		Element layout = compo.getLayout();
		Element div = layout.getByTag("div");
		assertEquals("P&G", div.getAttr("name"));
	}

	@Test
	public void widgetParameter() {
		Component compo = getCompo("parameter/compo");
		Element layout = compo.getLayout();
		Element div = layout.getByTag("h1");
		assertEquals("Widget Title", div.getText());
	}

	@Test
	public void widgetParameterWithEntity() {
		Component compo = getCompo("parameter-entity/compo");
		Element layout = compo.getLayout();
		Element div = layout.getByTag("h1");
		assertEquals("P&G", div.getAttr("data-name"));
		assertEquals("P&G", div.getText());
	}

	@Test
	public void stylesInclusion() {
		Component compo = getCompo("styles/compo");
		List<FilePath> styles = compo.getStyleFiles();

		assertEquals(3, styles.size());
		assertEquals("res/styles/widget/widget.css", styles.get(0).toString());
		assertEquals("res/styles/template/template.css", styles.get(1).toString());
		assertEquals("res/styles/compo/compo.css", styles.get(2).toString());
	}

	/**
	 * Test if template, widgets and script references are removed from component aggregated layout.
	 */
	@Test
	public void objectReferencesErasure() {
		for (String compoPath : new String[] { "simple/widget/compo", "simple/template/compo", "widgets-tree/compo", "templates-hierarchy/compo", "scripts/page-script" }) {
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
		Project project = new Project("src/test/resources/components");
		CompoPath path = new CompoPath(project, "references");
		final List<IReference> references = new ArrayList<>();

		Component compo = new Component(path, new IReferenceHandler() {
			@Override
			public String onResourceReference(IReference reference, FilePath sourcePath) {
				references.add(reference);
				return reference.toString();
			}
		});
		compo.scan(false);

		assertEquals(3, references.size());
		assertEquals("@string/title", references.get(0).toString());
		assertEquals("@image/logo", references.get(1).toString());
		assertEquals("@text/message", references.get(2).toString());
	}

	@Test
	public void inline() {
		Component compo = getCompo("inline");

		Element layout = compo.getLayout();
		assertNotNull(layout);
	}

	// ------------------------------------------------------
	// Integration test

	@Test
	public void fullComponent() {
		Component compo = getCompo("full/compo");
		Element layout = Classes.getFieldValue(compo, "layout");

		assertNotNull(layout);
		assertEquals("body", layout.getTag());
		EList children = layout.getChildren();
		assertEquals(4, children.size());
		assertEquals("header", children.item(0).getTag());
		assertEquals("nav", children.item(1).getTag());
		assertEquals("section", children.item(2).getTag());
		assertEquals("footer", children.item(3).getTag());

		Element mainSection = children.item(2);
		String mainSectionCssClass = mainSection.getAttr("class");
		assertTrue(mainSectionCssClass.contains("main"));
		assertTrue(mainSectionCssClass.contains("rounded-box"));
		assertNull(mainSection.getAttr("data-editable"));

		children = mainSection.getChildren();
		assertEquals(3, children.size());
		assertEquals("section", children.item(0).getTag());
		assertEquals("h1", children.item(1).getTag());
		assertEquals("section", children.item(2).getTag());

		Element sideBar = children.item(0);
		assertEquals("side-bar", sideBar.getAttr("class"));

		Element heading = children.item(1);
		assertEquals("child-caption", heading.getAttr("class"));
		assertEquals("js.wood.Title", heading.getAttr("data-class"));
		assertNull(heading.getAttr("data-editable"));
		assertEquals("caption-id", heading.getAttr("id"));
		assertEquals("Child Caption", heading.getText());

		Element content = children.item(2);
		String contentCssClass = content.getAttr("class");
		assertTrue(contentCssClass.contains("child-content"));
		assertTrue(contentCssClass.contains("content"));
		assertNull(content.getAttr("data-editable"));
		assertEquals("content-id", content.getAttr("id"));

		children = content.getChildren();
		assertEquals(2, children.size());
		assertEquals("h2", children.item(0).getTag());
		assertEquals("section", children.item(1).getTag());

		heading = children.item(0);
		assertEquals("Child Content", heading.getText());

		Element section = children.item(1);
		String sectionCssClass = section.getAttr("class");
		assertTrue(sectionCssClass.contains("child"));
		assertTrue(sectionCssClass.contains("rounded-box"));
		assertNull(section.getAttr("data-widget"));
		assertEquals("child-id", section.getAttr("id"));

		children = section.getChildren();
		assertEquals(4, children.size());
		assertEquals("h3", children.item(0).getTag());
		assertEquals("ul", children.item(3).getTag());

		Element ul = children.item(3);
		String ulCssClass = ul.getAttr("class");
		assertTrue(ulCssClass.contains("nephew"));
		assertTrue(ulCssClass.contains("menu"));
		assertEquals("js.wood.Menu", ul.getAttr("data-class"));
		assertNull(ul.getAttr("data-widget"));
		assertEquals("nephew-id", ul.getAttr("id"));

		children = ul.getChildren();
		assertEquals(4, children.size());
		assertEquals("Create", children.item(0).getText());
		assertEquals("Read", children.item(1).getText());
		assertEquals("Update", children.item(2).getText());
		assertEquals("Delete", children.item(3).getText());
	}

	// ------------------------------------------------------
	// Exceptional conditions

	@Test
	public void badLayoutName() {
		try {
			getCompo("exception/bad-layout-name");
			fail("Bad layout name should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().contains("The system cannot find the file specified"));
		}
	}

	@Test
	public void missingTemplateCompo() {
		try {
			getCompo("exception/missing-template-compo");
			fail("Missing template component should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().contains("Missing component path"));
		}
	}

	@Test
	public void missingWidgetCompo() {
		try {
			getCompo("exception/missing-widget-compo");
			fail("Missing widget component should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().contains("Missing component path"));
		}
	}

	@Test
	public void circularTemplateReference() {
		try {
			getCompo("exception/circular-template-reference");
			fail("Circular template reference should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().contains("Circular templates references suspicion."));
		}
	}

	@Test
	public void circularWidgetReference() {
		try {
			getCompo("exception/circular-widget-reference");
			fail("Cicular widget reference should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().startsWith("Circular templates references suspicion."));
		}
	}

	@Test
	public void emptyLayout() {
		try {
			getCompo("exception/empty-layout");
			fail("Empty layout should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().startsWith("Empty layout"));
		}
	}

	@Test
	public void missingScript() {
		try {
			getCompo("exception/missing-script");
			// fail("Missing script reference should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().startsWith("Broken script reference."));
		}
	}

	@Test
	public void missingEditable() {
		try {
			getCompo("exception/missing-editable/content");
			fail("Missing editable should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().contains("Missing editable"));
		}
	}

	// ------------------------------------------------------
	// Helper methods

	private static Component getCompo(String path) {
		Project project = new Project("src/test/resources/components");
		return createCompo(project, new CompoPath(project, path));
	}

	private static Component createCompo(final Project project, final CompoPath path) {
		Component compo = new Component(path, new IReferenceHandler() {
			@Override
			public String onResourceReference(IReference reference, FilePath sourcePath) {
				Variables variables = new Variables(sourcePath.getDirPath());
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

		compo.scan(false);
		return compo;
	}

	private static <T> T field(Object object, String field) {
		return Classes.getFieldValue(object, field);
	}
}
