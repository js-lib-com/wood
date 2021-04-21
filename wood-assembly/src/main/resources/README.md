# WOOD - Web Object Oriented Development
WOOD is a development tool for user interfaces based on web technologies. It uses HTML  and CSS  to describe user interface layout and and standard JavaScript to implement user interactions. There is no attempt to hide 'the low level details'  behind another layer of abstractions, choosing to deal with complexity by decomposition and reusability. DRY - Don't Repeat Yourself and SoC - Separation of Concerns are core principles.

WOOD promotes an object oriented development paradigm using decomposition of complex user interfaces into user interface components - for short, components. Component is the base unit for code reusability that keeps all its files together, in the component directory. A component can inherit from a component with editable elements - named template, and can aggregate child components. A child component can be reused between multiple parent components. A page is just a component that has the `<body>` element.

There is no formal limitation on templates inheritance hierarchy. A template can inherit from another template creating an arbitrary long templates chain. The same goes for child components. A component can aggregate more that one single child component that can aggregate its own components, creating a tree of not restricted complexity. And since template is indeed a component it can aggregate its own components tree. 

Components can be exported and imported to / from another projects and can be distributed in libraries. Moving components around is  just a mater of copying the component directory.

WOOD builder creates site files from project components - resulting files being standard HTML and related styles, scripts and media files. Build process reads component source files and resources from project, consolidates them into pages and write to the target file system. Target file system defines the directories structure for the site files. Different file systems structures are supported but a default implementation exists.

The two OOP relations, inheritance and aggregation, supported by WOOD are declared with operators. Use `wood:editable` to declare editable elements on template, and `wood:template` to declare template path on component that extends the template. For child components aggregation use `wood:compo` operator. There is also the option to define layout parameters with `wood:param` when declare template or child components. Layout parameters are used on template and child component allowing customizing a component reused in multiple places. 

For complete documentation see manual from distribution or online at http://wood.js-lib.com/
