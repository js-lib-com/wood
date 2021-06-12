package js.wood.cli.compo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import com.jslib.commons.cli.FilesUtil;

import js.dom.Document;
import js.dom.Element;
import js.wood.cli.compo.CompoImport.CompoDescriptor;

@RunWith(MockitoJUnitRunner.class)
public class CompoDescriptorTest {
	@Mock
	private FilesUtil files;
	@Mock
	private Path projectRoot;
	@Mock
	private Path descriptorFile;
	@Mock
	private Path scriptFile;

	private CompoImport.CompoDescriptor descriptor;

	@Before
	public void beforeTest() throws IOException {
		when(files.getProjectDir()).thenReturn(projectRoot);
		when(projectRoot.relativize(any())).thenReturn(scriptFile);
		when(scriptFile.toString()).thenReturn("lib/js-lib/js-lib.js");
	}

	@Test
	public void GivenDependencyDefined_WhenGetDependencies_ThenNotEmpty() throws IOException {
		// given
		String xml = "" + //
				"<compo>" + //
				"	<dependencies>" + //
				"		<dependency>" + //
				"		</dependency>" + //
				"	</dependencies>" + //
				"</compo>" + //
				"";
		when(files.getReader(any())).thenReturn(new StringReader(xml));
		descriptor = new CompoDescriptor(files, descriptorFile);

		// when
		List<CompoCoordinates> dependencies = descriptor.getDependencies();

		// then
		assertFalse(dependencies.isEmpty());
	}

	@Test
	public void GivenDependencyNotDefined_WhenGetDependencies_ThenEmpty() throws IOException {
		// given
		String xml = "" + //
				"<compo>" + //
				"	<dependencies>" + //
				"	</dependencies>" + //
				"</compo>" + //
				"";
		when(files.getReader(any())).thenReturn(new StringReader(xml));
		descriptor = new CompoDescriptor(files, descriptorFile);

		// when
		List<CompoCoordinates> dependencies = descriptor.getDependencies();

		// then
		assertTrue(dependencies.isEmpty());
	}

	@Test
	public void GivenDependencyDefined_WhenGetDependencies_ThenNotNull() throws IOException, SAXException {
		// given
		String xml = "" + //
				"<compo>" + //
				"	<dependencies>" + //
				"		<dependency>" + //
				"			<groupId>com.js-lib</groupId>" + //
				"			<artifactId>js-lib</artifactId>" + //
				"			<version>1.3.7</version>" + //
				"		</dependency>" + //
				"	</dependencies>" + //
				"</compo>" + //
				"";
		when(files.getReader(any())).thenReturn(new StringReader(xml));
		descriptor = new CompoDescriptor(files, descriptorFile);

		// when
		List<CompoCoordinates> dependencies = descriptor.getDependencies();

		// then
		CompoCoordinates coordinates = dependencies.get(0);
		assertThat(coordinates, notNullValue());
		assertThat(coordinates.getGroupId(), equalTo("com.js-lib"));
		assertThat(coordinates.getArtifactId(), equalTo("js-lib"));
		assertThat(coordinates.getVersion(), equalTo("1.3.7"));
	}

	@Test
	public void GivenDependenciesDefined_WhenRemoveDependencies_ThenGetNull() throws IOException {
		// given
		String xml = "" + //
				"<compo>" + //
				"	<dependencies>" + //
				"		<dependency>" + //
				"		</dependency>" + //
				"	</dependencies>" + //
				"</compo>" + //
				"";
		when(files.getReader(any())).thenReturn(new StringReader(xml));
		descriptor = new CompoDescriptor(files, descriptorFile);

		// when
		descriptor.removeDependencies();

		// then
		assertThat(descriptor.getDocument().getByTag("dependencies"), nullValue());
	}

	@Test
	public void GivenDependenciesDefined_WhenCreateScripts_ThenNotNull() throws IOException {
		// given
//		Path compoDir = mock(Path.class);
//		Path scriptFile = mock(Path.class);
//		when(compoDir.resolve(".js")).thenReturn(scriptFile);
//		when(scriptFile.toString()).thenReturn("lib/js-lib/js-lib.js");

		String xml = "" + //
				"<compo>" + //
				"	<dependencies>"+//
				"	</dependencies>"+//
				"</compo>" + //
				"";
		when(files.getReader(any())).thenReturn(new StringReader(xml));
		descriptor = new CompoDescriptor(files, descriptorFile);

		// when
		descriptor.createScripts();

		// then
		Document document = descriptor.getDocument();
		assertThat(document.getByTag("scripts"), notNullValue());
		Element script = document.getByTag("script");
		assertThat(script, notNullValue());
		assertThat(script.getAttr("src"), equalTo("lib/js-lib/js-lib.js"));
	}

	@Test
	public void GivenScripDefined_WhenAddScriptDependency_ThenAddChild() throws IOException {
		// given
		Path scriptFile = mock(Path.class);
		
		String xml = "" + //
				"<compo>" + //
				"	<scripts>" + //
				"		<script>" + //
				"		</script>" + //
				"	</scripts>" + //
				"</compo>" + //
				"";
		when(files.getReader(any())).thenReturn(new StringReader(xml));
		descriptor = new CompoDescriptor(files, descriptorFile);
		descriptor.setScriptElement(descriptor.getDocument().getByTag("script"));

		// when
		descriptor.addScriptDependency(scriptFile);
		
		// then
		Document document = descriptor.getDocument();
		Element dependency = document.getByTag("dependency");
		assertThat(dependency, notNullValue());
		assertThat(dependency.getAttr("src"), equalTo("lib/js-lib/js-lib.js"));
	}
	
	@Test
	public void GivenCoordinatesDefined_WhenGetCoordinates_ThenExpectedValues() throws IOException {
		// given
		String xml = "" + //
				"<compo>" + //
				"	<groupId>com.js-lib</groupId>" + //
				"	<artifactId>js-lib</artifactId>" + //
				"	<version>1.3.7</version>" + //
				"</compo>" + //
				"";
		when(files.getReader(any())).thenReturn(new StringReader(xml));
		descriptor = new CompoDescriptor(files, descriptorFile);
		
		// when
		CompoCoordinates coordinates = descriptor.getCoordinates();
		
		// then
		assertThat(coordinates.getGroupId(), equalTo("com.js-lib"));
		assertThat(coordinates.getArtifactId(), equalTo("js-lib"));
		assertThat(coordinates.getVersion(), equalTo("1.3.7"));
	}
}
