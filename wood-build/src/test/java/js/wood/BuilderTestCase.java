package js.wood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import js.dom.Document;
import js.dom.EList;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;

public class BuilderTestCase {
	protected static BuilderProject project(String projectDir) {
		try {
			BuilderProject project = new BuilderProject("src/test/resources/" + projectDir);
			if (project.getSiteDir().exists()) {
				Files.removeFilesHierarchy(project.getSiteDir());
			}
			return project;
		} catch (IOException e) {
			e.printStackTrace();
			fail("Fail to create project instance.");
		}

		throw new IllegalStateException();
	}

	protected static String path(String fileName) {
		return "src/test/resources/" + fileName;
	}

	protected static <T> T field(Object object, String field) {
		return Classes.getFieldValue(object, field);
	}

	protected static void field(Object object, String field, Object value) {
		Classes.setFieldValue(object, field, value);
	}

	protected static void assertStyle(String expected, EList styles, int index) {
		assertEquals(expected, styles.item(index).getAttr("href"));
	}

	protected static void assertAnchor(String expected, EList anchors, int index) {
		assertEquals(expected, anchors.item(index).getText());
	}

	protected static void assertImage(String expected, EList images, int index) {
		assertEquals(expected, images.item(index).getAttr("src"));
	}

	protected static void assertScript(String expected, EList scripts, int index) {
		assertEquals(expected, scripts.item(index).getAttr("src"));
	}

	protected static void resetProjectLocales(Project project) {
		List<Locale> locales = new ArrayList<Locale>();
		locales.add(new Locale("en"));
		field(Classes.getFieldValue(project, Project.class, "config"), "locales", locales);
	}

	protected static <T> T invoke(Object object, String method, Object... args) {
		try {
			return Classes.invoke(object, method, args);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
		throw new IllegalStateException();
	}

	protected static IReferenceHandler nullReferenceHandler() {
		return new IReferenceHandler() {
			@Override
			public String onResourceReference(IReference reference, FilePath sourceFile) throws IOException {
				return "null";
			}

			@Override
			public String toString() {
				return "null reference handler";
			}
		};
	}

	protected static String stringify(Document document) throws IOException {
		StringWriter writer = new StringWriter();
		document.serialize(writer);
		return writer.toString();
	}

	protected static File file(String fileName) {
		return new File(new File("src/test/resources"), fileName);
	}

	protected static File file(File dir, String... segments) {
		return new File(dir, Strings.join(segments, '/'));
	}
}
