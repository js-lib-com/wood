$package("js.compo");

js.compo.ListView = function(ownerDoc, node) {
	this.$super(ownerDoc, node);
	js.ua.System.alert(js.compo.ListView.Messages.alert);
};

js.compo.ListView.prototype = {
	toString : function() {
		return "js.compo.ListView";
	}
};
$extends(js.compo.ListView, js.dom.Element);
