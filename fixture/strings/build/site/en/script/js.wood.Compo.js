$package("js.wood");

js.wood.Compo = function (ownerDoc, node) {
    this.$super(ownerDoc, node);
    this.setCaption('Hello');
    this.setName('Component Name');
    this.setDescription('This is <strong>script</strong> <em>description</em>.');
};
