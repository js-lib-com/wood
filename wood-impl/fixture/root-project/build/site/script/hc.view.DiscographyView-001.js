$package("hc.view");

hc.view.DiscographyView = function(ownerDoc, node) {
	this.$super(ownerDoc, node);
};

hc.view.DiscographyView.prototype = {
	toString : function() {
		return "hc.view.DiscographyView";
	}
};
$extends(hc.view.DiscographyView, hc.view.VideoPlayer);
