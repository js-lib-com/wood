package js.wood.unit;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Locale;

import js.util.Files;
import js.util.Strings;
import js.wood.DirPath;
import js.wood.FilePath;
import js.wood.Project;
import js.wood.Reference;
import js.wood.ReferenceHandler;
import js.wood.ReferencesResolver;
import js.wood.ResourceType;
import js.wood.SourceReader;
import js.wood.Variables;
import js.wood.WoodException;

import org.junit.Before;
import org.junit.Test;

public class ResourcesTest extends WoodTestCase implements ReferenceHandler
{
  private Project project;

  @Before
  public void beforeTest() throws Exception
  {
    project = project("resources");
  }

  private FilePath filePath(String path)
  {
    return new FilePath(project, path);
  }

  private DirPath dirPath(String path)
  {
    return new DirPath(project, path);
  }

  // ------------------------------------------------------
  // Reference

  @Test
  public void referenceValuesConstructor()
  {
    assertEquals("@string/string-value", new Reference(ResourceType.STRING, "string-value").toString());
    assertEquals("@text/text-value", new Reference(ResourceType.TEXT, "text-value").toString());
    assertEquals("@color/color-value", new Reference(ResourceType.COLOR, "color-value").toString());
    assertEquals("@dimen/dimen-value", new Reference(ResourceType.DIMEN, "dimen-value").toString());
    assertEquals("@style/style-value", new Reference(ResourceType.STYLE, "style-value").toString());
    assertEquals("@audio/audio-value", new Reference(ResourceType.AUDIO, "audio-value").toString());
    assertEquals("@image/image-value", new Reference(ResourceType.IMAGE, "image-value").toString());
    assertEquals("@image/ext/java", new Reference(ResourceType.IMAGE, "ext/java").toString());
    assertEquals("@video/video-value", new Reference(ResourceType.VIDEO, "video-value").toString());
    assertEquals("@unknown/unknown-value", new Reference(ResourceType.UNKNOWN, "unknown-value").toString());
  }

  @Test
  public void referenceParserConstructor()
  {
    assertEquals("@string/string-value", new Reference("@string/string-value").toString());
    assertEquals("@text/text-value", new Reference("@text/text-value").toString());
    assertEquals("@color/color-value", new Reference("@color/color-value").toString());
    assertEquals("@dimen/dimen-value", new Reference("@dimen/dimen-value").toString());
    assertEquals("@style/style-value", new Reference("@style/style-value").toString());
    assertEquals("@audio/audio-value", new Reference("@audio/audio-value").toString());
    assertEquals("@image/image-value", new Reference("@image/image-value").toString());
    assertEquals("@video/video-value", new Reference("@video/video-value").toString());
    assertEquals("@unknown/game-value", new Reference("@game/game-value").toString());
  }

  @Test
  public void referenceIsValid()
  {
    assertTrue(new Reference("@string/string-value").isValid());
    assertFalse(new Reference("@game/game-value").isValid());
  }

  @Test
  public void referenceEquals()
  {
    Reference r1 = new Reference("@string/string-value");
    Reference r2 = new Reference("@string/string-value");
    assertFalse(r1 == r2);
    assertEquals(r1, r2);
  }

  @Test
  public void referenceBadFormat()
  {
    try {
      new Reference("res/exception/bad-type");
      fail("Bad reference format should rise exception.");
    }
    catch(Exception e) {
      assertTrue(e instanceof WoodException);
      assertTrue(e.getMessage().contains("Invalid reference"));
    }
  }

  // ------------------------------------------------------
  // Variables

  @Test
  public void variablesComponentConstructor() throws IOException
  {
    Variables variables = new Variables(project, dirPath("res/compo"));

    assertEquals("Component Title", variable(variables, ResourceType.STRING, "title"));
    assertEquals("This is <em>alert</em> message.", variable(variables, ResourceType.TEXT, "alert"));
    assertEquals("#000000", variable(variables, ResourceType.COLOR, "compo-header-bg"));
    assertEquals("80px", variable(variables, ResourceType.DIMEN, "compo-height"));
    assertTrue(variable(variables, ResourceType.STYLE, "compo").contains("background-color: #80A0A0;"));
  }

