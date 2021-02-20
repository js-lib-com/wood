package js.wood;

import java.io.File;

public class PreviewProject extends Project {
	public PreviewProject(File projectDir) throws IllegalArgumentException {
		super(projectDir);
	}

	/**
	 * Scan theme directory for style files. This method is designed for preview process and does not use cache.
	 * 
	 * @return site style files.
	 */
	@Override
	public ThemeStyles getThemeStyles() {
		return new ThemeStyles(getThemeDir());
	}
}
