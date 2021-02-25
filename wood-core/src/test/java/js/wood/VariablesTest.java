package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.impl.ResourceType;
import js.wood.impl.Variants;

@RunWith(MockitoJUnitRunner.class)
public class VariablesTest {
	@Mock
	private Project project;
	@Mock
	private Variants variants;
	@Mock
	private FilePath file;

	@Before
	public void beforeTest() {
		when(file.exists()).thenReturn(true);
		when(file.isVariables()).thenReturn(true);
		when(file.getVariants()).thenReturn(variants);
	}

	@Test
	public void constructor() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title>Title</title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		DirPath dir = Mockito.mock(DirPath.class);
		when(dir.getProject()).thenReturn(project);
		when(dir.iterator()).thenReturn(Arrays.asList(file).iterator());
		
		Variables variables = new Variables(dir);

		Map<Locale, Map<Reference, String>> localeValues = variables.getLocaleValues();
		assertThat(localeValues, notNullValue());
		assertThat(localeValues, aMapWithSize(1));

		Map<Reference, String> values = localeValues.get(null);
		assertThat(values, notNullValue());
		assertThat(values, aMapWithSize(1));
		assertThat(values.keySet(), contains(new Reference(ResourceType.STRING, "title")));
		assertThat(values.values(), contains("Title"));
	}

	@Test
	public void loadFilePath_String() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title>Title</title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		Variables variables = new Variables(project);
		variables.load(file);

		Map<Locale, Map<Reference, String>> localeValues = variables.getLocaleValues();
		assertThat(localeValues, notNullValue());
		assertThat(localeValues, aMapWithSize(1));

		Map<Reference, String> values = localeValues.get(null);
		assertThat(values, notNullValue());
		assertThat(values, aMapWithSize(1));
		assertThat(values.keySet(), contains(new Reference(ResourceType.STRING, "title")));
		assertThat(values.values(), contains("Title"));
	}

	@Test
	public void loadFilePath_StringLocale() {
		when(project.getDefaultLocale()).thenReturn(Locale.US);
		when(variants.getLocale()).thenReturn(Locale.US);

		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title>Title</title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		Variables variables = new Variables(project);
		variables.load(file);

		IReferenceHandler handler = Mockito.mock(IReferenceHandler.class);
		String value = variables.get(Locale.GERMAN, new Reference(ResourceType.STRING, "title"), file, handler);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("Title"));
	}

	@Test
	public void loadFilePath_StringEmpty() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title></title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		Variables variables = new Variables(project);
		variables.load(file);

		IReferenceHandler handler = Mockito.mock(IReferenceHandler.class);
		String value = variables.get(new Reference(ResourceType.STRING, "title"), file, handler);
		assertThat(value, nullValue());
	}

	@Test
	public void loadFilePath_Text() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<text>" + //
				"	<title><b>Big</b> Title</title>" + //
				"</text>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		Variables variables = new Variables(project);
		variables.load(file);

		Map<Locale, Map<Reference, String>> localeValues = variables.getLocaleValues();
		assertThat(localeValues, notNullValue());
		assertThat(localeValues, aMapWithSize(1));

		Map<Reference, String> values = localeValues.get(null);
		assertThat(values, notNullValue());
		assertThat(values, aMapWithSize(1));
		assertThat(values.keySet(), contains(new Reference(ResourceType.TEXT, "title")));
		assertThat(values.values(), contains("<b>Big</b> Title"));
	}

	@Test
	public void loadFilePath_NoVariablesDefinitionFile() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<no-variables-definition>" + //
				"	<title>content ignored</title>" + //
				"</no-variables-definition>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		Variables variables = new Variables(project);
		variables.load(file);

		Map<Locale, Map<Reference, String>> localeValues = variables.getLocaleValues();
		assertThat(localeValues, notNullValue());
		assertThat(localeValues, aMapWithSize(0));
	}

	@Test(expected = WoodException.class)
	public void loadFilePath_StringWithFormat() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title><b>Big</b> Title</title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		Variables variables = new Variables(project);
		variables.load(file);
	}

	@Test
	public void reload() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title>Title</title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		DirPath dir = Mockito.mock(DirPath.class);
		when(dir.iterator()).thenReturn(Arrays.asList(file).iterator());

		Variables variables = new Variables(project);
		variables.reload(dir);

		Map<Locale, Map<Reference, String>> localeValues = variables.getLocaleValues();
		assertThat(localeValues, notNullValue());
		assertThat(localeValues, aMapWithSize(1));

		Map<Reference, String> values = localeValues.get(null);
		assertThat(values, notNullValue());
		assertThat(values, aMapWithSize(1));
		assertThat(values.keySet(), contains(new Reference(ResourceType.STRING, "title")));
		assertThat(values.values(), contains("Title"));
	}

	@Test
	public void get() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title>Title</title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		Variables variables = new Variables(project);
		variables.load(file);

		IReferenceHandler handler = Mockito.mock(IReferenceHandler.class);
		String value = variables.get(new Reference(ResourceType.STRING, "title"), file, handler);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("Title"));
	}

	@Test
	public void circularDependecy() throws WoodException, IllegalArgumentException, IOException {
		FilePath[] files = new FilePath[2];
		files[0] = Mockito.mock(FilePath.class);
		when(files[0].isVariables()).thenReturn(true);
		when(files[0].getVariants()).thenReturn(variants);

		files[1] = Mockito.mock(FilePath.class);
		when(files[1].isVariables()).thenReturn(true);
		when(files[1].getVariants()).thenReturn(variants);

		String xml1 = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<app>app @string/name</app>" + //
				"</string>";
		when(files[0].getReader()).thenReturn(new StringReader(xml1));

		String xml2 = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<name>@string/app name</name>" + //
				"</string>";
		when(files[1].getReader()).thenReturn(new StringReader(xml2));

		DirPath dir = Mockito.mock(DirPath.class);
		when(dir.getProject()).thenReturn(project);
		when(dir.iterator()).thenReturn(Arrays.asList(files).iterator());

		Variables variables = new Variables(dir);
		IReferenceHandler handler = new IReferenceHandler() {
			@Override
			public String onResourceReference(Reference reference, FilePath sourceFile) throws IOException, WoodException {
				return variables.get(reference, sourceFile, this);
			}
		};

		try {
			variables.get(new Reference(ResourceType.STRING, "app"), file, handler);
			fail();
		} catch (WoodException e) {
			String message = "Circular variable references. Trace stack follows:\n" + //
					"\t- file:@string/app\n" + //
					"\t- file:@string/name\n";
			assertThat(e.getMessage(), equalTo(message));
		}
	}
}
