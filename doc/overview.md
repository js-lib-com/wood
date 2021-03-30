# WOOD - Web Object Oriented Development

WOOD is a development tool for user interfaces based on web technologies. It uses HTML  and CSS  to describe user interface layout and and standard JavaScript to implement user interactions. There is no attempt to hide 'the low level details'  behind another layer of abstractions, choosing to deal with complexity by decomposition and reusability. DRY - Don't Repeat Yourself and SoC - Separation of Concerns are core principles.



## Overview

WOOD promotes an object oriented development paradigm using decomposition of complex user interfaces into user interface components - for short, components. Component is the base unit for code reusability that keeps all its files together, in the component directory. A component can inherit from a component with editable elements - named template, and can aggregate child components. A child component can be reused between multiple parent components. A page is just a component that has the `<body>` element.

![](D:\docs\workspaces\js-lib\tools\wood\wood\doc\components-overview.svg)

There is no formal limitation on templates inheritance hierarchy. A template can inherit from another template creating an arbitrary long templates chain. The same goes for child components. A component can aggregate more that one single child component that can aggregate its own components, creating a tree of not restricted complexity. And since template is indeed a component it can aggregate its own components tree. 

Components can be exported and imported to / from another projects and can be distributed in libraries. Moving components around is  just a mater of copying the component directory.

WOOD builder creates site files from project components - resulting files being standard HTML and related styles, scripts and media files. Build process reads component source files and resources from project, consolidates them into pages and write to the target file system. Target file system defines the directories structure for the site files. Different file systems structures are supported but a default implementation exists.

![](D:\docs\workspaces\js-lib\tools\wood\wood\doc\build-concept.svg)



The two OOP relations, inheritance and aggregation, supported by WOOD are declared with operators. Use `wood:editable` to declare editable elements on template, and `wood:template` to declare template path on component that extends the template. For child components aggregation use `wood:compo` operator. There is also the option to define layout parameters with `wood:param` when declare template or child components. Layout parameters are used on template and child component allowing customizing a component reused in multiple places. 

WOOD builder first aggregates child components tree, in depth-first order, then inject content into editable areas from templates chain, bottom-up. Top template should have `<body>` element.

![](D:\docs\workspaces\js-lib\tools\wood\wood\doc\relations.svg)

Here is the HTML code for above diagram. For brevity name space prefix is reduced to 'w'.

```
res/template/page/page.htm
<body xmlns:w="js-lib.com/wood">
	<h1>@param/title</h1>
	<section w:editable="section"></section>
</body>
```
```
res/compo/dialog/dialog.htm
<div>
	<div class="close"></div>
	<h3>@param/title</h3>
	<form>
		<input type="text" name="user-name" />
		<input type="password" name="password" />
	</form>	
	<button>Save</button>
</div>
```
```
res/page/home/home.htm
<section w:template="res/template/page" w:param="title:Page Title" xmlns:w="js-lib.com/wood">
	<h2>Section Title</h2>
	<p>Section content...</p>
	<div w:compo="res/compo/dialog" w:param="title:Dialog Title"></div>
</section>
```

And here is the resulting page:

```
<body>
	<h1>Page Title</h1>
	<section>
		<h2>Section Title</h2>
		<p>Section content...</p>
		<div>
			<div class="close"></div>
			<h3>Dialog Title</h3>
			<form>
				<input type="text" name="user-name" />
				<input type="password" name="password" />
			</form>	
			<button>Save</button>
		</div>
	</section>
</body>
```

To sum-up, WOOD tools uses major object oriented features as follow:

| OOP | WOOD |
|-------------------------------------|-----------------------------|
| __encapsulation__ | by keeping all component files in a single directory that act as name space |
| __privacy__ | build and preview processes does search for resources only in component and global `asset` directories |
| __inheritance__ | via editable elements from templates |
| __aggregation__ | using child components that can also have own child components, in tree of not restricted complexity |
| __unit test__ | unit testing is not strictly speaking specific to OOP but is well supported by encapsulation|

