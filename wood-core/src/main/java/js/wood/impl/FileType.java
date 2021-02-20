package js.wood.impl;

import java.io.File;

import js.util.Files;
import js.util.Params;
import js.wood.CT;

/**
 * Project recognized file types, based on file extension. By convention a component has next file types: layout (HTM), style
 * (CSS), script (JS) and variables and descriptor (XML). All other are considered media files, mostly support images.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public enum FileType {
	/** Layout is HTM file that describe UI structure. */
	LAYOUT,

	/** CSS styles describe UI elements structure and aspect. */
	STYLE,

	/** Define component behavior. */
	SCRIPT,

	/** XML files are used for variables definition and descriptors. */
	XML,

	/** Media files, mostly support images. */
	MEDIA;

	/**
	 * Get the type of requested file. Use file extension to detect file type; if file extension is missing or not recognized
	 * consider media file.
	 * 
	 * @param file file to get its type.
	 * @return file type.
	 * @throws IllegalArgumentException if file parameter is null.
	 */
	public static FileType forFile(File file) {
		Params.notNull(file, "File");
		return forExtension(Files.getExtension(file));
	}

	/**
	 * Get file type related to given extension. If extension is not recognized file type is considered media; this holds true
	 * for empty extension too.
	 * 
	 * @param extension file extension.
	 * @return file type for extension.
	 * @throws IllegalArgumentException if file extension parameter is null.
	 */
	public static FileType forExtension(String extension) {
		Params.notNull(extension, "File extension");
		switch (extension) {
		case CT.LAYOUT_EXT:
			return FileType.LAYOUT;

		case CT.STYLE_EXT:
			return FileType.STYLE;

		case CT.SCRIPT_EXT:
			return FileType.SCRIPT;

		case CT.XML_EXT:
			return FileType.XML;
		}
		return MEDIA;
	}
}