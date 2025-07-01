package com.jslib.wood;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.util.List;

import com.jslib.wood.dom.EList;
import com.jslib.wood.dom.Element;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.wood.impl.AttrOperatorsHandler;
import com.jslib.wood.impl.DataAttrOperatorsHandler;
import com.jslib.wood.impl.FileType;
import com.jslib.wood.impl.XmlnsOperatorsHandler;

/**
 * Components inheritance via templates. Fixture contains mocks for a component and a hierarchy of two templates:
 * <code>page</code> and </code>template</code>.
 *
 * @author Iulian Rotaru
 * @since 1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class ComponentInheritanceTest {
    @Mock
    private Project project;

    @Mock
    private FilePath pageLayout; // layout file for page template
    @Mock
    private FilePath pageDescriptor;
    @Mock
    private FilePath pageStyle; // style file for page template
    @Mock
    private FilePath pageScript; // script file for page template
    @Mock
    private CompoPath pageCompo; // path to page editable element, declared on template

    @Mock
    private FilePath templateLayout; // layout file for template component
    @Mock
    private FilePath templateDescriptor;
    @Mock
    private FilePath templateStyle; // style file for template component
    @Mock
    private FilePath templateScript; // script file for template component
    @Mock
    private CompoPath templateCompo; // path to template editable element, declared on component

    @Mock
    private CompoPath compoPath;
    @Mock
    private FilePath compoLayout; // layout file for component
    @Mock
    private FilePath compoDescriptor;
    @Mock
    private FilePath compoStyle; // style file for component
    @Mock
    private FilePath compoScript; // script file for component

    @Mock
    private IReferenceHandler referenceHandler;

    @Before
    public void beforeTest() {
        when(project.getTitle()).thenReturn("Components");
        when(project.hasNamespace()).thenReturn(true);
        when(project.getOperatorsHandler()).thenReturn(new XmlnsOperatorsHandler());

        when(pageLayout.exists()).thenReturn(true);
        when(pageLayout.isLayout()).thenReturn(true);
        when(pageLayout.cloneTo(FileType.XML)).thenReturn(pageDescriptor);
        when(pageLayout.cloneTo(FileType.STYLE)).thenReturn(pageStyle);
        when(pageDescriptor.cloneTo(FileType.SCRIPT)).thenReturn(pageScript);
        when(pageCompo.getLayoutPath()).thenReturn(pageLayout);

        when(templateLayout.exists()).thenReturn(true);
        when(templateLayout.isLayout()).thenReturn(true);
        when(templateLayout.cloneTo(FileType.XML)).thenReturn(templateDescriptor);
        when(templateLayout.cloneTo(FileType.STYLE)).thenReturn(templateStyle);
        when(templateDescriptor.cloneTo(FileType.SCRIPT)).thenReturn(templateScript);
        when(templateCompo.getLayoutPath()).thenReturn(templateLayout);

        when(compoPath.getLayoutPath()).thenReturn(compoLayout);

        when(compoLayout.getProject()).thenReturn(project);
        when(compoLayout.exists()).thenReturn(true);
        when(compoLayout.isLayout()).thenReturn(true);
        when(compoLayout.cloneTo(FileType.XML)).thenReturn(compoDescriptor);
        when(compoLayout.cloneTo(FileType.STYLE)).thenReturn(compoStyle);
        when(compoDescriptor.cloneTo(FileType.SCRIPT)).thenReturn(compoScript);
    }

    @Test
    public void GivenSingleEditableAndShortNotation_ThenContentMerged() {
        // given
        String templateHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section w:editable='section'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<section w:template='res/template#section' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Content</h1>" + //
                "</section>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getTag(), equalTo("body"));

        assertThat(layout.getByTag("section"), notNullValue());

        EList headings = layout.findByTag("h1");
        assertThat(headings.size(), equalTo(2));
        assertThat(headings.item(0).getText(), equalTo("Template"));
        assertThat(headings.item(1).getText(), equalTo("Content"));
    }

    @Test
    public void GivenSingleEditableAndShortNotation_ThenOperatorsRemoved() {
        // given
        String templateHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section w:editable='section'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<section w:template='res/template#section' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Content</h1>" + //
                "</section>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();

        Element section = layout.getByTag("section");
        assertFalse(section.hasAttrNS(WOOD.NS, "editable"));
        assertFalse(section.hasAttrNS(WOOD.NS, "template"));
        assertFalse(section.hasAttr("xmlns:w"));
    }

    @Test
    public void GivenSingleEditableAndShortNotation_ThenClassMerged() {
        // given
        String templateHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section class='section' w:editable='section'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<section class='content' w:template='res/template#section' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Content</h1>" + //
                "</section>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertFalse(layout.hasAttr("xmlns:w"));

        Element section = layout.getByTag("section");
        assertTrue(section.hasCssClass("section"));
        assertTrue(section.hasCssClass("content"));
    }

    @Test
    public void GivenSingleEditableAndShortNotation_ThenTemplateClassNotChanged() {
        // given
        String templateHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section class='section' w:editable='section'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<section class='content' w:template='res/template#section' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Content</h1>" + //
                "</section>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertFalse(layout.hasAttr("xmlns:w"));

        assertFalse(layout.hasCssClass("section"));
        assertFalse(layout.hasCssClass("content"));
    }

    @Test
    public void GivenSingleEditableAndStandardNotation_ThenContentMerged() {
        // given
        String templateHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section w:editable='section'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<embed w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<section w:content='section'>" + //
                "		<h1>Content</h1>" + //
                "	</section>" + //
                "</embed>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getTag(), equalTo("body"));
        assertThat(layout.getByTag("embed"), nullValue());
        assertFalse(layout.hasAttr("xmlns:w"));

        EList headings = layout.findByTag("h1");
        assertThat(headings.size(), equalTo(2));
        assertThat(headings.item(0).getText(), equalTo("Template"));
        assertThat(headings.item(1).getText(), equalTo("Content"));
    }

    @Test
    public void GivenSingleEditableAndStandardNotation_ThenOperatorsRemoved() {
        // given
        String templateHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section w:editable='section'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<embed w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<section w:content='section'>" + //
                "		<h1>Content</h1>" + //
                "	</section>" + //
                "</embed>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        Element section = layout.getByTag("section");
        assertThat(section, notNullValue());
        assertFalse(section.hasAttrNS(WOOD.NS, "editable"));
        assertFalse(section.hasAttrNS(WOOD.NS, "template"));
        assertFalse(section.hasAttr("xmlns:w"));
    }

    @Test
    public void GivenEmptyTemplate_ThenCopyContent() {
        // given
        String templateHTML = "<article>" + //
                "</article>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<article w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<section>" + //
                "		<h1>Content</h1>" + //
                "	</section>" + //
                "</article>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();

        EList headings = layout.findByTag("h1");
        assertThat(headings.size(), equalTo(1));
        assertThat(headings.item(0).getText(), equalTo("Content"));

        assertThat(layout.getByTag("section"), notNullValue());
    }

    @Test
    public void GivenEmptyTemplateOnTagCompo_ThenCopyContent() {
        // given
        String templateHTML = "<tag>" + //
                "</tag>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<tag w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<section>" + //
                "		<h1>Content</h1>" + //
                "	</section>" + //
                "</tag>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();

        EList headings = layout.findByTag("h1");
        assertThat(headings.size(), equalTo(1));
        assertThat(headings.item(0).getText(), equalTo("Content"));

        assertThat(layout.getByTag("section"), notNullValue());
    }

    @Test
    public void GivenEmptyTemplateAndAttributes_ThenMergeAttributes() {
        // given
        String templateHTML = "<article name='base article' class='article'>" + //
                "</article>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<article name='article' class='news' w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<section>" + //
                "		<h1>Content</h1>" + //
                "	</section>" + //
                "</article>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getTag(), equalTo("article"));
        assertThat(layout.getAttr("class"), containsString("article"));
        assertThat(layout.getAttr("class"), containsString("new"));
        assertThat(layout.getAttr("name"), equalTo("article"));
    }

    @Test
    public void GivenEmptyTemplate_ThenOperatorRemoved() {
        // given
        String templateHTML = "<article>" + //
                "</article>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<article w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<section>" + //
                "		<h1>Content</h1>" + //
                "	</section>" + //
                "</article>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertFalse(layout.hasAttr("xmlns:w"));
        assertFalse(layout.hasAttrNS(WOOD.NS, "template"));
    }

    @Test
    public void GivenEmptyListTemplate_ThenCopyItems() {
        // given
        String templateHTML = "<ul>" + //
                "</ul>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<ul w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<li>Item <b>#1</b></li>" + //
                "	<li></li>" + //
                "	<li>Item <b>#2</b></li>" + //
                "</ul>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getTag(), equalTo("ul"));

        EList items = layout.findByTag("li");
        assertThat(items, notNullValue());
        assertThat(items.size(), equalTo(3));
    }

    @Test
    public void GivenEmptyListTemplateWithAttributes_ThenMergeAttributes() {
        // given
        String templateHTML = "<ul class='menu'>" + //
                "</ul>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<ul id='main' w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<li>Item <b>#1</b></li>" + //
                "	<li></li>" + //
                "	<li>Item <b>#2</b></li>" + //
                "</ul>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getAttr("class"), equalTo("menu"));
        assertThat(layout.getAttr("id"), equalTo("main"));
    }

    @Test
    public void GivenEmptyListTemplate_ThenOperatorRemoved() {
        // given
        String templateHTML = "<ul class='menu'>" + //
                "</ul>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<ul id='main' w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<li class='selected'>Item <b>#1</b></li>" + //
                "	<li class='divider'></li>" + //
                "	<li>Item <b>#2</b></li>" + //
                "</ul>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertFalse(layout.hasAttr("xmlns:w"));
        assertFalse(layout.hasAttrNS(WOOD.NS, "template"));
    }

    @Test
    public void GivenSingleInlineTemplate_ThenInjectContent() {
        // given
        String templateHTML = "<article xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section w:editable='section'></section>" + //
                "</article>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<section w:template='res/template#section'>" + //
                "		<h1>Content</h1>" + //
                "	</section>" + //
                "</body>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getTag(), equalTo("body"));

        EList headings = layout.findByTag("h1");
        assertThat(headings.size(), equalTo(2));
        assertThat(headings.item(0).getText(), equalTo("Template"));
        assertThat(headings.item(1).getText(), equalTo("Content"));

        assertThat(layout.getByTag("article"), notNullValue());
        assertThat(layout.getByTag("section"), notNullValue());
    }

    @Test
    public void GivenSingleInlineTemplate_ThenMergeAttributes() {
        // given
        String templateHTML = "<article xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section class='template' w:editable='section'></section>" + //
                "</article>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<section data-list='list' class='compo' w:template='res/template#section'>" + //
                "		<h1>Content</h1>" + //
                "	</section>" + //
                "</body>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        Element section = layout.getByTag("section");
        assertThat(section.getAttr("data-list"), equalTo("list"));
        assertThat(section.hasCssClass("template"), equalTo(true));
        assertThat(section.hasCssClass("compo"), equalTo(true));
    }

    @Test
    public void GivenSingleInlineTemplateAndAttributeCollision_ThenParentTakesPrecedence() {
        // given
        String templateHTML = "<article xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section id='list' w:editable='section'></section>" + //
                "</article>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<section id='section' w:template='res/template#section'>" + //
                "		<h1>Content</h1>" + //
                "	</section>" + //
                "</body>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        Element section = layout.getByTag("section");
        assertThat(section.getAttr("id"), equalTo("section"));
    }

    @Test
    public void GivenSingleInlineTemplateWithFormalNotation_ThenMergeAttributes() {
        // given
        String templateHTML = "<article xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section class='template' w:editable='section'></section>" + //
                "</article>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<embed w:template='res/template'>" + //
                "		<section data-list='list' class='compo' w:content='section'>" + //
                "			<h1>Content</h1>" + //
                "		</section>" + //
                "	</embed>" + //
                "</body>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        Element section = layout.getByTag("section");
        assertThat(section.getAttr("data-list"), equalTo("list"));
        assertThat(section.hasCssClass("template"), equalTo(true));
        assertThat(section.hasCssClass("compo"), equalTo(true));
    }

    @Test
    public void GivenSingleInlineTemplateWithFormalNotationAndRootAttributes_ThenMergeAttributes() {
        // given
        String templateHTML = "<article name='article' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section class='template' w:editable='section'></section>" + //
                "</article>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<embed name='embed' w:template='res/template'>" + //
                "		<section data-list='list' class='compo' w:content='section'>" + //
                "			<h1>Content</h1>" + //
                "		</section>" + //
                "	</embed>" + //
                "</body>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);
        Element layout = compo.getLayout();

        // then
        Element article = layout.getByTag("article");
        assertThat(article.getAttr("name"), equalTo("embed"));

        Element section = layout.getByTag("section");
        assertThat(section.getAttr("data-list"), equalTo("list"));
        assertThat(section.hasCssClass("template"), equalTo(true));
        assertThat(section.hasCssClass("compo"), equalTo(true));
    }

    @Test
    public void GivenSingleInlineTemplate_ThenOperatorsRemoved() {
        // given
        String templateHTML = "<article xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section w:editable='section'></section>" + //
                "</article>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<section w:template='res/template#section'>" + //
                "		<h1>Content</h1>" + //
                "	</section>" + //
                "</body>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertFalse(layout.hasAttr("xmlns:w"));

        Element article = layout.getByTag("article");
        assertFalse(article.hasAttr("xmlns:w"));

        Element section = layout.getByTag("section");
        assertFalse(section.hasAttrNS(WOOD.NS, "editable"));
        assertFalse(section.hasAttrNS(WOOD.NS, "template"));
    }

    @Test
    public void GivenSingleEditbaleAndInlineTemplating_ThenTemplateChromeInclude() {
        // given
        String pageHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Page</h1>" + //
                "	<article w:editable='article'></article>" + //
                "</body>";
        when(pageLayout.getReader()).thenReturn(new StringReader(pageHTML));
        when(project.createCompoPath("res/template/page")).thenReturn(pageCompo);

        String templateHTML = "<chapter xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Chapter</h1>" + //
                "	<section w:editable='section'></section>" + //
                "</chapter>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));
        when(project.createCompoPath("res/template/chapter")).thenReturn(templateCompo);

        String compoHTML = "<article w:template='res/template/page#article' xmlns:w='js-lib.com/wood'>" + //
                "	<section w:template='res/template/chapter#section'>" + //
                "		<h1>Section</h1>" + //
                "	</section>" + //
                "</article>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getTag(), equalTo("body"));

        EList headings = layout.findByTag("h1");
        assertThat(headings.size(), equalTo(3));
        assertThat(headings.item(0).getText(), equalTo("Page"));
        assertThat(headings.item(1).getText(), equalTo("Chapter"));
        assertThat(headings.item(2).getText(), equalTo("Section"));

        assertThat(layout.getByTag("article"), notNullValue());
        assertThat(layout.getByTag("chapter"), notNullValue());
        assertThat(layout.getByTag("section"), notNullValue());
    }

    @Test
    public void GivenSingleEditbaleAndInlineTemplating_ThenOperatorsRemoved() {
        // given
        String pageHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Page</h1>" + //
                "	<article w:editable='article'></article>" + //
                "</body>";
        when(pageLayout.getReader()).thenReturn(new StringReader(pageHTML));
        when(project.createCompoPath("res/template/page")).thenReturn(pageCompo);

        String templateHTML = "<chapter xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Chapter</h1>" + //
                "	<section w:editable='section'></section>" + //
                "</chapter>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));
        when(project.createCompoPath("res/template/chapter")).thenReturn(templateCompo);

        String compoHTML = "<article w:template='res/template/page#article' xmlns:w='js-lib.com/wood'>" + //
                "	<section w:template='res/template/chapter#section'>" + //
                "		<h1>Section</h1>" + //
                "	</section>" + //
                "</article>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertFalse(layout.hasAttr("xmlns:w"));

        Element article = layout.getByTag("article");
        assertFalse(article.hasAttr("xmlns:w"));
        assertFalse(article.hasAttrNS(WOOD.NS, "template"));

        Element chapter = layout.getByTag("chapter");
        assertFalse(chapter.hasAttr("xmlns:w"));
        assertFalse(chapter.hasAttrNS(WOOD.NS, "template"));

        Element section = layout.getByTag("section");
        assertFalse(section.hasAttrNS(WOOD.NS, "editable"));
        assertFalse(section.hasAttrNS(WOOD.NS, "template"));
    }

    @Test
    public void GivenParameterizedTemplate_ThenParametersInjected() {
        // given
        String templateHTML = "<body title='@param/caption' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>@param/title</h1>" + //
                "	<section w:editable='section'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<section w:template='res/template#section' w:param='caption:Compo Caption;title:Compo Title' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Content</h1>" + //
                "</section>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getAttr("title"), equalTo("Compo Caption"));
        assertThat(layout.getByTag("h1").getText(), equalTo("Compo Title"));

        assertThat(layout.getByTag("section"), notNullValue());
    }

    @Test
    public void GivenParameterizedTemplate_ThenOperatorsRemoved() {
        // given
        String templateHTML = "<body title='@param/caption' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>@param/title</h1>" + //
                "	<section w:editable='section'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<section w:template='res/template#section' w:param='caption:Compo Caption;title:Compo Title' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Content</h1>" + //
                "</section>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        Element section = layout.getByTag("section");
        assertFalse(section.hasAttrNS(WOOD.NS, "editable"));
        assertFalse(section.hasAttrNS(WOOD.NS, "template"));
        assertFalse(section.hasAttrNS(WOOD.NS, "param"));
    }

    @Test(expected = WoodException.class)
    public void GivenParameterMissing_ThenWoodException() {
        // given
        String templateHTML = "<body title='@param/caption' xmlns:w='js-lib.com/wood'>" + //
                "	<section w:editable='section'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<section w:template='res/template#section' w:param='title:Compo Title' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Content</h1>" + //
                "</section>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        new Component(compoPath, referenceHandler, true);

        // then
    }

    /**
     * Component with template and multiple implementations for the same editable element.
     */
    @Test
    public void GivenRepeatingContent_ThenAllMerged() {
        // given
        String templateHTML = "<form xmlns:w='js-lib.com/wood'>" + //
                "	<fieldset w:editable='fieldset'></fieldset>" + //
                "	<button>Submit</button>" + //
                "</form>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<embed type='text/html' w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<fieldset w:content='fieldset'>" + //
                "		<input name='user-name' />" + //
                "	</fieldset>" + //
                " " + //
                "	<fieldset w:content='fieldset'>" + //
                "		<input name='address' />" + //
                "	</fieldset>" + //
                " " + //
                "	<fieldset w:content='fieldset'>" + //
                "		<input name='password' />" + //
                "	</fieldset>" + //
                "</embed>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getTag(), equalTo("form"));
        assertThat(layout.getByTag("embed"), nullValue());

        assertThat(layout.findByTag("fieldset").size(), equalTo(3));

        EList inputs = layout.findByTag("input");
        assertThat(inputs.size(), equalTo(3));
        assertThat(inputs.item(0).getAttr("name"), equalTo("user-name"));
        assertThat(inputs.item(1).getAttr("name"), equalTo("address"));
        assertThat(inputs.item(2).getAttr("name"), equalTo("password"));
    }

    @Test
    public void GivenRepeatingContent_ThenOperatorsRemoved() {
        // given
        String templateHTML = "<form xmlns:w='js-lib.com/wood'>" + //
                "	<fieldset w:editable='fieldset'></fieldset>" + //
                "	<button>Submit</button>" + //
                "</form>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<embed type='text/html' w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<fieldset w:content='fieldset'>" + //
                "		<input name='user-name' />" + //
                "	</fieldset>" + //
                " " + //
                "	<fieldset w:content='fieldset'>" + //
                "		<input name='address' />" + //
                "	</fieldset>" + //
                " " + //
                "	<fieldset w:content='fieldset'>" + //
                "		<input name='password' />" + //
                "	</fieldset>" + //
                "</embed>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertFalse(layout.hasAttr("xmlns:w"));

        EList fieldsets = layout.findByTag("fieldset");
        for (int i = 0; i < fieldsets.size(); ++i) {
            assertFalse(fieldsets.item(i).hasAttrNS(WOOD.NS, "editable"));
            assertFalse(fieldsets.item(i).hasAttrNS(WOOD.NS, "content"));
        }
    }

    /**
     * Template with two editable sections. Component inherits from template and implements both editables.
     */
    @Test
    public void GivenTwoEditables_ThenBothContentsMerged() {
        // given
        String templateHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section w:editable='section-1'></section>" + //
                "	<section w:editable='section-2'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<embed type='text/html' w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<section w:content='section-1'>" + //
                "		<h1>Content One</h1>" + //
                "	</section>" + //
                " " + //
                "	<section w:content='section-2'>" + //
                "		<h1>Content Two</h1>" + //
                "	</section>" + //
                "</embed>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getTag(), equalTo("body"));
        assertThat(layout.getByTag("embed"), nullValue());

        EList headings = layout.findByTag("h1");
        assertThat(headings.size(), equalTo(3));
        assertThat(headings.item(0).getText(), equalTo("Template"));
        assertThat(headings.item(1).getText(), equalTo("Content One"));
        assertThat(headings.item(2).getText(), equalTo("Content Two"));

        assertThat(layout.findByTag("section").size(), equalTo(2));
    }

    @Test
    public void GivenTwoEditablesAndDataAttrOperators_ThenBothContentsMerged() {
        // given
        when(project.getOperatorsHandler()).thenReturn(new DataAttrOperatorsHandler());

        String templateHTML = "<body>" + //
                "	<h1>Template</h1>" + //
                "	<section data-editable='section-1'></section>" + //
                "	<section data-editable='section-2'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<embed type='text/html' data-template='res/template'>" + //
                "	<section data-content='section-1'>" + //
                "		<h1>Content One</h1>" + //
                "	</section>" + //
                " " + //
                "	<section data-content='section-2'>" + //
                "		<h1>Content Two</h1>" + //
                "	</section>" + //
                "</embed>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getTag(), equalTo("body"));
        assertThat(layout.getByTag("embed"), nullValue());

        EList headings = layout.findByTag("h1");
        assertThat(headings.size(), equalTo(3));
        assertThat(headings.item(0).getText(), equalTo("Template"));
        assertThat(headings.item(1).getText(), equalTo("Content One"));
        assertThat(headings.item(2).getText(), equalTo("Content Two"));

        assertThat(layout.findByTag("section").size(), equalTo(2));
    }

    @Test
    public void GivenTwoEditables_ThenOperatorsRemoved() {
        // given
        String templateHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section w:editable='section-1'></section>" + //
                "	<section w:editable='section-2'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<embed type='text/html' w:template='res/template' xmlns:w='js-lib.com/wood'>" + //
                "	<section w:content='section-1'>" + //
                "		<h1>Content One</h1>" + //
                "	</section>" + //
                " " + //
                "	<section w:content='section-2'>" + //
                "		<h1>Content Two</h1>" + //
                "	</section>" + //
                "</embed>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertFalse(layout.hasAttr("xmlns:w"));

        EList sections = layout.findByTag("section");
        for (int i = 0; i < sections.size(); ++i) {
            assertFalse(sections.item(i).hasAttrNS(WOOD.NS, "editable"));
            assertFalse(sections.item(i).hasAttrNS(WOOD.NS, "content"));
        }
    }

    @Test
    public void GivenTwoEditablesInlineTemplate_ThenBothContentsMerged() {
        // given
        String templateHTML = "<article xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section w:editable='section-1'></section>" + //
                "	<section w:editable='section-2'></section>" + //
                "</article>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Page</h1>" + //
                "	<article type='text/html' w:template='res/template'>" + //
                "		<section w:content='section-1'>" + //
                "			<h1>Content One</h1>" + //
                "		</section>" + //
                " " + //
                "		<section w:content='section-2'>" + //
                "			<h1>Content Two</h1>" + //
                "		</section>" + //
                "	</article>" + //
                "</body>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getTag(), equalTo("body"));
        assertThat(layout.getByTag("embed"), nullValue());

        EList headings = layout.findByTag("h1");
        assertThat(headings.size(), equalTo(4));
        assertThat(headings.item(0).getText(), equalTo("Page"));
        assertThat(headings.item(1).getText(), equalTo("Template"));
        assertThat(headings.item(2).getText(), equalTo("Content One"));
        assertThat(headings.item(3).getText(), equalTo("Content Two"));

        assertThat(layout.findByTag("section").size(), equalTo(2));
    }

    @Test
    public void GivenTwoEditablesInlineTemplate_ThenAttributesMerged() {
        // given
        String templateHTML = "<article name='template' class='template' xmlns:w='js-lib.com/wood'>" + //
                "	<section w:editable='section-1'></section>" + //
                "	<section w:editable='section-2'></section>" + //
                "</article>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<article name='compo' id='top' class='compo' data-cfg='select:true' w:template='res/template'>" + //
                "		<section w:content='section-1'></section>" + //
                "		<section w:content='section-2'></section>" + //
                "	</article>" + //
                "</body>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);
        Element layout = compo.getLayout();

        // then
        Element article = layout.getByTag("article");
        assertThat(article.getAttr("name"), equalTo("compo"));
        assertThat(article.getAttr("id"), equalTo("top"));
        assertThat(article.getAttr("data-cfg"), equalTo("select:true"));
        assertThat(article.hasCssClass("template"), equalTo(true));
        assertThat(article.hasCssClass("compo"), equalTo(true));
    }

    @Test
    public void GivenTwoEditablesAndOneInlineTemplate_ThenAllThreeContentsMerged() {
        // given
        String pageHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section w:editable='section-1'></section>" + //
                "	<section w:editable='section-2'></section>" + //
                "</body>";
        when(pageLayout.getReader()).thenReturn(new StringReader(pageHTML));
        when(project.createCompoPath("res/template/page")).thenReturn(pageCompo);

        String templateHTML = "<chapter xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Chapter</h1>" + //
                "	<section class='chapter' name='chapter-1' title='chapter one' w:editable='section'></section>" + //
                "</chapter>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));
        when(project.createCompoPath("res/template/chapter")).thenReturn(templateCompo);

        String compoHTML = "<embed type='text/html' w:template='res/template/page' xmlns:w='js-lib.com/wood'>" + //
                "	<section w:content='section-1'>" + //
                "		<h1>Content One</h1>" + //
                "		<section id='section-1' class='one' title='section one' w:template='res/template/chapter#section'>" + //
                "			<h1>Section</h1>" + //
                "		</section>" + //
                "	</section>" + //
                " " + //
                "	<section w:content='section-2'>" + //
                "		<h1>Content Two</h1>" + //
                "	</section>" + //
                "</embed>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));

        // when
        Component compo = new Component(compoPath, referenceHandler, true);
        Element layout = compo.getLayout();

        // then
        assertThat(layout.getTag(), equalTo("body"));
        assertThat(layout.getByTag("embed"), nullValue());

        EList headings = layout.findByTag("h1");
        assertThat(headings.size(), equalTo(5));
        assertThat(headings.item(0).getText(), equalTo("Template"));
        assertThat(headings.item(1).getText(), equalTo("Content One"));
        assertThat(headings.item(2).getText(), equalTo("Chapter"));
        assertThat(headings.item(3).getText(), equalTo("Section"));
        assertThat(headings.item(4).getText(), equalTo("Content Two"));

        EList sections = layout.findByTag("section");
        assertThat(sections.size(), equalTo(3));

        Element chapter = sections.item(1).getParent();
        assertThat(chapter.getTag(), equalTo("chapter"));

        assertThat(sections.item(1).getAttr("class"), equalTo("chapter one"));
        assertThat(sections.item(1).getAttr("id"), equalTo("section-1"));
        assertThat(sections.item(1).getAttr("name"), equalTo("chapter-1"));
        assertThat(sections.item(1).getAttr("title"), equalTo("section one"));
    }

    @Test
    public void GivenTwoEditablesAndOneInlineTemplateAndDataAttrOperators_ThenAllThreeContentsMerged() {
        // given
        when(project.getOperatorsHandler()).thenReturn(new DataAttrOperatorsHandler());

        String pageHTML = "<body>" + //
                "	<h1>Template</h1>" + //
                "	<section data-editable='section-1'></section>" + //
                "	<section data-editable='section-2'></section>" + //
                "</body>";
        when(pageLayout.getReader()).thenReturn(new StringReader(pageHTML));
        when(project.createCompoPath("res/template/page")).thenReturn(pageCompo);

        String templateHTML = "<chapter>" + //
                "	<h1>Chapter</h1>" + //
                "	<section class='chapter' name='chapter-1' title='chapter one' data-editable='section'></section>" + //
                "</chapter>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));
        when(project.createCompoPath("res/template/chapter")).thenReturn(templateCompo);

        String compoHTML = "<embed type='text/html' data-template='res/template/page'>" + //
                "	<section data-content='section-1'>" + //
                "		<h1>Content One</h1>" + //
                "		<section id='section-1' class='one' title='section one' data-template='res/template/chapter#section'>" + //
                "			<h1>Section</h1>" + //
                "		</section>" + //
                "	</section>" + //
                " " + //
                "	<section data-content='section-2'>" + //
                "		<h1>Content Two</h1>" + //
                "	</section>" + //
                "</embed>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getTag(), equalTo("body"));
        assertThat(layout.getByTag("embed"), nullValue());

        EList headings = layout.findByTag("h1");
        assertThat(headings.size(), equalTo(5));
        assertThat(headings.item(0).getText(), equalTo("Template"));
        assertThat(headings.item(1).getText(), equalTo("Content One"));
        assertThat(headings.item(2).getText(), equalTo("Chapter"));
        assertThat(headings.item(3).getText(), equalTo("Section"));
        assertThat(headings.item(4).getText(), equalTo("Content Two"));

        EList sections = layout.findByTag("section");
        assertThat(sections.size(), equalTo(3));

        Element chapter = sections.item(1).getParent();
        assertThat(chapter.getTag(), equalTo("chapter"));

        assertThat(sections.item(1).getAttr("class"), equalTo("chapter one"));
        assertThat(sections.item(1).getAttr("id"), equalTo("section-1"));
        assertThat(sections.item(1).getAttr("name"), equalTo("chapter-1"));
        assertThat(sections.item(1).getAttr("title"), equalTo("section one"));
    }

    @Test
    public void GivenTwoEditablesAndOneInlineTemplate_ThenOperatorsRemoved() {
        // given
        String pageHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section w:editable='section-1'></section>" + //
                "	<section w:editable='section-2'></section>" + //
                "</body>";
        when(pageLayout.getReader()).thenReturn(new StringReader(pageHTML));
        when(project.createCompoPath("res/template/page")).thenReturn(pageCompo);

        String templateHTML = "<chapter xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Chapter</h1>" + //
                "	<section class='chapter' name='chapter-1' w:editable='section'></section>" + //
                "</chapter>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));
        when(project.createCompoPath("res/template/chapter")).thenReturn(templateCompo);

        String compoHTML = "<embed type='text/html' w:template='res/template/page' xmlns:w='js-lib.com/wood'>" + //
                "	<section w:content='section-1'>" + //
                "		<h1>Content One</h1>" + //
                "		<section id='section-1' class='one' w:template='res/template/chapter#section'>" + //
                "			<h1>Section</h1>" + //
                "		</section>" + //
                "	</section>" + //
                " " + //
                "	<section w:content='section-2'>" + //
                "		<h1>Content Two</h1>" + //
                "	</section>" + //
                "</embed>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertFalse(layout.hasAttr("xmlns:w"));

        EList sections = layout.findByTag("section");
        Element chapter = sections.item(1).getParent();
        assertFalse(chapter.hasAttr("xmlns:w"));

        assertFalse(sections.item(0).hasAttrNS(WOOD.NS, "editable"));
        assertFalse(sections.item(0).hasAttrNS(WOOD.NS, "content"));
        assertFalse(sections.item(1).hasAttrNS(WOOD.NS, "editable"));
        assertFalse(sections.item(1).hasAttrNS(WOOD.NS, "template"));
        assertFalse(sections.item(2).hasAttrNS(WOOD.NS, "editable"));
        assertFalse(sections.item(2).hasAttrNS(WOOD.NS, "content"));
    }

    /**
     * Component inherits 'paragraph' from template that inherits 'section' from page.
     */
    @Test
    public void GivenTwoLevelsHierarchy_ThenAllContentMerged() {
        // given
        String pageHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Page</h1>" + //
                "	<section w:editable='section'></section>" + //
                "</body>";
        when(pageLayout.getReader()).thenReturn(new StringReader(pageHTML));

        String templateHTML = "<section w:template='res/page#section' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<div w:editable='paragraph'></div>" + //
                "</section>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<div w:template='res/template#paragraph' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Component</h1>" + //
                "</div>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/page")).thenReturn(pageCompo);
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertThat(layout.getTag(), equalTo("body"));

        assertThat(layout.getByTag("section"), notNullValue());
        assertThat(layout.getByTag("div"), notNullValue());

        EList headings = layout.findByTag("h1");
        assertThat(headings.size(), equalTo(3));
        assertThat(headings.item(0).getText(), equalTo("Page"));
        assertThat(headings.item(1).getText(), equalTo("Template"));
        assertThat(headings.item(2).getText(), equalTo("Component"));
    }

    @Test
    public void GivenTwoLevelsHierarchy_ThenOperatorsRemoved() {
        // given
        String pageHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Page</h1>" + //
                "	<section w:editable='section'></section>" + //
                "</body>";
        when(pageLayout.getReader()).thenReturn(new StringReader(pageHTML));

        String templateHTML = "<section w:template='res/page#section' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<div w:editable='paragraph'></div>" + //
                "</section>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<div w:template='res/template#paragraph' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Component</h1>" + //
                "</div>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/page")).thenReturn(pageCompo);
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertFalse(layout.hasAttr("xmlns:w"));

        Element section = layout.getByTag("section");
        assertFalse(section.hasAttr("xmlns:w"));
        assertFalse(section.hasAttrNS(WOOD.NS, "editable"));
        assertFalse(section.hasAttrNS(WOOD.NS, "template"));

        Element div = layout.getByTag("div");
        assertFalse(div.hasAttr("xmlns:w"));
        assertFalse(div.hasAttrNS(WOOD.NS, "editable"));
        assertFalse(div.hasAttrNS(WOOD.NS, "template"));
    }

    @Test
    public void GivenTwoLevelsHierarchyAndDataAttrOperators_ThenOperatorsRemoved() {
        // given
        when(project.getOperatorsHandler()).thenReturn(new DataAttrOperatorsHandler());

        String pageHTML = "<body>" + //
                "	<h1>Page</h1>" + //
                "	<section data-editable='section'></section>" + //
                "</body>";
        when(pageLayout.getReader()).thenReturn(new StringReader(pageHTML));

        String templateHTML = "<section data-template='res/page#section'>" + //
                "	<h1>Template</h1>" + //
                "	<div data-editable='paragraph'></div>" + //
                "</section>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<div data-template='res/template#paragraph'>" + //
                "	<h1>Component</h1>" + //
                "</div>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/page")).thenReturn(pageCompo);
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        Element section = layout.getByTag("section");
        assertThat(section, notNullValue());
        assertFalse(section.hasAttr("data-editable"));
        assertFalse(section.hasAttr("data-template"));

        Element div = layout.getByTag("div");
        assertFalse(div.hasAttr("data-editable"));
        assertFalse(div.hasAttr("data-template"));
    }

    @Test
    public void GivenTwoLevelsHierarchyAndAttrOperators_ThenOperatorsRemoved() {
        // given
        when(project.getOperatorsHandler()).thenReturn(new AttrOperatorsHandler());

        String pageHTML = "<body>" + //
                "	<h1>Page</h1>" + //
                "	<section editable='section'></section>" + //
                "</body>";
        when(pageLayout.getReader()).thenReturn(new StringReader(pageHTML));

        String templateHTML = "<section template='res/page#section'>" + //
                "	<h1>Template</h1>" + //
                "	<div editable='paragraph'></div>" + //
                "</section>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<div template='res/template#paragraph'>" + //
                "	<h1>Component</h1>" + //
                "</div>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/page")).thenReturn(pageCompo);
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        Element section = layout.getByTag("section");
        assertFalse(section.hasAttr("editable"));
        assertFalse(section.hasAttr("template"));

        Element div = layout.getByTag("div");
        assertFalse(div.hasAttr("editable"));
        assertFalse(div.hasAttr("template"));
    }

    /**
     * Test attributes merging for editable element on simple inheritance: template has an editable section and component
     * implements content for that editable. Both template and component have attributes. Component attributes takes precedence
     * over template attributes.
     */
    @Test
    public void GivenComponentsWithAttributes_ThenAttributesMerged() {
        // given
        String templateHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section id='template-id' name='template' class='template-class' w:editable='section'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<section name='component' class='component-class' disabled='true' w:template='res/template#section' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Content</h1>" + //
                "</section>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element section = compo.getLayout().getByTag("section");
        assertThat(section.getAttr("id"), equalTo("template-id"));
        assertThat(section.getAttr("name"), equalTo("component"));
        assertThat(section.getAttr("class"), equalTo("component-class template-class"));
        assertThat(section.getAttr("disabled"), equalTo("true"));
    }

    @Test
    public void GivenThirdPartyNamespace_ThenNamespacePreserved() {
        // given
        String templateHTML = "<body xmlns:w='js-lib.com/wood' xmlns:t='js-lib.com/test'>" + //
                "	<h1>Template</h1>" + //
                "	<section t:name='template' w:editable='section'></section>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<section name='component' w:template='res/template#section' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Content</h1>" + //
                "</section>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element section = compo.getLayout().getByTag("section");
        assertThat(section.getAttrNS("js-lib.com/test", "name"), equalTo("template"));
        assertThat(section.getAttr("name"), equalTo("component"));
    }

    @Test
    public void GivenComponentsWithStyles_ThenCollectStyles() {
        // given
        when(templateStyle.exists()).thenReturn(true);
        when(templateStyle.value()).thenReturn("template.css");
        when(compoStyle.exists()).thenReturn(true);
        when(compoStyle.value()).thenReturn("compo.css");

        String templateLayoutHTML = "<div>" + //
                "	<h1 w:editable='h1' xmlns:w='js-lib.com/wood'></h1>" + //
                "</div>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateLayoutHTML));

        String compoLayoutHTML = "<h1 w:template='res/template#h1' xmlns:w='js-lib.com/wood'><p>Content</p></h1>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoLayoutHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        List<FilePath> styles = compo.getStyleFiles();
        assertThat(styles, hasSize(2));
        assertThat(styles.get(0).value(), equalTo("template.css"));
        assertThat(styles.get(1).value(), equalTo("compo.css"));
    }

    @Test
    public void GivenComponentsWithDescriptors_ThenMergeDescriptors() {
        // given
        String templateDescriptorXML = "<compo>" + //
                "	<script src='libs/js-lib.js'></script>" + //
                "	<script src='scripts/Template.js'></script>" + //
                "</compo>";
        when(templateDescriptor.exists()).thenReturn(true);
        when(templateDescriptor.getReader()).thenReturn(new StringReader(templateDescriptorXML));

        String templateLayoutHTML = "<div>" + //
                "	<h1 w:editable='h1' xmlns:w='js-lib.com/wood'></h1>" + //
                "</div>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateLayoutHTML));

        String compoDescriptorXML = "<compo>" + //
                "	<script src='libs/js-lib.js'></script>" + //
                "	<script src='scripts/Compo.js'></script>" + //
                "</compo>";
        when(compoDescriptor.exists()).thenReturn(true);
        when(compoDescriptor.getReader()).thenReturn(new StringReader(compoDescriptorXML));

        String compoLayoutHTML = "<h1 w:template='res/template#h1' xmlns:w='js-lib.com/wood'><p>Content</p></h1>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoLayoutHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        List<IScriptDescriptor> scripts = compo.getScriptDescriptors();
        assertThat(scripts, hasSize(3));
        assertThat(scripts.get(0).getSource(), equalTo("libs/js-lib.js"));
        assertThat(scripts.get(1).getSource(), equalTo("scripts/Template.js"));
        assertThat(scripts.get(2).getSource(), equalTo("scripts/Compo.js"));
    }

    @Test
    public void GivenComponentsWithDescriptorsAndInlineTemplating_ThenMergeDescriptors() {
        // given
        String templateDescriptorXML = "<compo>" + //
                "	<script src='libs/js-lib.js'></script>" + //
                "	<script src='scripts/Template.js'></script>" + //
                "</compo>";
        when(templateDescriptor.exists()).thenReturn(true);
        when(templateDescriptor.getReader()).thenReturn(new StringReader(templateDescriptorXML));

        String templateLayoutHTML = "<div>" + //
                "	<h1 w:editable='h1' xmlns:w='js-lib.com/wood'></h1>" + //
                "</div>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateLayoutHTML));

        String compoDescriptorXML = "<compo>" + //
                "	<script src='libs/js-lib.js'></script>" + //
                "	<script src='scripts/Compo.js'></script>" + //
                "</compo>";
        when(compoDescriptor.exists()).thenReturn(true);
        when(compoDescriptor.getReader()).thenReturn(new StringReader(compoDescriptorXML));

        String compoLayoutHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1 w:template='res/template#h1'><b>Content</b></h1>" + //
                "</body>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoLayoutHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        List<IScriptDescriptor> scripts = compo.getScriptDescriptors();
        assertThat(scripts, hasSize(3));
        assertThat(scripts.get(0).getSource(), equalTo("libs/js-lib.js"));
        assertThat(scripts.get(1).getSource(), equalTo("scripts/Template.js"));
        assertThat(scripts.get(2).getSource(), equalTo("scripts/Compo.js"));
    }

    @Test
    public void GivenStandaloneTemplating_WhenClean_ThenNamespaceDeclarationsRemoved() {
        // given
        String templateHTML = "<body xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Template</h1>" + //
                "	<section w:editable='section'></section>" + //
                "	<div w:editable='empty'></div>" + //
                "</body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateHTML));

        String compoHTML = "<section w:template='res/template#section' xmlns:w='js-lib.com/wood'>" + //
                "	<h1>Content</h1>" + //
                "</section>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        Component compo = new Component(compoPath, referenceHandler, true);

        // then
        Element layout = compo.getLayout();
        assertFalse(layout.hasAttr("xmlns:w"));
        assertThat(layout.getByTag("section"), notNullValue());
        assertFalse(layout.getByTag("section").hasAttr("xmlns:w"));
        assertThat(layout.getByTag("div"), nullValue());
    }

    /**
     * Template editable element is named 'h1' but component fragment references it as 'fake'.
     */
    @Test(expected = WoodException.class)
    public void GivenMissingEditable_ThenWoodException() {
        // given
        String templateLayoutHTML = "<body><h1 w:editable='h1' xmlns:w='js-lib.com/wood'></h1></body>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateLayoutHTML));

        String compoLayoutHTML = "<h1 w:template='res/template#fake' xmlns:w='js-lib.com/wood'><p></p></h1>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoLayoutHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        new Component(compoPath, referenceHandler, true);

        // then
    }

    /**
     * Template editable element has children.
     */
    @Test(expected = WoodException.class)
    public void GivenEditableWithChildren_ThenWoodException() {
        // given
        String templateLayoutHTML = "<h1 w:editable='h1' xmlns:w='js-lib.com/wood'><b>Title</b></h1>";
        when(templateLayout.getReader()).thenReturn(new StringReader(templateLayoutHTML));

        String compoLayoutHTML = "<h1 w:template='res/template#h1' xmlns:w='js-lib.com/wood'><p></p></h1>";
        when(compoLayout.getReader()).thenReturn(new StringReader(compoLayoutHTML));
        when(project.createCompoPath("res/template")).thenReturn(templateCompo);

        // when
        new Component(compoPath, referenceHandler, true);

        // then
    }
}
