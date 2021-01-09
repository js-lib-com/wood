$package("js.compo");

js.compo.Dialog = function(ownerDoc, node) {
	this.$super(ownerDoc, node);
};

js.compo.Dialog.prototype = {
	toString : function() {
		return "js.compo.Dialog";
	}
};
$extends(js.compo.Dialog, js.dom.Element);
