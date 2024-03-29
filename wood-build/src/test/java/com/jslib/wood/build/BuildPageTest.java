package com.jslib.wood.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import com.jslib.api.dom.Document;
import com.jslib.api.dom.DocumentBuilder;
import com.jslib.api.dom.EList;
import com.jslib.api.dom.Element;
import com.jslib.util.Classes;
import com.jslib.wood.Component;
import com.jslib.wood.FilePath;
import com.jslib.wood.ILinkDescriptor;
import com.jslib.wood.IMetaDescriptor;
import com.jslib.wood.IReferenceHandler;
import com.jslib.wood.IScriptDescriptor;
import com.jslib.wood.SourceReader;
import com.jslib.wood.ThemeStyles;

@RunWith(MockitoJUnitRunner.class)
public class BuildPageTest {
	@Mock
	private BuilderProject project;
	@Mock
	private Component page;
	@Mock
	private BuildFS buildFS;

	@Mock
	private FilePath pwaManifest;
	@Mock
	private FilePath pwaLoader;
	@Mock
	private FilePath pwaWorker;
	@Mock
	private FilePath favicon;
	@Mock
	private ThemeStyles theme;

	@Mock
	private IReferenceHandler referenceHandler;

	private Builder builder;

	@Before
	public void beforeTest() throws IOException {
		when(project.getAuthors()).thenReturn(Arrays.asList("Iulian Rotaru"));
		when(project.getPwaManifest()).thenReturn(pwaManifest);
		when(project.getPwaLoader()).thenReturn(pwaLoader);
		when(project.getFavicon()).thenReturn(favicon);
		when(project.getThemeStyles()).thenReturn(theme);

		//when(page.getProject()).thenReturn(project);
		when(page.getTitle()).thenReturn("Test Page");
		when(page.getDescription()).thenReturn("Test page description.");

		builder = new Builder(project, buildFS);
	}

	@Test
	public void buildPage() throws IOException, SAXException, XPathExpressionException {
		// project fixture

		when(pwaManifest.exists()).thenReturn(true);
		SourceReader reader = mock(SourceReader.class);
		when(pwaManifest.getReader()).thenReturn(reader);
		when(buildFS.writePwaManifest(any(SourceReader.class))).thenReturn("manifest.json");

		when(favicon.exists()).thenReturn(true);
		when(buildFS.writeFavicon(any(Component.class), eq(favicon))).thenReturn("favicon.ico");

		List<IMetaDescriptor> projectMetas = metas("og:url", "http://kids-cademy.com");
		when(project.getMetaDescriptors()).thenReturn(projectMetas);

		List<ILinkDescriptor> projectStyles = styles("https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css");
		when(project.getLinkDescriptors()).thenReturn(projectStyles);

		List<IScriptDescriptor> projectScripts = scripts("lib/js-lib.js");
		when(project.getScriptDescriptors()).thenReturn(projectScripts);
		when(buildFS.writeScript(any(Component.class), any(SourceReader.class))).thenReturn("script/js-lib.js");

		FilePath themeReset = Mockito.mock(FilePath.class);
		when(theme.getDefaultStyles()).thenReturn(themeReset);
		when(buildFS.writeStyle(any(Component.class), eq(themeReset), any(IReferenceHandler.class))).thenReturn("styles/reset.css");

		FilePath themeFx = Mockito.mock(FilePath.class);
		when(theme.getAnimations()).thenReturn(themeFx);
		when(buildFS.writeStyle(any(Component.class), eq(themeFx), any(IReferenceHandler.class))).thenReturn("styles/fx.css");

		FilePath themeStyle = Mockito.mock(FilePath.class);
		when(theme.getStyles()).thenReturn(Arrays.asList(themeStyle));
		when(buildFS.writeStyle(any(Component.class), eq(themeStyle), any(IReferenceHandler.class))).thenReturn("styles/form.css");

		// page fixture

		List<IMetaDescriptor> pageMetas = metas("og:title", "Test Page");
		when(page.getMetaDescriptors()).thenReturn(pageMetas);

		List<ILinkDescriptor> pageStyles = styles("http://fonts.googleapis.com/css?family=Lato");
		when(page.getLinkDescriptors()).thenReturn(pageStyles);

		List<IScriptDescriptor> pageScripts = scripts("https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js");
		when(page.getScriptDescriptors()).thenReturn(pageScripts);

		FilePath pageStyleFile = Mockito.mock(FilePath.class);
		when(page.getStyleFiles()).thenReturn(Arrays.asList(pageStyleFile));
		when(buildFS.writeStyle(any(Component.class), eq(pageStyleFile), any(IReferenceHandler.class))).thenReturn("styles/page.css");

		String layout = "" + //
				"<body>" + //
				"	<h1>Test Page</h1>" + //
				"</body>";
		DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);
		when(page.getLayout()).thenReturn(documentBuilder.parseHTML(layout).getRoot());

		// exercise

		builder.setLanguage("en");
		builder.buildPage(page);

		// assert results

