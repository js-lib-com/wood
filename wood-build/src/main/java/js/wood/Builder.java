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
 * Optionally, one may set the build number.
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
	private final Collection<CompoPath> pages;

	/** Cache for resource variables. */
	private final Map<DirPath, IVariables> variables;

	/** Current processing build file system, that is, build site directory. */
	private final BuildFS buildFS;

	/** Current processing locale variant. */
	private Locale locale;

	/**
	 * Construct builder instance. Create {@link Project} instance with given project root directory. Scan for project layout
	 * and script files and initialize project pages and variables map. Create build FS instance.
	 * 
	 * @param projectDir path to existing project root directory,
	 * @param siteDir
	 * @throws IOException 
	 */
	public Builder(BuilderConfig config) throws IOException {
		log.trace("Builder(BuilderConfig)");
		this.project = new BuilderProject(config.getProjectDir());
		this.project.scan();

		this.pages = this.project.getPages();
		this.variables = this.project.getVariables();
		this.buildFS = new DefaultBuildFS(config.getBuildDir(), config.getBuildNumber());
	}

	/**
	 * Test constructor.
	 * 
	 * @param project
	 * @throws IOException 
	 */
	public Builder(BuilderProject project) throws IOException {
		this(project, new DefaultBuildFS(new File(project.getProjectDir(), CT.DEF_BUILD_DIR), 0));
	}

	/**
	 * Test constructor.
	 * 
	 * @param project
	 * @param buildFS
	 * @throws IOException 
	 */
	public Builder(BuilderProject project, BuildFS buildFS) throws IOException {
		this.project = project;
		this.project.scan();

		this.pages = this.project.getPages();
		this.variables = this.project.getVariables();
		this.buildFS = buildFS;
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
			if (project.isMultiLocale()) {
				buildFS.setLocale(locale);
			}

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
	void buildPage(CompoPath compoPath) throws IOException {
		log.debug("Build page |%s|.", compoPath);

		Component pageComponent = new Component(compoPath, this);
		currentCompo.set(pageComponent);
		pageComponent.scan();

		PageDocument pageDocument = new PageDocument(pageComponent);
		pageDocument.setLanguage((locale != null ? locale : project.getDefaultLocale()).toLanguageTag());
		pageDocument.setContentType("text/html; charset=UTF-8");
		pageDocument.setTitle(pageComponent.getDisplay());
		pageDocument.setAuthor(project.getAuthor());
		pageDocument.setDescription(pageComponent.getDescription());

		for (IMetaReference meta : project.getMetaReferences()) {
			pageDocument.addMeta(meta);
		}
		for (IMetaReference meta : pageComponent.getMetaReferences()) {
			pageDocument.addMeta(meta);
		}

		if (project.getFavicon().exists()) {
			pageDocument.addFavicon(buildFS.writeFavicon(pageComponent, project.getFavicon()));
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
		for (ILinkReference link : pageComponent.getLinkReferences()) {
			pageDocument.addLink(link);
		}

		ThemeStyles themeStyles = new ThemeStyles(project.getThemeStyles());
		if (themeStyles.reset != null) {
			pageDocument.addStyle(buildFS.writeStyle(pageComponent, themeStyles.reset, this));
		}
		if (themeStyles.fx != null) {
			pageDocument.addStyle(buildFS.writeStyle(pageComponent, themeStyles.fx, this));
		}
		for (FilePath styleFile : themeStyles.styles) {
			pageDocument.addStyle(buildFS.writeStyle(pageComponent, styleFile, this));
		}

		for (FilePath styleFile : pageComponent.getStyleFiles()) {
			pageDocument.addStyle(buildFS.writeStyle(pageComponent, styleFile, this));
		}

		for (IScriptReference script : project.getScriptReferences()) {
			pageDocument.addScript(script, file -> buildFS.writeScript(pageComponent, file, this));
		}
		for (IScriptReference script : pageComponent.getScriptReferences()) {
			pageDocument.addScript(script, file -> buildFS.writeScript(pageComponent, file, this));
		}

		buildFS.writePage(pageComponent, pageDocument.getDocument());
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
			IVariables dirVariables = variables.get(source.getParentDirPath());
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
			return buildFS.writeScriptMedia(project.getName(), mediaFile);

		default:
		}
		return null;
	}

	// --------------------------------------------------------------------------------------------
	// Test Support

	BuilderProject getProject() {
		return project;
	}

	BuildFS getBuildFS() {
		return buildFS;
	}

	Collection<CompoPath> getPages() {
		return pages;
	}

	Map<DirPath, IVariables> getVariables() {
		return variables;
	}

	void setLocale(Locale locale) {
		this.locale = locale;
	}

	// --------------------------------------------------------------------------------------------
	// Internal Types

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
