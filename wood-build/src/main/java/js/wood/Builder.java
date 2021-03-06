package js.wood;

import java.io.IOException;
import java.util.Locale;

import js.lang.BugError;
import js.lang.Handler;
import js.log.Log;
import js.log.LogFactory;

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

	/** Builder project instance. */
	private final BuilderProject project;

	/** Build file system is there all pages and resources are created. */
	private final BuildFS buildFS;

	/** Current processing locale variant. */
	private Locale locale;

	/**
	 * Construct builder instance. Create {@link Project} instance with given project root directory. Scan for project layout
	 * and script files and initialize project pages and variables map. Create build FS instance.
	 * 
	 * @param config builder configuration.
	 */
	public Builder(BuilderConfig config) {
		log.trace("Builder(BuilderConfig)");
		this.project = new BuilderProject(config.getProjectDir(), config.getBuildDir());
		this.buildFS = new DefaultBuildFS(config.getBuildDir(), config.getBuildNumber());
	}

	/**
	 * Test constructor.
	 * 
	 * @param project builder project,
	 * @param buildFS build file system.
	 */
	Builder(BuilderProject project, BuildFS buildFS) {
		this.project = project;
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

			for (CompoPath page : project.getPages()) {
				Component pageComponent = new Component(page, this);
				pageComponent.clean();
				currentCompo.set(pageComponent);
				buildPage(pageComponent);
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
	void buildPage(Component pageComponent) throws IOException {
		log.debug("Build page |%s|.", pageComponent);

		PageDocument pageDocument = new PageDocument(pageComponent);
		pageDocument.setLanguage(locale.toLanguageTag());
		pageDocument.setContentType("text/html; charset=UTF-8");
		pageDocument.setTitle(pageComponent.getDisplay());
		pageDocument.setAuthor(project.getAuthor());
		pageDocument.setDescription(pageComponent.getDescription());

		for (IMetaDescriptor meta : project.getMetaDescriptors()) {
			pageDocument.addMeta(meta);
		}
		for (IMetaDescriptor meta : pageComponent.getMetaDescriptors()) {
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

		for (ILinkDescriptor link : project.getLinkDescriptors()) {
			pageDocument.addLink(link);
		}
		for (ILinkDescriptor link : pageComponent.getLinkDescriptors()) {
			pageDocument.addLink(link);
		}

		ThemeStyles themeStyles = project.getThemeStyles();
		if (themeStyles.getReset() != null) {
			pageDocument.addStyle(buildFS.writeStyle(pageComponent, themeStyles.getReset(), this));
		}
		if (themeStyles.getFx() != null) {
			pageDocument.addStyle(buildFS.writeStyle(pageComponent, themeStyles.getFx(), this));
		}
		for (FilePath styleFile : themeStyles.getStyles()) {
			pageDocument.addStyle(buildFS.writeStyle(pageComponent, styleFile, this));
		}

		for (FilePath styleFile : pageComponent.getStyleFiles()) {
			pageDocument.addStyle(buildFS.writeStyle(pageComponent, styleFile, this));
		}

		for (IScriptDescriptor script : project.getScriptDescriptors()) {
			pageDocument.addScript(script, exlambda(file -> buildFS.writeScript(pageComponent, file, this)));
		}
		for (IScriptDescriptor script : pageComponent.getScriptDescriptors()) {
			pageDocument.addScript(script, exlambda(file -> buildFS.writeScript(pageComponent, file, this)));
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
	public String onResourceReference(Reference reference, FilePath source) throws IOException, WoodException {
		if (locale == null) {
			throw new BugError("Builder locale not initialized.");
		}
		if (reference.isVariable()) {
			String value = null;
			Variables dirVariables = project.getVariables().get(source.getParentDirPath());
			if (dirVariables != null) {
				value = dirVariables.get(locale, reference, source, this);
			}
			if (value == null) {
				value = project.getAssetVariables().get(locale, reference, source, this);
			}
			if (value == null) {
				throw new WoodException("Missing variable value for reference |%s:%s|.", source, reference);
			}
			return value;
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
			throw new WoodException("Media files not supported on scripts for reference |%s:%s|.", source, reference);

		default:
		}
		return null;
	}

	// --------------------------------------------------------------------------------------------

	@FunctionalInterface
	public interface ThrowingConsumer<Value, Argument> {
		Value accept(Argument argument) throws Exception;
	}

	private static <Value, Argument> Handler<Value, Argument> exlambda(ThrowingConsumer<Value, Argument> handler) {
		return argument -> {
			try {
				return handler.accept(argument);
			} catch (Exception ex) {
				throw new WoodException(ex);
			}
		};
	}

	// --------------------------------------------------------------------------------------------
	// Test Support

	BuilderProject getProject() {
		return project;
	}

	BuildFS getBuildFS() {
		return buildFS;
	}

	void setLocale(Locale locale) {
		this.locale = locale;
	}
}