		ArgumentCaptor<Component> componentArgument = ArgumentCaptor.forClass(Component.class);
		ArgumentCaptor<Document> documentArgument = ArgumentCaptor.forClass(Document.class);
		verify(buildFS).writePage(componentArgument.capture(), documentArgument.capture());

		Component component = componentArgument.getValue();
		assertThat(component, notNullValue());
		Document document = documentArgument.getValue();
		assertThat(document, notNullValue());

		Element html = document.getByTag("html");
		assertThat(html, notNullValue());
		assertThat(html.getAttr("lang"), equalTo("en"));

		Element head = document.getByTag("head");
		assertThat(head, notNullValue());
		EList heads = head.getChildren();
		int index = 0;
		assertHead(heads.item(index++), "meta", "http-equiv", "Content-Type", "content", "text/html; charset=UTF-8");
		assertHead(heads.item(index++), "title", "Test Page");
		assertHead(heads.item(index++), "meta", "name", "Description", "content", "Test page description.");
		assertHead(heads.item(index++), "meta", "name", "Author", "content", "Iulian Rotaru");
		assertHead(heads.item(index++), "meta", "property", "og:url", "content", "http://kids-cademy.com");
		assertHead(heads.item(index++), "meta", "property", "og:title", "content", "Test Page");
		assertHead(heads.item(index++), "link", "rel", "manifest", "href", "manifest.json");
		assertHead(heads.item(index++), "link", "rel", "shortcut icon", "href", "favicon.ico", "type", "image/x-icon");
		assertHead(heads.item(index++), "link", "rel", "stylesheet", "href", "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css", "type", "text/css");
		assertHead(heads.item(index++), "link", "rel", "stylesheet", "href", "http://fonts.googleapis.com/css?family=Lato", "type", "text/css");
		assertHead(heads.item(index++), "link", "rel", "stylesheet", "href", "styles/reset.css", "type", "text/css");
		assertHead(heads.item(index++), "link", "rel", "stylesheet", "href", "styles/fx.css", "type", "text/css");
		assertHead(heads.item(index++), "link", "rel", "stylesheet", "href", "styles/form.css", "type", "text/css");
		assertHead(heads.item(index++), "link", "rel", "stylesheet", "href", "styles/page.css", "type", "text/css");
		assertHead(heads.item(index++), "script", "src", "script/js-lib.js", "type", "text/javascript");
		assertHead(heads.item(index++), "script", "src", "https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js", "type", "text/javascript");

		Element body = document.getByTag("body");
		assertThat(body, notNullValue());
		assertThat(body.getByTag("h1").getText(), equalTo("Test Page"));
	}

	// --------------------------------------------------------------------------------------------

	private List<IScriptDescriptor> scripts(String... sources) throws IOException {
		List<IScriptDescriptor> scripts = new ArrayList<>();
		for (String source : sources) {
			IScriptDescriptor script = Mockito.mock(IScriptDescriptor.class);
			when(script.getSource()).thenReturn(source);
			if (FilePath.accept(source)) {
				FilePath path = Mockito.mock(FilePath.class);
				when(path.exists()).thenReturn(true);
				Reader reader = Mockito.mock(Reader.class);
				when(path.getReader()).thenReturn(reader);
				when(project.createFilePath(source)).thenReturn(path);
			}
			scripts.add(script);
		}
		return scripts;
	}

	// --------------------------------------------------------------------------------------------

	private static void assertHead(Element element, String tag, String textContent) {
		assertThat(element, notNullValue());
		assertThat(element.getTag(), equalTo(tag));
	}

	private static void assertHead(Element element, String tag, String attribute1, String value1, String attribute2, String value2) {
		assertThat(element, notNullValue());
		assertThat(element.getTag(), equalTo(tag));
		assertThat(element.getAttr(attribute1), equalTo(value1));
		assertThat(element.getAttr(attribute2), equalTo(value2));
	}

	private static void assertHead(Element element, String tag, String attribute1, String value1, String attribute2, String value2, String attribute3, String value3) {
		assertThat(element, notNullValue());
		assertThat(element.getTag(), equalTo(tag));
		assertThat(element.getAttr(attribute1), equalTo(value1));
		assertThat(element.getAttr(attribute2), equalTo(value2));
		assertThat(element.getAttr(attribute3), equalTo(value3));
	}

	private static List<IMetaDescriptor> metas(String property, String content) {
		IMetaDescriptor meta = Mockito.mock(IMetaDescriptor.class);
		when(meta.getProperty()).thenReturn(property);
		when(meta.getContent()).thenReturn(content);
		return Arrays.asList(meta);
	}

	private static List<ILinkDescriptor> styles(String... sources) {
		List<ILinkDescriptor> styles = new ArrayList<>();
		for (String source : sources) {
			ILinkDescriptor style = Mockito.mock(ILinkDescriptor.class);
			when(style.getRelationship()).thenReturn("stylesheet");
			when(style.getHref()).thenReturn(source);
			styles.add(style);
		}
		return styles;
	}
}
