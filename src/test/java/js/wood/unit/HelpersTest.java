package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.w3c.DocumentBuilderImpl;
import js.log.Log;
import js.log.LogFactory;
import js.util.Classes;
import js.util.Files;
import js.wood.ClassDefinitionScanner;
import js.wood.CompoPath;
import js.wood.ComponentDescriptor;
import js.wood.FilePath;
import js.wood.FileType;
import js.wood.LayoutReader;
import js.wood.Path;
import js.wood.Project;
import js.wood.ProjectConfig;
import js.wood.Reference;
import js.wood.ReferenceHandler;
import js.wood.ResourceType;
import js.wood.ScriptFile;
import js.wood.StyleExtensionReader;
import js.wood.StyleReader;
import js.wood.Variables;
import js.wood.Variants;

import org.junit.Test;

public class HelpersTest extends WoodTestCase
{
  private static final Log log = LogFactory.getLog(HelpersTest.class);

  private Project project;

  private FilePath filePath(String path)
  {
    return new FilePath(project, path);
  }

  // ------------------------------------------------------
  // ClassDefinitionScanner

  @Test
  public void classDefinitionScannerLocalClasses()
  {
    log.trace("testClassDefinitionScannerLocalClasses()");
    project = project("scripts");

    File scriptFile = new File("fixture/scripts/defs//ClassDefinition.js");
    // getClasses() returns a set that has no order guaranteed
    List<String> classNames = new ArrayList<String>(ClassDefinitionScanner.getClasses(scriptFile));
    Collections.sort(classNames);

    assertEquals(5, classNames.size());

    int index = 0;
    assertEquals("test.format.RichText", classNames.get(index++));
    assertEquals("test.widget.Paging", classNames.get(index++));
    assertEquals("test.widget.RichText.Description", classNames.get(index++));
    assertEquals("test.wood.GeoMap", classNames.get(index++));
    assertEquals("test.wood.Index", classNames.get(index++));
  }

  @Test
  public void classDefinitionScannerThirdPartyClasses()
  {
    log.trace("testClassDefinitionScannerThirdPartyClasses()");
    project = project("scripts");

    FilePath scriptFile = filePath("lib/google-maps-api.js");
    // getClasses() returns a set that has no order guaranteed
    List<String> classNames = new ArrayList<String>(ClassDefinitionScanner.getClasses(scriptFile.toFile()));
    Collections.sort(classNames);

    assertEquals(5, classNames.size());

    int index = 0;
    assertEquals("google.maps.LatLng", classNames.get(index++));
    assertEquals("google.maps.LatLngBounds", classNames.get(index++));
    assertEquals("google.maps.Map", classNames.get(index++));
    assertEquals("google.maps.MapTypeId", classNames.get(index++));
    assertEquals("google.maps.Marker", classNames.get(index++));
  }

  // ------------------------------------------------------
  // ScriptFile

  @Test
  public void scriptFileConstructor()
  {
    log.trace("testScriptFileConstructor()");
    project = project("scripts");

    ScriptFile script = new ScriptFile(project, filePath("script/js/wood/IndexPage.js"));
    assertEquals("script/js/wood/IndexPage.js", field(script, "sourceFile").toString());
    assertEquals("script/js/wood/IndexPage.js", script.toString());
    assertTrue(((Set<?>)field(script, "strongDependencies")).isEmpty());
    assertTrue(((Set<?>)field(script, "weakDependencies")).isEmpty());
    assertTrue(((Set<?>)field(script, "thirdPartyDependencies")).isEmpty());

    Set<String> classes = script.getDefinedClasses();
    assertEquals(1, classes.size());
    assertEquals("js.wood.IndexPage", classes.iterator().next());

    script = new ScriptFile(project, filePath("lib/js-lib/js-lib.js"));
    classes = script.getDefinedClasses();

    assertEquals(4, classes.size());
    assertTrue(classes.contains("js.lang.Operator"));
    assertTrue(classes.contains("js.dom.Element"));
    assertTrue(classes.contains("js.ua.System"));
    assertTrue(classes.contains("js.format.DateFormat"));
  }

