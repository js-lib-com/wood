$package("js.hood");

js.wood.GeoMap = function (ownerDoc, node) {
    this.$super(ownerDoc, node);
};

js.wood.GeoMap.prototype = {
    init : function () {
        this._map = new google.maps.Map(this._canvas, opts);
    },

    toString : function () {
        return "js.wood.GeoMap";
    }
};
$extends(js.wood.GeoMap, js.dom.Element);
