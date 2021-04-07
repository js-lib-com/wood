## Style Preprocessing

A component has private resources that could be referenced from source files. This is true for component style too. A style source can use variables, declared into variables declaration XML files. Also media file references can be used, for example background images. Style variables and media files references are first style pre-processing support.

In sample code `@image/background` is a reference to an image file located into component directory or global assets. By convention media file reference is the file name but without extension. References `@dimen/desktop-width` and `@color/text-color` are variables declared into XML files from component directory or global assets. For a discussion about references see <a href="#resources">Resource and Reference</a>.


```
    body {
        background-image: url(@image/background);
        width: @dimen/desktop-width;
        color: @color/text-color;
    }
```

There is also support for responsive design, styles based on media queries. A style file can have variant, for example, `compo_w800.css`. When processed, content from that file is appended to base `page.css` in a media query block for `max-width` of 800px.

In example, base style has center text align and styles for devices less than 800px uses left align.

```
    compo.css
    header {
        position: absolute;
        text-align: center;
    }
    
    compo_w800.css
    header {
        text-align: left;
    }
```

Resulting style file will merge both base and variant style files with media query prefix for styles from variant. For details about media query processing see `js.wood.StyleReader` class description.

```
    compo.css
    header {
        position: absolute;
        text-align: center;
    }
    
    @media screen and (max-width : 800px) {
    header {
        text-align: left;
    }
    }
```

Both variant and related media query should be declared on project descriptor, see below sample; more on project descriptor.

```
	<media-queries>
		<media-query alias="w800" expression="max-width: 800px" />
		...
	</media-queries>
```



## Style Namespace

HTML styling is intrinsically complex. WOOD component based approach breaks complexity in manageable units but is prone to name collision and style overriding. Now, overriding can be on purpose but also by accident and for complex sites can be hard to detect.

In order to avoid accidental overriding WOOD uses namespace for component CSS style file. A namespace is simple an unique name used as element class that prefixes all style rules related to component. Name uniqueness should be ensured by developer but a good practice is to use component script class converted to dash case. For example ro.gnotis.UserPage is converted to ro-gnotis-userpage.

When including child widgets, parent should avoiding using global namespace. Instead every parent section should use namespace on its direct scope. In example below do not use `ro-gnotis-userpage` class on `body` element but to first and last `div` elements.

```
    +------------------------------+    <body>                                       user-page.css
    | +--------------------------+ |      <div class="ro-gnotis-userpage">           .ro-gnotis-userpage .header {
    | | .ro-gnotis-userpage      | |        <div class="header"></div>                 background-color: red;
    | | +----------------------+ | |      </div>                                     } 
    | | | .header              | | |       
    | | +----------------------+ | |      <div wood:widget="lib/list-view"></div>    list-view.css
    | +--------------------------+ |                                                 .js-widget-listview .header {
    |                              |      <div class="ro-gnotis-userpage">             background-color: green;
    | +--------------------------+ |        <div class="footer"></div>               }
    | | .js-widget-listview      | |      </div>    
    | | +----------------------+ | |    </body>
    | | | .header              | | |
    | | +----------------------+ | |
    | +--------------------------+ | 
    |                              |
    | +--------------------------+ |
    | | .ro-gnotis-userpage      | | 
    | +--------------------------+ |
    +------------------------------+
```

In depicted context, user page has an header section and include a list view widget somewhere below. List view has its own header. Into generated page, WOOD includes widget style before parent style. This way parent is able to customize widget style, although this technique may be controversial since breaks encapsulation.

Resulting style, after browser processing, may look like:

```
    .js-widget-listview .header {
      background-color: green;
    }
    .ro-gnotis-userpage .header {
      background-color: red;
    }    
```

If 'ro-gnotis-userpage' class would be global on 'body' element the parent header style would override list view header and its color would be red instead of green.

