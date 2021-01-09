$package("js.hood");

js.wood.Compo = function(ownerDoc, node) {
	this.$super(ownerDoc, node);
};

js.wood.Compo.prototype = {
	toString : function() {
		return "js.wood.Compo";
	}
};
$extends(js.wood.Compo, js.dom.Element);
