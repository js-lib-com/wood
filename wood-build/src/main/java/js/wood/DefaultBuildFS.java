package js.wood;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import js.util.Strings;

/**
 * Default build file system implementation. This build file system uses separated directories for styles, scripts and media
 * files; all media files are stored in the same place. Pages are placed in build directory root. For multi-locale projects,
 * replicates single locale directories layout for every locale.
 * <p>
 * For multi-locale build uses locale language tag for locale directory name, see {@link Locale#toLanguageTag()}. Locale
 * language tag is BCP encoded: language encoded ISO 639 alpha-2 and is always lower case and country, if present, is encoded
 * ISO 3166 alpha-2 and is upper case, separated by hyphen.
 * <p>
 * Here is a build directory layout for both single and multi locale projects.
 * 
 * <pre>
 *  /                              /                  
 *  /media/                        /en-US/
 *  /script/                       |  /media/
 *  /style/                        |  /script/
 *  +-page.htm                     |  /style/    
 *                                 |  +-page.htm  
 *                                 |
 *                                 /ro/
 *                                 |  /media/
 *                                 |  /script/
 *                                 |  /style/
 *                                 |  +-page.htm
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
class DefaultBuildFS extends BuildFS {
	/**
	 * Create default build file system instance for given project.
	 * 
	 * @param project project reference.
	 * @param buildNumber
	 */
	public DefaultBuildFS(File buildDir, int buildNumber) {
		super(buildDir, buildNumber);
	}

	/**
	 * Usually page layout files are stored on the root of the build file system. Anyway, if component has
	 * <code>security-role</code> uses it to create role specific sub-directory where to store layout files. Takes care to
	 * create directories path. For <code>security-role</code> description see {@link Component#getSecurityRole()}.
	 */
	@Override
	protected File getPageDir(Component page) {
		if (page != null) {
			// uses security role declared on page component to create specific sub-directory, where to store layout files
			String directory = page.getSecurityRole();
			if (directory != null) {
				if (!directory.startsWith("/")) {
					directory = "/" + directory;
				}
				if (!directory.endsWith("/")) {
					directory += "/";
				}
				return createDirectory(directory);
			}
		}
		return createDirectory(CT.CURRENT_DIR);
	}

	/**
	 * Style files are stored in a single directory named <code>style</code>, relative to build root.
	 */
	@Override
	protected File getStyleDir() {
		return createDirectory("style");
	}

	/**
	 * Script files are stored in a single directory named <code>script</code>, relative to build root.
	 */
	@Override
	protected File getScriptDir() {
		return createDirectory("script");
	}

	/**
	 * All media files are stored in a single directory named <code>media</code>, relative to build root.
	 */
	@Override
	protected File getMediaDir() {
		return createDirectory("media");
	}

	/**
	 * Returns original page name, unchanged.
	 * 
	 * @param pageName page name.
	 * @return page name.
	 */
	@Override
	protected String formatPageName(String pageName) {
		return pageName;
	}

	/**
	 * Return qualified style file guaranteed to be unique in order to avoid name collision. Returned file name contains both
	 * path directories and file name separated by underscore (_). Directory names are separated by dash (-). Do not include
	 * last directory if has the same name as script file basename.
	 * <p>
	 * For your convenience here are couple examples.
	 * <table border="1" style="border-collapse:collapse;">
	 * <tr>
	 * <td><b>Original Style Name
	 * <td><b>Formatted Style Name
	 * <tr>
	 * <td>res/page/index/index.css
	 * <td>res-page_index.css
	 * <tr>
	 * <td>res/theme/style.css
	 * <td>res-theme_style.css
	 * <tr>
	 * <td>lib/video-player/style.css
	 * <td>lib-video-player_style.css
	 * </table>
	 * 
	 * @param styleFile style file.
	 * @return formatted style file name.
	 */
	@Override
	protected String formatStyleName(FilePath styleFile) {
		DirPath dir = styleFile.getParentDirPath();
		List<String> segments = new ArrayList<String>(dir.getPathSegments());
		if (!segments.isEmpty() && segments.get(segments.size() - 1).equals(styleFile.getBaseName())) {
			segments.remove(segments.size() - 1);
		}
		// see #formatMediaName(FilePath) comment
		return Strings.concat(Strings.join(segments, '-'), '_', styleFile.getName());
	}

	/**
	 * Return qualified file name separated by dot. Do not include last directory if has the same name as script file basename.
	 * <p>
	 * For your convenience here are couple examples.
	 * <table border="1" style="border-collapse:collapse;">
	 * <tr>
	 * <td><b>Original Script Name
	 * <td><b>Formatted Script Name
	 * <tr>
	 * <td>script/hc/page/Index.js
	 * <td>script.hc.page.Index.js
	 * <tr>
	 * <td>gen/js/wood/Controller.js
	 * <td>gen.js.wood.Controller.js
	 * <tr>
	 * <td>lib/video-player/media.js
	 * <td>lib.video-player.media.js
	 * <tr>
	 * <td>lib/paging.js
	 * <td>lib.paging.js
	 * <tr>
	 * <td>lib/js-lib/js-lib.js
	 * <td>lib.js-lib.js
	 * </table>
	 * 
	 * @param scriptFile script file.
	 * @return formatted script file name.
	 */
	@Override
	protected String formatScriptName(FilePath scriptFile) {
		DirPath dir = scriptFile.getParentDirPath();
		List<String> segments = new ArrayList<String>(dir.getPathSegments());
		if (!segments.isEmpty() && segments.get(segments.size() - 1).equals(scriptFile.getBaseName())) {
			segments.remove(segments.size() - 1);
		}
		segments.add(scriptFile.getName());
		return Strings.join(segments, '.');
	}

	/**
	 * Return qualified media file guaranteed to be unique in order to avoid name collision. Returned file name contains both
	 * path directories and file name separated by underscore (_). Directory names are separated by dash (-).
	 * <p>
	 * For your convenience here are couple examples.
	 * <table border="1" style="border-collapse:collapse;">
	 * <tr>
	 * <td><b>Original Media Name
	 * <td><b>Formatted Media Name
	 * <tr>
	 * <td>res/page/index/background.png
	 * <td>res-page-index_background.png
	 * <tr>
	 * <td>res/theme/background.png
	 * <td>res-theme_background.png
	 * <tr>
	 * <td>res/asset/background.png
	 * <td>res-asset_background.png
	 * <tr>
	 * <td>script/js/wood/player/background.png
	 * <td>script-js-wood-player_background.png
	 * <tr>
	 * <td>lib/paging/next-page.png
	 * <td>lib-paging_next-page.png
	 * </table>
	 * 
	 * @param mediaFile media file.
	 * @return formatted media file name.
	 */
	@Override
	protected String formatMediaName(FilePath mediaFile) {
		DirPath dir = mediaFile.getParentDirPath();
		List<String> segments = dir.getPathSegments();
		// uses underscore to separated directories path from file name in order to avoid name collision on sub-directories:
		// res/template/page/icon-logo.png -> res/template-page_icon-logo.png
		// res/template/page/icon/logo.png -> res/template-page-icon_logo.png
		return Strings.concat(Strings.join(segments, '-'), '_', mediaFile.getName());
	}
}
