package js.wood;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import js.log.Log;
import js.log.LogFactory;
import js.wood.impl.Reference;

/**
 * Project builder for user interface resources. Builder acts as a bridge between {@link Project} and {@link BuildFS}. It reads
 * component source and resource files from project, consolidates them into pages then write to build site directory. Build
 * class implements {@link #onResourceReference(Reference, FilePath)} used to process resource references declared into source
 * files.
 * <p>
 * Using this class is pretty straightforward: create instance providing project directory then just invoke {@link #build()}.
 * Optionally, one may set the build number. Build site directory is controlled by build process and is project configurable; it
 * can be obtaining via {@link #getSitePath()}.
 * 
 * <pre>
 * Builder builder = new Builder(projectDir);
 * builder.setBuildNumber(buildNumber);
 * builder.build();
 * ...
 * String sitePath = builder.getSitePath();
 * </pre>
 * 
 * Builder interface is designed for integration with external tools. There are extensions for Ant tasks and Eclipse plug-in
 * using this Builder class, but not part of this library distribution.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public class Builder implements IReferenceHandler {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(Builder.class);

	/** Project instance. */
	private final BuilderProject project;

	/** Project discovered pages. */
	private final Collection<CompoPath> pages = new ArrayList<>();

	/** Cache for resource variables. */
	private final Map<DirPath, IVariables> variables;

	/** Current processing build file system, that is, build site directory. */
	private final BuildFS buildFS;

	/** Current processing locale variant. */
	private Locale locale;

	public Builder(String projectPath) {
		this(projectPath, null);
	}

	/**
	 * Construct builder instance. Create {@link Project} instance with given project root directory. Scan for project layout
	 * and script files and initialize project pages and variables map. Create build FS instance.
	 * 
	 * @param projectPath path to project root directory.
	 */
	public Builder(String projectPath, File siteDir) {
		log.trace("Builder(String, File)");
		this.project = new BuilderProject(projectPath, siteDir);
		this.project.scanBuildFiles();

		// scan project layout files then initialize pages list and global variables map
		for (LayoutFile layoutFile : this.project.getLayouts()) {
			if (layoutFile.isPage()) {
				this.pages.add(layoutFile.getCompoPath());
			}
		}

		this.variables = this.project.getVariables();
		this.buildFS = new DefaultBuildFS(project);
	}

	/**
	 * Test constructor.
	 * 
	 * @param project
	 */
	public Builder(BuilderProject project) {
		this.project = project;
		this.project.scanBuildFiles();

		// scan project layout files then initialize pages list and global variables map
		for (LayoutFile layoutFile : this.project.getLayouts()) {
			if (layoutFile.isPage()) {
				this.pages.add(layoutFile.getCompoPath());
			}
		}

		this.variables = this.project.getVariables();
		this.buildFS = new DefaultBuildFS(project);
	}
	
	/**
	 * Set build number.
	 * 
	 * @param buildNumber build number.
	 */
	public void setBuildNumber(int buildNumber) {
		buildFS.setBuildNumber(buildNumber);
	}

	/**
	 * Get the site build directory path, relative to project. Returned value is that from project configuration - see
	 * {@link Project#getSiteDir(String)}, or default value {@link CT#DEF_SITE_DIR}. Path is guaranteed to have trailing file
	 * separator.
	 * 
	 * @return site build path.
	 */
	public String getSitePath() {
		return project.getSitePath();
	}

	/**
	 * Run project building process. Uses project detected locale(s) to initialize current processing {@link #locale} then
	 * delegates {@link #buildPage(CompoPath)} for every discovered page, see {@link #pages}.
	 * 
	 * @throws IOException for error related to underlying file system operations.
	 */
	public void build() throws IOException {
		for (Locale locale : project.getLocales()) {
			this.locale = locale;
			buildFS.setLocale(locale);

			for (CompoPath page : pages) {
				buildPage(page);
			}
		}
	}

	private ThreadLocal<Component> currentCompo = new ThreadLocal<Component>();

	/**
	 * Build page identified by given component path and copy to build directory. Component should be designed for page
	 * generation so that it should have body root or to use a template that has body.
	 * <p>
	 * This method create a page document with the component instance and initialize it as follow:
	 * <ul>
	 * <li>create X(HT)ML document with header and body loaded from component layout,
	 * <li>set content type, metas from project configuration and page title,
	 * <li>add header styles in order: third party fonts, reset and key-frames, component styles and themes,
	 * <li>include script files at the body end,
	 * <li>update default values for missing attributes,
	 * <li>serialize document to build page directory.
	 * </ul>
	 * <p>
	 * Be aware that {@link #locale} should be properly initialized before calling this method, if project has multi-locale
	 * support.
	 * 
	 * @param compoPath page component path.
	 * @throws IOException if files operation fails.
	 */
	private void buildPage(CompoPath compoPath) throws IOException {
		log.debug("Build page |%s|.", compoPath);

		Component page = new Component(compoPath, this);
		currentCompo.set(page);
		page.scan(false);
		PageDocument pageDocument = new PageDocument(page);

		pageDocument.setLanguage((locale != null ? locale : project.getDefaultLocale()).toLanguageTag());
		pageDocument.setContentType("text/html; charset=UTF-8");
		pageDocument.setTitle(page.getDisplay());
		pageDocument.setAuthor(project.getAuthor());
		pageDocument.setDescription(page.getDescription());

		for (IMetaReference meta : project.getMetaReferences()) {
			pageDocument.addMeta(meta);
		}
		for (IMetaReference meta : page.getMetaReferences()) {
			pageDocument.addMeta(meta);
		}

		if (project.getFavicon().exists()) {
			pageDocument.addFavicon(buildFS.writeFavicon(page, project.getFavicon()));
		}

		// links order:
		// 1. external links defined by project
		// 2. external links defined by page
		// 3. reset.css
		// 4. fx.css
		// 5. theme styles - theme styles are in no particular order since they are independent of each other
		// 6. component styles - first use template and child component styles then parent component

		for (ILinkReference link : project.getLinkReferences()) {
			pageDocument.addLink(link);
		}
		for (ILinkReference link : page.getLinkReferences()) {
			pageDocument.addLink(link);
		}

		ThemeStyles themeStyles = new ThemeStyles(project.getThemeStyles());
		if (themeStyles.reset != null) {
			pageDocument.addStyle(buildFS.writeStyle(page, themeStyles.reset, this));
		}
		if (themeStyles.fx != null) {
			pageDocument.addStyle(buildFS.writeStyle(page, themeStyles.fx, this));
		}
		for (FilePath styleFile : themeStyles.styles) {
			pageDocument.addStyle(buildFS.writeStyle(page, styleFile, this));
		}

		for (FilePath styleFile : page.getStyleFiles()) {
			pageDocument.addStyle(buildFS.writeStyle(page, styleFile, this));
		}

		for (IScriptReference script : project.getScriptReferences()) {
			pageDocument.addScript(script, file -> buildFS.writeScript(page, file, this));
		}
		for (IScriptReference script : page.getScriptReferences()) {
			pageDocument.addScript(script, file -> buildFS.writeScript(page, file, this));
		}

		buildFS.writePage(page, pageDocument);
	}

	/**
	 * Resource reference handler invoked by {@link SourceReader} when discover a resource reference into source file. Returned
	 * value is used by source reader to replace the reference, into source file. If reference points to a variable, returns its
	 * value or null if not found. If media file, copy it to build media directory then return build media file path.
	 * 
	 * @param reference resource reference,
	 * @param source source file where <code>reference</code> is used.
	 * @return value to replace reference on source file.
	 * @throws IOException if media file write operation fails.
	 * @throws WoodException if directory variables or media file is missing.
	 */
	@Override
	public String onResourceReference(IReference reference, FilePath source) throws IOException, WoodException {
		if (reference.isVariable()) {
			IVariables dirVariables = variables.get(source.getDirPath());
			if (dirVariables == null) {
				throw new WoodException("Missing variable value for reference |%s:%s|.", source, reference);
			}
			if (locale == null) {
				throw new IllegalStateException("Builder locale not initialized.");
			}
			return dirVariables.get(locale, reference, source, this);
		}

		FilePath mediaFile = project.getMediaFile(locale, reference, source);
		if (mediaFile == null) {
			throw new WoodException("Missing media file for reference |%s:%s|.", source, reference);
		}

		switch (source.getType()) {
		case LAYOUT:
			return buildFS.writePageMedia(currentCompo.get(), mediaFile);

		case STYLE:
			return buildFS.writeStyleMedia(mediaFile);

		case SCRIPT:
			return buildFS.writeScriptMedia(mediaFile);

		default:
		}
		return null;
	}

	private static class ThemeStyles {
		public final FilePath reset;
		public final FilePath fx;
		public final List<FilePath> styles = new ArrayList<>();

		public ThemeStyles(List<FilePath> themeStyles) {
			FilePath reset = null;
			FilePath fx = null;
			for (FilePath style : themeStyles) {
				switch (style.getName()) {
				case CT.RESET_CSS:
					reset = style;
					break;

				case CT.FX_CSS:
					fx = style;
					break;

				default:
					this.styles.add(style);
				}
			}
			this.reset = reset;
			this.fx = fx;
		}
	}
}
