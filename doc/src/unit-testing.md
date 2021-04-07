## Unit Testing

A component grows in a development silo and can be developed as a unit and when integrated is fully functional. Testing is part of interactive development process and as a consequence is enacted by preview.

Component under development can have an unit testing file named 'preview.htm'. When browser asks for a component, preview process replaces component layout with its 'preview.htm' content, of course if present. Preview uses component as widget and takes care to display it. There are also preview style and script files, namely 'preview.css' and 'preview.js' that support this testing mechanism.

Here is simplified preview for 'paging' component. In order to test paging functionality it also uses an external component that display a list of items.

```
    <div>
        <div wood:compo="res/compo/list-ctrl"></div>
        <ul wood:compo="res/compo/paging"></ul>
    </div>
```

Note that 'preview' files are used only for development and are not part of distribution. Also build process ignores them.