  @Test
  public void variablesAssetConstructor() throws Throwable
  {
    Variables variables = new Variables(project, dirPath("res/asset"));

    assertEquals("kids (a)cademy", variable(variables, ResourceType.STRING, "logo-type"));
    assertEquals("This is <em>alert</em> message.", variable(variables, ResourceType.TEXT, "alert"));
    assertEquals("#80A0A0", variable(variables, ResourceType.COLOR, "page-header-link"));
    assertEquals("80px", variable(variables, ResourceType.DIMEN, "page-header-height"));
    assertTrue(variable(variables, ResourceType.STYLE, "dialog").contains("background-color: #000000;"));
  }

  @Test
  public void variablesIncompleteConstructor() throws Throwable
  {
    Variables variables = new Variables(project, dirPath("res/incomplete"));
    variables.setAssetVariables(new Variables(project, dirPath("res/asset")));

    assertEquals("kids (a)cademy", variable(variables, ResourceType.STRING, "logo-type"));
    assertEquals("#80A0A0", variable(variables, ResourceType.COLOR, "page-header-link"));
    assertEquals("80px", variable(variables, ResourceType.DIMEN, "page-header-height"));
    assertTrue(variable(variables, ResourceType.STYLE, "dialog").contains("background-color: #000000;"));
  }

  @Test
  public void variablesLoadStyle()
  {
    Variables variables = new Variables(project, dirPath("res/compo"));
    variables.setAssetVariables(new Variables(project, dirPath("res/asset")));

    String value = variable(variables, ResourceType.STYLE, "window");
    assertTrue(value.contains("display: block;"));

    value = variable(variables, ResourceType.STYLE, "dialog");
    assertTrue(value.contains("display: block;"));
    assertTrue(value.contains("background-color: #000000;"));
    assertTrue(value.contains("color: #FFFFFF;"));

    value = variable(variables, ResourceType.STYLE, "compo");

    assertTrue(value.contains("display: block;"));
    assertTrue(value.contains("background-color: #80A0A0;"));
    assertTrue(value.contains("color: #FFFFFF;"));
    assertTrue(value.contains("background-color: #000000;"));
    assertTrue(value.contains("width: 50%;"));
    assertTrue(value.contains("height: 80px;"));
  }

  @Test
  public void variablesNestedValues() throws IOException
  {
    Variables variables = new Variables(project, dirPath("res/asset"));

    assertNotNull("Message text variable not found", variable(variables, ResourceType.TEXT, "message"));
    assertTrue(variable(variables, ResourceType.TEXT, "message").contains("kids (a)cademy"));
  }

  @Test
  public void variablesMultilanguage() throws IOException
  {
    Variables variables = new Variables(project, dirPath("res/multilanguage"));

    assertEquals("kids (a)cademy", variable(variables, "en", ResourceType.STRING, "logo-type"));
    assertEquals("This is <em>alert</em> message.", variable(variables, "en", ResourceType.TEXT, "alert"));

    assertEquals("academia copiilor", variable(variables, "ro", ResourceType.STRING, "logo-type"));
    assertEquals("Acesta este o <em>alertă</em>.", variable(variables, "ro", ResourceType.TEXT, "alert"));
  }

  @Test
  public void variablesMissingValue()
  {
    try {
      Variables variables = new Variables(project, dirPath("res/compo"));
      variable(variables, ResourceType.STRING, "fake-variable");
      fail("Missing variables value should rise exception.");
    }
    catch(Exception e) {
      assertTrue(e instanceof WoodException);
      assertTrue(e.getMessage().equals("Missing variables |@string/fake-variable| referenced from |res/compo/compo.htm|."));
    }
  }

  @Test
  public void variablesBadResourceType()
  {
    try {
      new Variables(project, dirPath("res/exception/bad-type"));
      fail("Bad resource type into variables file should rise exception.");
    }
    catch(Exception e) {
      assertTrue(e instanceof WoodException);
      assertTrue(e.getMessage().contains("Bad resource type "));
    }
  }

  @Test
  public void variablesCircularReferences()
  {
    try {
      Variables variables = new Variables(project, dirPath("res/exception/circular-reference"));
      FilePath source = filePath("res/exception/circular-reference/circular-reference.htm");
      variables.get(null, new Reference(source, ResourceType.STRING, "reference"), source, this);
      fail("Circular reference into variables file should rise exception.");
    }
    catch(Exception e) {
      assertTrue(e instanceof WoodException);
      assertTrue(e.getMessage().contains("Circular variable references"));
    }
  }

