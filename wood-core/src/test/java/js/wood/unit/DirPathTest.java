package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import js.wood.DirPath;
import js.wood.FilePath;
import js.wood.Project;
import js.wood.WoodException;
import js.wood.impl.FileType;
import js.wood.impl.FilesHandler;

public class DirPathTest {
	private Project project;

	@Before
	public void beforeTest() {
		project = new Project(new File("src/test/resources/project"));
	}

	@Test
	public void constructor() {
		assertDirPath("res/", "res/", "res");
		assertDirPath("res/path/compo", "res/path/compo/", "compo");
		assertDirPath("res/path/compo/", "res/path/compo/", "compo");
		assertDirPath("res/compo/video-player", "res/compo/video-player/", "video-player");
		assertDirPath("lib/js-lib", "lib/js-lib/", "js-lib");
		assertDirPath("script/js/wood/test", "script/js/wood/test/", "test");
		assertDirPath("gen/js/wood/test", "gen/js/wood/test/", "test");
	}

	private void assertDirPath(String pathValue, String value, String name) {
		DirPath dir = new DirPath(project, pathValue);
		assertThat(dir.value(), equalTo(value));
		assertThat(dir.toString(), equalTo(value));
		assertThat(dir.getName(), equalTo(name));
	}

	@Test
	public void constructor_InvalidValue() {
		for (String path : new String[] { "invalid-source-dir", "/res/absolute/path", "res/invalid_name/" }) {
			try {
				new DirPath(project, path);
				fail("Invlaid directory path should rise exception.");
			} catch (Exception e) {
				assertThat(e, instanceOf(WoodException.class));
			}
		}
	}

	@Test
	public void segments() {
		DirPath dir = new DirPath(project, "script/js/tools/wood/tests/");
		assertThat(dir.getPathSegments(), notNullValue());
		assertThat(dir.getPathSegments(), hasSize(5));
		assertThat(dir.getPathSegments(), hasItems("script", "js", "tools", "wood", "tests"));
	}

	@Test
	public void segments_SourceDirectoryOnly() {
		DirPath dir = new DirPath(project, "script/");
		assertThat(dir.getPathSegments(), notNullValue());
		assertThat(dir.getPathSegments(), hasSize(1));
		assertThat(dir.getPathSegments(), hasItems("script"));
	}

	@Test
	public void files_Iterable() {
		DirPath dir = new DirPath(project, "res/compo/discography");

		List<String> files = new ArrayList<String>();
		for (FilePath file : dir.files()) {
			files.add(file.value());
		}

		assertFiles(files);
	}

