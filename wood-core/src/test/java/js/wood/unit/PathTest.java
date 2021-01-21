package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.wood.CompoPath;
import js.wood.DirPath;
import js.wood.FilePath;
import js.wood.Path;
import js.wood.Project;
import js.wood.WoodException;
import js.wood.impl.EditablePath;
import js.wood.impl.FileType;
import js.wood.impl.FilesHandler;
import js.wood.impl.Reference;
import js.wood.impl.ResourceType;
import js.wood.impl.Variants;

public class PathTest extends WoodTestCase
{
  private Project project;

  @Before
  public void beforeTest() throws Exception
  {
    project = project("project");
  }

  private FilePath filePath(String path)
  {
    return new FilePath(project, path);
  }

  private DirPath dirPath(String path)
  {
    return new DirPath(project, path);
  }

  private CompoPath compoPath(String path)
  {
    return new CompoPath(project, path);
  }

  // ------------------------------------------------------
  // Path

  @Test
  public void pathConstructor()
  {
    // uses FilePath to create Path instance that is abstract
    Path path = filePath("res/compo/discography/discography.htm");
    assertEquals("res/compo/discography/discography.htm", path.value());
    assertTrue(path.exists());
    assertEquals("src/test/resources/project/res/compo/discography/discography.htm", path.toFile().getPath().replace('\\', '/'));
    assertTrue(path.hashCode() != 0);
  }

  @Test
  public void pathNotExists()
  {
    // uses FilePath to create Path instance that is abstract
    Path path = filePath("res/compo/fake/fake.htm");
    assertFalse(path.exists());
  }

  @Test
  public void pathToFileException()
  {
    // uses FilePath to create Path instance that is abstract
    Path path = filePath("res/compo/fake/fake.htm");
    try {
      path.toFile();
      fail("Path#toFile() for not existing should rise exception.");
    }
    catch(Exception e) {
      assertTrue(e instanceof WoodException);
    }
  }

  @Test
  public void pathEquals()
  {
    final String path = "res/compo/video-player";
    Path dirPath = dirPath(path);
    Path compoPath = compoPath(path);
    assertTrue(dirPath.equals(compoPath));
  }

  @Test
  public void pathToFile() throws Throwable
  {
    assertPath("project/res/compo/discography", "compo/discography", CompoPath.class);
    assertPath("project/lib/js-lib/js-lib.js", "lib/js-lib/js-lib.js", FilePath.class);
    assertPath("project/script/hc/view/DiscographyView.js", "script/hc/view/DiscographyView.js", FilePath.class);
    assertPath("project/gen/js/controller/MainController.js", "gen/js/controller/MainController.js", FilePath.class);
  }

  private void assertPath(String expectedFile, String pathValue, Class<?> pathClass) throws Throwable
  {
    Path path = (Path)Classes.newInstance(pathClass, project, pathValue);
    assertEquals(file(expectedFile), path.toFile());
  }

  @Test
  public void pathCreate()
  {
    assertTrue(Path.create(project, "res/template/page") instanceof CompoPath);
    assertTrue(Path.create(project, "res/template/page/page.htm") instanceof FilePath);
    assertTrue(Path.create(project, "res/template/page") instanceof DirPath);
  }

  // ------------------------------------------------------
  // FilePath