  @Test
  public void variablesSelfReference()
  {
    try {
      Variables variables = new Variables(project, dirPath("res/exception/self-reference"));
      FilePath source = filePath("res/exception/self-reference/self-reference.htm");
      variables.get(null, new Reference(source, ResourceType.STRING, "reference"), source, this);
      fail("Self reference into variables file should rise exception.");
    }
    catch(Exception e) {
      assertTrue(e instanceof WoodException);
      assertTrue(e.getMessage().contains("Circular variable references"));
    }
  }

  @Test
  public void variableStringWithFormattingTag()
  {
    try {
      new Variables(project, dirPath("res/exception/nested-element"));
    }
    catch(Exception e) {
      assertTrue(e instanceof WoodException);
      assertTrue(e.getMessage().contains("Not allowed nested element |code| in  file |"));
    }
  }

  private String variable(Variables variables, ResourceType type, String name)
  {
    FilePath source = filePath("res/compo/compo.htm");
    return variables.get(new Locale("en"), new Reference(source, type, name), source, this);
  }

  private String variable(Variables variables, String language, ResourceType type, String name)
  {
    FilePath source = filePath("res/compo/compo.htm");
    return variables.get(new Locale(language), new Reference(source, type, name), source, this);
  }

  // ------------------------------------------------------
  // ReferencesResolver

  @Test
  public void valueReferencesResolverParse()
  {
    String value = "<h1>@string/title</h1>";
    FilePath sourceFile = filePath("res/compo/compo.htm");

    ReferencesResolver resolver = new ReferencesResolver();
    value = resolver.parse(value, sourceFile, new ReferenceHandler()
    {
      @Override
      public String onResourceReference(Reference reference, FilePath sourceFile) throws IOException
      {
        return "resource value";
      }
    });

    assertEquals("<h1>resource value</h1>", value);
  }

  // ------------------------------------------------------
  // SourceReader$MetaBuilder

  @Test
  public void sourceReaderBuilderReferenceEndMark()
  {
    char[] endMarks = ".<\"'; )\r\n".toCharArray();
    for(int i = 0; i < endMarks.length; ++i) {
      assertEquals("@string/title", metaBuilder("@string/title" + endMarks[i]).toString());
    }
  }

  @Test
  public void sourceReaderBuilderPredicates()
  {
    assertTrue((boolean)invoke(metaBuilder("@string/title"), "isReference"));
    assertFalse((boolean)invoke(metaBuilder("@string/title"), "isParameter"));
    assertFalse((boolean)invoke(metaBuilder("@string/title"), "isEvaluation"));

    assertFalse((boolean)invoke(metaBuilder("@param/caption"), "isReference"));
    assertTrue((boolean)invoke(metaBuilder("@param/caption"), "isParameter"));
    assertFalse((boolean)invoke(metaBuilder("@param/caption"), "isEvaluation"));

    assertFalse((boolean)invoke(metaBuilder("@eval(add, @dimen/header-height, @dimen/footer-height)"), "isReference"));
    assertFalse((boolean)invoke(metaBuilder("@eval(add, @dimen/header-height, @dimen/footer-height)"), "isParameter"));
    assertTrue((boolean)invoke(metaBuilder("@eval(add, @dimen/header-height, @dimen/footer-height)"), "isEvaluation"));

    assertFalse((boolean)invoke(metaBuilder("@keyframes"), "isReference"));
    assertFalse((boolean)invoke(metaBuilder("@keyframes"), "isParameter"));
    assertFalse((boolean)invoke(metaBuilder("@keyframes"), "isEvaluation"));
  }

  @Test
  public void sourceReaderBuilderGetReference()
  {
    assertEquals(new Reference(ResourceType.STRING, "title"), invoke(metaBuilder("@string/title"), "getReference"));
    assertEquals(new Reference(ResourceType.IMAGE, "icon/logo"), invoke(metaBuilder("@image/icon/logo"), "getReference"));
  }

