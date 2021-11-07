package js.wood.preview;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.FilePath;
import js.wood.IReferenceHandler;
import js.wood.Project;
import js.wood.Reference;
import js.wood.Variables;
import js.wood.impl.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class VariablesCacheTest {
	@Mock
	private Project project;
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
		when(project.getAssetDir()).thenReturn(assetDir);
		when(project.createVariables(assetDir)).thenReturn(assetVariables);
		when(project.createVariables(sourceDir)).thenReturn(sourceVariables);

		variablesCache = new VariablesCache(project);
	}

	@Test
	public void GivenVariablesCache_WhenUpdate_ThenReloadAssetAndClearMap() {
		// given
		
		// when
		variablesCache.update();
		
		// then
		verify(assetVariables, times(1)).reload(assetDir);
		assertThat(variablesCache.getSourceVariablesMap().size(), equalTo(0));
	}

	@Test
	public void GivenVariableFoundOnSource_WhenGet_ThenAssetNotInvoked() {
		// given
		Reference reference = new Reference(ResourceType.STRING, "title");
		FilePath source = mock(FilePath.class);
		when(source.getParentDir()).thenReturn(sourceDir);
		when(sourceVariables.get(Locale.ENGLISH, reference, source, referenceHandler)).thenReturn("Compo Title");

		// when
		String value = variablesCache.get(Locale.ENGLISH, reference, source, referenceHandler);
		
		// then
		assertThat(value, equalTo("Compo Title"));
		verify(sourceVariables, times(1)).get(Locale.ENGLISH, reference, source, referenceHandler);
		verify(assetVariables, times(0)).get(Locale.ENGLISH, reference, source, referenceHandler);
	}

	@Test
	public void GivenVariableNotFoundOnSource_WhenGet_ThenAssetInvoked() {
		// given
		Reference reference = new Reference(ResourceType.STRING, "title");
		FilePath source = mock(FilePath.class);
		when(source.getParentDir()).thenReturn(sourceDir);
		when(assetVariables.get(Locale.ENGLISH, reference, source, referenceHandler)).thenReturn("Asset Title");

		// when
		String value = variablesCache.get(Locale.ENGLISH, reference, source, referenceHandler);
		
		// then
		assertThat(value, equalTo("Asset Title"));
		verify(sourceVariables, times(1)).get(Locale.ENGLISH, reference, source, referenceHandler);
		verify(assetVariables, times(1)).get(Locale.ENGLISH, reference, source, referenceHandler);
	}

	@Test
	public void GivenVariableNotFoundOnSourceOrAsset_WhenGet_ThenNull() {
		// given
		Reference reference = new Reference(ResourceType.STRING, "title");
		FilePath source = mock(FilePath.class);
		when(source.getParentDir()).thenReturn(sourceDir);

		// when
		String value = variablesCache.get(Locale.ENGLISH, reference, source, referenceHandler);
		
		// then
		assertThat(value, nullValue());
		verify(sourceVariables, times(1)).get(Locale.ENGLISH, reference, source, referenceHandler);
		verify(assetVariables, times(1)).get(Locale.ENGLISH, reference, source, referenceHandler);
	}
}
