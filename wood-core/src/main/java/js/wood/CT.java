package js.wood;

/**
 * Globals constants.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public final class CT {
	/** Project configuration file. */
	public static final String PROJECT_CONFIG = "project.xml";

	public static final String MANIFEST_FILE = "manifest.json";

	// ------------------------------------------------------
	// Files types recognized by this build tool

	/** Layout file describe UI elements. */
	public static final String LAYOUT_EXT = "htm";
	/** The same as {@link #LAYOUT_EXT} but with leading dot. */
	public static final String DOT_LAYOUT_EXT = ".htm";

	/** Style file for layout structure and theme styles. */
	public static final String STYLE_EXT = "css";
	/** The same as {@link #STYLE_EXT} but with leading dot. */
	public static final String DOT_STYLE_EXT = ".css";

	/** Script for component behavior and application logic. */
	public static final String SCRIPT_EXT = "js";
	/** The same as {@link #SCRIPT_EXT} but with leading dot. */
	public static final String DOT_SCRIPT_EXT = ".js";

	/** XML files are used to define variables and descriptors. */
	public static final String XML_EXT = "xml";
	/** Same as {@link #XML_EXT} but with leading dot. */
	public static final String DOT_XML_EXT = ".xml";

	/** Synthetic file used to convey path information about component variables. */
	public static final String VAR_EXT = "var";

	// ------------------------------------------------------
	// Project directories relative to project root

	public static final String DEF_BUILD_DIR = "build/";

	/**
	 * Project assets directory stores global variables and media files. This field stores the default value; it can be
	 * overwritten by <code>asset.dir</code> property from project properties file.
	 */
	public static final String DEF_ASSET_DIR = "res/asset/";

	/**
	 * Project theme directory for styles related to UI primitive elements design and theme variables. This field sores the
	 * default value; it can be overwritten by <code>theme.dir</code> property from project properties file.
	 */
	public static final String DEF_THEME_DIR = "res/theme/";

	// ------------------------------------------------------
	// Dynamic generated files

	/** File name for styles reset. */
	public static final String RESET_CSS = "reset.css";

	/** File name for key frames used for CSS animations. */
	public static final String FX_CSS = "fx.css";

	// ------------------------------------------------------
	// Preview files

	/** The name of style file used for preview. */
	public static final String PREVIEW_STYLE = "preview.css";

	/** The name of script file used for preview. */
	public static final String PREVIEW_SCRIPT = "preview.js";

	/** Prevent default constructor synthesis. */
	private CT() {
	}
}
