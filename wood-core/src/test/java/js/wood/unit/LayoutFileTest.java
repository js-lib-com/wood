package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.wood.CompoPath;
import js.wood.FilePath;
import js.wood.Project;
import js.wood.WoodException;
import js.wood.impl.LayoutFile;

public class LayoutFileTest extends WoodTestCase
{
  private Project project;

  @Before
  public void beforeTest() throws Exception
  {
    project = project("layout-file");
  }

  private FilePath filePath(String path)
  {
    return new FilePath(project, path);
  }

  @Test
  public void content()
  {
    LayoutFile layoutFile = new LayoutFile(filePath("res/page/index/index.htm"));

    assertEquals("res/page/index/", layoutFile.getCompoPath().value());
    assertFalse((boolean)Classes.getFieldValue(layoutFile, "hasBody"));

    Set<String> editables = Classes.getFieldValue(layoutFile, "editables");
    assertTrue(editables.isEmpty());

    Set<String> templates = Classes.getFieldValue(layoutFile, "templates");
    assertEquals(1, templates.size());
    assertEquals("page-body", templates.iterator().next());

    CompoPath templatePath = Classes.getFieldValue(layoutFile, "templatePath");
    assertEquals("res/template/sidebar-page/", templatePath.value());
  }

  @Test
  public void template()
  {
    LayoutFile layoutFile = new LayoutFile(filePath("res/template/sidebar-page/sidebar-page.htm"));

    assertEquals("res/template/sidebar-page/", layoutFile.getCompoPath().value());
    assertFalse((boolean)Classes.getFieldValue(layoutFile, "hasBody"));

    Set<String> editables = Classes.getFieldValue(layoutFile, "editables");
    assertEquals(1, editables.size());
    assertEquals("sidebar", editables.iterator().next());

    Set<String> templates = Classes.getFieldValue(layoutFile, "templates");
    assertEquals(1, templates.size());
    assertEquals("page-body", templates.iterator().next());

    CompoPath templatePath = Classes.getFieldValue(layoutFile, "templatePath");
    assertEquals("res/template/page/", templatePath.value());
  }

  @Test
  public void pageTemplate()
  {
    LayoutFile layoutFile = new LayoutFile(filePath("res/template/page/page.htm"));

    assertEquals("res/template/page/", layoutFile.getCompoPath().value());
    assertTrue((boolean)Classes.getFieldValue(layoutFile, "hasBody"));

    Set<String> editables = Classes.getFieldValue(layoutFile, "editables");
    assertEquals(1, editables.size());
    assertEquals("page-body", editables.iterator().next());

    Set<String> templates = Classes.getFieldValue(layoutFile, "templates");
    assertTrue(templates.isEmpty());

    CompoPath templatePath = Classes.getFieldValue(layoutFile, "templatePath");
    assertNull(templatePath);
  }

  @Test
  public void widget()
  {
    LayoutFile layoutFile = new LayoutFile(filePath("res/compo/widget/widget.htm"));

    assertEquals("res/compo/widget/", layoutFile.getCompoPath().value());
    assertFalse((boolean)Classes.getFieldValue(layoutFile, "hasBody"));

    Set<String> editables = Classes.getFieldValue(layoutFile, "editables");
    assertTrue(editables.isEmpty());

    Set<String> templates = Classes.getFieldValue(layoutFile, "templates");
    assertTrue(templates.isEmpty());

    assertNull(Classes.getFieldValue(layoutFile, "templatePath"));
  }

  @Test
  public void singleIndex()
  {
    LayoutFile layoutFile = new LayoutFile(filePath("res/page/single-index/single-index.htm"));

    assertEquals("res/page/single-index/", layoutFile.getCompoPath().value());
    assertFalse((boolean)Classes.getFieldValue(layoutFile, "hasBody"));

    Set<String> editables = Classes.getFieldValue(layoutFile, "editables");
    assertTrue(editables.isEmpty());

    Set<String> templates = Classes.getFieldValue(layoutFile, "templates");
    assertEquals(1, templates.size());
    assertEquals("page-body", templates.iterator().next());

    CompoPath templatePath = Classes.getFieldValue(layoutFile, "templatePath");
    assertEquals("res/template/page/", templatePath.value());
  }

  @Test
  public void contentIsPage()
  {
    layoutFiles(project, "res/template/sidebar-page/sidebar-page.htm", "res/template/page/page.htm");

    LayoutFile layoutFile = new LayoutFile(filePath("res/page/index/index.htm"));
    assertNull(Classes.getFieldValue(layoutFile, "isPage"));
    assertTrue(layoutFile.isPage());
  }

  @Test
  public void templateIsPage()
  {
    layoutFiles(project, "res/template/page/page.htm");

    LayoutFile layoutFile = new LayoutFile(filePath("res/template/sidebar-page/sidebar-page.htm"));
    assertNull(Classes.getFieldValue(layoutFile, "isPage"));
    assertFalse(layoutFile.isPage());
  }

  @Test
  public void pageTemplateIsPage()
  {
    layoutFiles(project);

    LayoutFile layoutFile = new LayoutFile(filePath("res/template/page/page.htm"));
    assertNull(Classes.getFieldValue(layoutFile, "isPage"));
    assertFalse(layoutFile.isPage());
  }

  @Test
  public void widgetIsPage()
  {
    layoutFiles(project);

    LayoutFile layoutFile = new LayoutFile(filePath("res/compo/widget/widget.htm"));
    assertFalse(layoutFile.isPage());
  }

  @Test
  public void singleIndexIsPage()
  {
    layoutFiles(project, "res/template/page/page.htm");

    LayoutFile layoutFile = new LayoutFile(filePath("res/page/single-index/single-index.htm"));
    assertTrue(layoutFile.isPage());
  }

  @Test
  public void notResolvedTemplates()
  {
    layoutFiles(project, "res/template/sidebar-page/sidebar-page.htm", "res/template/page/page.htm");

    LayoutFile layoutFile = new LayoutFile(filePath("res/page/bad-index/bad-index.htm"));
    try {
      assertTrue(layoutFile.isPage());
      fail("Bad template reference should rise exception.");
    }
    catch(Exception e) {
      assertTrue(e instanceof WoodException);
      assertTrue(e.getMessage().contains("unresolved templates"));
    }
  }

  @Test
  public void editableOverwritten()
  {
    layoutFiles(project, "res/template/over-page/over-page.htm", "res/template/page/page.htm");

    LayoutFile layoutFile = new LayoutFile(filePath("res/page/over-index/over-index.htm"));
    try {
      assertTrue(layoutFile.isPage());
      fail("Overwritten editable should rise exception.");
    }
    catch(Exception e) {
      assertTrue(e instanceof WoodException);
      assertTrue(e.getMessage().contains("overwritten"));
    }
  }

  private void layoutFiles(Project project, String... paths)
  {
    Collection<LayoutFile> layouts = new HashSet<LayoutFile>();
    field(project, "layouts", layouts);
    for(String path : paths) {
      layouts.add(new LayoutFile(filePath(path)));
    }
  }
}
