package com.jslib.wood;

import java.util.ArrayList;
import java.util.List;

public class ThemeStyles {
	private final FilePath variables;
	private final FilePath defaultStyles;
	private final FilePath animations;
	private final List<FilePath> styles = new ArrayList<>();

	public ThemeStyles(FilePath themeDir) {
		FilePath variables = null;
		FilePath defaultStyles = null;
		FilePath animations = null;
		
		for (FilePath file : themeDir) {
			if(!file.isStyle()) {
				continue;
			}
			switch (file.getName()) {
			case CT.VARIABLES_CSS:
				variables = file;
				break;
				
			case CT.DEFAULT_CSS:
				defaultStyles = file;
				break;

			case CT.FX_CSS:
				animations = file;
				break;

			default:
				if (file.isStyle()) {
					this.styles.add(file);
				}
			}
		}
		
		this.variables = variables;
		this.defaultStyles = defaultStyles;
		this.animations = animations;
	}

	public FilePath getVariables() {
		return variables;
	}

	public FilePath getDefaultStyles() {
		return defaultStyles;
	}

	public FilePath getAnimations() {
		return animations;
	}

	public List<FilePath> getStyles() {
		return styles;
	}
}
