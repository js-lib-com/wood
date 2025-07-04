package com.jslib.wood.build;

import com.jslib.wood.*;
import com.jslib.wood.dom.Document;
import com.jslib.wood.dom.Element;
import com.jslib.wood.impl.FileType;
import com.jslib.wood.impl.XmlnsOperatorsHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

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
	public void beforeTest() {
		when(project.getVariables(null)).thenReturn(dirVariables);
		when(project.getAssetVariables()).thenReturn(assetVariables);

		builder = new Builder(project, buildFS);
		builder.setLanguage("en");
	}

	@Test
	public void build() throws IOException, XPathExpressionException {
		when(project.getProjectRoot()).thenReturn(new File("test"));
		when(project.getLanguages()).thenReturn(Collections.singletonList("en"));
		when(project.getOperatorsHandler()).thenReturn(new XmlnsOperatorsHandler());
		when(project.getPwaManifest()).thenReturn(Mockito.mock(FilePath.class));
		when(project.getPwaLoader()).thenReturn(Mockito.mock(FilePath.class));
		when(project.getPwaWorker()).thenReturn(Mockito.mock(FilePath.class));
		when(project.getFavicon()).thenReturn(Mockito.mock(FilePath.class));
		when(project.getThemeStyles()).thenReturn(Mockito.mock(ThemeStyles.class));

		FilePath layoutPath = Mockito.mock(FilePath.class);
		when(layoutPath.exists()).thenReturn(true);
		when(layoutPath.isLayout()).thenReturn(true);
		when(layoutPath.getProject()).thenReturn(project);

		FilePath descriptorPath = Mockito.mock(FilePath.class);
		when(layoutPath.cloneTo(FileType.XML)).thenReturn(descriptorPath);
		when(layoutPath.cloneTo(FileType.STYLE)).thenReturn(Mockito.mock(FilePath.class));
		when(descriptorPath.cloneTo(FileType.SCRIPT)).thenReturn(Mockito.mock(FilePath.class));
		when(layoutPath.getReader()).thenReturn(new StringReader("<body><h1>Test Page</h1></body>"));

		CompoPath compoPath = Mockito.mock(CompoPath.class);
		when(project.getPages()).thenReturn(Collections.singletonList(compoPath));
		when(compoPath.getLayoutPath()).thenReturn(layoutPath);

		builder.build();

		when(project.isMultiLanguage()).thenReturn(true);
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
		assertThat(head.getChildren().size(), equalTo(2));

		Element body = document.getByTag("body");
		assertThat(body, notNullValue());
		assertThat(body.getByTag("h1").getText(), equalTo("Test Page"));
	}

	@Test
	public void onResourceReference_DirVariables() throws IOException {
		Reference reference = new Reference(Reference.Type.STRING, "title");
		FilePath source = Mockito.mock(FilePath.class);
		when(dirVariables.get("en", reference, source, builder)).thenReturn("Page Title");

		String value = builder.onResourceReference(reference, source);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("Page Title"));
	}

	@Test
	public void onResourceReference_AssetVariables() throws IOException {
		Reference reference = new Reference(Reference.Type.STRING, "title");
		FilePath source = Mockito.mock(FilePath.class);
		when(assetVariables.get("en", reference, source, builder)).thenReturn("Project Title");

		String value = builder.onResourceReference(reference, source);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("Project Title"));
	}

	@Test(expected = WoodException.class)
	public void onResourceReference_MissingVariables() throws IOException {
		Reference reference = new Reference(Reference.Type.STRING, "title");
		FilePath source = Mockito.mock(FilePath.class);
		builder.onResourceReference(reference, source);
	}

	@Test
	public void onResourceReference_LayoutMedia() throws IOException {
		Reference reference = new Reference(Reference.Type.IMAGE, "icon");
		FilePath source = Mockito.mock(FilePath.class);
		when(source.getType()).thenReturn(FileType.LAYOUT);

		FilePath mediaFile = Mockito.mock(FilePath.class);
		when(project.getResourceFile("en", reference, source)).thenReturn(mediaFile);
		when(buildFS.writePageMedia(null, mediaFile)).thenReturn("../media/icon.png");

		String value = builder.onResourceReference(reference, source);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("../media/icon.png"));
	}

	@Test
	public void onResourceReference_StyleMedia() throws IOException {
		Reference reference = new Reference(Reference.Type.IMAGE, "icon");
		FilePath source = Mockito.mock(FilePath.class);
		when(source.getType()).thenReturn(FileType.STYLE);

		FilePath mediaFile = Mockito.mock(FilePath.class);
		when(project.getResourceFile("en", reference, source)).thenReturn(mediaFile);
		when(buildFS.writeStyleMedia(mediaFile)).thenReturn("../media/icon.png");

		String value = builder.onResourceReference(reference, source);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("../media/icon.png"));
	}

	@Test
	public void onResourceReference_BadSourceForMedia() throws IOException {
		Reference reference = new Reference(Reference.Type.IMAGE, "icon");
		FilePath source = Mockito.mock(FilePath.class);
		when(source.getType()).thenReturn(FileType.XML);

		FilePath mediaFile = Mockito.mock(FilePath.class);
		when(project.getResourceFile("en", reference, source)).thenReturn(mediaFile);

		assertThat(builder.onResourceReference(reference, source), nullValue());
	}

	@Test
	public void onResourceReference_ScriptMedia() throws IOException {
		Reference reference = new Reference(Reference.Type.IMAGE, "icon");
		FilePath source = Mockito.mock(FilePath.class);
		when(source.getType()).thenReturn(FileType.SCRIPT);

		FilePath mediaFile = Mockito.mock(FilePath.class);
		when(project.getResourceFile("en", reference, source)).thenReturn(mediaFile);
		when(buildFS.writeScriptMedia(mediaFile)).thenReturn("../media/icon.png");

		String value = builder.onResourceReference(reference, source);
		assertThat(value, notNullValue());
		assertThat(value, equalTo("../media/icon.png"));
	}

	@Test(expected = WoodException.class)
	public void onResourceReference_MissingMedia() throws IOException {
		Reference reference = new Reference(Reference.Type.IMAGE, "icon");
		FilePath source = Mockito.mock(FilePath.class);
		builder.onResourceReference(reference, source);
	}
}
