$package("hc.page");

hc.page.Index = function() {
	this.$super();
	this._view = new hc.view.DiscographyView();
	js.controller.Controller.getContact(this._onContactLoaded, this);
};

hc.page.Index.prototype = {
	_onContactLoaded : function(contact) {
		js.ua.System.alert(contact.name);
	},

	toString : function() {
		return "hc.page.Index";
	}
};
$extends(hc.page.Index, js.ua.Page);
