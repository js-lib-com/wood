package js.wood;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.util.Strings;
import js.wood.DirPath;
import js.wood.FilePath;
import js.wood.IReferenceHandler;
import js.wood.Project;
import js.wood.Reference;
import js.wood.Variables;
import js.wood.WoodException;
import js.wood.impl.ResourceType;

public class VariablesTest implements IReferenceHandler {
	private Project project;

	@Before
	public void beforeTest() {
		project = new Project(new File("src/test/resources/resources"));
	}

	private DirPath dirPath(String path) {
		return new DirPath(project, path);
	}

	@Test
	public void componentConstructor() throws IOException {
		Variables variables = new Variables(dirPath("res/compo"));

		assertThat(variable(variables, ResourceType.STRING, "title"), equalTo("Component Title"));
		assertThat(variable(variables, ResourceType.TEXT, "alert"), equalTo("This is <em>alert</em> message."));
		assertThat(variable(variables, ResourceType.COLOR, "compo-header-bg"), equalTo("#000000"));
		assertThat(variable(variables, ResourceType.DIMEN, "compo-height"), equalTo("80px"));
	}

	@Test
	public void assetConstructor() throws Throwable {
		Variables variables = new Variables(dirPath("res/asset"));

		assertThat(variable(variables, ResourceType.STRING, "logo-type"), equalTo("kids (a)cademy"));
		assertThat(variable(variables, ResourceType.TEXT, "alert"), equalTo("This is <em>alert</em> message."));
		assertThat(variable(variables, ResourceType.COLOR, "page-header-link"), equalTo("#80A0A0"));
		assertThat(variable(variables, ResourceType.DIMEN, "page-header-height"), equalTo("80px"));
	}

	@Test
	public void nestedValues() throws IOException {
		Variables variables = new Variables(dirPath("res/asset"));

		assertNotNull("Message text variable not found", variable(variables, ResourceType.TEXT, "message"));
		assertTrue(variable(variables, ResourceType.TEXT, "message").contains("kids (a)cademy"));
	}

	@Test
	public void multilanguage() throws IOException {
		Variables variables = new Variables(dirPath("res/multilanguage"));

		assertThat(variable(variables, "en", ResourceType.STRING, "logo-type"), equalTo("kids (a)cademy"));
		assertThat(variable(variables, "en", ResourceType.TEXT, "alert"), equalTo("This is <em>alert</em> message."));

		assertThat(variable(variables, "ro", ResourceType.STRING, "logo-type"), equalTo("academia copiilor"));
		assertThat(variable(variables, "ro", ResourceType.TEXT, "alert"), equalTo("Acesta este o <em>alertă</em>."));
	}

	@Test
	public void missingValue() {
		Variables variables = new Variables(dirPath("res/compo"));
		FilePath source = new FilePath(project, "res/compo/compo.htm");
		assertThat(variables.get(new Locale("en"), new Reference(source, ResourceType.STRING, "fake-variable"), source, this), nullValue());
	}

	@Test(expected = WoodException.class)
	public void badResourceType() {
		new Variables(dirPath("res/exception/bad-type"));
	}

	@Test(expected = WoodException.class)
	public void circularReferences() {
		Variables variables = new Variables(dirPath("res/exception/circular-reference"));
		FilePath source = new FilePath(project, "res/exception/circular-reference/circular-reference.htm");
		variables.get(null, new Reference(source, ResourceType.STRING, "reference"), source, this);
	}

	@Test(expected = WoodException.class)
	public void selfReference() {
		Variables variables = new Variables(dirPath("res/exception/self-reference"));
		FilePath source = new FilePath(project, "res/exception/self-reference/self-reference.htm");
		variables.get(null, new Reference(source, ResourceType.STRING, "reference"), source, this);
	}

	@Test(expected = WoodException.class)
	public void stringWithFormattingTag() {
		new Variables(dirPath("res/exception/nested-element"));
	}

	private String variable(Variables variables, ResourceType type, String name) {
		FilePath source = new FilePath(project, "res/compo/compo.htm");
		return variables.get(new Locale("en"), new Reference(source, type, name), source, this);
	}

	private String variable(Variables variables, String language, ResourceType type, String name) {
		FilePath source = new FilePath(project, "res/compo/compo.htm");
		return variables.get(new Locale(language), new Reference(source, type, name), source, this);
	}

	@Override
	public String onResourceReference(Reference reference, FilePath sourcePath) throws IOException, WoodException {
		if (reference.isVariable()) {
			Variables variables = new Variables(sourcePath.getParentDirPath());
			if (project.getAssetsDir().exists()) {
				try {
					Classes.invoke(variables, "load", project.getAssetsDir());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return variables.get(new Locale("en"), reference, sourcePath, this);
		} else {
			return Strings.concat("media/", reference.getName(), ".png");
		}
	}
}
