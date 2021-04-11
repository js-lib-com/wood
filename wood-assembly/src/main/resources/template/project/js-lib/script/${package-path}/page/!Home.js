$package("${package}.page");

/**
 * Home page class.
 * 
 * @author ${author}
 * @since 1.0
 * 
 * @constructor Construct an instance of home page class.
 */
${package}.page.Home = function() {
	this.$super();
};

${package}.page.Home.prototype = {
	/**
	 * Class string representation.
	 * 
	 * @return this class string representation.
	 */
	toString : function() {
		return "${package}.page.Home";
	}
};
$extends(${package}.page.Home, ${package}.page.Page);

WinMain.createPage(${package}.page.Home);
