package com.jslib.wood.build;

import com.jslib.wood.CompoPath;
import com.jslib.wood.Component;
import com.jslib.wood.dom.Document;
import com.jslib.wood.dom.DocumentBuilder;
import com.jslib.wood.dom.EList;
import com.jslib.wood.util.FilesUtil;
import org.junit.Test;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BuilderIntegrationTest {
    @Test
    public void constructor() throws IOException {
        Builder builder = builder("project");

        BuilderProject project = builder.getProject();
        assertNotNull(project);

        BuildFS buildFS = builder.getBuildFS();
        assertNotNull(buildFS);

        Collection<CompoPath> pages = builder.getProject().getPages();
        assertNotNull(pages);
        assertThat(pages, hasSize(4));
        assertTrue(pages.contains(new CompoPath(project, "res/page/about")));
        assertTrue(pages.contains(new CompoPath(project, "res/page/index")));
        assertTrue(pages.contains(new CompoPath(project, "res/page/video-player")));
        assertTrue(pages.contains(new CompoPath(project, "res/page/videos")));
    }

    @Test
    public void build() throws IOException {
        Builder builder = builder("project");
        builder.build();
    }

    @Test
    public void buildPage() throws Exception {
        Builder builder = builder("project");
        BuilderProject project = builder.getProject();

        builder.setLanguage("en");

        CompoPath indexPagePath = new CompoPath(project, "res/page/index");
        Component indexPage = new Component(indexPagePath, builder);
        builder.setCurrentComponent(indexPage);
        indexPage.scan();
        builder.buildPage(indexPage);

        DocumentBuilder documentBuilder = DocumentBuilder.getInstance();
        assertPageDocument(documentBuilder.loadHTML(new File("src/test/resources/project/build/index.htm")));
    }

    private static void assertPageDocument(Document doc) throws XPathExpressionException {
        assertThat(doc.getByTag("title").getText(), equalTo("Test Project - Index Page"));

        EList metas = doc.findByTag("meta");
        assertThat(metas.size(), equalTo(3));

        assertThat(metas.item(0).getAttr("http-equiv"), equalTo("Content-Type"));
        assertThat(metas.item(0).getAttr("content"), equalTo("text/html; charset=UTF-8"));
        assertThat(metas.item(1).getAttr("name"), equalTo("Description"));
        assertThat(metas.item(1).getAttr("content"), equalTo("Index page description."));
        assertThat(metas.item(2).getAttr("name"), equalTo("Author"));
        assertThat(metas.item(2).getAttr("content"), equalTo("j(s)-lib"));

        EList headerLinks = doc.findByTag("link");
        doc.dump();
        assertThat(headerLinks.size(), equalTo(10));

        int index = 0;
        assertHeaderLink("manifest.json", headerLinks, index++);
        assertHeaderLink("style/res-theme_var.css", headerLinks, index++);
        assertHeaderLink("style/res-theme_default.css", headerLinks, index++);
        assertHeaderLink("style/res-theme_fx.css", headerLinks, index++);

        // site styles order, beside reset and fx is not guaranteed
        assertNotNull(doc.getByXPath("//LINK[@href='style/res-theme_form.css']"));
        assertNotNull(doc.getByXPath("//LINK[@href='style/res-theme_style.css']"));

        index += 2; // skip form.css and fx.css
        assertHeaderLink("style/lib_paging.css", headerLinks, index++);
        assertHeaderLink("style/lib_list-view.css", headerLinks, index++);
        assertHeaderLink("style/res-template_sidebar-page.css", headerLinks, index++);
        assertHeaderLink("style/res-page_index.css", headerLinks, index);

        EList elist = doc.findByTag("script");
        List<String> scripts = new ArrayList<>();
        for (int i = 0; i < elist.size(); ++i) {
            scripts.add(elist.item(i).getAttr("src"));
        }
        assertThat(scripts, hasSize(9));

        assertTrue(scripts.indexOf("script/script.hc.page.Index.js") > scripts.indexOf("script/lib.js-lib.js"));
        assertTrue(scripts.indexOf("script/script.hc.view.DiscographyView.js") > scripts.indexOf("script/lib.js-lib.js"));
        assertTrue(scripts.indexOf("script/script.hc.view.DiscographyView.js") > scripts.indexOf("script/script.hc.view.VideoPlayer.js"));
        assertTrue(scripts.indexOf("script/script.hc.view.VideoPlayer.js") > scripts.indexOf("script/lib.js-lib.js"));

        assertTrue(scripts.contains("script/script.js.compo.Dialog.js"));
        assertTrue(scripts.contains("script/script.hc.view.VideoPlayer.js"));
        assertTrue(scripts.contains("script/script.js.hood.MainMenu.js"));
        assertTrue(scripts.contains("script/script.hc.page.Index.js"));
        assertTrue(scripts.contains("script/script.hc.view.DiscographyView.js"));
        assertTrue(scripts.contains("script/gen.js.controller.MainController.js"));
        assertTrue(scripts.contains("script/lib.list-view.js"));
        assertTrue(scripts.contains("script/lib.paging.js"));
        assertTrue(scripts.contains("script/script.js.hood.TopMenu.js"));
    }

    // --------------------------------------------------------------------------------------------

    private Builder builder(String projectDir, int... buildNumber) throws IOException {
        BuilderConfig config = new BuilderConfig();
        config.setProjectDir(new File("src/test/resources/" + projectDir));

        File buildDir = new File(config.getProjectDir(), "build");
        FilesUtil.removeFilesHierarchy(buildDir);

        if (buildNumber.length == 1) {
            config.setBuildNumber(buildNumber[0]);
        }

        return new Builder(config);
    }

    private static void assertHeaderLink(String expected, EList styles, int index) {
        assertThat(styles.item(index).getAttr("href"), equalTo(expected));
    }
}
