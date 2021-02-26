package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.impl.FilesHandler;

@RunWith(MockitoJUnitRunner.class)
public class DirPathTest {
	@Mock
	private Project project;

	@Before
	public void beforeTest() {
		when(project.getProjectRoot()).thenReturn(new File("."));
	}

	@Test
	public void constructor() {
		assertDirPath("res/", "res/", "res");
		assertDirPath("res/path/compo/", "res/path/compo/", "compo");
		assertDirPath("res/compo/video-player/", "res/compo/video-player/", "video-player");
		assertDirPath("lib/js-lib/", "lib/js-lib/", "js-lib");
		assertDirPath("script/js/wood/test/", "script/js/wood/test/", "test");
		assertDirPath("gen/js/wood/test/", "gen/js/wood/test/", "test");
	}

	private void assertDirPath(String pathValue, String value, String name) {
		DirPath dirPath = new DirPath(project, pathValue);
		assertThat(dirPath.value(), equalTo(value));
		assertThat(dirPath.toString(), equalTo(value));
		assertThat(dirPath.getName(), equalTo(name));
		assertFalse(dirPath.isRoot());
		assertFalse(dirPath.isComponent());
	}

	@Test
	public void constructor_RootDirectory() {
		DirPath dirPath = new DirPath(project);
		assertThat(dirPath.value(), equalTo("."));
		assertThat(dirPath.toString(), equalTo("."));
		assertThat(dirPath.getName(), equalTo("."));
		assertTrue(dirPath.isRoot());
		assertFalse(dirPath.isComponent());
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
	public void getFilePath() {
		DirPath dirPath = new DirPath(project, "res/page/");
		FilePath filePath = dirPath.getFilePath("strings.xml");
		assertThat(filePath, notNullValue());
		assertThat(filePath.value(), equalTo("res/page/strings.xml"));
	}

	@Test
	public void getSubdirPath() {
		DirPath dirPath = new DirPath(project, "res/page/");
		DirPath subdirPath = dirPath.getSubdirPath("media");
		assertThat(subdirPath, notNullValue());
		assertThat(subdirPath.value(), equalTo("res/page/media/"));
	}

	@Test
	public void getSubdirPath_WithTrailingSeparator() {
		DirPath dirPath = new DirPath(project, "res/page/");
		DirPath subdirPath = dirPath.getSubdirPath("media/");
		assertThat(subdirPath, notNullValue());
		assertThat(subdirPath.value(), equalTo("res/page/media/"));
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
	public void iterable() {
		File[] sources = new File[] { new SourceFile("compo.htm"), new SourceFile("compo.css") };
		initSources(sources);
		DirPath dirPath = new DirPath(project, directory(sources));

		List<String> files = new ArrayList<String>();
		for (FilePath file : dirPath) {
			files.add(file.value());
		}

		assertThat(files, hasSize(2));
		assertThat(files, hasItems("compo.htm", "compo.css"));
	}

	@Test
	public void iterable_Subdirectories() {
		File[] sources = new File[] { new SourceDirectory("res/compo") };
		DirPath dirPath = new DirPath(project, directory(sources));

		final List<String> files = new ArrayList<>();
		for (FilePath file : dirPath) {
			files.add(file.value());
		}

		assertThat(files, empty());
	}

	@Test
	public void iterable_NotExists() {
		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn("res/compo");
		when(dir.exists()).thenReturn(false);

		DirPath dirPath = new DirPath(project, dir);

		List<String> files = new ArrayList<String>();
		for (FilePath file : dirPath) {
			files.add(file.value());
		}

		assertThat(files, empty());
	}

	@Test
	public void iterable_NullListFiles() {
		DirPath dirPath = new DirPath(project, directory(null));

		List<String> files = new ArrayList<String>();
		for (FilePath file : dirPath) {
			files.add(file.value());
		}

		assertThat(files, empty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void iterable_Remove() {
		DirPath dirPath = new DirPath(project, directory(new File[0]));
		dirPath.iterator().remove();
	}

	@Test
	public void filter() {
		File[] sources = new File[] { new SourceFile("compo.htm"), new SourceFile("compo.css") };
		initSources(sources);
		DirPath dirPath = new DirPath(project, directory(sources));
		List<FilePath> files = dirPath.filter(filePath -> filePath.isStyle());

		assertThat(files, hasSize(1));
		assertThat(files.get(0).value(), equalTo("compo.css"));
	}

	@Test
	public void findFirst() {
		File[] sources = new File[] { new SourceFile("compo.htm"), new SourceFile("compo.css") };
		initSources(sources);
		DirPath dirPath = new DirPath(project, directory(sources));

		FilePath file = dirPath.findFirst(filePath -> filePath.isStyle());
		assertThat(file, notNullValue());
		assertThat(file.value(), equalTo("compo.css"));
	}

	@Test
	public void findFirst_NotFound() {
		File[] sources = new File[] { new SourceFile("compo.htm"), new SourceFile("compo.css") };
		initSources(sources);
		DirPath dirPath = new DirPath(project, directory(sources));
		assertThat(dirPath.findFirst(filePath -> filePath.isScript()), nullValue());
	}

	@Test
	public void files() {
		File[] sources = new File[] { new SourceFile("compo.htm"), new SourceFile("compo.css") };
		Path[] paths = initSources(sources);

		DirPath dirPath = new DirPath(project, directory(sources));
		dirPath.files(new FilesHandler() {
			private int index = 0;

			@Override
			public void onFile(FilePath filePath) {
				assertThat(filePath, equalTo(paths[index++]));
			}
		});
	}

	@Test
	public void files_RootDirectory() {
		File[] sources = new File[] { new SourceFile("project.xml") };
		Path[] paths = initSources(sources);

		DirPath dirPath = new DirPath(project, directory(sources));
		dirPath.files(new FilesHandler() {
			private int index = 0;

			@Override
			public void onFile(FilePath filePath) {
				assertThat(filePath, equalTo(paths[index++]));
			}
		});
	}

	@Test
	public void files_Accept() {
		File[] sources = new File[] { new SourceFile("compo.htm"), new SourceFile("compo.css") };
		initSources(sources);

		DirPath dirPath = new DirPath(project, directory(sources));
		final List<String> files = new ArrayList<String>();
		dirPath.files(new FilesHandler() {
			@Override
			public boolean accept(FilePath file) {
				return file.isLayout();
			}

			@Override
			public void onFile(FilePath file) {
				files.add(file.value());
			}
		});

		assertThat(files, hasSize(1));
		assertThat(files, hasItems("compo.htm"));
	}

	@Test
	public void files_HiddenFile() {
		File[] sources = new File[] { new SourceFile(".gitignore"), new SourceFile("compo.htm"), new SourceFile("compo.css") };
		Path[] paths = initSources(sources);

		DirPath dirPath = new DirPath(project, directory(sources));
		dirPath.files(new FilesHandler() {
			private int index = 0;

			@Override
			public void onFile(FilePath filePath) {
				assertThat(index, lessThan(2));
				assertThat(filePath, equalTo(paths[index++]));
			}
		});
	}

	@Test
	public void files_Subdir() {
		File[] sources = new File[] { new SourceDirectory("media") };

		DirPath subdir = new DirPath(project, "media/");
		when(project.createDirPath(sources[0])).thenReturn(subdir);

		DirPath dirPath = new DirPath(project, directory(sources));
		dirPath.files(new FilesHandler() {
			@Override
			public void onDirectory(DirPath dir) {
				assertThat(dir, equalTo(subdir));
			}
		});
	}

	@Test
	public void files_Subdir_TrailingSeparator() {
		File[] sources = new File[] { new SourceDirectory("media/") };
		Path[] paths = initSources(sources);

		DirPath dirPath = new DirPath(project, directory(sources));
		dirPath.files(new FilesHandler() {
			@Override
			public void onDirectory(DirPath dir) {
				assertThat(dir, equalTo(paths[0]));
			}
		});
	}

	@Test
	public void files_SubdirAndFiles() {
		File[] sources = new File[] { new SourceDirectory("media"), new SourceFile("compo.htm"), new SourceFile("compo.css") };
		initSources(sources);

		DirPath dirPath = new DirPath(project, directory(sources));

		final List<String> result = new ArrayList<String>();
		dirPath.files(new FilesHandler() {
			@Override
			public void onDirectory(DirPath dir) {
				assertThat(dir.value(), equalTo("media/"));
			}

			@Override
			public void onFile(FilePath file) {
				result.add(file.value());
			}
		});

		assertThat(result, hasSize(2));
		assertThat(result, hasItems("compo.htm", "compo.css"));
	}

	@Test
	public void files_NotExists() {
		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn("res/compo");
		when(dir.exists()).thenReturn(false);

		DirPath dirPath = new DirPath(project, dir);
		dirPath.files(null);
	}

	@Test(expected = WoodException.class)
	public void files_NulllistFiles() {
		DirPath dirPath = new DirPath(project, directory(null));
		dirPath.files(null);
	}

	@Test
	public void accept() {
		assertTrue(DirPath.accept("lib/video-player/"));
		assertTrue(DirPath.accept("script/js/wood/test/"));
		assertTrue(DirPath.accept("gen/js/wood/controller/"));
		assertTrue(DirPath.accept("java/js/wood/test/"));

		assertFalse(DirPath.accept("lib/video-player"));
		assertFalse(DirPath.accept("lib/video-player/video-player.htm"));
		assertFalse(DirPath.accept("lib/video-player#body"));
	}

	@Test
	public void excluded() {
		DirPath dirPath = new DirPath(project, "res/page/about/");
		when(project.getExcludes()).thenReturn(Arrays.asList(dirPath));

		assertTrue(new DirPath(project, "res/page/about/").isExcluded());
		assertFalse(new DirPath(project, "res/compo/fake/").isExcluded());
	}

	// --------------------------------------------------------------------------------------------

	private static class SourceFile extends File {
		private static final long serialVersionUID = -5975578621510948684L;

		public SourceFile(String pathname) {
			super(pathname);
		}

		@Override
		public boolean isFile() {
			return true;
		}
	}

	private static class SourceDirectory extends File {
		private static final long serialVersionUID = 962174848140145344L;

		public SourceDirectory(String pathname) {
			super(pathname);
		}

		@Override
		public boolean isDirectory() {
			return true;
		}
	}

	private File directory(File[] files) {
		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn(".");
		when(dir.exists()).thenReturn(true);
		when(dir.listFiles()).thenReturn(files);
		return dir;
	}

	private Path[] initSources(File[] files) {
		List<Path> paths = new ArrayList<>();
		for (int i = 0; i < files.length; ++i) {
			if (files[i].getName().charAt(0) == '.') {
				continue;
			}
			if (files[i] instanceof SourceDirectory) {
				DirPath path = new DirPath(project, files[i]);
				when(project.createDirPath(files[i])).thenReturn(path);
				paths.add(path);
			} else if (files[i] instanceof SourceFile) {
				FilePath path = new FilePath(project, files[i]);
				when(project.createFilePath(files[i])).thenReturn(path);
				paths.add(path);
			}
		}
		return paths.toArray(new Path[0]);
	}
}
