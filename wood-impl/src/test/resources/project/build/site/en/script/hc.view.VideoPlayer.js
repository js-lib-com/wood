$package("hc.view");

hc.view.VideoPlayer = function(ownerDoc, node) {
	this.$super(ownerDoc, node);
};

hc.view.VideoPlayer.prototype = {
	toString : function() {
		return "hc.view.VideoPlayer";
	}
};
$extends(hc.view.VideoPlayer, js.compo.Dialog);