Below is a more complex example. It is a contrived example but helps in getting the big picture.

It focuses on component relations that are represented as arrows, simple arrow for inheritance and arrow with diamond for aggregation. Note that inheritance is also know as `IS A` relation whereas aggregation is a `HAS A` relation. Also, a component that implements content for an editable area is known as content component, whereas a component that has child components is named parent component.

![](D:\docs\workspaces\js-lib\tools\wood\wood\doc\components-sample.svg)



## Project

A WOOD project is a collection of loosely coupled components. And a reusable child component can be indeed completely decoupled, with no dependency on any project resource. Now, if you image a component as a tree you will see that WOOD name makes sense.

A project has a root directory and all paths are relative to this project root. Project instance is initialized from {@link ProjectDescriptor}, but all descriptor properties have sensible default values, therefore descriptor is optional.

Project is a not constrained tree of directories and files, source files - layouts, styles, scripts, medias and variables, and descriptors. Directories hierarchy is indeed not constrained but there are naming conventions for component files - see {@link Component} class description.

It is recommended as good practice to have separated modules for user interface and back-end logic. Anyway, there are no constraints that impose this separation of concerns. It is, for example, allowed to embed WOOD project directories in a master Java project, perhaps Eclipse, e.g. a `lib` directory for both WOOD components and Java archives. Anyway, master project files must not use file extensions expected by this tool library and files name should obey syntax described by {@link Path} classes hierarchy. Finally, there is the option to exclude certain directories from WOOD builder processing.

### Project Descriptor

Project descriptor contains project settings and configurations. All properties have sensible default values therefore project descriptor can miss.

```
<?xml version="1.0" encoding="UTF-8"?>
<project>
	<author>Iulian Rotaru</author>
	<display>kids (a)cademy</display>
	<description>kids (a)cademy is a self-educational ecosystem targeted at children of all ages.</description>
	<locale>en</locale>

	<media-queries>
		<media-query alias="lgd" expression="min-width: 1200px" />
		<media-query alias="mdd" expression="max-width: 992px" />
		<media-query alias="smd" expression="max-width: 768px" />
		<media-query alias="xsd" expression="max-width: 560px" />
	</media-queries>

	<head>
		<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0"></meta>

		<link rel="stylesheet" href="http://fonts.googleapis.com/css?family=Lato" type="text/css"></link>

		<script src="lib/sdk/analytics.js" type="text/javascript" defer="false" embedded="true"></script>
		<script src="lib/js-lib/js-lib.js" type="text/javascript" defer="true"></script>
	</head>

	<excludes>res/page/apps2/, res/page/atlas/, res/page/blog/, res/page/legal/, res/page/legal/terms/</excludes>
</project>
```

All properties are searched by element tag name therefore is not relevant how elements are grouped. For example, above `head` group element can be replaced with three groups `metas`, `links` and `scripts`. Or it can be eliminated and put its child elements directly on root.

### author
Developer name inserted into generated page, on head meta author. 

### display
Project name inserted into generated page, on title element.

### description
Project description inserted into generated page, on head meta description.

### locales
Comma separated locale list. A locale item contains ISO 639 alpha-2 language code, and optional  ISO 3166 alpha-2 country code, separated by dash. For example `en` and `en-US` are both valid locale.

```
<locale>en, ro</locale>
```

 Default locale is `en`.

### naming

Naming convention used to declare WOOD operators into layout files. Supported values are ATTR, DATA_ATTR and XMLNS. Upper case should be preserved.

```
<naming>DATA_ATTR</naming>
```

Default naming is XMLNS.

### media-query

Media queries are used to bind an alias to a CSS media query expression. After its declaration the alias can be used as CSS variant, e.g. `page_lgd.css`. Is an error attempting to use a CSS variant that is not defined. Media query definitions are used by style file reader to consolidate style files with media query blocks.

