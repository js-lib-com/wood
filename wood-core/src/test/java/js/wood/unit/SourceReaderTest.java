package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.util.Files;
import js.util.Strings;
import js.wood.FilePath;
import js.wood.IReference;
import js.wood.IReferenceHandler;
import js.wood.Project;
import js.wood.SourceReader;
import js.wood.WoodException;
import js.wood.impl.LayoutParameters;
import js.wood.impl.Reference;
import js.wood.impl.ResourceType;
import js.wood.impl.Variables;

public class SourceReaderTest implements IReferenceHandler {
	private Project project;

	@Before
	public void beforeTest() throws Exception {
		project = new Project(new File("src/test/resources/resources"));
	}

	private FilePath filePath(String path) {
		return new FilePath(project, path);
	}

	@Test
	public void fileConstructor() {
		FilePath sourceFile = filePath("res/compo/compo.htm");
		SourceReader reader = new SourceReader(sourceFile, this);
		assertReader(reader);
	}

	@Test
	public void readerConstructor() throws FileNotFoundException {
		FilePath sourceFile = filePath("res/compo/compo.htm");
		FileReader fileReader = new FileReader(sourceFile.toFile());
		SourceReader reader = new SourceReader(fileReader, sourceFile, this);
		assertReader(reader);
	}

	private void assertReader(SourceReader reader) {
		assertThat(field(reader, "sourceFile").toString(), equalTo("res/compo/compo.htm"));
		assertThat(field(reader, "referenceHandler"), equalTo(this));
		assertThat(field(reader, "reader"), notNullValue());
		assertThat(field(reader, "metaBuilder"), notNullValue());
		assertThat(field(reader, "state").toString(), equalTo("TEXT"));
		assertThat(field(reader, "value"), nullValue());
		assertThat((int) field(reader, "valueIndex"), equalTo(0));
		assertThat((int) field(reader, "charAfterMeta"), equalTo(0));
	}

	@Test
	public void referenceHandlerParameter() throws IOException {
		FilePath sourceFile = filePath("res/compo/compo.htm");

		SourceReader reader = new SourceReader(sourceFile, new IReferenceHandler() {
			@Override
			public String onResourceReference(IReference reference, FilePath sourceFile) throws IOException {
				assertThat(((Reference) reference).getResourceType(), equalTo(ResourceType.STRING));
				assertThat(reference.getName(), equalTo("title"));
				assertThat(sourceFile.value(), equalTo("res/compo/compo.htm"));
				return reference.toString();
			}
		});

		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
	}

	@Test
	public void copy_StringResolve() throws IOException {
		FilePath sourceFile = filePath("res/compo/compo.htm");
		SourceReader reader = new SourceReader(sourceFile, this);

		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
		assertTrue(writer.toString().contains("<h1>Component Title</h1>"));
	}

	@Test
	public void copy_ExpressionEval() throws IOException {
		FilePath sourceFile = filePath("res/compo/compo.css");
		SourceReader reader = new SourceReader(sourceFile, this);

		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
		assertTrue(writer.toString().contains("width: 9.0px;"));
	}

	@Test
	public void copy_ParamResolve() throws IOException {
		LayoutParameters parameters = new LayoutParameters();
		parameters.load("title:Component Parameter;");

		FilePath sourceFile = filePath("res/generic-compo/generic-compo.htm");
		SourceReader reader = new SourceReader(sourceFile, parameters, this);

		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
		assertTrue(writer.toString().contains("<h1>Component Parameter</h1>"));
	}

	@Test(expected = WoodException.class)
	public void copy_ParamResolve_MissingParameter() throws IOException {
		LayoutParameters parameters = new LayoutParameters();

		FilePath sourceFile = filePath("res/generic-compo/generic-compo.htm");
		SourceReader reader = new SourceReader(sourceFile, parameters, this);

		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
	}

	@Test(expected = WoodException.class)
	public void unknownType() throws IOException {
		FilePath sourceFile = filePath("res/exception/unknown-type/unknown-type.htm");
		SourceReader reader = new SourceReader(sourceFile, this);
		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
	}

	// ------------------------------------------------------
	// Internal helpers

	@Override
	public String onResourceReference(IReference reference, FilePath sourcePath) throws IOException {
		if (reference.isVariable()) {
			Variables variables = new Variables(sourcePath.getParentDirPath());
			if (project.getAssetsDir().exists()) {
				invoke(variables, "load", project.getAssetsDir());
			}
			return variables.get(new Locale("en"), reference, sourcePath, this);
		} else {
			return Strings.concat("media/", reference.getName(), ".png");
		}
	}

	private static <T> T field(Object object, String field) {
		return Classes.getFieldValue(object, field);
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
