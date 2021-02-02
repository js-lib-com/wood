package js.wood.script.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Locale;

import org.junit.Test;

import js.util.Classes;
import js.wood.CompoPath;
import js.wood.Component;
import js.wood.FilePath;
import js.wood.IReference;
import js.wood.IReferenceHandler;
import js.wood.Project;
import js.wood.WoodException;
import js.wood.impl.Variables;

public class ComponentTest {

	@Test
	public void scriptedWidgetStyle() {
		Component compo = getCompo("scripted-compo");
		List<FilePath> styles = compo.getStyleFiles();

		assertEquals(1, styles.size());
		assertEquals("lib/scripted-widget/scripted-widget.css", styles.get(0).toString());
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
		return new Project("src/test/resources/" + projectDir);
	}

	private static Component getCompo(String path) {
		Project project = project("components");
		return createCompo(new CompoPath(project, path));
	}

	private static Component createCompo(final CompoPath path) {
		Component compo = new Component(path, new IReferenceHandler() {
			@Override
			public String onResourceReference(IReference reference, FilePath sourcePath) {
				Variables variables = new Variables(sourcePath.getDirPath());
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
