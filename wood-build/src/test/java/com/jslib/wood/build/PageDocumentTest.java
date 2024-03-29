package com.jslib.wood.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

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
import com.jslib.util.Classes;
import com.jslib.wood.Component;
import com.jslib.wood.FilePath;
import com.jslib.wood.ILinkDescriptor;
import com.jslib.wood.IMetaDescriptor;
import com.jslib.wood.IScriptDescriptor;

@RunWith(MockitoJUnitRunner.class)
public class PageDocumentTest {
	@Mock
	private BuilderProject project;
	@Mock
	private Component compo;

	private PageDocument page;

	@Before
	public void beforeTest() throws IOException, SAXException {
		DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
		when(compo.getProject()).thenReturn(project);
		when(compo.getLayout()).thenReturn(builder.parseXML("<body><h1>page</h1></body>").getRoot());

		page = new PageDocument(compo);
	}

	@Test
	public void setters() throws IOException, XPathExpressionException {
		IMetaDescriptor meta = Mockito.mock(IMetaDescriptor.class);
		when(meta.getHttpEquiv()).thenReturn("X-UA-Compatible");
		when(meta.getContent()).thenReturn("IE=9; IE=8; IE=7; IE=EDGE");

		ILinkDescriptor link = Mockito.mock(ILinkDescriptor.class);
		when(link.getHref()).thenReturn("styles/reset.css");

		IScriptDescriptor script = Mockito.mock(IScriptDescriptor.class);
		when(script.getSource()).thenReturn("script/index.js");

		FilePath scriptPath = new FilePath(project, script.getSource());
		when(project.createFilePath(script.getSource())).thenReturn(scriptPath);

		page.setLanguage("ro-RO");
		page.setContentType("text/html; charset=UTF-8");
		page.setTitle("title");
		page.setAuthors(Arrays.asList("author"));
		page.setDescription("description");
		page.addMeta(meta);
		page.addFavicon("media/favicon.ico");
		page.addLink(link, filePath -> filePath.value());
		page.addStyle("style/index.css");
		page.addScript(script, filePath -> filePath.value());

		String[] doc = stringify(page.getDocument()).split("\r\n");

		int index = 0;
		assertThat(doc[index++], equalTo("<!DOCTYPE html>"));
		assertThat(doc[index++], equalTo("<HTML lang=\"ro-RO\">"));
		assertThat(doc[index++], equalTo("\t<HEAD>"));
		assertThat(doc[index++], equalTo("\t\t<META content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\" />"));
		assertThat(doc[index++], equalTo("\t\t<TITLE>title</TITLE>"));
		assertThat(doc[index++], equalTo("\t\t<META content=\"author\" name=\"Author\" />"));
		assertThat(doc[index++], equalTo("\t\t<META content=\"description\" name=\"Description\" />"));
		assertThat(doc[index++], equalTo("\t\t<META content=\"IE=9; IE=8; IE=7; IE=EDGE\" http-equiv=\"X-UA-Compatible\" />"));
		assertThat(doc[index++], equalTo("\t\t<LINK href=\"media/favicon.ico\" rel=\"shortcut icon\" type=\"image/x-icon\" />"));
		assertThat(doc[index++], equalTo("\t\t<LINK href=\"styles/reset.css\" rel=\"stylesheet\" type=\"text/css\" />"));
		assertThat(doc[index++], equalTo("\t\t<LINK href=\"style/index.css\" rel=\"stylesheet\" type=\"text/css\" />"));
		assertThat(doc[index++], equalTo("\t\t<SCRIPT src=\"script/index.js\" type=\"text/javascript\"></SCRIPT>"));
		assertThat(doc[index++], equalTo("\t</HEAD>"));
		assertThat(doc[index++], equalTo("\t<BODY>"));
		assertThat(doc[index++], equalTo("\t\t<H1>page</H1>"));
		assertThat(doc[index++], equalTo("\t</BODY>"));
		assertThat(doc[index++], equalTo("</HTML>"));
	}

