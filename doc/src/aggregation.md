## Aggregation

OOP aggregation is implemented by inserting layout defined in external components into parent layout. Parent layout defines widget path elements pointing to components defined by this project or imported in library. For performance reasons referring to widgets from remote servers is not supported. If need to use remote components one should import them to this project library. Widget component is a standard component but specifically crafted for reuse.

Both parent and widget components may have styles, resources and scripts. As with inheritance, widgets style are included before parent style so that parent can overwrite and customize widgets.

Widget path elements defined by parent layout acts as insertion points where referred widget layout is inserted. Attributes define by widget layout root element are merged with attributes defined by parent widget path element; parent attributes take precedence. To state it clearly, parent can customize both widget styles and attributes.

Is legal for widget layout to use templates. Also, an widget layout may define its own widget path elements pointing to other widget layouts. There is no restriction on the number of widgets a parent can include. This way a tree of widgets is created; combining both templates and widgets can result in pretty complex components graph.

```
    parent layout
    +----------------------------+         
    |  widget path element       |      widget layout
    |  +--------------------+    |      +---------------+
    |  |    wood:widget     +-----------> . . .         |
    |  +--------------------+    |      +---------------+ 
    | . . .                      |
    +----------------------------+
```

Here we have a parent with a child that have a child, that is, parent nephew. Parent <section> element has 'wood:widget' operator that contain child widget path. Similar for child <div> element. Widget path is a {@link js.wood.CompoPath}.

```
    parent.htm
    <body>
       <h1>Parent</h1>
       <section wood:widget="child"></section>
    </body>
    
    child.htm
    <section>
        <h2>Child</h2>
        <div wood:widget="nephew"></div>
    </section>
    
    nephew.htm
    <div>
        <h3>Nephew</h3>
    </div>
```

Widgets tree processing is recursive in depth-first order. Nephew <div> and its descendants replaces child <div> element. Child <section> and its descendants - that now include nephew, replaces parent <section> element. Resulted document is below.

```
    <body>
        <h1>Parent</h1>
        <section>
            <h2>Child</h2>
            <div>
                <h3>Nephew</h3>
            </div>
        </section>
    </body>
```

One may notice that inheritance and aggregation results are identical. And this is indeed true in OOP and as with OOP also aggregation is preferred versus inheritance. In fact inheritance is used mostly for page skeletons; the rest is widgets aggregation.
    

## Widget Parameters

When an widget is reused in many places it may be necessary to customize every widget instance with specific parameters. For example there may be many lists of objects but every one need to have its specific caption. For this, widget layout has '@param' reference that is text replaced with 'wood:param' parameters list from widget path element.

In sample code, parent has two references to the same child widget but customized with different 'caption' value. On child widget, 'caption' parameter is declared with the same name as that used by parent parameters list.

```
    parent.htm
    <body>
       <h1>Parent</h1>
       <section wood:widget="child" wood:param="caption:John Doe"></section>
       <section wood:widget="child" wood:param="caption:Jane Doe"></section>
    </body>
    
    child.htm
    <section>
        <h2>@param/caption</h2>
        <div class="body"></div>
    </section>
```

After processing parent will have two child sections but every section with its own heading value.

```
    <body>
        <h1>Parent</h1>
        <section>
            <h2>John Doe</h2>
            <div class="body"></div>
        </section>
        <section>
            <h2>Jane Doe</h2>
            <div class="body"></div>
        </section>
    </body>
```

See {@link js.wood.LayoutParameters} for parameters list syntax and {@link js.wood.SourceReader} for parameters injection.