  @Test
  public void filePathPattern()
  {
    Pattern pattern = field(FilePath.class, "PATTERN");
    assertNotNull(pattern);

    assertFilePattern(pattern, "res/path/compo/compo.htm", "res/path/compo/", "compo", null, "htm");
    assertFilePattern(pattern, "res/path/compo/compo_port.htm", "res/path/compo/", "compo", "port", "htm");
    assertFilePattern(pattern, "res/path/second-compo/second-compo.css", "res/path/second-compo/", "second-compo", null, "css");
    assertFilePattern(pattern, "res/path/second-compo/second-compo_w800.css", "res/path/second-compo/", "second-compo", "w800", "css");
    assertFilePattern(pattern, "lib/js-lib.js", "lib/", "js-lib", null, "js");
    assertFilePattern(pattern, "script/js/format/RichText.js", "script/js/format/", "RichText", null, "js");
    assertFilePattern(pattern, "gen/js/widget/Paging.js", "gen/js/widget/", "Paging", null, "js");
    assertFilePattern(pattern, "res/path/compo/background_port_ro.png", "res/path/compo/", "background", "port_ro", "png");
    assertFilePattern(pattern, "res/3pty-scripts/3pty-scripts.htm", "res/3pty-scripts/", "3pty-scripts", null, "htm");
  }

  private static void assertFilePattern(Pattern pattern, String value, String... groups)
  {
    Matcher m = pattern.matcher(value);
    assertTrue("Invalid path value: " + value, m.find());
    assertEquals("Path not found.", groups[0], m.group(1));
    assertEquals("File name not found.", groups[1], m.group(2));
    assertEquals("Variants not match.", groups[2], m.group(3));
    assertEquals("Extension not found.", groups[3], m.group(4));
  }

  @Test
  public void filePathConstructor()
  {
    assertFilePath("res/compo/discography/discography_ro.css", "res/compo/discography/", "discography.css", FileType.STYLE, "ro");
    assertFilePath("res/compo/discography/strings.xml", "res/compo/discography/", "strings.xml", FileType.XML, null);
    assertFilePath("res/compo/discography/logo_de.png", "res/compo/discography/", "logo.png", FileType.MEDIA, "de");
    assertFilePath("lib/js-lib.js", "lib/", "js-lib.js", FileType.SCRIPT, null);
    assertFilePath("script/js/compo/Dialog.js", "script/js/compo/", "Dialog.js", FileType.SCRIPT, null);
  }

  private void assertFilePath(String pathValue, String path, String fileName, FileType fileType, String language)
  {
    FilePath p = filePath(pathValue);
    assertEquals(pathValue, p.value());
    assertEquals(path, p.getDirPath().value());
    assertEquals(fileName, p.getName());
    assertEquals(fileType, p.getType());
    assertNotNull(p.getVariants());
    assertEquals(language != null ? new Locale(language) : null, p.getVariants().getLocale());
  }

  @Test
  public void filePathBaseName()
  {
    FilePath path = filePath("res/compo/discography/discography.css");
    assertEquals("discography", path.getBaseName());
    assertTrue(path.isBaseName("discography"));
    assertTrue(path.isBaseName(new Reference(path, ResourceType.STRING, "discography")));

    path = filePath("res/compo/discography/strings_de.xml");
    assertEquals("strings", path.getBaseName());
    assertTrue(path.isBaseName("strings"));
    // assertTrue(path.isBaseName(new Reference(path, ResourceType.STRING, "strings")));
  }

  @Test
  public void filePathCloneToStyle()
  {
    FilePath layoutFile = filePath("res/compo/discography/discography_w800.htm");
    FilePath styleFile = layoutFile.cloneToStyle();
    assertTrue(layoutFile.isLayout());
    assertTrue(styleFile.isStyle());
    assertEquals(layoutFile.getBaseName(), styleFile.getBaseName());
    assertEquals(layoutFile.getDirPath(), styleFile.getDirPath());
    assertEquals(layoutFile.getVariants().getLocale(), styleFile.getVariants().getLocale());
    assertEquals(layoutFile.getVariants().getViewportWidth(), styleFile.getVariants().getViewportWidth());
    assertEquals(layoutFile.getVariants().getViewportHeight(), styleFile.getVariants().getViewportHeight());
  }

  @Test
  public void filePathGetDirPath()
  {
    FilePath path = filePath("res/asset/background.jpg");
    assertEquals("res/asset/", path.getDirPath().value());
  }

