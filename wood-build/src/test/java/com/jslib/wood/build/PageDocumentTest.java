package com.jslib.wood.build;

import com.jslib.wood.*;
import com.jslib.wood.dom.DocumentBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PageDocumentTest {
    @Mock
    private Component compo;

    private PageDocument page;

    @Before
    public void beforeTest() throws SAXException {
        DocumentBuilder builder = DocumentBuilder.getInstance();
        when(compo.getLayout()).thenReturn(builder.parseXML("<body><h1>page</h1></body>").getRoot());

        page = new PageDocument(compo);
    }

    @Test
    public void GivenSettersValues_WhenInvokeSetters_ThenHeaderElementsCreated() {
        // GIVEN
        String language = "ro-RO";
        String contentType = "text/html; charset=UTF-8";
        String title = "title";
        String author = "author";
        String description = "description";

        // WHEN
        page.setLanguage(language);
        page.setContentType(contentType);
        page.setTitle(title);
        page.setAuthors(Collections.singletonList(author));
        page.setDescription(description);

        // THEN
        String[] doc = page.getDocument().stringify().split("\r\n");

        int index = 0;
        assertThat(doc[index++], equalTo("<!DOCTYPE html>"));
        assertThat(doc[index++], equalTo("<HTML lang=\"ro-RO\">"));
        assertThat(doc[index++], equalTo("\t<HEAD>"));
        assertThat(doc[index++], equalTo("\t\t<META content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\" />"));
        assertThat(doc[index++], equalTo("\t\t<TITLE>title</TITLE>"));
        assertThat(doc[index++], equalTo("\t\t<META content=\"author\" name=\"Author\" />"));
        assertThat(doc[index++], equalTo("\t\t<META content=\"description\" name=\"Description\" />"));
        assertThat(doc[index++], equalTo("\t</HEAD>"));
        assertThat(doc[index++], equalTo("\t<BODY>"));
        assertThat(doc[index++], equalTo("\t\t<H1>page</H1>"));
        assertThat(doc[index++], equalTo("\t</BODY>"));
        assertThat(doc[index], equalTo("</HTML>"));
    }

    @Test
    public void GivenMetaProperty_WhenAddMeta_ThenMetaElementCreated() {
        // GIVEN
        IMetaDescriptor meta = mock(IMetaDescriptor.class);
        when(meta.getProperty()).thenReturn("og:title");
        when(meta.getContent()).thenReturn("kids (a)cademy");

        // WHEN
        page.addMeta(meta);

        // THEN
        String doc = page.getDocument().stringify();
        assertThat(doc, containsString("<META content=\"kids (a)cademy\" property=\"og:title\" />"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void GivenNullMetaDescriptor_WhenAddMeta_ThenAssertionError() {
        // GIVEN
        IMetaDescriptor meta = null;

        // WHEN
        page.addMeta(meta);

        // THEN
    }

    @Test
    public void GivenExternalLinkUrl_WhenAddLink_ThenLinkElementCreated() {
        // GIVEN
        ILinkDescriptor link = mock(ILinkDescriptor.class);
        when(link.getHref()).thenReturn("http://js-lib.com/styles/reset.css");

        // WHEN
        page.addLink(link, FilePath::value);

        // THEN
        String doc = page.getDocument().stringify();
        assertThat(doc, containsString("<LINK href=\"http://js-lib.com/styles/reset.css\" rel=\"stylesheet\" type=\"text/css\" />"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void GivenNullLinkDescriptor_WhenAddLink_Then() {
        // GIVEN
        ILinkDescriptor link = null;

        // WHEN
        page.addLink(link, FilePath::value);

        // THEN
    }

    @Test(expected = AssertionError.class)
    public void GivenNullLinkHref_WhenAddLink_ThenAssertionError() {
        // GIVEN
        ILinkDescriptor link = mock(ILinkDescriptor.class);
        when(link.getHref()).thenReturn(null);

        // WHEN
        page.addLink(link, FilePath::value);

        // THEN
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void GivenNullHrefArgument_WhenAddStyle_ThenAssertionError() {
        // GIVEN
        String href = null;

        // WHEN
        page.addStyle(href);

        // THEN
    }

    @Test
    public void GivenScriptDefer_WhenAddScript_ThenDeferAttributeCreated() {
        // GIVEN
        IScriptDescriptor script = mock(IScriptDescriptor.class);
        when(script.getSource()).thenReturn("script/index.js");
        when(script.getDefer()).thenReturn("true");

        // WHEN
        page.addScript(script, "script/index.js");

        // THEN
        String doc = page.getDocument().stringify();
        assertThat(doc, containsString("<SCRIPT defer=\"true\" src=\"script/index.js\" type=\"text/javascript\"></SCRIPT>"));
    }

    @Test
    public void GivenEmbeddedScript_WhenAddScript_ThenScriptElementCreated() {
        // GIVEN
        IScriptDescriptor script = mock(IScriptDescriptor.class);
        when(script.isEmbedded()).thenReturn(true).thenReturn(true);
        when(script.getSource()).thenReturn("lib/sdk/analytics.js");

        // WHEN
        page.addScript(script, "lib/sdk/analytics.js", "alert('hello world!');");

        // THEN
        String doc = page.getDocument().stringify();
        assertThat(doc, containsString("<SCRIPT type=\"text/javascript\">alert('hello world!');</SCRIPT>"));
    }

    @Test
    public void GivenExternalScriptUrl_WhenAddScript_ThenScriptElementCreated() {
        // GIVEN
        IScriptDescriptor script = mock(IScriptDescriptor.class);
        when(script.getSource()).thenReturn("http://js-lib.com/sdk/analytics.js");

        // WHEN
        page.addScript(script, "http://js-lib.com/sdk/analytics.js");

        // THEN
        String doc = page.getDocument().stringify();
        assertThat(doc, containsString("<SCRIPT src=\"http://js-lib.com/sdk/analytics.js\" type=\"text/javascript\"></SCRIPT>"));
    }

    @Test
    public void GivenLocalScriptPath_WhenAddScript_ThenScriptElementCreated() {
        // GIVEN
        IScriptDescriptor script = mock(IScriptDescriptor.class);
        when(script.getSource()).thenReturn("script/index.js");

        // WHEN
        page.addScript(script, "/context/script/index.js");

        // THEN
        String doc = page.getDocument().stringify();
        assertThat(doc, containsString("<SCRIPT src=\"/context/script/index.js\" type=\"text/javascript\"></SCRIPT>"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void GivenNullScriptDescriptor_WhenAddScript_ThenAssertionError() {
        // GIVEN
        IScriptDescriptor scriptDescriptor = null;

        // WHEN
        page.addScript(scriptDescriptor, "");

        // THEN
    }

    @Test(expected = AssertionError.class)
    public void GivenMissingScriptSource_WhenAddScript_ThenAssertionError() {
        // GIVEN
        IScriptDescriptor script = mock(IScriptDescriptor.class);

        // WHEN
        page.addScript(script, null);

        // THEN
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void GivenNullRelativeSource_WhenAddScript_ThenAssertionError() {
        // GIVEN
        IScriptDescriptor script = mock(IScriptDescriptor.class);
        when(script.getSource()).thenReturn("script/index.js");

        // WHEN
        page.addScript(script, null);

        // THEN
    }
}
