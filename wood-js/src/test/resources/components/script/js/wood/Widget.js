$package("js.hood");

js.wood.Widget = function(ownerDoc, node) {
	this.$super(ownerDoc, node);
};

js.wood.Widget.prototype = {
	toString : function() {
		return "js.wood.Widget";
	}
};
$extends(js.wood.Widget, js.dom.Element);
