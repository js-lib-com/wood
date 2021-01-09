$package("js.compo");

js.compo.ListCtrl = function(ownerDoc, node) {
	this.$super(ownerDoc, node);
};

js.compo.ListCtrl.prototype = {
	toString : function() {
		return "js.compo.ListCtrl";
	}
};
$extends(js.compo.ListCtrl, js.dom.Element);
