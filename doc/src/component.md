## Component

Component is designed to break down inherent web interfaces complexity. It is self-containing and fully described and is the basic building block. A component resides in its own directory and all its source, resource and descriptor files are hosted there. Preview files are for unit testing.

![](component-structure.svg)

All component files are optional but at least layout or script should exist. It is legal to have a component with only layout, e.g. a select with static options reused in multiple forms. In this case component director can miss.

| Files          | Description                                                  |
| -------------- | ------------------------------------------------------------ |
| __layout__     | describes component user interface using HTML markup language |
| __style__      | standard CSS used to fix layout elements position and dimension; note that presentational styles are kept in global `theme` directory |
| __script__     | script for component behavior can be included in component directory or in a separated source directory |
| __resource__   | support media files and variables declared in XML files and referred from source files with at-meta references, e.g. `@string/title` |
| __descriptor__ | component meta-data                                          |
| __preview__    | preview files are used to unit test the component; there are preview files for layout, style and script |

By convention component name, directory name and its layout, style, script and descriptor files basename are the same. For example, a component named `progress-bar` has `progress-bar.htm` layout file and `progress-bar.css` style that are hosted in `progress-bar` directory. Directory path relative to project root is the component qualified name, e.g. `res/compo/progress-bar`.

Components are designed to work together into complex structures. Structure is described by component relations and a relation is defined using an WOOD operator. Note that operators are used only on layout files.

Now, considering their relations, there are couple component kinds. This taxonomy is for concept inception but in the end all are components.

| Kind                  | Description                                                  |
| --------------------- | ------------------------------------------------------------ |
| __template__          | contain editable elements and cannot be used for build; in a sense they are abstract components |
| __content component__ | component that describe content to be injected into template editable areas; we can see it as subclass |
| __child component__   | are reusable components that can be included in both template and another components |
| __page__              | is a component with `<body>` element or that inherit from a template with `<body>` element |

Component promotes reusability via classic OOP paradigms: encapsulation, inheritance and aggregation. Both inheritance and aggregation act upon layout files, so that when talking about components inheritance/aggregation we actually refer to layout files. Anyway, because of structuring user interface in components, styles are object oriented too in the sense that a component style file contains rules only for that component - a sort of OOP encapsulation.

Note that WOOD requires separation of concerns, especially separation of presentation and content. For this reason component style should define only layout specific rules, all presentational rules being defined on global `theme` directory. For now this policy is just a good practice recommendation.

Both child components and templates can be reused as part of components libraries. Also libraries can provide ready to use page templates. When start an empty project just import desired page template and implements its editable elements.

Developer can preview components using any browser; this is true for templates too. Browser is the viewer for WYSIWYG development and WOOD preview mechanism has auto-refresh triggered by project files changes. Also components are part of building process that generates site pages. Anyway, building templates have not much sense since its functionality is not completely described.

### Preview

### Component Descriptor