Media query element has two attributes: alias and expression.
```
<media-query alias="lgd" expression="min-width: 1200px" />
```

If there is no media query defined, project uses hard coded default values:

```
<media-query alias="smd" expression="min-width: 560px" />
<media-query alias="mdd" expression="min-width: 768px" />
<media-query alias="lgd" expression="min-width: 992px" />
<media-query alias="xld" expression="min-width: 1200px" />
```

### meta

Meta element copied as it is on generated page head. 

```
<meta http-equiv="X-UA-Compatible" content="IE=9; IE=8; IE=7; IE=EDGE"></meta>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0"></meta>
```

### link
Link element copied as it is on generated page head. 
```
<link rel="shortcut icon" href="media/favicon.ico" type="image/x-icon"></link>
<link rel="stylesheet" href="http://fonts.googleapis.com/css?family=Lato" type="text/css"></link>
```

### script
Script element copied on generated page head. All declared attributes are copied as they are. There is though an exception: custom embedded attribute. 

If embedded attribute is present and is true script source is copied into HTML page as text content for script element.

```
<script src="lib/sdk/analytics.js" type="text/javascript" defer="false" embedded="true"></script>
<script src="lib/sdk/facebook.js" type="text/javascript" defer="false" embedded="true"></script>
<script src="lib/js-lib/js-lib.js" type="text/javascript" defer="true"></script>
```

### excludes
Directories to exclude from project scanning. Project scanning occurs only on building process and aims to discover components variables and pages.

This property is a comma separated list of directory paths - relative to project root. Since items are directories they should properly ended with path separator. For legibility, space after comma is accepted but not mandatory. 

```
<excludes>res/page/apps2/, res/page/atlas/, res/page/blog/, res/page/legal/, res/page/legal/terms/</excludes>
```



## Component

Component is designed to break down inherent web interfaces complexity. It is self-containing and fully described and is the basic building block. A component resides in its own directory and all its source, resource and descriptor files are hosted there. Preview files are for unit testing.

![](D:\docs\workspaces\js-lib\tools\wood\wood\doc\component-structure.svg)

All component files are optional but at least layout or script should exist. It is legal to have a component with only layout, e.g. a select with static options reused in multiple forms. In this case component director can miss.

| Files | Description      |
|-------|------------------|
| __layout__ | describes component user interface using HTML markup language |
| __style__ | standard CSS used to fix layout elements position and dimension; note that presentational styles are kept in global `theme` directory |
| __script__ | script for component behavior can be included in component directory or in a separated source directory |
| __resource__ | support media files and variables declared in XML files and referred from source files with at-meta references, e.g. `@string/title` |
| __descriptor__ | component meta-data |
| __preview__ | preview files are used to unit test the component; there are preview files for layout, style and script |

By convention component name, directory name and its layout, style, script and descriptor files basename are the same. For example, a component named `progress-bar` has `progress-bar.htm` layout file and `progress-bar.css` style that are hosted in `progress-bar` directory. Directory path relative to project root is the component qualified name, e.g. `res/compo/progress-bar`.

Components are designed to work together into complex structures. Structure is described by component relations and a relation is defined using an WOOD operator. Note that operators are used only on layout files.

Now, considering their relations, there are couple component kinds. This taxonomy is for concept inception but in the end all are components.

| Kind | Description |
|------|-------------|
| __template__ | contain editable elements and cannot be used for build; in a sense they are abstract components |
| __content component__ | component that describe content to be injected into template editable areas; we can see it as subclass |
| __child component__ | are reusable components that can be included in both template and another components |
| __page__ | is a component with `<body>` element or that inherit from a template with `<body>` element |

