package js.wood;

import java.util.ArrayList;
import java.util.List;

public class ThemeStyles {
	public final FilePath reset;
	public final FilePath fx;
	public final List<FilePath> styles = new ArrayList<>();

	public ThemeStyles(DirPath themeDir) {
		FilePath reset = null;
		FilePath fx = null;
		
		for (FilePath file : themeDir.files()) {
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
}
