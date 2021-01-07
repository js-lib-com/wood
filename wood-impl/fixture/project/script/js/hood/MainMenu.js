$package("js.hood");

js.hood.MainMenu = function(ownerDoc, node) {
	this.$super(ownerDoc, node);
};

js.hood.MainMenu.prototype = {
	toString : function() {
		return "js.hood.MainMenu";
	}
};
$extends(js.hood.MainMenu, js.dom.Element);