  @Test
  public void scriptFileScanDepenencies()
  {
    log.trace("testScriptFileScanDepenencies()");
    project = project("scripts");
    project.scanBuildFiles();
    Map<String, ScriptFile> classScripts = field(project, "classScripts");

    ScriptFile script = new ScriptFile(project, filePath("script/js/wood/IndexPage.js"));
    script.scanDependencies(classScripts);

    Set<ScriptFile> strongDependecies = field(script, "strongDependencies");
    assertEquals(1, strongDependecies.size());
    assertScript(strongDependecies, "lib/js-lib/js-lib.js");

    Set<ScriptFile> weakDependencies = field(script, "weakDependencies");
    assertEquals(2, weakDependencies.size());
    assertScript(weakDependencies, "script/js/widget/Description.js");
    assertScript(weakDependencies, "script/js/format/RichText.js");
  }

  private void assertScript(Set<ScriptFile> scripts, String scriptFile)
  {
    assertTrue(scripts.contains(new ScriptFile(project, filePath(scriptFile))));
  }

  // ------------------------------------------------------
  // Variants

  @Test
  public void variantsConstructor()
  {
    log.trace("testVariantsConstructor()");
    Variants variants = new Variants("xsd");
    assertEquals(Variants.Screen.EXTRA_SMALL, variants.getScreen());
  }

  @Test
  public void variantsPatterns()
  {
    log.trace("testVariantsPatterns()");
    Pattern pattern = Variants.Screen.PATTERN;
    assertTrue(match(pattern, "lgd"));
    assertTrue(match(pattern, "mdd"));
    assertTrue(match(pattern, "smd"));
    assertTrue(match(pattern, "xsd"));
  }

  private static boolean match(Pattern pattern, String variant)
  {
    Matcher matcher = pattern.matcher(variant);
    return matcher.find();
  }

  // ------------------------------------------------------
  // LayoutReader

  @Test
  public void layoutReader() throws IOException
  {
    log.trace("testLayoutReader()");
    Reader reader = new StringReader("<h1>header 1</h1><h2>header 2</h2>");
    Reader layoutReader = new LayoutReader(reader);

    StringWriter stringWriter = new StringWriter();
    Files.copy(layoutReader, stringWriter);

    DocumentBuilder builder = new DocumentBuilderImpl();
    Document doc = builder.parseXML(stringWriter.toString());
    assertEquals("layout", doc.getRoot().getTag());
    assertEquals("header 1", doc.getRoot().getByTag("h1").getText());
    assertEquals("header 2", doc.getRoot().getByTag("h2").getText());
  }

  // ------------------------------------------------------
  // StyleReader

  @Test
  public void styleReaderConstructor()
  {
    log.trace("testStyleReaderConstructor()");
    project = project("styles");
    FilePath styleFile = filePath("res/page/page.css");
    StyleReader reader = new StyleReader(styleFile);

    assertNotNull(field(reader, "reader"));
    assertEquals("BASE_CONTENT", field(reader, "state").toString());

    Map<FilePath, String> variants = field(reader, "variants");
    assertNotNull(variants);
    assertEquals(12, variants.size());

    assertTrue(variants.keySet().contains(filePath("res/page/page_mdd.css")));
    assertTrue(variants.keySet().contains(filePath("res/page/page_mdd_portrait.css")));
    assertTrue(variants.keySet().contains(filePath("res/page/page_mdd_landscape.css")));

    assertTrue(variants.keySet().contains(filePath("res/page/page_smd.css")));
    assertTrue(variants.keySet().contains(filePath("res/page/page_smd_portrait.css")));
    assertTrue(variants.keySet().contains(filePath("res/page/page_smd_landscape.css")));

    assertTrue(variants.keySet().contains(filePath("res/page/page_xsd.css")));
    assertTrue(variants.keySet().contains(filePath("res/page/page_xsd_portrait.css")));
    assertTrue(variants.keySet().contains(filePath("res/page/page_xsd_landscape.css")));

    assertTrue(variants.keySet().contains(filePath("res/page/page_w1200.css")));
    assertTrue(variants.keySet().contains(filePath("res/page/page_w800.css")));
    assertTrue(variants.keySet().contains(filePath("res/page/page_h600.css")));
  }