Component promotes reusability via classic OOP paradigms: encapsulation, inheritance and aggregation. Both inheritance and aggregation act upon layout files, so that when talking about components inheritance/aggregation we actually refer to layout files. Anyway, because of structuring user interface in components, styles are object oriented too in the sense that a component style file contains rules only for that component - a sort of OOP encapsulation.

Note that WOOD requires separation of concerns, especially separation of presentation and content. For this reason component style should define only layout specific rules, all presentational rules being defined on global `theme` directory. For now this policy is just a good practice recommendation.

Both child components and templates can be reused as part of components libraries. Also libraries can provide ready to use page templates. When start an empty project just import desired page template and implements its editable elements.

Developer can preview components using any browser; this is true for templates too. Browser is the viewer for WYSIWYG development and WOOD preview mechanism has auto-refresh triggered by project files changes. Also components are part of building process that generates site pages. Anyway, building templates have not much sense since its functionality is not completely described.

### Preview

### Component Descriptor




## Operators

WOOD operators are standard element attributes with predefined names recognized by WOOD tools. Operators are used to define relations between components, like inheritance and aggregation allowing build and preview tools to put together all components into pages. After processing, operators are removed from resulting page.

Clearly an operator should have a name and we should avoid name collision with standard HTML attribute names. On the other hand, in order to accommodate developer style there are couple naming strategies. See below table for supported naming strategy and `js.wood.NamingStrategy` class for details.

| Strategy | Name                  | Description |
|------------------------|------------------------|------------------|
| __ATTR__        | Attribute name        | Simple attribute name. Because it does not use prefix it is prone to name collision but is simple to use. |
| __DATA_ATTR__   | Custom attribute name | Uses HTML custom attribute name, that is, prefixed with `data-`. This naming convention is a trade-off between simplicity to use and avoiding name collisions. |
| __XMLNS__       | XML name space        | This is default naming strategy and offer a clear separation for WOOD operator name space. Anyway, add complexity because name space should be declared with WOOD URI: `xmlns:wood="js-lib.com/wood"`. |

Default operators naming strategy is XML name space and this is also used in example from this document. Both build and preview have options to select desired naming strategy but selected naming convention is global per project.

Below are listed supported operators with short description. Following sections describe every operator in details. One can observe that with a limited number of operators WOOD is able to describe arbitrary complex component graphs.

| Operator         | Description                          | Operand            | Example                                       |
|------------------|--------------------------------------|--------------------|-----------------------------------------------|
|__wood:editable__ | Define editable area into templates. | Editable area name | `<section wood:editable="section"></section>` |
| __wood:template__ | Template link declaration contains both template path and editable name. | Reference to editable area | `<section wood:template="page#section">` |
| __wood:compo__ | Child component link declaration. | Reference to component | `<div wood:compo="list-view"></div>` |
| __wood:param__ | Used in conjunction with `wood:compo` and `wood:template` to define layout parameters list. | Parameters list with syntax similar to inline CSS style | `<div ... wood:param="caption:Users List;"></div>` |



### wood:editable

A template is a component with one or more editable areas. An editable area is an element marked so with `wood:editable` operator. This operator has a name that should be unique on template layout.



### wood:template



### wood:compo



### wood:param

Layout parameters are a mean to customize a reusable component. Parameters are defined at the place where component is linked whereas parameters value is used inside that component.  

Layout parameters are loaded with `wood:param` operator and used by source reader to inject values into layout file, on the fly. Into layout file, parameters are declared using `@param/name` reference. Parameter reference acts as a placeholder that is replaced with parameter value.

The number of parameters, referenced from the reusable layout is not limited but all should be defined when link the component. Also the same parameter reference can be used multiple times in a given layout document. Because parameter references are replaced with string values, they can appear anywhere a string is valid.

Parameters definition syntax is similar to inline CSS style. It is a list of parameters separated by semicolon whereas a parameter is a name / value pair separated by colon. Spaces are not allowed around separators.

