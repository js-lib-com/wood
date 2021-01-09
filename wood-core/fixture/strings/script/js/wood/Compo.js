$package("js.wood");

js.wood.Compo = function (ownerDoc, node) {
    this.$super(ownerDoc, node);
    this.setCaption('@string/hello');
    this.setName('@string/name');
    this.setDescription('@text/description');
};