  @Test
  public void filePathPredicates()
  {
    assertTrue(filePath("res/asset/background.jpg").isMedia());
    assertTrue(filePath("res/compo/discography/discography.css").isStyle());
    assertTrue(filePath("res/compo/discography/preview.js").isScript());
    assertTrue(filePath("res/compo/discography/preview.js").isPreviewScript());
    assertTrue(filePath("res/compo/discography/discography.xml").isCompoDescriptor());
    assertFalse(filePath("res/compo/discography/discography.xml").isVariables());
    assertTrue(filePath("res/compo/discography/strings.xml").isVariables());
  }

  @Test
  public void filePathVariants()
  {
    FilePath path = filePath("res/compo/discography/strings_de.xml");
    Variants variants = path.getVariants();
    assertEquals(new Locale("de"), variants.getLocale());
    assertEquals(0, variants.getViewportWidth());
    assertEquals(0, variants.getViewportHeight());
    assertEquals(Variants.Orientation.NONE, variants.getOrientation());

    path = filePath("res/compo/discography/discography_w800.css");
    variants = path.getVariants();
    assertNull(variants.getLocale());
    assertEquals(800, variants.getViewportWidth());
    assertEquals(0, variants.getViewportHeight());

    path = filePath("res/compo/discography/discography_h800.css");
    variants = path.getVariants();
    assertNull(variants.getLocale());
    assertEquals(0, variants.getViewportWidth());
    assertEquals(800, variants.getViewportHeight());

    path = filePath("res/compo/discography/colors_ro_w800.xml");
    variants = path.getVariants();
    assertEquals(new Locale("ro"), variants.getLocale());
    assertEquals(800, variants.getViewportWidth());

    path = filePath("res/compo/discography/colors_w800_ro.xml");
    variants = path.getVariants();
    assertEquals(new Locale("ro"), variants.getLocale());
    assertEquals(800, variants.getViewportWidth());

    path = filePath("res/compo/discography/colors_jp_w800_ro.xml");
    variants = path.getVariants();
    assertEquals(new Locale("ro"), variants.getLocale());
    assertEquals(800, variants.getViewportWidth());
  }

  // this test case is not related to file path but does related to variants used primarily by file path
  @Test
  public void variantSafeConstructor()
  {
    Variants variants = new Variants("de");
    assertEquals(new Locale("de"), variants.getLocale());
    assertEquals(0, variants.getViewportWidth());
    assertEquals(0, variants.getViewportHeight());

    variants = new Variants("en-US");
    assertEquals(new Locale("en", "US"), variants.getLocale());
    assertEquals(0, variants.getViewportWidth());
    assertEquals(0, variants.getViewportHeight());

    variants = new Variants("w800");
    assertNull(variants.getLocale());
    assertEquals(800, variants.getViewportWidth());

    variants = new Variants("h800");
    assertNull(variants.getLocale());
    assertEquals(800, variants.getViewportHeight());

    variants = new Variants("ro_w800");
    assertEquals(new Locale("ro"), variants.getLocale());
    assertEquals(800, variants.getViewportWidth());

    variants = new Variants("ro_h800");
    assertEquals(new Locale("ro"), variants.getLocale());
    assertEquals(800, variants.getViewportHeight());

    variants = new Variants("w800_ro");
    assertEquals(new Locale("ro"), variants.getLocale());
    assertEquals(800, variants.getViewportWidth());

    variants = new Variants("jp_w800_ro");
    assertEquals(new Locale("ro"), variants.getLocale());
    assertEquals(800, variants.getViewportWidth());

    variants = new Variants("portrait");
    assertNull(variants.getLocale());
    assertEquals(0, variants.getViewportWidth());
    assertEquals(Variants.Orientation.PORTRAIT, variants.getOrientation());
  }

  @Test
  public void filePathNotRecognizedVariant()
  {
    try {
      filePath("res/compo/discography/colors_q800.xml");
      fail("Not recognized file variant should rise exception.");
    }
    catch(Exception e) {
      assertTrue(e instanceof WoodException);
    }
  }

