package com.jslib.wood.impl;

import com.jslib.wood.FilePath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReferencesResolverTest {
	@Mock
	private FilePath sourceFile;
	@Mock
	private FilePath varFile;

	private ReferencesResolver resolver;

	@Before
	public void beforeTest() {
		when(sourceFile.cloneTo(FileType.VAR)).thenReturn(varFile);
		when(varFile.isSynthetic()).thenReturn(true);
		resolver = new ReferencesResolver();
	}

	@Test
	public void GivenStringReference_WhenParse_ThenValueInjected() {
		// GIVEN
		String value = "<h1>@string/title</h1>";

		// WHEN
		value = resolver.parse(value, sourceFile, (reference, sourceFile) -> "resource value");

		// THEN
		assertThat(value, equalTo("<h1>resource value</h1>"));
	}

	@Test(expected = AssertionError.class)
	public void GivenNullValue_WhenParse_ThenAssertionError() {
		resolver.parse(null, sourceFile, null);
	}
}
