package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.impl.FileType;
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
		File[] xfiles = new XFile[] { new XFile("compo.htm"), new XFile("compo.css") };

		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn("res/compo");
		when(dir.exists()).thenReturn(true);
		when(dir.listFiles()).thenReturn(xfiles);

		DirPath dirPath = new DirPath(project, dir);

		List<String> files = new ArrayList<String>();
		for (FilePath file : dirPath.files()) {
			files.add(file.value());
		}

		assertThat(files, hasSize(2));
		assertThat(files, hasItems("compo.htm", "compo.css"));
	}

	@Test
	public void files_Subdirectories() {
		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn("res");
		when(dir.exists()).thenReturn(true);
		when(dir.listFiles()).thenReturn(new DFile[] { new DFile("res/compo") });

		DirPath dirPath = new DirPath(project, dir);

		final List<String> files = new ArrayList<>();
		for (FilePath file : dirPath.files()) {
			files.add(file.value());
		}

		assertThat(files, empty());
	}

	@Test
	public void files_FilesHandler_RootDirectory() {
		File[] xfiles = new XFile[] { new XFile("project.xml") };

		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn(".");
		when(dir.exists()).thenReturn(true);
		when(dir.listFiles()).thenReturn(xfiles);

		DirPath dirPath = new DirPath(project, dir);

		final List<String> files = new ArrayList<String>();
		dirPath.files(new FilesHandler() {
			@Override
			public void onFile(FilePath file) throws Exception {
				files.add(file.value());
			}
		});

		assertThat(files, hasSize(1));
		assertThat(files, hasItems("project.xml"));
	}

	@Test
	public void files_Iterable_NullListFiles() {
		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn("res/compo");
		when(dir.exists()).thenReturn(true);
		when(dir.listFiles()).thenReturn(null);

		DirPath dirPath = new DirPath(project, dir);

		List<String> files = new ArrayList<String>();
		for (FilePath file : dirPath.files()) {
			files.add(file.value());
		}

		assertThat(files, empty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void files_Iterable_Remove() {
		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn("res/compo");
		when(dir.exists()).thenReturn(true);
		when(dir.listFiles()).thenReturn(new File[0]);

		DirPath dirPath = new DirPath(project, dir);
		dirPath.files().iterator().remove();
	}

	@Test
	public void files_FilesHandler() {
		File[] xfiles = new XFile[] { new XFile("compo.htm"), new XFile("compo.css") };

		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn("res/compo");
		when(dir.exists()).thenReturn(true);
		when(dir.listFiles()).thenReturn(xfiles);

		DirPath dirPath = new DirPath(project, dir);

		final List<String> files = new ArrayList<String>();
		dirPath.files(new FilesHandler() {
			@Override
			public void onFile(FilePath file) throws Exception {
				files.add(file.value());
			}
		});

		assertThat(files, hasSize(2));
		assertThat(files, hasItems("compo.htm", "compo.css"));
	}

	@Test
	public void files_FilesHandler_Accept() {
		File[] xfiles = new XFile[] { new XFile("compo.htm"), new XFile("compo.css") };

		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn("res/compo");
		when(dir.exists()).thenReturn(true);
		when(dir.listFiles()).thenReturn(xfiles);

		DirPath dirPath = new DirPath(project, dir);

		final List<String> files = new ArrayList<String>();
		dirPath.files(new FilesHandler() {
			@Override
			public boolean accept(FilePath file) {
				return file.isLayout();
			}

			@Override
			public void onFile(FilePath file) throws Exception {
				files.add(file.value());
			}
		});

		assertThat(files, hasSize(1));
		assertThat(files, hasItems("compo.htm"));
	}

	@Test
	public void files_FilesHandler_HiddenFile() {
		File[] xfiles = new XFile[] { new XFile(".gitignore"), new XFile("compo.htm"), new XFile("compo.css") };

		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn("res/compo");
		when(dir.exists()).thenReturn(true);
		when(dir.listFiles()).thenReturn(xfiles);

		DirPath dirPath = new DirPath(project, dir);

		final List<String> files = new ArrayList<String>();
		dirPath.files(new FilesHandler() {
			@Override
			public void onFile(FilePath file) throws Exception {
				files.add(file.value());
			}
		});

		assertThat(files, hasSize(2));
		assertThat(files, hasItems("compo.htm", "compo.css"));
	}

	@Test
	public void files_MissingDirectory() {
		DirPath dir = new DirPath(project, "res/compo/missing/");
		assertThat(dir.files(), notNullValue());
		assertThat(dir.files().iterator().hasNext(), is(false));
	}

	@Test
	public void files_ListByType() {
		File[] xfiles = new XFile[] { new XFile("compo.htm"), new XFile("compo.css") };

		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn("res/compo");
		when(dir.exists()).thenReturn(true);
		when(dir.listFiles()).thenReturn(xfiles);

		DirPath dirPath = new DirPath(project, dir);

		final List<String> files = new ArrayList<>();
		dirPath.files(FileType.LAYOUT, new FilesHandler() {
			@Override
			public void onFile(FilePath file) throws Exception {
				files.add(file.value());
			}
		});

		assertThat(files, hasSize(1));
		assertThat(files, hasItems("compo.htm"));
	}

	@Test
	public void files_ListByType_Subdirectories() {
		File resDir = Mockito.mock(File.class);
		when(resDir.getPath()).thenReturn("res");
		when(resDir.exists()).thenReturn(true);
		when(resDir.listFiles()).thenReturn(new DFile[] { new DFile("res/compo/") });

		File compoDir = Mockito.mock(File.class);
		when(compoDir.getPath()).thenReturn("res/compo/");
		when(compoDir.exists()).thenReturn(true);
		when(compoDir.listFiles()).thenReturn(new XFile[] { new XFile("compo.htm"), new XFile("compo.css") });

		DirPath dirPath = new DirPath(project, resDir);

		final List<String> files = new ArrayList<>();
		dirPath.files(FileType.LAYOUT, new FilesHandler() {
			@Override
			public void onDirectory(DirPath dir) throws Exception {
				// 'dir' argument is a concrete DirPath instance with a real Java directory
				// replace it with new instance with mock Java directory
				dir = new DirPath(project, compoDir);
				dir.files(FileType.LAYOUT, this);
			}

			@Override
			public void onFile(FilePath file) throws Exception {
				files.add(file.value());
			}
		});

		assertThat(files, hasSize(1));
		assertThat(files, hasItems("compo.htm"));
	}

	@Test
	public void files_ListByType_DoNoAccept() {
		DirPath dir = new DirPath(project, "res/compo/");

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
		File[] xfiles = new XFile[] { new XFile("compo.htm") };

		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn("res/compo");
		when(dir.exists()).thenReturn(true);
		when(dir.listFiles()).thenReturn(xfiles);

		DirPath dirPath = new DirPath(project, dir);

		dirPath.files(FileType.LAYOUT, new FilesHandler() {
			@Override
			public void onFile(FilePath file) throws Exception {
				throw new IOException();
			}
		});
	}

	@Test(expected = WoodException.class)
	public void files_ListByType_WoodException() {
		File[] xfiles = new XFile[] { new XFile("compo.htm") };

		File dir = Mockito.mock(File.class);
		when(dir.getPath()).thenReturn("res/compo");
		when(dir.exists()).thenReturn(true);
		when(dir.listFiles()).thenReturn(xfiles);

		DirPath dirPath = new DirPath(project, dir);

		dirPath.files(FileType.LAYOUT, new FilesHandler() {
			@Override
			public void onFile(FilePath file) throws Exception {
				throw new WoodException("");
			}
		});
	}

	@Test
	public void files_ListByType_MissingDirectory() {
		DirPath dir = new DirPath(project, "res/missing-directory/");

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

	private static class XFile extends File {
		private static final long serialVersionUID = -5975578621510948684L;

		public XFile(String pathname) {
			super(pathname);
		}

		@Override
		public boolean isFile() {
			return true;
		}
	}

	private static class DFile extends File {
		private static final long serialVersionUID = 962174848140145344L;

		public DFile(String pathname) {
			super(pathname);
		}

		@Override
		public boolean isDirectory() {
			return true;
		}
	}
}
