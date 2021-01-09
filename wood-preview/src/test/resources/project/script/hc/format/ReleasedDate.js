$package("hc.format");

hc.format.ReleasedDate = function(ownerDoc, node) {
	this.$super(ownerDoc, node);
};

hc.format.ReleasedDate.prototype = {
	toString : function() {
		return "hc.format.ReleasedDate";
	}
};
$extends(hc.format.ReleasedDate, js.dom.Element);
