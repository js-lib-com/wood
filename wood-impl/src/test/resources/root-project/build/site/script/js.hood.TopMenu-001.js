$package("js.hood");

js.hood.TopMenu = function(ownerDoc, node) {
	this.$super(ownerDoc, node);
};

js.hood.TopMenu.prototype = {
	toString : function() {
		return "js.hood.TopMenu";
	}
};
$extends(js.hood.TopMenu, js.dom.Element);
$preload(js.hood.TopMenu);