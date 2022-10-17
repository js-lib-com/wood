package com.jslib.wood.preview;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import com.jslib.wood.IScriptDescriptor;
import com.jslib.wood.Project;
import com.jslib.wood.ThemeStyles;

@RunWith(MockitoJUnitRunner.class)
public class PreviewTest {
	@Mock
	private Project project;
	@Mock
	private Component compo;
	@Mock
	private ThemeStyles theme;

	private Preview preview;

	@Before
	public void beforeTest() {
		preview = new Preview(project, compo, "test", true);
	}

	@Test
	public void serialize() throws IOException, SAXException, XPathExpressionException {
		// project fixture

		when(project.getDefaultLocale()).thenReturn(Locale.ENGLISH);
		when(project.getAuthors()).thenReturn(Arrays.asList("Iulian Rotaru"));
		when(project.getThemeStyles()).thenReturn(theme);

		List<IMetaDescriptor> projectMetas = metas("og:url", "http://kids-cademy.com");
		when(project.getMetaDescriptors()).thenReturn(projectMetas);

		List<ILinkDescriptor> projectStyles = styles("https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css");
		when(project.getLinkDescriptors()).thenReturn(projectStyles);

		List<IScriptDescriptor> projectScripts = scripts("lib/js-lib.js");
		when(project.getScriptDescriptors()).thenReturn(projectScripts);

		FilePath themeReset = Mockito.mock(FilePath.class);
		when(themeReset.value()).thenReturn("theme/reset.css");
		when(theme.getDefaultStyles()).thenReturn(themeReset);

		FilePath themeFx = Mockito.mock(FilePath.class);
		when(themeFx.value()).thenReturn("theme/fx.css");
		when(theme.getAnimations()).thenReturn(themeFx);

		FilePath themeStyle = Mockito.mock(FilePath.class);
		when(themeStyle.value()).thenReturn("theme/form.css");
		when(theme.getStyles()).thenReturn(Arrays.asList(themeStyle));

		// component fixture

		when(compo.getTitle()).thenReturn("Test compo");
		when(compo.getDescription()).thenReturn("Test compo description.");

		List<IMetaDescriptor> compoMetas = metas("og:title", "Test compo");
		when(compo.getMetaDescriptors()).thenReturn(compoMetas);

		List<ILinkDescriptor> compoStyles = styles("http://fonts.googleapis.com/css?family=Lato");
		when(compo.getLinkDescriptors()).thenReturn(compoStyles);

		List<IScriptDescriptor> compoScripts = scripts("https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js");
		when(compo.getScriptDescriptors()).thenReturn(compoScripts);

		FilePath compoStyleFile = Mockito.mock(FilePath.class);
		when(compoStyleFile.value()).thenReturn("compo/compo.css");
		when(compo.getStyleFiles()).thenReturn(Arrays.asList(compoStyleFile));

		String layout = "" + //
				"<body>" + //
				"	<h1>Test Compo</h1>" + //
				"</body>";
		DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);
		when(compo.getLayout()).thenReturn(documentBuilder.parseHTML(layout).getRoot());

		// exercise test
		Writer writer = new StringWriter();
		preview.serialize(writer);

		// verify test
		Document document = documentBuilder.parseXML(writer.toString());
		Element html = document.getByTag("HTML");
		assertThat(html, notNullValue());
		assertThat(html.getAttr("lang"), equalTo("en"));

		Element head = document.getByTag("HEAD");
		assertThat(head, notNullValue());
		EList heads = head.getChildren();
		int index = 0;
		assertHead(heads.item(index++), "meta", "http-equiv", "Content-Type", "content", "text/html; charset=UTF-8");
		assertHead(heads.item(index++), "title", "Test compo");
		assertHead(heads.item(index++), "meta", "name", "Author", "content", "Iulian Rotaru");
		assertHead(heads.item(index++), "meta", "name", "Description", "content", "Test compo description.");
		assertHead(heads.item(index++), "meta", "property", "og:url", "content", "http://kids-cademy.com");
		assertHead(heads.item(index++), "meta", "property", "og:title", "content", "Test compo");
		assertHead(heads.item(index++), "link", "rel", "stylesheet", "href", "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css", "type", "text/css");
		assertHead(heads.item(index++), "link", "rel", "stylesheet", "href", "http://fonts.googleapis.com/css?family=Lato", "type", "text/css");
		assertHead(heads.item(index++), "link", "rel", "stylesheet", "href", "test/theme/reset.css", "type", "text/css");
		assertHead(heads.item(index++), "link", "rel", "stylesheet", "href", "test/theme/fx.css", "type", "text/css");
		assertHead(heads.item(index++), "link", "rel", "stylesheet", "href", "test/theme/form.css", "type", "text/css");
		assertHead(heads.item(index++), "link", "rel", "stylesheet", "href", "test/compo/compo.css", "type", "text/css");
		assertHead(heads.item(index++), "script", "src", null, "type", "text/javascript");
		assertHead(heads.item(index++), "script", "src", "test/lib/js-lib.js", "type", "text/javascript");
		assertHead(heads.item(index++), "script", "src", "https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js", "type", "text/javascript");

		Element body = document.getByTag("BODY");
		assertThat(body, notNullValue());
		assertThat(body.getByTag("H1").getText(), equalTo("Test Compo"));
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

	private static List<IScriptDescriptor> scripts(String... sources) throws IOException {
		List<IScriptDescriptor> scripts = new ArrayList<>();
		for (String source : sources) {
			IScriptDescriptor script = Mockito.mock(IScriptDescriptor.class);
			when(script.getSource()).thenReturn(source);
			scripts.add(script);
		}
		return scripts;
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
