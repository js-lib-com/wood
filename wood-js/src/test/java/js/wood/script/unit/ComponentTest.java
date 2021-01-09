package js.wood.script.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import js.dom.Element;
import js.util.Classes;
import js.util.Files;
import js.wood.CompoPath;
import js.wood.Component;
import js.wood.FilePath;
import js.wood.IReference;
import js.wood.IReferenceHandler;
import js.wood.IScriptFile;
import js.wood.Project;
import js.wood.WoodException;
import js.wood.impl.Variables;

public class ComponentTest {
	@Test
	public void pageScript() {
		Component compo = getCompo("scripts/page-script");
		List<IScriptFile> scripts = compo.getScriptFiles();

		assertEquals(2, scripts.size());
		assertEquals("lib/js-lib.js", scripts.get(0).toString());
		assertEquals("script/js/wood/Page.js", scripts.get(1).toString());
	}

	@Test
	public void scriptsInclusion() {
		Component compo = getCompo("scripts/compo");
		List<IScriptFile> scripts = compo.getScriptFiles();

		assertEquals(3, scripts.size());
		assertEquals("lib/js-lib.js", scripts.get(0).toString());
		assertEquals("script/js/wood/Title.js", scripts.get(1).toString());
		assertEquals("script/js/wood/Widget.js", scripts.get(2).toString());
	}

	@Test
	public void scriptedWidgetStyle() {
		Component compo = getCompo("scripted-compo");
		List<FilePath> styles = compo.getStyleFiles();

		assertEquals(1, styles.size());
		assertEquals("lib/scripted-widget/scripted-widget.css", styles.get(0).toString());
	}

	@Test
	public void thirdPartyScripts() {
		Component compo = getCompo("scripts/3pty-scripts");
		Element layout = compo.getLayout();
		assertNotNull(layout);

		assertEquals(1, compo.getThirdPartyScripts().size());
		assertEquals("http://maps.google.com/maps/api/js?sensor=false", compo.getThirdPartyScripts().iterator().next());

		assertEquals(3, compo.getScriptFiles().size());
		assertEquals("lib/js-lib.js", compo.getScriptFiles().get(0).toString());
		assertEquals("script/js/wood/GeoMap.js", compo.getScriptFiles().get(1).toString());
		assertEquals("lib/google-maps-api.js", compo.getScriptFiles().get(2).toString());
	}

	@Test
	public void missingScript() {
		try {
			getCompo("exception/missing-script");
			fail("Missing script reference should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().startsWith("Broken script reference."));
		}
	}

	// ------------------------------------------------------
	// Helper methods

	protected static Project project(String projectDir) {
		Project project = new Project("src/test/resources/" + projectDir);
		if (project.getSiteDir().exists()) {
			try {
				Files.removeFilesHierarchy(project.getSiteDir());
			} catch (IOException e) {
				e.printStackTrace();
				fail();
			}
		}
		project.previewScriptFiles();
		return project;
	}

	private static Component getCompo(String path) {
		Project project = project("components");
		return createCompo(project, new CompoPath(project, path));
	}

	private static Component createCompo(final Project project, final CompoPath path) {
		Component compo = new Component(path, new IReferenceHandler() {
			@Override
			public String onResourceReference(IReference reference, FilePath sourcePath) {
				Variables variables = new Variables(project, sourcePath.getDirPath());
				if (path.getProject().getAssetsDir().exists()) {
					invoke(variables, "load", path.getProject().getAssetsDir());
				}
				return variables.get(new Locale("en"), reference, sourcePath, this);
			}
		});

		compo.scan(false);
		return compo;
	}

	private static <T> T invoke(Object object, String method, Object... args) {
		try {
			return Classes.invoke(object, method, args);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		throw new IllegalStateException();
	}
}