  @Test
  public void filePathResolvedOnAssetsFile() throws Throwable
  {
    FilePath path = filePath("res/asset/background.jpg");
    File file = path.toFile();
    assertNotNull(file);
    assertTrue(file.toString().endsWith("project\\res\\asset\\background.jpg"));
  }

  @Test
  public void filePathAccept()
  {
    assertTrue(FilePath.accept("res/template/page/page.htm"));
    assertTrue(FilePath.accept("lib/js-lib.js"));
    assertTrue(FilePath.accept("res/compo/video-player/video-player.xml"));
    assertTrue(FilePath.accept("lib/js-lib.1.2.3.js"));
    assertFalse(FilePath.accept("template/page/page.htm"));
    assertFalse(FilePath.accept("res/template/page/page.htm#body"));
    assertFalse(FilePath.accept("dir/template/page/page.htm"));
    assertFalse(FilePath.accept("dir/template/page#body"));
  }

  // ------------------------------------------------------
  // DirPath

  @Test
  public void dirPathPattern()
  {
    Pattern pattern = field(DirPath.class, "PATTERN");
    assertNotNull(pattern);

    assertDirPattern(pattern, "res/compo", "res", "/compo");
    assertDirPattern(pattern, "lib/js-lib", "lib", "/js-lib");
    assertDirPattern(pattern, "res/path/compo/", "res", "/path/compo");
    assertDirPattern(pattern, "gen/js/wood/test", "gen", "/js/wood/test");
    assertDirPattern(pattern, "script/js/wood/test", "script", "/js/wood/test");
  }

  private static void assertDirPattern(Pattern pattern, String value, String sourceDir, String segments)
  {
    Matcher m = pattern.matcher(value);
    assertTrue("Invalid directory path value: " + value, m.find());
    assertEquals("Source directory not found.", sourceDir, m.group(1));
    assertEquals("Path segments not found.", segments, m.group(2));
  }

  @Test
  public void dirPathConstructor()
  {
    assertDirPath("res/path/compo", "res/path/compo/", "res", "compo");
    assertDirPath("res/path/compo/", "res/path/compo/", "res", "compo");
    assertDirPath("res/compo/video-player", "res/compo/video-player/", "res", "video-player");
    assertDirPath("lib/js-lib", "lib/js-lib/", "lib", "js-lib");
    assertDirPath("script/js/wood/test", "script/js/wood/test/", "script", "test");
    assertDirPath("gen/js/wood/test", "gen/js/wood/test/", "gen", "test");
  }

  private void assertDirPath(String pathValue, String value, String sourceDir, String name)
  {
    DirPath dir = dirPath(pathValue);
    assertEquals(value, dir.value());
    assertEquals(sourceDir, field(dir, "sourceDir").toString());
    assertEquals(name, dir.getName());
  }

  @Test
  public void dirPathInvalidValue()
  {
    for(String path : new String[]
    {
        "invalid-source-dir", "/res/absolute/path", "res/invalid_name/"
    }) {
      try {
        dirPath(path);
        fail("Invlaid directory path should rise exception.");
      }
      catch(Exception e) {
        assertTrue(e instanceof WoodException);
      }
    }
  }

  @Test
  public void dirPathSegments()
  {
    List<String> expected = new ArrayList<String>();
    expected.add("js");
    expected.add("tools");
    expected.add("wood");
    expected.add("tests");

    DirPath dir = dirPath("script/js/tools/wood/tests/");
    assertEquals(expected, dir.getPathSegments());
  }

  @Test
  public void dirPathIterable()
  {
    DirPath dir = dirPath("res/compo/discography");

    List<String> files = new ArrayList<String>();
    for(FilePath file : dir.files()) {
      files.add(file.value());
    }

    assertFiles(files);
  }

