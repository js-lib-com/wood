package com.jslib.wood.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.jslib.wood.Component;
import com.jslib.wood.FilePath;
import com.jslib.wood.util.StringsUtil;

/**
 * Default build file system implementation. This build file system uses separated directories for styles, scripts and media
 * files; all media files are stored in the same place. Pages are placed in build directory root. For multi-language projects,
 * replicates single language directories layout for every language.
 * <p>
 * For multi-language build uses language for subdirectory name. Language is BCP encoded: language encoded ISO 639 alpha-2 and
 * is always lower case and country, if present, is encoded ISO 3166 alpha-2 and is upper case, separated by hyphen.
 * <p>
 * Here is a build directory layout for both single and multi-language projects.
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
	 * @param buildDir project directory.
	 * @param buildNumber build number.
	 */
	public DefaultBuildFS(File buildDir, int buildNumber) {
		super(buildDir, buildNumber);
	}

	/**
	 * Usually page layout files are stored on the root of the build file system. Anyway, if component has
	 * <code>group</code> uses it to create resources groups specific subdirectory where to store layout files. Takes care to
	 * create directories path.
	 */
	@Override
	protected File getPageDir(Component page) {
		if (page != null) {
			// uses resources group declared on page component descriptor to create specific subdirectory, where to store layout files
			String directory = page.getResourcesGroup();
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
		return createDirectory(".");
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

	@Override
	protected File getMediaDir() {
		return createDirectory("media");
	}

	@Override
	protected File pwaDir() {
		return createDirectory(".");
	}

	@Override
	protected File getFontDir() {
		return createDirectory("style");
	}

	@Override
	protected File getFilesDir() {
		return createDirectory("files");
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
	 * last directory if it has the same name as script file basename.
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
		FilePath dir = styleFile.getParentDir();
		List<String> segments = dir != null ? new ArrayList<>(dir.getPathSegments()) : new ArrayList<>();
		if (!segments.isEmpty() && segments.get(segments.size() - 1).equals(styleFile.getBasename())) {
			segments.remove(segments.size() - 1);
		}
		// see #formatMediaName(FilePath) comment
		return StringsUtil.concat(StringsUtil.join(segments, '-'), '_', styleFile.getName());
	}

	/**
	 * Return qualified file name separated by dot. Do not include last directory if it has the same name as script file basename.
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
		FilePath dir = scriptFile.getParentDir();
		List<String> segments = dir != null ? new ArrayList<>(dir.getPathSegments()) : new ArrayList<>();
		if (!segments.isEmpty() && segments.get(segments.size() - 1).equals(scriptFile.getBasename())) {
			segments.remove(segments.size() - 1);
		}
		segments.add(scriptFile.getName());
		return StringsUtil.join(segments, '.');
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
		FilePath dir = mediaFile.getParentDir();
		List<String> segments = dir != null ? dir.getPathSegments() : new ArrayList<>();
		// uses underscore to separated directories path from file name in order to avoid name collision on subdirectories:
		// res/template/page/icon-logo.png -> res/template-page_icon-logo.png
		// res/template/page/icon/logo.png -> res/template-page-icon_logo.png
		return StringsUtil.concat(StringsUtil.join(segments, '-'), '_', mediaFile.getName());
	}
}