  @Test
  public void sourceReaderBuilderGetExpression()
  {
    assertEquals("(add, 1, 2, 3)", invoke(metaBuilder("@eval(add, 1, 2, 3)"), "getExpression"));
    assertEquals("(add, @dimen/header-height, @dimen/footer-height)", invoke(metaBuilder("@eval(add, @dimen/header-height, @dimen/footer-height)"), "getExpression"));
    assertEquals("(add, (mul, 1, 2), (mul, 3, 4))", invoke(metaBuilder("@eval(add, (mul, 1, 2), (mul, 3, 4))"), "getExpression"));
  }

  @Test
  public void sourceReaderBuilderGetParameter()
  {
    assertEquals("caption", invoke(metaBuilder("@param/caption"), "getParameter"));
  }

  private static Object metaBuilder(String value)
  {
    Project project = new Project("fixture/project");
    FilePath sourceFile = new FilePath(project, "res/page/index/index.htm");
    Object builder = newInstance("js.wood.SourceReader$MetaBuilder", sourceFile);
    for(int i = 0; i < value.length(); ++i) {
      invoke(builder, "add", (int)value.charAt(i));
    }
    invoke(builder, "add", -1);
    return builder;
  }

  // ------------------------------------------------------
  // SourceReader

  @Test
  public void sourceReaderFileConstructor()
  {
    FilePath sourceFile = filePath("res/compo/compo.htm");
    SourceReader reader = new SourceReader(sourceFile, this);
    assertReader(reader);
  }

  @Test
  public void sourceReaderReaderConstructor() throws FileNotFoundException
  {
    FilePath sourceFile = filePath("res/compo/compo.htm");
    FileReader fileReader = new FileReader(sourceFile.toFile());
    SourceReader reader = new SourceReader(fileReader, sourceFile, this);
    assertReader(reader);
  }

  private void assertReader(SourceReader reader)
  {
    assertEquals("res/compo/compo.htm", field(reader, "sourceFile").toString());
    assertEquals(this, field(reader, "referenceHandler"));
    assertNotNull(field(reader, "reader"));
    assertNotNull(field(reader, "metaBuilder"));
    assertEquals("TEXT", field(reader, "state").toString());
    assertNull(field(reader, "value"));
    assertEquals(0, (int)field(reader, "valueIndex"));
    assertEquals(0, (int)field(reader, "charAfterMeta"));
  }

  @Test
  public void sourceReaderReferenceHandler() throws IOException
  {
    FilePath sourceFile = filePath("res/compo/compo.htm");

    SourceReader reader = new SourceReader(sourceFile, new ReferenceHandler()
    {
      @Override
      public String onResourceReference(Reference reference, FilePath sourceFile) throws IOException
      {
        assertEquals(ResourceType.STRING, reference.getResourceType());
        assertEquals("title", reference.getName());
        assertEquals("res/compo/compo.htm", sourceFile.value());
        return reference.toString();
      }
    });

    StringWriter writer = new StringWriter();
    Files.copy(reader, writer);
  }

  @Test
  public void sourceReaderCopy() throws IOException
  {
    FilePath sourceFile = filePath("res/compo/compo.htm");
    SourceReader reader = new SourceReader(sourceFile, this);

    StringWriter writer = new StringWriter();
    Files.copy(reader, writer);
    assertTrue(writer.toString().contains("Component Title"));
  }

  @Test
  public void referenceUnknownType() throws IOException
  {
    FilePath sourceFile = filePath("res/exception/unknown-type/unknown-type.htm");
    SourceReader reader = new SourceReader(sourceFile, this);
    StringWriter writer = new StringWriter();

    try {
      Files.copy(reader, writer);
    }
    catch(Exception e) {
      assertTrue(e instanceof WoodException);
      assertTrue(e.getMessage().contains("|@strings/value|"));
    }
  }

  // ------------------------------------------------------
  // Internal helpers

  @Override
  public String onResourceReference(Reference reference, FilePath sourcePath) throws IOException
  {
    if(reference.isVariable()) {
      Variables variables = new Variables(project, sourcePath.getDirPath());
      if(project.getAssetsDir().exists()) {
        invoke(variables, "load", project.getAssetsDir());
      }
      return variables.get(new Locale("en"), reference, sourcePath, this);
    }
    else {
      return Strings.concat("media/", reference.getName(), ".png");
    }
  }
}
