$package("js.wood");

js.wood.IndexPage = function() {
	this._description = new js.widget.Description("description");
	this._formatter = new js.format.RichText();
	js.ua.System.alert("message");
};

js.wood.IndexPage.prototype = {};
$extends(js.wood.IndexPage, js.dom.Element);
