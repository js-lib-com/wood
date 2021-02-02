package js.wood.unit;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import js.wood.FilePath;
import js.wood.IReference;
import js.wood.IReferenceHandler;
import js.wood.Project;
import js.wood.impl.ReferencesResolver;

public class ReferencesResolverTest {
	private Project project;

	@Before
	public void beforeTest() {
		project = new Project("src/test/resources/resources");
	}

	@Test
	public void valueReferencesResolverParse() {
		String value = "<h1>@string/title</h1>";
		FilePath sourceFile = new FilePath(project, "res/compo/compo.htm");

		ReferencesResolver resolver = new ReferencesResolver();
		value = resolver.parse(value, sourceFile, new IReferenceHandler() {
			@Override
			public String onResourceReference(IReference reference, FilePath sourceFile) throws IOException {
				return "resource value";
			}
		});

		assertEquals("<h1>resource value</h1>", value);
	}
}