	@Test
	public void addMeta_Property() throws IOException {
		IMetaDescriptor meta = Mockito.mock(IMetaDescriptor.class);
		when(meta.getProperty()).thenReturn("og:title");
		when(meta.getContent()).thenReturn("kids (a)cademy");

		page.addMeta(meta);
		String doc = stringify(page.getDocument());
		assertThat(doc, containsString("<META content=\"kids (a)cademy\" property=\"og:title\" />"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addMeta_NullMetaReference() {
		page.addMeta(null);
	}

	@Test
	public void addLink_ThirdParty() throws IOException {
		ILinkDescriptor link = Mockito.mock(ILinkDescriptor.class);
		when(link.getHref()).thenReturn("http://js-lib.com/styles/reset.css");

		page.addLink(link, filePath -> filePath.value());
		String doc = stringify(page.getDocument());
		assertThat(doc, containsString("<LINK href=\"http://js-lib.com/styles/reset.css\" rel=\"stylesheet\" type=\"text/css\" />"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addLink_NullLinkReference() {
		page.addLink(null, filePath -> filePath.value());
	}

	@Test(expected = IllegalArgumentException.class)
	public void addLink_MissingHref() throws IOException {
		ILinkDescriptor link = Mockito.mock(ILinkDescriptor.class);
		when(link.getHref()).thenReturn(null);
		page.addLink(link, filePath -> filePath.value());
	}

	@Test(expected = IllegalArgumentException.class)
	public void addStyle_NullHref() {
		page.addStyle(null);
	}

	@Test
	public void addScript_Defer() throws IOException, XPathExpressionException {
		IScriptDescriptor script = Mockito.mock(IScriptDescriptor.class);
		when(script.getSource()).thenReturn("script/index.js");
		when(script.getDefer()).thenReturn("true");

		FilePath scriptPath = new FilePath(project, script.getSource());
		when(project.createFilePath(script.getSource())).thenReturn(scriptPath);

		page.addScript(script, filePath -> filePath.value());
		String doc = stringify(page.getDocument());
		assertThat(doc, containsString("<SCRIPT defer=\"true\" src=\"script/index.js\" type=\"text/javascript\"></SCRIPT>"));
	}

	@Test
	public void addScript_Embedded() throws IOException, XPathExpressionException {
		IScriptDescriptor script = Mockito.mock(IScriptDescriptor.class);
		when(script.isEmbedded()).thenReturn(true).thenReturn(true);
		when(script.getSource()).thenReturn("lib/sdk/analytics.js");

		when(project.loadFile("lib/sdk/analytics.js")).thenReturn("alert('hello world!');");

		page.addScript(script, filePath -> null);
		String doc = stringify(page.getDocument());
		assertThat(doc, containsString("<SCRIPT type=\"text/javascript\">alert('hello world!');</SCRIPT>"));
	}

	@Test
	public void addScript_External() throws IOException, XPathExpressionException {
		IScriptDescriptor script = Mockito.mock(IScriptDescriptor.class);
		when(script.getSource()).thenReturn("http://js-lib.com/sdk/analytics.js");

		page.addScript(script, filePath -> null);
		String doc = stringify(page.getDocument());
		assertThat(doc, containsString("<SCRIPT src=\"http://js-lib.com/sdk/analytics.js\" type=\"text/javascript\"></SCRIPT>"));
	}

	@Test
	public void addScript_UrlAbsolutePath() throws IOException, XPathExpressionException {
		IScriptDescriptor script = Mockito.mock(IScriptDescriptor.class);
		when(script.getSource()).thenReturn("script/index.js");

		FilePath scriptPath = new FilePath(project, script.getSource());
		when(project.createFilePath(script.getSource())).thenReturn(scriptPath);

		page.addScript(script, filePath -> "/context/" + filePath.value());
		String doc = stringify(page.getDocument());
		assertThat(doc, containsString("<SCRIPT src=\"/context/script/index.js\" type=\"text/javascript\"></SCRIPT>"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addScript_NullScriptReference() throws IOException, XPathExpressionException {
		page.addScript(null, filePath -> null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addScript_MissingSource() throws IOException, XPathExpressionException {
		IScriptDescriptor script = Mockito.mock(IScriptDescriptor.class);
		page.addScript(script, filePath -> null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addScript_NullHandler() throws IOException, XPathExpressionException {
		IScriptDescriptor script = Mockito.mock(IScriptDescriptor.class);
		when(script.getSource()).thenReturn("script/index.js");
		page.addScript(script, null);
	}

	private static String stringify(Document document) throws IOException {
		StringWriter writer = new StringWriter();
		document.serialize(writer);
		return writer.toString();
	}
}