```
parameters = parameter *(PARAM_SEP parameter) [PARAM_SEP]
parameter = name NAME_SEP value
PARAM_SEP = ';'
NAME_SEP = ':'
```



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



## Encapsulation and Privacy

Encapsulation is achieved by structuring project user interface in self-containing components. A component is fully defined and possess all resources it needs. A direct side effect is that an component is unit testing friendly, see <a href="#testing">Unit Testing</a> section. Also component acts as a name space. For example, an image from a component may have the same name as an image from another component.

Build and preview processes search for variables and media files only inside component directory. This ensure one can freely change resources from a component without the risk of breaking other components. Anyway, if one really needs global resources there is project 'asset' directory just for that.



## Inheritance

OOP inheritance is emulated using templates with editable elements. An editable is an element with `wood:editable` operator; attribute value is that editable name. A component with layout with editable elements is called template. An editable element define an insertion point where content layout will be injected by WOOD tool. Content element is the root of content layout and will literally replace editable element from template. A content element is an element that has `wood:template` operator that contains the path to named editable element. In resulting page, template styles are included before content style so that content component can overwrite template styles. Variables are replaced by their defined values at layout and style files reading, on the fly.

Because a template layout file can contain multiple editable elements the path referring to an editable element should include both component path and editable name, e.g. 'template/page#body' . See below simplified rule and {@link js.wood.EditablePath} for details about path syntax.

```
    editable-path = component-path '#' editable-name
```

Below ASCII diagram describe component inheritance via template mechanism. For brevity only one editable is figured but a template may have a not limited number of editable elements. A component that uses a template but does not define content for all template editable elements become a template on its turn. This way one can create an arbitrary large inheritance chain; a sub-template can define content for any super-template from hierarchy and can include its own editable elements.

![](D:\docs\workspaces\js-lib\tools\wood\wood\doc\inheritance.svg)

Note that multiple inheritance is not supported. A content layout may define content for multiple editable elements but all should belong to the same template.

```
    grand-parent.htm
    <body>   														
        <h1>Grand Parent</h1> 
        <section wood:editable="section"></section>
    </body> 
                                   
    parent.htm        
    <section wood:template="grand-parent#section">                    
        <h2>Parent</h2>                       
        <div wood:editable="paragraph"></div>
    </section>
    
    child.htm
    <div wood:template="parent#paragraph">
        <h3>Child</h3>
    </div>
```
In above snippet we have three component layout files: child, parent and grand-parent. Child define content for 'paragraph' editable section from parent. Parent define content for 'section' editable element from grand parent. Resulted document is below.

```
    <body>
        <h1>Grand Parent</h1>
        <section>
            <h2>Parent</h2>
            <div>
                <h3>Child</h3>
            </div>
        </section>
    </body>                                                                            
```
Child <div> and its descendants replaces parent <div>. Parent <section> and its descendants - that now include already inserted child <div>, replaces grand-parent <section>.



## Template Parameters

Beside editable areas a template may contain parameter references for template customization - see `@param/title` . For example, a template for a dialog box may have a title and every dialog, based on this dialog template, can have its own title value.

```
    template/dialog/dialog.htm
    <div class="dialog">
        <h2 class="title">@param/title</h2>
        <div class="box-close"></div>
        ...
        <div wood:editable="body"></div>
        ...
    </div>
    
    dialog/user/user.htm
    <div wood:template="template/dialog#body" wood:param="title:Edit User">
        <form>
        ...
        </form>
    </div>
```
Generated user dialog layout will look like below sample code. Body editable is replaces by provided `form` and title updated from parameter.

```
    <div class="dialog">
        <h2 class="title">Edit User</h2>
        <div class="box-close"></div>
        ...
        <div>
            <form>
            ...
            </form>
        </div>
        ...
    </div>
```
See {@link js.wood.LayoutParameters} for parameters list syntax and {@link js.wood.SourceReader} for parameters injection.



