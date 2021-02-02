package js.wood;

import java.util.List;

import js.wood.impl.Variables;

public class PreviewProject extends Project {
	private final String previewName;

	public PreviewProject(String projectPath) throws IllegalArgumentException {
		super(projectPath);
		String name = this.getName();
		this.previewName = name.isEmpty() ? "preview" : name + "-preview";
	}

	/**
	 * Get project name for preview servlet.
	 * 
	 * @return project name used for preview.
	 * @see #previewName
	 */
	public String getPreviewName() {
		return previewName;
	}

	public IVariables getVariables(DirPath dir) {
		return new Variables(dir);
	}

	/**
	 * Scan theme directory for style files. This method is designed for preview process and does not use cache.
	 * 
	 * @return site style files.
	 */
	public List<FilePath> getThemeStyles() {
		themeStyles.clear();
		DirPath dir = new DirPath(this, CT.THEME_DIR);
		dir.files(new ThemeStylesScanner());
		return themeStyles;
	}
}
