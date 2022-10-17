package com.jslib.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import com.jslib.api.dom.EList;
import com.jslib.api.dom.Element;
import com.jslib.util.Classes;

public class ComponentIntegrationTest {
	private Project project;
	private IReferenceHandler referenceHandler;

	@Before
	public void beforeTest() {
		project = Project.create(new File("src/test/resources/compo"));

		referenceHandler = new IReferenceHandler() {
			@Override
			public String onResourceReference(Reference reference, FilePath sourcePath) {
				Variables variables = new Variables(sourcePath.getParentDir());
				if (project.getAssetDir().exists()) {
					try {
						Classes.invoke(variables, "load", project.getAssetDir());
					} catch (Exception e) {
						throw new IllegalStateException(e);
					}
				}
				return variables.get(new Locale("en"), reference, sourcePath, this);
			}
		};
	}

	@Test
	public void createComponent() {
		Component compo = new Component(new CompoPath(project, "res/compo"), referenceHandler);
		Element layout = compo.getLayout();

		assertThat(layout, notNullValue());
		assertThat(layout.getTag(), equalTo("body"));
		assertThat(compo.getProject(), equalTo(project));

		assertThat(compo, properties());
		assertThat(compo, scriptDescriptors());

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

		assertTrue(layout.hasAttr("xmlns:w"));
		assertTrue(layout.getByTag("section").hasAttr("xmlns:w"));

		compo.clean();

		assertFalse(layout.hasAttr("xmlns:w"));
		assertFalse(layout.getByTag("section").hasAttr("xmlns:w"));

		assertThat(layout.getByTag("section"), notNullValue());
		assertThat(layout.getByTag("div"), nullValue());
	}

	// --------------------------------------------------------------------------------------------
	
	private static Matcher<Component> properties() {
		return new TypeSafeMatcher<Component>() {
			@Override
			public void describeTo(Description description) {
			}

			@Override
			protected boolean matchesSafely(Component compo) {
				assertThat(compo.getBaseLayoutPath().value(), equalTo("res/compo/compo.htm"));
				assertThat(compo.getName(), equalTo("compo"));
				assertThat(compo.getTitle(), equalTo("Page Compo"));
				assertThat(compo.getDescription(), equalTo("Page description."));
				assertThat(compo.getLayoutFileName(), equalTo("compo.htm"));
				assertThat(compo.toString(), equalTo("Page Compo"));
				assertThat(compo.getScriptDescriptor(CT.PREVIEW_SCRIPT), nullValue());
				assertThat(compo.getLinkDescriptors(), empty());
				assertThat(compo.getMetaDescriptors(), empty());
				assertThat(compo.getResourcesGroup(), nullValue());
				return true;
			}
		};
	}

	private static Matcher<Component> scriptDescriptors() {
		return new TypeSafeMatcher<Component>() {
			@Override
			public void describeTo(Description description) {
			}

			@Override
			protected boolean matchesSafely(Component compo) {
				List<IScriptDescriptor> scripts = compo.getScriptDescriptors();
				assertThat(scripts, hasSize(2));
				assertThat(scripts.get(0).getSource(), equalTo("lib/js-lib.js"));
				assertThat(scripts.get(1).getSource(), equalTo("script/js/wood/Compo.js"));
				return true;
			}
		};
	}
}
