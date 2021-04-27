## Overview

WOOD stands for Web Object Oriented Development and is a command line build tool for web user interfaces, based on W3C technologies - HTML and CSS, and plain ECMA Script. Its main goals are fighting complexity via decomposition and improved code reusability.

It is based on HTML components, linked in structures of  arbitrary complexity by only 5 operators. Operators are declared using well known HTML attributes syntax. There is no programing language mixed in the HTML components using some fancy syntax - just pure HTML. Final HTML pages are created by build tool at development phase, __not on runtime__.

Since WOOD is a build tool it does not relies on any particular library or applications framework. Generated HTML pages are static but any application framework using HTML code  can be used to add dynamic content support, e.g. JSP, AngularJS or j(s)-lib. Server side technologies like JSP can also benefit from WOOD decomposition.

WOOD is not particularly efficient for single page applications... recommend using multiple pages organized on specific tasks or activities. 

WOOD promotes an object oriented development paradigm using decomposition of complex user interfaces into user interface components - for short, components. Component is the base unit for code reusability that keeps all its files together, in the component directory. A component can inherit from a component with editable elements - named template, and can aggregate child components. A child component can be reused between multiple parent components. A page is just a component that has the `<body>` element.



![](components-overview.svg)



There is no formal limitation on templates inheritance hierarchy. A template can inherit from another template creating an arbitrary long templates chain. The same goes for child components. A component can aggregate more that one single child component that can aggregate its own components, creating a tree of not restricted complexity. And since template is indeed a component it can aggregate its own components tree. 

Components can be exported and imported to / from another projects and can be distributed in libraries. Moving components around is  just a mater of copying the component directory.

WOOD builder creates site files from project components - resulting files being standard HTML and related styles, scripts and media files. Build process reads component source files and resources from project, consolidates them into pages and write to the target file system. Target file system defines the directories structure for the site files. Different file systems structures are supported but a default implementation exists.

![](build-concept.svg)



The two OOP relations, inheritance and aggregation, supported by WOOD are declared with operators. Use `wood:editable` to declare editable elements on template, and `wood:template` to declare template path on component that extends the template. For child components aggregation use `wood:compo` operator. There is also the option to define layout parameters with `wood:param` when declare template or child components. Layout parameters are used on template and child component allowing customizing a component reused in multiple places. 

WOOD builder first aggregates child components tree, in depth-first order, then inject content into editable areas from templates chain, bottom-up. Top template should have `<body>` element.



![](components-relations.png)



Here is the HTML code for above diagram.

`res/template/page/page.htm`

```html
<body xmlns:w="js-lib.com/wood">
	<h1>@param/title</h1>
	<section w:editable="section"></section>
</body>
```
`res/compo/dialog/dialog.htm`

```html
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
`res/page/home/home.htm`

```html
<section w:template="res/template/page" w:param="title:Page Title" xmlns:w="js-lib.com/wood">
	<h2>Section Title</h2>
	<p>Section content...</p>
	<div w:compo="res/compo/dialog" w:param="title:Dialog Title"></div>
</section>
```

And here is the resulting page:

```html
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

![](components-sample.svg)



