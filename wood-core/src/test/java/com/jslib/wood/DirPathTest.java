package com.jslib.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.wood.impl.FilesHandler;

@RunWith(MockitoJUnitRunner.class)
public class DirPathTest {
	@Mock
	private Project project;

	@Before
	public void beforeTest() {
		when(project.getProjectRoot()).thenReturn(new File("."));
	}

	@Test
	public void GivenValidPath_WhenConstructor_ThenInternalStateInit() {
		assertDirPath("res/", "res/", "res");
		assertDirPath("res/path/compo/", "res/path/compo/", "compo");
		assertDirPath("res/compo/video-player/", "res/compo/video-player/", "video-player");
		assertDirPath("lib/js-lib/", "lib/js-lib/", "js-lib");
		assertDirPath("script/js/wood/test/", "script/js/wood/test/", "test");
		assertDirPath("gen/js/wood/test/", "gen/js/wood/test/", "test");
	}

	private void assertDirPath(String pathValue, String value, String name) {
		FilePath dirPath = new FilePath(project, pathValue);
		assertThat(dirPath.value(), equalTo(value));
		assertThat(dirPath.toString(), equalTo(value));
		assertThat(dirPath.getName(), equalTo(name));
		assertFalse(dirPath.isProjectRoot());
		assertFalse(dirPath.isComponent());
	}

	@Test
	public void GivenValidDotPath_WhenConstructor_ThenInitAsProjectRoot() {
		FilePath dirPath = new FilePath(project, ".");
		assertThat(dirPath.value(), equalTo("."));
		assertThat(dirPath.toString(), equalTo("."));
		assertThat(dirPath.getName(), equalTo("."));
		assertTrue(dirPath.isProjectRoot());
		assertFalse(dirPath.isComponent());
	}

	@Test(expected = IllegalArgumentException.class)
	public void GivenValidEmptyPath_WhenConstructor_ThenException() {
		new FilePath(project, "");
	}

	@Test(expected = WoodException.class)
	public void GivenPathWithUnderscore_WhenConstruct_ThenException() {
		new FilePath(project, "res/page_en");
	}

	@Test
	public void GivenDirPathWithoutTrailingSeparator_WhenGetFilePath_ThenNotNullValue() {
		// given
		when(project.createFilePath("res/page/strings.xml")).thenReturn(mock(FilePath.class));
		FilePath dirPath = new FilePath(project, "res/page");

		// when
		FilePath filePath = dirPath.getFilePath("strings.xml");

		// then
		assertThat(filePath, notNullValue());
	}

	@Test
	public void GivenDirPathWithoutTrailingSeparator_WhenGetSubdirPath_ThenNotNullValue() {
		// given
		when(project.createFilePath("res/page/media/")).thenReturn(mock(FilePath.class));
		FilePath dirPath = new FilePath(project, "res/page");
		
		// when
		FilePath subdirPath = dirPath.getSubdirPath("media");
		
		// then
		assertThat(subdirPath, notNullValue());
	}

	@Test
	public void getSubdirPath_WithTrailingSeparator() {
		FilePath subdirPath = Mockito.mock(FilePath.class);
		when(subdirPath.value()).thenReturn("res/page/media/");
		when(project.createFilePath("res/page/media/")).thenReturn(subdirPath);

		FilePath dirPath = new FilePath(project, "res/page/");
		subdirPath = dirPath.getSubdirPath("media/");
		assertThat(subdirPath, notNullValue());
		assertThat(subdirPath.value(), equalTo("res/page/media/"));
	}

	@Test
	public void GivenLongPath_WhenGetPathSegments_ThenNotEmptyStringsList() {
		// given
		FilePath dir = new FilePath(project, "script/js/tools/wood/tests/");
		
		// when
		List<String> segments = dir.getPathSegments();
		
		assertThat(segments, notNullValue());
		assertThat(segments, hasSize(5));
		assertThat(segments, hasItems("script", "js", "tools", "wood", "tests"));
	}

	@Test
	public void GivenSingleSegmentPath_WhenGetPathSegments_ThenListWithOneItem() {
		// given
		FilePath dir = new FilePath(project, "script/");
		
		// when
		List<String> segments = dir.getPathSegments();
		
		assertThat(segments, notNullValue());
		assertThat(segments, hasSize(1));
		assertThat(segments, hasItems("script"));
	}

	@Test
	public void GivenDirWithFiles_WhenLoopForEach_ThenCollectAllFiles() {
		// given
		File[] sources = new File[] { new SourceFile("compo.htm"), new SourceFile("compo.css") };
		initSources(sources);
		FilePath dirPath = new FilePath(project, directory(sources));

		// when
		List<String> files = new ArrayList<String>();
		for (FilePath file : dirPath) {
			files.add(file.value());
		}

		// then
		assertThat(files, hasSize(2));
		assertThat(files, hasItems("compo.htm", "compo.css"));
	}