  @Test
  public void styleReaderProcessing() throws IOException
  {
    log.trace("testStyleReaderProcessing()");
    project = project("styles");
    FilePath styleFile = filePath("res/page/page.css");
    StyleReader reader = new StyleReader(styleFile);

    StringWriter writer = new StringWriter();
    Files.copy(reader, writer);
    String style = writer.toString();

    assertTrue(style.contains("@style/dialog"));
    assertTrue(style.contains("width: 960px;"));
    assertTrue(style.contains("width: @eval(add @dimen/mobile-width @dimen/desktop-width);"));
    assertTrue(style.contains("@media screen and (max-width : 1200px) {"));
    assertTrue(style.contains("width: @dimen/desktop-width;"));
    assertTrue(style.contains("@media screen and (max-width : 800px) {"));
    assertTrue(style.contains("width: @dimen/mobile-width;"));
    assertTrue(style.contains("@media screen and (max-height : 600px) {"));
    assertTrue(style.contains("height: @dimen/mobile-height;"));
    // assertTrue(style.contains("@media screen and (max-device-width : 767px) and (orientation: portrait) {"));
    // assertTrue(style.contains("@media screen and (max-device-width : 767px) {"));
    // assertTrue(style.contains("@media screen and (min-device-width : 768px) {"));
  }

  // ------------------------------------------------------
  // StyleExtensionReader

  @Test
  public void styleExtensionReaderIterator()
  {
    log.trace("testStyleExtensionReaderIterator()");
    Iterator<Character> iterator = newInstance("js.wood.StyleExtensionReader$ExtensionsIterator");
    String[] properties = new String[]
    {
        "-moz-column-count", "-webkit-column-count"
    };
    invoke(iterator, "init", properties, "3");

    StringBuilder extensions = new StringBuilder();
    while(iterator.hasNext()) {
      extensions.append(iterator.next());
    }

    assertEquals("\r\n\t-moz-column-count: 3;\r\n\t-webkit-column-count: 3;", extensions.toString());
  }

  @Test
  public void styleExtensionReaderExtensions()
  {
    log.trace("testStyleExtensionReaderExtensions()");
    Object extensions = newInstance("js.wood.StyleExtensionReader$Extensions");
    assertExtensions(extensions, "column-count: 3;", "\r\n\t-moz-column-count: 3;\r\n\t-webkit-column-count: 3;");
    assertExtensions(extensions, "column-width: auto;", "\r\n\t-moz-column-width: auto;\r\n\t-webkit-column-width: auto;");
    assertExtensions(extensions, "column-gap: 40px;", "\r\n\t-moz-column-gap: 40px;\r\n\t-webkit-column-gap: 40px;");
    assertExtensions(extensions, "column-fill: auto;", "\r\n\t-moz-column-fill: auto;");
  }

  private static void assertExtensions(Object extensions, String declaration, String expected)
  {
    Object builder = newInstance("js.wood.StyleExtensionReader$DeclarationBuilder");

    invoke(builder, "reset");
    for(int i = 0; i < declaration.length(); ++i) {
      invoke(builder, "add", (int)declaration.charAt(i));
    }

    Iterator<Character> iterator = invoke(extensions, "getIterator", builder);
    StringBuilder test = new StringBuilder();
    while(iterator.hasNext()) {
      test.append(iterator.next());
    }

    assertEquals(expected, test.toString());
  }

  @Test
  public void styleExtensionReaderBuilder()
  {
    log.trace("testStyleExtensionReaderBuilder()");
    Object builder = newInstance("js.wood.StyleExtensionReader$DeclarationBuilder");
    assertBuilder(builder, "column-count:3;", "column-count", "3");
    assertBuilder(builder, "column-count : 3;", "column-count", "3");
    assertBuilder(builder, "column-count\t:\t3;", "column-count", "3");
  }

  private static void assertBuilder(Object builder, String declaration, String property, String value)
  {
    invoke(builder, "reset");
    for(int i = 0; i < declaration.length(); ++i) {
      invoke(builder, "add", (int)declaration.charAt(i));
    }

    assertEquals(property, invoke(builder, "getProperty"));
    assertEquals(value, invoke(builder, "getValue"));
  }