  @Test
  public void dirPathAllFilesList()
  {
    DirPath dir = dirPath("res/compo/discography");

    final List<String> files = new ArrayList<String>();
    dir.files(new FilesHandler()
    {
      @Override
      public void onFile(FilePath file) throws Exception
      {
        files.add(file.value());
      }
    });

    assertFiles(files);
  }

  private static void assertFiles(List<String> files)
  {
    assertEquals(4, files.size());
    Collections.sort(files);

    int index = 0;
    assertEquals("res/compo/discography/discography.css", files.get(index++));
    assertEquals("res/compo/discography/discography.htm", files.get(index++));
    assertEquals("res/compo/discography/logo.png", files.get(index++));
    assertEquals("res/compo/discography/preview.js", files.get(index++));
  }

  @Test
  public void dirPathFilesListByType()
  {
    DirPath dir = dirPath("res/asset");

    final List<String> files = new ArrayList<String>();
    dir.files(FileType.XML, new FilesHandler()
    {
      @Override
      public void onFile(FilePath file) throws Exception
      {
        files.add(file.value());
      }
    });

    assertEquals(6, files.size());
    Collections.sort(files);

    int index = 0;
    assertEquals("res/asset/colors.xml", files.get(index++));
    assertEquals("res/asset/dimens.xml", files.get(index++));
    assertEquals("res/asset/links.xml", files.get(index++));
    assertEquals("res/asset/strings.xml", files.get(index++));
    assertEquals("res/asset/styles.xml", files.get(index++));
    assertEquals("res/asset/text.xml", files.get(index++));
  }

  @Test
  public void dirPathGetFilePath()
  {
    DirPath dir = dirPath("res/compo/discography");
    assertEquals("res/compo/discography/strings.xml", dir.getFilePath("strings.xml").value());
  }

  @Test
  public void dirPathPredicates()
  {
    assertTrue(dirPath("res/asset").isAssets());
    assertTrue(dirPath("res/theme").isTheme());
    assertTrue(dirPath("res/asset/").isResources());
    assertTrue(dirPath("res/compo/discography").isResources());
    assertTrue(dirPath("gen/js/tools").isGenerated());
    assertTrue(dirPath("lib/js-lib/").isLibrary());

    assertFalse(dirPath("res/asset").isTheme());
    assertFalse(dirPath("res/theme").isAssets());
    assertFalse(dirPath("res/compo/discography").isAssets());
    assertFalse(dirPath("gen/js/tools").isLibrary());
    assertFalse(dirPath("lib/js-lib/").isGenerated());
  }

  @Test
  public void dirPathAccept()
  {
    assertTrue(DirPath.accept("lib/video-player/"));
    assertTrue(DirPath.accept("lib/video-player"));
    assertTrue(DirPath.accept("script/js/wood/test"));
    assertTrue(DirPath.accept("gen/js/wood/controller"));
    assertFalse(DirPath.accept("lib/video-player/video-player.htm"));
    assertFalse(DirPath.accept("lib/video-player#body"));
    assertFalse(DirPath.accept("java/js/wood/test"));
  }

  // ------------------------------------------------------
  // CompoPath

  @Test
  public void compoPathPattern()
  {
    Pattern pattern = field(CompoPath.class, "PATTERN");
    assertNotNull(pattern);

    assertCompoPattern(pattern, "res/compo", "res/compo", "res");
    assertCompoPattern(pattern, "compo", "compo", null);
    assertCompoPattern(pattern, "res/path/compo", "res/path/compo", "res");
    assertCompoPattern(pattern, "path/compo", "path/compo", null);

    assertCompoPattern(pattern, "res/compo/", "res/compo", "res");
    assertCompoPattern(pattern, "compo/", "compo", null);
    assertCompoPattern(pattern, "res/path/compo/", "res/path/compo", "res");
    assertCompoPattern(pattern, "path/compo/", "path/compo", null);
  }