	@Test
	public void iterable_Subdirectories() {
		File[] sources = new File[] { new SourceDirectory("res/compo") };
		FilePath dirPath = new FilePath(project, directory(sources));

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

		FilePath dirPath = new FilePath(project, dir);

		List<String> files = new ArrayList<String>();
		for (FilePath file : dirPath) {
			files.add(file.value());
		}

		assertThat(files, empty());
	}

	@Test
	public void iterable_NullListFiles() {
		FilePath dirPath = new FilePath(project, directory(null));

		List<String> files = new ArrayList<String>();
		for (FilePath file : dirPath) {
			files.add(file.value());
		}

		assertThat(files, empty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void iterable_Remove() {
		FilePath dirPath = new FilePath(project, directory(new File[0]));
		dirPath.iterator().remove();
	}

	@Test
	public void filter() {
		File[] sources = new File[] { new SourceFile("compo.htm"), new SourceFile("compo.css") };
		initSources(sources);
		FilePath dirPath = new FilePath(project, directory(sources));
		List<FilePath> files = dirPath.filter(filePath -> filePath.isStyle());

		assertThat(files, hasSize(1));
		assertThat(files.get(0).value(), equalTo("compo.css"));
	}

	@Test
	public void findFirst() {
		File[] sources = new File[] { new SourceFile("compo.htm"), new SourceFile("compo.css") };
		initSources(sources);
		FilePath dirPath = new FilePath(project, directory(sources));

		FilePath file = dirPath.findFirst(filePath -> filePath.isStyle());
		assertThat(file, notNullValue());
		assertThat(file.value(), equalTo("compo.css"));
	}

	@Test
	public void findFirst_NotFound() {
		File[] sources = new File[] { new SourceFile("compo.htm"), new SourceFile("compo.css") };
		initSources(sources);
		FilePath dirPath = new FilePath(project, directory(sources));
		assertThat(dirPath.findFirst(filePath -> filePath.isScript()), nullValue());
	}

	@Test
	public void files() {
		File[] sources = new File[] { new SourceFile("compo.htm"), new SourceFile("compo.css") };
		FilePath[] paths = initSources(sources);

		FilePath dirPath = new FilePath(project, directory(sources));
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
		FilePath[] paths = initSources(sources);

		FilePath dirPath = new FilePath(project, directory(sources));
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

		FilePath dirPath = new FilePath(project, directory(sources));
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
		FilePath[] paths = initSources(sources);

		FilePath dirPath = new FilePath(project, directory(sources));
		dirPath.files(new FilesHandler() {
			private int index = 1;

			@Override
			public void onFile(FilePath filePath) {
				assertThat(index, lessThan(3));
				assertThat(filePath, equalTo(paths[index++]));
			}
		});
	}

	@Test
	public void files_Subdir() {
		File[] sources = new File[] { new SourceDirectory("media") };

		FilePath subdir = new FilePath(project, "media/");
		when(project.createFilePath(sources[0])).thenReturn(subdir);

		FilePath dirPath = new FilePath(project, directory(sources));
		dirPath.files(new FilesHandler() {
			@Override
			public void onDirectory(FilePath dir) {
				assertThat(dir, equalTo(subdir));
			}
		});
	}

	@Test
	public void files_Subdir_TrailingSeparator() {
		File[] sources = new File[] { new SourceDirectory("media/") };
		FilePath[] paths = initSources(sources);

		FilePath dirPath = new FilePath(project, directory(sources));
		dirPath.files(new FilesHandler() {
			@Override
			public void onDirectory(FilePath dir) {
				assertThat(dir, equalTo(paths[0]));
			}
		});
	}

	@Test
	public void files_SubdirAndFiles() {
		File[] sources = new File[] { new SourceDirectory("media"), new SourceFile("compo.htm"), new SourceFile("compo.css") };
		initSources(sources);

		FilePath dirPath = new FilePath(project, directory(sources));

		final List<String> result = new ArrayList<String>();
		dirPath.files(new FilesHandler() {
			@Override
			public void onDirectory(FilePath dir) {
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

		FilePath dirPath = new FilePath(project, dir);
		dirPath.files(null);
	}

	@Test(expected = WoodException.class)
	public void files_NulllistFiles() {
		FilePath dirPath = new FilePath(project, directory(null));
		dirPath.files(null);
	}

	@Test
	public void GivenDirPath_WhenAccept_ThenAlwaysFalse() {
		assertFalse(FilePath.accept("res/"));
		assertFalse(FilePath.accept("res"));
		assertFalse(FilePath.accept("res/template/page/"));
		assertFalse(FilePath.accept("res/template/page"));
		assertFalse(FilePath.accept("res_de"));
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

	private FilePath[] initSources(File[] files) {
		List<FilePath> paths = new ArrayList<>();
		for (int i = 0; i < files.length; ++i) {
			if (files[i] instanceof SourceDirectory) {
				FilePath path = new FilePath(project, files[i]);
				when(project.createFilePath(files[i])).thenReturn(path);
				paths.add(path);
			} else if (files[i] instanceof SourceFile) {
				FilePath path = new FilePath(project, files[i]);
				when(project.createFilePath(files[i])).thenReturn(path);
				paths.add(path);
			}
		}
		return paths.toArray(new FilePath[0]);
	}
}