## Template Sample
There are three user dialogs for user creation and edit and password change. All have basically the same look but differ by form fields, dialog caption and submit button label.

![](D:\docs\workspaces\js-lib\tools\wood\wood\doc\dialogs.svg)

Is obvious we will have a template and three content components. Template `res/compo/form-dialog` has an editable form element. Content components, `res/user/create-dialog`, `res/user/edit-dialog` and `res/user/password-dialog` implements specific form fields. Content components use `wood:template="res/compo/form-dialog#form"` operator to identify template and its editable area.



![](D:\docs\workspaces\js-lib\tools\wood\wood\doc\dialogs-hierarchy.svg)

To customize dialog caption and submit button label there are `wood:param` operators. For example, on user creation dialog it is `wood:param="caption:CREATE USER;btn:Create"`. Parameters names need to match those declared on template: `@param\caption`, respective `@param\btn`. For other two dialogs parameters handling is similar.



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



<a id="testing"></a>

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



<a id="resources"></a>

## Resource and Reference

Resources are external entities used by source files. There are two major kinds of resources: variables and media files. Variables are text replaced with values defined in variables files. A variables file can contain variable of the same type; XML root is the variable type - see {@link js.wood.ResourceType} for supported types. Media are files referenced from sources like images, audio and video files.

A reference is a pointer from a source file to a resource, be it variable or media file. It has a type and a name, uniquely identifying the resource in its scope. Reference scope is the component to which source file belongs plus global assets scope.

Reference syntax is described below. This syntax is the same, no mater the source file type where reference is used: layout, style or script.

```
    reference = MARK resource-type SEP name
    name      = 1CH  
    ; resource-type is defined by {@link js.wood.ResourceType}, to lower case
      
    ; terminal symbols definition
    MARK = "@"                 ; reference mark
    SEP  = "/"                 ; reference name separator
    CH   = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
      
    ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
```