  @Test
  public void styleExtensionReaderProcessing() throws IOException
  {
    log.trace("testStyleExtensionReaderProcessing()");
    project = project("styles");
    FilePath styleFile = filePath("res/index/index.css");
    StyleExtensionReader reader = new StyleExtensionReader(new FileReader(styleFile.toFile()));

    StringWriter writer = new StringWriter();
    Files.copy(reader, writer);
    String style = writer.toString();

    assertTrue(style.contains("min-height: 400px;"));
    assertTrue(style.contains("column-count: 4;"));
    assertTrue(style.contains("-moz-column-count: 4;"));
    assertTrue(style.contains("-webkit-column-count: 4;"));
  }

  @Test
  public void styleExtensionReaderVariantProcessing() throws IOException
  {
    log.trace("testStyleExtensionReaderVariantProcessing()");
    project = project("styles");
    FilePath styleFile = filePath("res/index/index.css");
    StyleReader reader = new StyleReader(styleFile);

    StringWriter writer = new StringWriter();
    Files.copy(reader, writer);
    String style = writer.toString();

    // base style
    assertTrue(style.contains("min-height: 400px;"));
    assertTrue(style.contains("column-count: 4;"));
    assertTrue(style.contains("-moz-column-count: 4;"));
    assertTrue(style.contains("-webkit-column-count: 4;"));

    // mobile variant
    assertTrue(style.contains("column-count: 3;"));
    assertTrue(style.contains("-moz-column-count: 3;"));
    assertTrue(style.contains("-webkit-column-count: 3;"));
  }

  // ------------------------------------------------------
  // ResourceType

  @Test
  public void resourceTypeValueOf()
  {
    log.trace("testResourceTypeValueOf()");
    assertEquals(ResourceType.STRING, ResourceType.getValueOf("string"));
    assertEquals(ResourceType.STRING, ResourceType.getValueOf("STRING"));
    assertEquals(ResourceType.UNKNOWN, ResourceType.getValueOf("STRINGx"));
  }

  @Test
  public void resourceTypePredicates()
  {
    log.trace("testResourceTypePredicates()");
    assertTrue(ResourceType.STRING.isVariable());
    assertTrue(ResourceType.TEXT.isVariable());
    assertTrue(ResourceType.COLOR.isVariable());
    assertTrue(ResourceType.DIMEN.isVariable());
    assertTrue(ResourceType.STYLE.isVariable());

    assertFalse(ResourceType.IMAGE.isVariable());
    assertFalse(ResourceType.AUDIO.isVariable());
    assertFalse(ResourceType.VIDEO.isVariable());
    assertFalse(ResourceType.UNKNOWN.isVariable());

    assertTrue(ResourceType.IMAGE.isMedia());
    assertTrue(ResourceType.AUDIO.isMedia());
    assertTrue(ResourceType.VIDEO.isMedia());

    assertFalse(ResourceType.STRING.isMedia());
    assertFalse(ResourceType.TEXT.isMedia());
    assertFalse(ResourceType.COLOR.isMedia());
    assertFalse(ResourceType.DIMEN.isMedia());
    assertFalse(ResourceType.STYLE.isMedia());
    assertFalse(ResourceType.UNKNOWN.isMedia());
  }

  // ------------------------------------------------------
  // FileType

  @Test
  public void fileTypeForExtension()
  {
    log.trace("testFileTypeForExtension()");
    assertEquals(FileType.LAYOUT, FileType.forExtension("htm"));
    assertEquals(FileType.STYLE, FileType.forExtension("css"));
    assertEquals(FileType.SCRIPT, FileType.forExtension("js"));
    assertEquals(FileType.XML, FileType.forExtension("xml"));
    assertEquals(FileType.MEDIA, FileType.forExtension(null));
    assertEquals(FileType.MEDIA, FileType.forExtension(""));
    assertEquals(FileType.MEDIA, FileType.forExtension("png"));
    assertEquals(FileType.MEDIA, FileType.forExtension("avi"));
  }

