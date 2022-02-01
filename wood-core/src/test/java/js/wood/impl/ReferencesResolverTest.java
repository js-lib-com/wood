package js.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.FilePath;
import js.wood.IReferenceHandler;
import js.wood.Reference;

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
	public void parse() {
		String value = "<h1>@string/title</h1>";

		value = resolver.parse(value, sourceFile, new IReferenceHandler() {
			@Override
			public String onResourceReference(Reference reference, FilePath sourceFile) throws IOException {
				return "resource value";
			}
		});

		assertThat(value, equalTo("<h1>resource value</h1>"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullValue() {
		resolver.parse(null, sourceFile, null);
	}
}
