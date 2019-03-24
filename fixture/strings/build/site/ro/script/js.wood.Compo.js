$package("js.wood");

js.wood.Compo = function (ownerDoc, node) {
    this.$super(ownerDoc, node);
    this.setCaption('Hello');
    this.setName('Nume component');
    this.setDescription('Acesta este <em>descrierea</em> <strong>scriptului</strong>.');
};