  @Test
  public void fileTypeEquals()
  {
    log.trace("testFileTypeEquals()");
    assertTrue(FileType.LAYOUT.equals(new File("path/file.htm")));
    assertTrue(FileType.STYLE.equals(new File("path/file.css")));
    assertTrue(FileType.SCRIPT.equals(new File("path/file.js")));
    assertTrue(FileType.XML.equals(new File("path/file.xml")));
    assertTrue(FileType.MEDIA.equals(new File("path/file")));
    assertTrue(FileType.MEDIA.equals(new File("path/file.png")));
    assertTrue(FileType.MEDIA.equals(new File("path/file.avi")));

    assertFalse(FileType.LAYOUT.equals(new File("path/file.css")));
    assertFalse(FileType.LAYOUT.equals(new File("path/file.js")));
    assertFalse(FileType.LAYOUT.equals(new File("path/file.xml")));
    assertFalse(FileType.LAYOUT.equals(new File("path/file")));
  }

  // ------------------------------------------------------
  // Config

  @Test
  public void configConstructor()
  {
    log.trace("testConfigConstructor()");
    project = project("project");
    ProjectConfig config = new ProjectConfig(project);

    assertEquals("j(s)-lib", config.getAuthor());
    assertEquals("project", config.getName(null));
    assertEquals("Test Project", config.getDisplay(null));
    assertEquals("Project used as fixture for unit testing.", config.getDescription(null));
    assertEquals("build/site", config.getSiteDir(null));
    assertEquals("UA-12345678-1", config.getSDKID("analytics"));

    assertEquals(4, config.getLocales().size());
    assertEquals(new Locale("en"), config.getLocales().get(0));
    assertEquals(new Locale("de"), config.getLocales().get(1));
    assertEquals(new Locale("fr"), config.getLocales().get(2));
    assertEquals(new Locale("ro"), config.getLocales().get(3));

    EList metas = config.getMetas();
    assertEquals(2, metas.size());
    assertEquals("meta", metas.item(0).getTag());
    assertEquals("X-UA-Compatible", metas.item(0).getAttr("http-equiv"));
    assertEquals("IE=9; IE=8; IE=7; IE=EDGE", metas.item(0).getAttr("content"));

    List<String> fonts = config.getFonts();
    assertEquals(2, fonts.size());
    assertEquals("http://fonts.googleapis.com/css?family=Roboto", fonts.get(0));
  }

  @Test
  public void configGetExcludes()
  {
    log.trace("testConfigGetExcludes()");
    DocumentBuilder builder = new DocumentBuilderImpl();
    Document doc = builder.parseXML("<project><excludes>page/about, res/compo/video-player/video-player.xml</excludes></project>");

    project = project("project");
    ProjectConfig config = new ProjectConfig(project);
    Classes.setFieldValue(config, "doc", doc);
    List<Path> excludes = config.getExcludes();

    assertNotNull(excludes);
    assertEquals(2, excludes.size());
    assertTrue(excludes.get(0) instanceof CompoPath);
    assertTrue(excludes.get(1) instanceof FilePath);
  }

  // ------------------------------------------------------
  // Descriptor

  @Test
  public void descriptorConstructor()
  {
    log.trace("testDescriptorConstructor()");
    project = project("project");
    ComponentDescriptor descriptor = new ComponentDescriptor(project.getFile("res/page/index/index.xml"), nullReferenceHandler());

    assertNotNull(field(descriptor, "doc"));
    assertEquals("res/page/index/index.xml", field(descriptor, "filePath").toString());
    assertEquals("null reference handler", field(descriptor, "referenceHandler").toString());
    assertNotNull(field(descriptor, "resolver"));
  }

  @Test
  public void descriptorValues()
  {
    log.trace("testDescriptorValues()");
    Project project = project("project");
    project.scanBuildFiles();
    CompoPath compoPath = new CompoPath(project, "page/index");

    final Variables variables = project.getVariables().get(compoPath);
    ComponentDescriptor descriptor = new ComponentDescriptor(project.getFile("res/page/index/index.xml"), new ReferenceHandler()
    {
      @Override
      public String onResourceReference(Reference reference, FilePath sourceFile) throws IOException
      {
        return variables.get(new Locale("en"), reference, sourceFile, this);
      }
    });

    assertEquals("Index Page", descriptor.getTitle(null));
    assertEquals("Index page description.", descriptor.getDescription(null));
  }
}