	@Test
	public void files_Iterator() {
		DirPath dir = new DirPath(project, "res/compo/discography");
		Iterator<FilePath> it = dir.files().iterator();

		List<String> files = new ArrayList<String>();
		while (it.hasNext()) {
			files.add(it.next().value());
		}

		assertFiles(files);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void files_Iterable_Remove() {
		DirPath dir = new DirPath(project, "res/compo/discography");
		dir.files().iterator().remove();
	}

	@Test
	public void files_FilesHandler() {
		DirPath dir = new DirPath(project, "res/compo/discography");

		final List<String> files = new ArrayList<String>();
		dir.files(new FilesHandler() {
			@Override
			public void onFile(FilePath file) throws Exception {
				files.add(file.value());
			}
		});

		assertFiles(files);
	}

	@Test
	public void files_MissingDirectory() {
		DirPath dir = new DirPath(project, "res/compo/missing");
		assertThat(dir.files(), notNullValue());
		assertThat(dir.files().iterator().hasNext(), is(false));
	}

	private static void assertFiles(List<String> files) {
		assertThat(files, hasSize(4));
		assertThat(files, hasItems("res/compo/discography/discography.css", "res/compo/discography/discography.htm", "res/compo/discography/logo.png", "res/compo/discography/preview.js"));
	}

	@Test
	public void files_ListByType() {
		DirPath dir = new DirPath(project, "res/asset");

		final List<String> files = new ArrayList<>();
		dir.files(FileType.XML, new FilesHandler() {
			@Override
			public void onFile(FilePath file) throws Exception {
				files.add(file.value());
			}
		});

		assertThat(files, hasSize(6));
		assertThat(files, hasItems("res/asset/colors.xml", "res/asset/dimens.xml", "res/asset/links.xml", "res/asset/strings.xml", "res/asset/styles.xml", "res/asset/text.xml"));
	}

	@Test
	public void files_ListByType_Subdirectories() {
		DirPath dir = new DirPath(project, "res/compo");

		final List<String> files = new ArrayList<>();
		dir.files(FileType.LAYOUT, new FilesHandler() {
			@Override
			public void onDirectory(DirPath dir) throws Exception {
				dir.files(FileType.LAYOUT, this);
			}

			@Override
			public void onFile(FilePath file) throws Exception {
				files.add(file.value());
			}
		});

		assertThat(files, hasSize(3));
		assertThat(files, hasItems("res/compo/discography/discography.htm", "res/compo/select.htm", "res/compo/video-player/video-player.htm"));
	}

	@Test
	public void files_ListByType_DoNoAccept() {
		DirPath dir = new DirPath(project, "res/compo");

		final List<String> files = new ArrayList<>();
		dir.files(FileType.LAYOUT, new FilesHandler() {
			@Override
			public void onFile(FilePath file) throws Exception {
				files.add(file.value());
			}

			@Override
			public boolean accept(FilePath file) {
				return false;
			}
		});

		assertThat(files, empty());
	}

	@Test(expected = WoodException.class)
	public void files_ListByType_IOException() {
		DirPath dir = new DirPath(project, "res/compo");

		dir.files(FileType.LAYOUT, new FilesHandler() {
			@Override
			public void onFile(FilePath file) throws Exception {
				throw new IOException();
			}
		});
	}

	@Test(expected = WoodException.class)
	public void files_ListByType_WoodException() {
		DirPath dir = new DirPath(project, "res/compo");

		dir.files(FileType.LAYOUT, new FilesHandler() {
			@Override
			public void onFile(FilePath file) throws Exception {
				throw new WoodException("");
			}
		});
	}

	@Test
	public void files_ListByType_MissingDirectory() {
		DirPath dir = new DirPath(project, "res/missing");

		final List<String> files = new ArrayList<>();
		dir.files(FileType.XML, new FilesHandler() {
			@Override
			public void onFile(FilePath file) throws Exception {
				files.add(file.value());
			}
		});

		assertThat(files, empty());
	}

	@Test
	public void getSubdirPath() {
		DirPath dir = new DirPath(project, "res/compo/discography");
		assertThat(dir.getSubdirPath("actions").value(), equalTo("res/compo/discography/actions/"));
	}

	@Test
	public void getFilePath() {
		DirPath dir = new DirPath(project, "res/compo/discography");
		assertThat(dir.getFilePath("strings.xml").value(), equalTo("res/compo/discography/strings.xml"));
	}

	@Test
	public void accept() {
		assertTrue(DirPath.accept("lib/video-player/"));
		assertTrue(DirPath.accept("lib/video-player"));
		assertTrue(DirPath.accept("script/js/wood/test"));
		assertTrue(DirPath.accept("gen/js/wood/controller"));
		assertTrue(DirPath.accept("java/js/wood/test"));

		assertFalse(DirPath.accept("lib/video-player/video-player.htm"));
		assertFalse(DirPath.accept("lib/video-player#body"));
	}

	@Test
	public void excluded() {
		assertTrue(new DirPath(project, "res/page/about/").isExcluded());
		assertTrue(new DirPath(project, "res/page/about").isExcluded());
		assertFalse(new DirPath(project, "res/compo/fake/").isExcluded());
	}
}
