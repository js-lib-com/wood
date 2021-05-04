package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import js.dom.Document;
import js.dom.Element;
import js.wood.impl.FileType;
import js.wood.impl.ResourceType;
import js.wood.impl.XmlnsOperatorsHandler;

@RunWith(MockitoJUnitRunner.class)
public class BuilderTest {
	@Mock
	private BuilderProject project;
	@Mock
	private BuildFS buildFS;
	@Mock
	private Variables dirVariables;
	@Mock
	private Variables assetVariables;

	private Builder builder;

	@Before
	public void beforeTest() throws IOException {
		Map<DirPath, Variables> variables = new HashMap<>();
		variables.put(null, dirVariables);
		when(project.getVariables()).thenReturn(variables);
		when(project.getAssetVariables()).thenReturn(assetVariables);

		builder = new Builder(project, buildFS);
		builder.setLocale(Locale.ENGLISH);
	}

	@Test
	public void build() throws IOException, XPathExpressionException {
		when(project.getLocales()).thenReturn(Arrays.asList(Locale.ENGLISH));
		when(project.getOperatorsHandler()).thenReturn(new XmlnsOperatorsHandler());
		when(project.getManifest()).thenReturn(Mockito.mock(FilePath.class));
		when(project.getServiceWorker()).thenReturn(Mockito.mock(FilePath.class));
		when(project.getFavicon()).thenReturn(Mockito.mock(FilePath.class));
		when(project.getThemeStyles()).thenReturn(Mockito.mock(ThemeStyles.class));

		FilePath layoutPath = Mockito.mock(FilePath.class);
		when(layoutPath.exists()).thenReturn(true);
		when(layoutPath.isLayout()).thenReturn(true);
		when(layoutPath.getProject()).thenReturn(project);
		when(layoutPath.cloneTo(FileType.XML)).thenReturn(Mockito.mock(FilePath.class));
		when(layoutPath.cloneTo(FileType.STYLE)).thenReturn(Mockito.mock(FilePath.class));
		when(layoutPath.getReader()).thenReturn(new StringReader("<body><h1>Test Page</h1></body>"));

		CompoPath compoPath = Mockito.mock(CompoPath.class);
		when(project.getPages()).thenReturn(Arrays.asList(compoPath));
		when(compoPath.getLayoutPathEx()).thenReturn(layoutPath);

		builder.build();

		when(project.isMultiLocale()).thenReturn(true);
		when(layoutPath.getReader()).thenReturn(new StringReader("<body><h1>Test Page</h1></body>"));
		builder.build();

		ArgumentCaptor<Component> componentArgument = ArgumentCaptor.forClass(Component.class);
		ArgumentCaptor<Document> documentArgument = ArgumentCaptor.forClass(Document.class);
		verify(buildFS, times(2)).writePage(componentArgument.capture(), documentArgument.capture());

		Component component = componentArgument.getValue();
		assertThat(component, notNullValue());
		Document document = documentArgument.getValue();
		assertThat(document, notNullValue());

		Element html = document.getByTag("html");
		assertThat(html, notNullValue());
		assertThat(html.getAttr("lang"), equalTo("en"));

		Element head = document.getByTag("head");
		assertThat(head, notNullValue());
		assertThat(head.getChildren().size(), equalTo(3));

		Element body = document.getByTag("body");
		assertThat(body, notNullValue());
		assertThat(body.getByTag("h1").getText(), equalTo("Test Page"));
	}

	@Test
	public void onResourceReference_DirVariables() throws IOException {
		Reference reference = new Reference(ResourceType.STRING, "title");
		FilePath source = Mockito.mock(FilePath.class);
		when(dirVariables.get(Locale.ENGLISH, reference, source, builder)).thenReturn("Page Title");

		String value = builder.onResourceReference(reference, source);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("Page Title"));
	}

	@Test
	public void onResourceReference_AssetVariables() throws IOException {
		Reference reference = new Reference(ResourceType.STRING, "title");
		FilePath source = Mockito.mock(FilePath.class);
		when(assetVariables.get(Locale.ENGLISH, reference, source, builder)).thenReturn("Project Title");

		String value = builder.onResourceReference(reference, source);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("Project Title"));
	}

	@Test(expected = WoodException.class)
	public void onResourceReference_MissingVariables() throws IOException {
		Reference reference = new Reference(ResourceType.STRING, "title");
		FilePath source = Mockito.mock(FilePath.class);
		builder.onResourceReference(reference, source);
	}

	@Test
	public void onResourceReference_LayoutMedia() throws IOException {
		Reference reference = new Reference(ResourceType.IMAGE, "icon");
		FilePath source = Mockito.mock(FilePath.class);
		when(source.getType()).thenReturn(FileType.LAYOUT);

		FilePath mediaFile = Mockito.mock(FilePath.class);
		when(project.getMediaFile(Locale.ENGLISH, reference, source)).thenReturn(mediaFile);
		when(buildFS.writePageMedia(null, mediaFile)).thenReturn("../media/icon.png");

		String value = builder.onResourceReference(reference, source);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("../media/icon.png"));
	}

	@Test
	public void onResourceReference_StyleMedia() throws IOException {
		Reference reference = new Reference(ResourceType.IMAGE, "icon");
		FilePath source = Mockito.mock(FilePath.class);
		when(source.getType()).thenReturn(FileType.STYLE);

		FilePath mediaFile = Mockito.mock(FilePath.class);
		when(project.getMediaFile(Locale.ENGLISH, reference, source)).thenReturn(mediaFile);
		when(buildFS.writeStyleMedia(mediaFile)).thenReturn("../media/icon.png");

		String value = builder.onResourceReference(reference, source);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("../media/icon.png"));
	}

	@Test
	public void onResourceReference_BadSourceForMedia() throws IOException {
		Reference reference = new Reference(ResourceType.IMAGE, "icon");
		FilePath source = Mockito.mock(FilePath.class);
		when(source.getType()).thenReturn(FileType.XML);

		FilePath mediaFile = Mockito.mock(FilePath.class);
		when(project.getMediaFile(Locale.ENGLISH, reference, source)).thenReturn(mediaFile);

		assertThat(builder.onResourceReference(reference, source), nullValue());
	}

	@Test
	public void onResourceReference_ScriptMedia() throws IOException {
		Reference reference = new Reference(ResourceType.IMAGE, "icon");
		FilePath source = Mockito.mock(FilePath.class);
		when(source.getType()).thenReturn(FileType.SCRIPT);

		FilePath mediaFile = Mockito.mock(FilePath.class);
		when(project.getMediaFile(Locale.ENGLISH, reference, source)).thenReturn(mediaFile);
		when(buildFS.writeScriptMedia(mediaFile)).thenReturn("../media/icon.png");

		String value = builder.onResourceReference(reference, source);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("../media/icon.png"));
	}

	@Test(expected = WoodException.class)
	public void onResourceReference_MissingMedia() throws IOException {
		Reference reference = new Reference(ResourceType.IMAGE, "icon");
		FilePath source = Mockito.mock(FilePath.class);
		builder.onResourceReference(reference, source);
	}
}
