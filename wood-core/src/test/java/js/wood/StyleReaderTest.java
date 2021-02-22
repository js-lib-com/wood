package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import js.util.Files;
import js.wood.impl.MediaQueries;
import js.wood.impl.MediaQueryDefinition;
import js.wood.impl.Variants;

@RunWith(MockitoJUnitRunner.class)
public class StyleReaderTest {
	@Mock
	private Project project;
	@Mock
	private FilePath styleFile;

	@Before
	public void beforeTest() {
	}

	@Test
	public void constructor() throws IOException {
		class XFile extends File {
			private static final long serialVersionUID = -5975578621510948684L;

			public XFile(String pathname) {
				super(pathname);
			}

			@Override
			public boolean isFile() {
				return true;
			}
		}

		when(project.getProjectRoot()).thenReturn(new File("."));
		when(project.getMediaQueryDefinition("h600")).thenReturn(new MediaQueryDefinition("h600", "min-height: 600px", 0));
		when(project.getMediaQueryDefinition("w800")).thenReturn(new MediaQueryDefinition("w800", "min-width: 800px", 0));
		when(project.getMediaQueryDefinition("w1200")).thenReturn(new MediaQueryDefinition("w1200", "min-width: 1200px", 0));
		when(project.getMediaQueryDefinition("xsd")).thenReturn(new MediaQueryDefinition("xsd", "min-height: 560px", 0));
		when(project.getMediaQueryDefinition("smd")).thenReturn(new MediaQueryDefinition("smd", "min-height: 560px", 0));
		when(project.getMediaQueryDefinition("nod")).thenReturn(new MediaQueryDefinition("nod", "min-height: 560px", 0));
		when(project.getMediaQueryDefinition("mdd")).thenReturn(new MediaQueryDefinition("mdd", "min-height: 768px", 0));
		when(project.getMediaQueryDefinition("lgd")).thenReturn(new MediaQueryDefinition("lgd", "min-height: 992px", 0));
		when(project.getMediaQueryDefinition("landscape")).thenReturn(new MediaQueryDefinition("landscape", "orientation: landscape", 0));
		when(project.getMediaQueryDefinition("portrait")).thenReturn(new MediaQueryDefinition("portrait", "orientation: portrait", 0));

		File[] stylesFiles = new XFile[] { //
				new XFile("res/page/page_lgd.css"), //
				new XFile("res/page/page_nod.css"), //
				new XFile("res/page/page_mdd.css"), //
				new XFile("res/page/page_mdd_portrait.css"), //
				new XFile("res/page/page_mdd_landscape.css"), //
				new XFile("res/page/page_smd.css"), //
				new XFile("res/page/page_smd_portrait.css"), //
				new XFile("res/page/page_smd_landscape.css"), //
				new XFile("res/page/page_xsd.css"), //
				new XFile("res/page/page_xsd_portrait.css"), //
				new XFile("res/page/page_xsd_landscape.css"), //
				new XFile("res/page/page_w1200.css"), //
				new XFile("res/page/page_w800.css"), //
				new XFile("res/page/page_h600.css"), //
				new XFile("res/page/preview.css"), //
				new XFile("res/page/preview.htm"), //
				new XFile("res/page/page.htm") //
		};

		File stylesDir = Mockito.mock(File.class);
		when(stylesDir.exists()).thenReturn(true);
		when(stylesDir.getPath()).thenReturn("res/page");
		when(stylesDir.listFiles()).thenReturn(stylesFiles);
		DirPath parentDir = new DirPath(project, stylesDir);

		when(styleFile.getBaseName()).thenReturn("page");
		when(styleFile.getParentDirPath()).thenReturn(parentDir);

		String source = "body { width: 960px; }";
		when(styleFile.getReader()).thenReturn(new StringReader(source));

		StyleReader reader = new StyleReader(styleFile);
		reader.close();

		assertNotNull(reader.getReader());
		assertThat(reader.getState().toString(), equalTo("BASE_CONTENT"));

		List<FilePath> variants = reader.getVariants();
		assertThat(variants, notNullValue());
		assertThat(variants.size(), equalTo(14));

		assertThat(variants, hasItem(key("res/page/page_lgd.css")));
		assertThat(variants, hasItem(key("res/page/page_nod.css")));

		assertThat(variants, hasItem(key("res/page/page_mdd.css")));
		assertThat(variants, hasItem(key("res/page/page_mdd_portrait.css")));
		assertThat(variants, hasItem(key("res/page/page_mdd_landscape.css")));

		assertThat(variants, hasItem(key("res/page/page_smd.css")));
		assertThat(variants, hasItem(key("res/page/page_smd_portrait.css")));
		assertThat(variants, hasItem(key("res/page/page_smd_landscape.css")));

		assertThat(variants, hasItem(key("res/page/page_xsd.css")));
		assertThat(variants, hasItem(key("res/page/page_xsd_portrait.css")));
		assertThat(variants, hasItem(key("res/page/page_xsd_landscape.css")));

		assertThat(variants, hasItem(key("res/page/page_w1200.css")));
		assertThat(variants, hasItem(key("res/page/page_w800.css")));
		assertThat(variants, hasItem(key("res/page/page_h600.css")));
	}

	private FilePath key(String path) {
		return new FilePath(project, path);
	}

	@Test
	public void processing() throws IOException {
		class Mock {
			FilePath file;

			Mock(String expression, String content) {
				MediaQueries mediaQueries = Mockito.mock(MediaQueries.class);
				when(mediaQueries.getExpression()).thenReturn(expression);

				Variants variants = Mockito.mock(Variants.class);
				when(variants.getMediaQueries()).thenReturn(mediaQueries);

				file = Mockito.mock(FilePath.class);
				when(file.getVariants()).thenReturn(variants);
				when(file.getReader()).thenReturn(new StringReader(content));

			}
		}

		Mock[] mocks = new Mock[] { //
				new Mock("min-width: 680px", "body { width: 680px; }"), //
				new Mock("min-width: 960px", "body { width: 960px; }"), //
				new Mock("min-width: 1200px", "body { width: 1200px; }") //
		};

		DirPath parentDir = Mockito.mock(DirPath.class);
		List<FilePath> files = Arrays.stream(mocks).map(mock -> mock.file).collect(Collectors.toList());
		when(parentDir.filter(any())).thenReturn(files);

		when(styleFile.getParentDirPath()).thenReturn(parentDir);

		String source = "body { width: 560px; }";
		when(styleFile.getReader()).thenReturn(new StringReader(source));

		StyleReader reader = new StyleReader(styleFile);

		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);

		String expected = "" + //
				"body { width: 560px; }\r\n" + //
				"@media screen and min-width: 680px {\r\n" + //
				"\r\n" + //
				"body { width: 680px; }\r\n" + //
				"}\r\n" + //
				"\r\n" + //
				"@media screen and min-width: 960px {\r\n" + //
				"\r\n" + //
				"body { width: 960px; }\r\n" + //
				"}\r\n" + //
				"\r\n" + //
				"@media screen and min-width: 1200px {\r\n" + //
				"\r\n" + //
				"body { width: 1200px; }\r\n" + //
				"}\r\n";

		assertThat(writer.toString(), equalTo(expected));
	}
}
