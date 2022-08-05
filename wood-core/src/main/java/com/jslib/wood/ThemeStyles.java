package com.jslib.wood;

import java.util.ArrayList;
import java.util.List;

public class ThemeStyles {
	private final FilePath reset;
	private final FilePath fx;
	private final List<FilePath> styles = new ArrayList<>();

	public ThemeStyles(FilePath themeDir) {
		FilePath reset = null;
		FilePath fx = null;
		
		for (FilePath file : themeDir) {
			if(!file.isStyle()) {
				continue;
			}
			switch (file.getName()) {
			case CT.RESET_CSS:
				reset = file;
				break;

			case CT.FX_CSS:
				fx = file;
				break;

			default:
				if (file.isStyle()) {
					this.styles.add(file);
				}
			}
		}
		
		this.reset = reset;
		this.fx = fx;
	}

	public FilePath getReset() {
		return reset;
	}

	public FilePath getFx() {
		return fx;
	}

	public List<FilePath> getStyles() {
		return styles;
	}
}
