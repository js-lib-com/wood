$package("js.wood");

js.wood.GeoMap = function(ownerDoc, node) {
	this.$super(ownerDoc, node);
	this._map = new google.maps.Map(this._node, {});
};

js.wood.GeoMap.prototype = {
	toString : function() {
		return "js.wood.GeoMap";
	}
};
$extends(js.wood.GeoMap, js.dom.Element);
