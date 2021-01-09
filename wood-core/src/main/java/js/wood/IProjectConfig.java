package js.wood;

import java.util.List;
import java.util.Locale;

import js.dom.EList;

public interface IProjectConfig {
	Locale getDefaultLocale();
	
	EList getMetas();
	
	String getAuthor();
	
	EList getStyles();
	
	EList getScripts();
	
	List<String> getFonts();
}
