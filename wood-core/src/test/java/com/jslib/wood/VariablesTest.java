package com.jslib.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.wood.impl.FileType;
import com.jslib.wood.impl.Variants;

@RunWith(MockitoJUnitRunner.class)
public class VariablesTest {
	@Mock
	private Variants variants;
	@Mock
	private FilePath file;
	@Mock
	private FilePath varFile; 

	@Before
	public void beforeTest() {
		when(file.isVariables()).thenReturn(true);
		when(file.getVariants()).thenReturn(variants);
		
		when(file.cloneTo(FileType.VAR)).thenReturn(varFile);
		when(varFile.isSynthetic()).thenReturn(true);
	}

	@Test
	public void GivenDirWithVariablesFile_WhenConstructor_ThenInitState() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title>Title</title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		FilePath dir = Mockito.mock(FilePath.class);
		when(dir.iterator()).thenReturn(Arrays.asList(file).iterator());

		// when
		Variables variables = new Variables(dir);

		// then
		Map<String, Map<Reference, String>> languageValues = variables.getLanguageValues();
		assertThat(languageValues, notNullValue());
		assertThat(languageValues, aMapWithSize(1));

		Map<Reference, String> values = languageValues.get(null);
		assertThat(values, notNullValue());
		assertThat(values, aMapWithSize(1));
		assertThat(values.keySet(), contains(new Reference(Reference.Type.STRING, "title")));
		assertThat(values.values(), contains("Title"));
	}

	@Test
	public void GivenVariablesFile_WhenLoad_ThenInitState() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title>Title</title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		// when
		Variables variables = new Variables();
		variables.load(file);

		// then
		Map<String, Map<Reference, String>> languageValues = variables.getLanguageValues();
		assertThat(languageValues, notNullValue());
		assertThat(languageValues, aMapWithSize(1));

		Map<Reference, String> values = languageValues.get(null);
		assertThat(values, notNullValue());
		assertThat(values, aMapWithSize(1));
		assertThat(values.keySet(), contains(new Reference(Reference.Type.STRING, "title")));
		assertThat(values.values(), contains("Title"));
	}

	@Test
	public void GivenVariablesFileWithlanguage_WhenLoad_ThenGetLocalizedValue() {
		// given
		when(variants.getLanguage()).thenReturn("en-US");

		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title>Title</title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		// given
		Variables variables = new Variables();
		variables.load(file);

		// then
		IReferenceHandler handler = mock(IReferenceHandler.class);
		String value = variables.get("en-US", new Reference(Reference.Type.STRING, "title"), file, handler);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("Title"));
	}

	@Test
	public void GivenEmptyValue_WhenGet_ThenNull() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title></title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		Variables variables = new Variables();
		variables.load(file);

		// when
		IReferenceHandler handler = mock(IReferenceHandler.class);
		String value = variables.get(new Reference(Reference.Type.STRING, "title"), file, handler);

		// then
		assertThat(value, nullValue());
	}

	@Test
	public void GivenTextVariablesFile_WhenLoad_ThyenInitState() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<text>" + //
				"	<title><b>Big</b> Title</title>" + //
				"</text>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		// when
		Variables variables = new Variables();
		variables.load(file);

		// then
		Map<String, Map<Reference, String>> languageValues = variables.getLanguageValues();
		assertThat(languageValues, notNullValue());
		assertThat(languageValues, aMapWithSize(1));

		Map<Reference, String> values = languageValues.get(null);
		assertThat(values, notNullValue());
		assertThat(values, aMapWithSize(1));
		assertThat(values.keySet(), contains(new Reference(Reference.Type.TEXT, "title")));
		assertThat(values.values(), contains("<b>Big</b> Title"));
	}

	@Test
	public void GivenVariablesFileWithUnknownRoot_WhenLoad_ThenEmpty() {
		// given
		String xml = "" + //
				"<no-variables-definition>" + //
				"	<title>content ignored</title>" + //
				"</no-variables-definition>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		// when
		Variables variables = new Variables();
		variables.load(file);

		// then
		Map<String, Map<Reference, String>> languageValues = variables.getLanguageValues();
		assertThat(languageValues, notNullValue());
		assertThat(languageValues, aMapWithSize(0));
	}

	@Test(expected = WoodException.class)
	public void GivenStringVariablesFileWithRichText_WhenLoad_ThenException() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title><b>Big</b> Title</title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		Variables variables = new Variables();
		variables.load(file);
	}

	@Test
	public void GivenDirWithVariablesFile_WhenReload_ThenVaribleFound() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title>Title</title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		FilePath dir = Mockito.mock(FilePath.class);
		when(dir.iterator()).thenReturn(Arrays.asList(file).iterator());

		Variables variables = new Variables();
		variables.reload(dir);

		Map<String, Map<Reference, String>> languageValues = variables.getLanguageValues();
		assertThat(languageValues, notNullValue());
		assertThat(languageValues, aMapWithSize(1));

		Map<Reference, String> values = languageValues.get(null);
		assertThat(values, notNullValue());
		assertThat(values, aMapWithSize(1));
		assertThat(values.keySet(), contains(new Reference(Reference.Type.STRING, "title")));
		assertThat(values.values(), contains("Title"));
	}

	@Test
	public void GivenStringVariblesFile_WhenGetVariable_ThenExpectedValue() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<string>" + //
				"	<title>Title</title>" + //
				"</string>";

		when(file.getReader()).thenReturn(new StringReader(xml));

		Variables variables = new Variables();
		variables.load(file);

		IReferenceHandler handler = Mockito.mock(IReferenceHandler.class);

		// when
		String value = variables.get(new Reference(Reference.Type.STRING, "title"), file, handler);

		// then
		assertThat(value, notNullValue());
		assertThat(value, equalTo("Title"));
	}

	@Test(expected = WoodException.class)
	public void GivenTwoVariablesFilesWithCircularDependecy_WhenGetVariable_ThenException() {
		// given
		FilePath[] files = new FilePath[] { file(), file() };

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

		FilePath dir = Mockito.mock(FilePath.class);
		when(dir.iterator()).thenReturn(Arrays.asList(files).iterator());

		Variables variables = new Variables(dir);
		IReferenceHandler handler = new IReferenceHandler() {
			@Override
			public String onResourceReference(Reference reference, FilePath sourceFile) throws IOException, WoodException {
				return variables.get(reference, sourceFile, this);
			}
		};

		// when
		variables.get(new Reference(Reference.Type.STRING, "app"), file, handler);
	}

	private FilePath file() {
		FilePath file = Mockito.mock(FilePath.class);
		when(file.isVariables()).thenReturn(true);
		when(file.getVariants()).thenReturn(variants);
		return file;
	}
}
