package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.impl.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class VariablesCacheTest {
	@Mock
	private Project project;
	@Mock
	private Factory factory;
	@Mock
	private FilePath assetDir;
	@Mock
	private Variables assetVariables;
	@Mock
	private FilePath sourceDir;
	@Mock
	private Variables sourceVariables;

	@Mock
	private IReferenceHandler referenceHandler;

	private VariablesCache variablesCache;

	@Before
	public void beforeTest() {
		when(project.getFactory()).thenReturn(factory);
		when(project.getAssetsDir()).thenReturn(assetDir);

		when(factory.createVariables(assetDir)).thenReturn(assetVariables);
		when(factory.createVariables(sourceDir)).thenReturn(sourceVariables);

		variablesCache = new VariablesCache(project);
	}

	@Test
	public void update() {
		variablesCache.update();
		verify(assetVariables, times(1)).reload(assetDir);
		assertThat(variablesCache.getVariablesMap().size(), equalTo(0));
	}

	@Test
	public void get() {
		Reference reference = new Reference(ResourceType.STRING, "title");
		FilePath source = mock(FilePath.class);
		when(source.getParentDir()).thenReturn(sourceDir);
		when(sourceVariables.get(Locale.ENGLISH, reference, source, referenceHandler)).thenReturn("Compo Title");

		String value = variablesCache.get(Locale.ENGLISH, reference, source, referenceHandler);
		assertThat(value, equalTo("Compo Title"));
		verify(sourceVariables, times(1)).get(Locale.ENGLISH, reference, source, referenceHandler);
		verify(assetVariables, times(0)).get(Locale.ENGLISH, reference, source, referenceHandler);
	}

	@Test
	public void get_Asset() {
		Reference reference = new Reference(ResourceType.STRING, "title");
		FilePath source = mock(FilePath.class);
		when(source.getParentDir()).thenReturn(sourceDir);
		when(assetVariables.get(Locale.ENGLISH, reference, source, referenceHandler)).thenReturn("Asset Title");

		String value = variablesCache.get(Locale.ENGLISH, reference, source, referenceHandler);
		assertThat(value, equalTo("Asset Title"));
		verify(sourceVariables, times(1)).get(Locale.ENGLISH, reference, source, referenceHandler);
		verify(assetVariables, times(1)).get(Locale.ENGLISH, reference, source, referenceHandler);
	}

	@Test
	public void get_NotFound() {
		Reference reference = new Reference(ResourceType.STRING, "title");
		FilePath source = mock(FilePath.class);
		when(source.getParentDir()).thenReturn(sourceDir);

		String value = variablesCache.get(Locale.ENGLISH, reference, source, referenceHandler);
		assertThat(value, nullValue());
		verify(sourceVariables, times(1)).get(Locale.ENGLISH, reference, source, referenceHandler);
		verify(assetVariables, times(1)).get(Locale.ENGLISH, reference, source, referenceHandler);
	}
}