  private static void assertCompoPattern(Pattern pattern, String value, String... groups)
  {
    Matcher m = pattern.matcher(value);
    assertTrue("Invalid path value: " + value, m.find());
    assertEquals("Path not found.", groups[0], m.group(1));
    assertEquals("Code base not found.", groups[1], m.group(2));
  }

  @Test
  public void compoPathConstructor()
  {
    assertCompoPath("res/path/compo", "res/path/compo/compo.htm", "res/path/compo/", "compo");
    assertCompoPath("path/compo", "res/path/compo/compo.htm", "res/path/compo/", "compo");
    assertCompoPath("res/path/compo/", "res/path/compo/compo.htm", "res/path/compo/", "compo");
    assertCompoPath("path/compo/", "res/path/compo/compo.htm", "res/path/compo/", "compo");
    assertCompoPath("res/compo/discography", "res/compo/discography/discography.htm", "res/compo/discography/", "discography");
  }

  private void assertCompoPath(String pathValue, String layout, String value, String name)
  {
    CompoPath p = compoPath(pathValue);
    assertEquals(layout, p.getLayoutPath().value());
    assertEquals(value, p.value());
    assertEquals(name, p.getName());
  }

  @Test
  public void compoPathGetFilePath()
  {
    CompoPath compo = compoPath("res/path/compo");
    FilePath file = compo.getFilePath("picture.png");

    assertEquals("res/path/compo/picture.png", file.value());
    assertEquals("res/path/compo/", file.getDirPath().value());
    assertEquals("picture.png", file.getName());
  }

  @Test
  public void compoPathGetLayoutFile()
  {
    CompoPath compo = compoPath("res/path/compo");
    assertEquals("res/path/compo/compo.htm", compo.getLayoutPath().value());
  }

  @Test
  public void compoPathAccept() throws Throwable
  {
    assertTrue(CompoPath.accept("compo"));
    assertTrue(CompoPath.accept("res/compo"));
    assertTrue(CompoPath.accept("path/compo"));
    assertTrue(CompoPath.accept("res/path/compo"));
    assertFalse(CompoPath.accept("res/path/compo/compo.htm"));
    assertFalse(CompoPath.accept("res/path/compo/compo.htm#fragment-id"));
    assertFalse(CompoPath.accept("res/path/compo/compo.css"));

    // here is a component path logic flaw
    // because res directory is optional source directory cannot reliable be detected
    // accordingly syntax description next path value should be rejected but is accepted
    assertTrue(CompoPath.accept("dir/template/page"));
  }

  // ------------------------------------------------------
  // EditablePath

  @Test
  public void editablePath()
  {
    FilePath layoutPath = filePath("res/page/index/index.htm");
    EditablePath path = new EditablePath(project, layoutPath, "template/page#page-body");

    assertEquals("res/template/page/page.htm", path.getLayoutPath().value());
    assertEquals("res/template/page/", path.value());
    assertEquals("page", path.getName());
    assertEquals("page-body", path.getEditableName());

    assertEquals(new File("src/test/resources/project/res/template/page"), path.toFile());
  }

  @Test
  public void editablePathBadConstructor()
  {
    FilePath layoutPath = filePath("res/page/index/index.htm");

    for(String path : new String[]
    {
        "", "template/page", "dir/template/page#body", "template/page/page.htm#body"
    }) {
      try {
        new EditablePath(project, layoutPath, path);
        fail("Editable path constructor with bad path value should rise exception.");
      }
      catch(Exception e) {
        assertTrue(e instanceof WoodException);
      }
    }
  }

  @Test
  public void editablePathAccept()
  {
    assertTrue(EditablePath.accept("template/page#body"));
    assertTrue(EditablePath.accept("res/template/page#body"));
    assertFalse(EditablePath.accept(""));
    assertFalse(EditablePath.accept("template/page"));
    assertFalse(EditablePath.accept("template/page/page.htm#body"));

    // this case is a flaw in component path logic; see testCompoPathAccept() comment
    assertTrue(EditablePath.accept("dir/template/page#body"));
  }
}