As stated variables and media files can be referenced from any source file: layout, style or script. Here are sample usage for all three cases. Note that references are text replaced and where source file syntax requires quotes (") they should be explicitly used.

```
    <body>
        <h1>@string/title</h1>
        <img src="@image/logo" />
        <p>@text/message</p>
        . . .
    </body>
       
    body {
        {@literal @}style/page
        width: {@literal @}dimen/page-width;
        background-image: url("@image/page-bg");
        background-color: {@literal @}color/page-color;
        . . .
    }
      
    js.ua.System.alert("@string/exception");
    this.setRichText("@text/message");
    this.logo.setSrc("@image/logo");
    this.audioPlayer.play("@audio/beep");
```



## Process

WOOD supplies two major services: preview and build. Preview is used with a standard web browser and a Tomcat runtime. There is a preview Servlet that redirect browser requests to project source files. Developer change source files and press F5 in browser to see results. Build process prepares site files for production deployment. Note that build outcome are standard web user interface files that can be integrated into any web project.

Source files reside in a project with a predefined files layout, see below section. WOOD tool works only on its own files and ignores all others. This way an WOOD project can be part of a larger, master project - perhaps an Eclipse project. WOOD tools is designed for web user interfaces; server side coding is not in scope. Although WOOD is written in Java and operates using Tomcat, there is no formal restriction regarding the language used for server side logic.
    
## Project

In essence an WOOD project is just a directory with source files. By convention project name is the that directory name.

Current project file system is depicted below. There are four source directories and one build target. There is also a project configuration XML file. It is acceptable to share directories with master project, if any. For example lib can hold Java archives.

```
    /                     ; project root
    /build/site           ; site build target directory
    /gen/                 ; optional generated scripts, mostly HTTP-RMI stubs
    /lib/                 ; third-party user interface components and script libraries
    /res/                 ; application user interface resources
    |   /asset            ; project assets directory stores global variables and media files
    |   /theme            ; UI resources theme for presentational styles
    ~                     ; application defined components
    /script/              ; application specific scripts structured in packages
    +-project.xml         ; project configuration file
```

Note that default build target directory is a sub-directory of master project build. This is to allow storing all build files in the same place.
    
## Preview

Preview process is supported by {@link js.wood.PreviewServlet} class that is configured to get all requests for a particular project. Every project has its own root context and there is no formal limit on the number of projects from a Tomcat runtime. Note that preview Servlet is not part of project WAR but is integrated into WOOD library that should be installed into Tomcat runtime.

In fact project preview WAR is empty. It should contains only 'web.xml' descriptor in the standard path. It should declare preview Servlet as context listener and configure request path. Also needs to supply project directory absolute path, as context parameter. See sample below.

```
    <context-param>
    	<param-name>PROJECT_DIR</param-name>
    	<param-value>path/to/project</param-value>
    </context-param>
       
    <listener>
    	<listener-class>js.wood.PreviewServlet</listener-class>
    </listener>
       
    <servlet>
    	<servlet-name>development</servlet-name>
    	<servlet-class>js.wood.PreviewServlet</servlet-class>
    	<load-on-startup>1</load-on-startup>
    </servlet>
       
    <servlet-mapping>
    	<servlet-name>development</servlet-name>
    	<url-pattern>/</url-pattern>
    </servlet-mapping>
```

Project preview WAR creation is not in WOOD library scope. It is delegated to external tools. For example Eclipse plug-in knows to create project preview and deploy it in Tomcat runtime.
    
## Build

This library supplies a {@link js.wood.Builder} class that is designed for integration with external tools. There are extensions for Ant tasks and Eclipse plug-in using this Builder class, but not part of this library distribution.

To use this class, create instance providing project directory then just invoke build(). Optionally, one may set the build number. Build site directory is controlled by build process and is project configurable; it can be obtaining via site path getter.

```
    Builder builder = new Builder(projectDir);
    builder.setBuildNumber(buildNumber);
    builder.build();
    ...
    String sitePath = builder.getSitePath();
```
{@link js.wood.Builder} acts as a bridge between {@link js.wood.Project} and {@link js.wood.BuildFS}. It reads component source and resource files from project, consolidates them into pages then write to build site directory.
    
## Build File System

Build file system is the directory structure for site files. Different build file systems are supported but a default implementation exists, see {@link js.wood.DefaultBuildFS}.

Default build file system uses separated directories for styles, scripts and media files; all media files are stored in the same place. Pages are placed in build directory root. For multi-language projects, replicates single language directories layout for every language. Language directories have the same language ISO 639-1 name.

```
    /                       /                  
    /media/                 /en/
    /script/                |  /media/
    /style/                 |  /script/
    +-page.htm              |  /style/    
                            |  +-page.htm  
                            |
                            /ro/
                            |  /media/
                            |  /script/
                            |  /style/
                            |  +-page.htm
```
Here is a build directory layout for both single and multi language projects.
    
## Files Naming Conventions

Path names used by WOOD obey Java file names convention but with couple syntax constrains. There are reserved character for different semantic extensions. For this reason allowed character set for names is US-ASCII alphanumeric characters, dash (-) and dot (.); dot is for file names with version like 'js-lib-1.2.3.js'. Also always uses slash (/) for file separator. Note that underscore (_) is used for variants separator and is not allowed in names.

```
    SEP = "/"                          ; file separator
    CH  = ALPHA / DIGIT / "-" / "."    ; character is US-ASCII alphanumeric, dash and dot
      
    ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
```



## Page Header

There are two configuration files affecting page header elements. One is project global with impact on all pages, {@link js.wood.Config} whereas page descriptor defines only page specific settings, see {@link js.wood.Descriptor}. Page descriptor is located into page directory and has the same name as page but with 'xml' extension.

Page title and description is defined by page descriptor. It 'title' element is missing uses 'project / page' display and if 'description' element is missing uses 'title'.

