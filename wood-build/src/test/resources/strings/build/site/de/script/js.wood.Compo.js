$package("js.wood");

js.wood.Compo = function (ownerDoc, node) {
    this.$super(ownerDoc, node);
    this.setCaption('Hello');
    this.setName('Komponentennamen');
    this.setDescription('Dies ist <strong>Skript</strong> <em>Beschreibung</em>.');
};
