## Project

A WOOD project is a collection of loosely coupled components. And a reusable child component can be indeed completely decoupled, with no dependency on any project resource. Now, if you image a component as a tree you will see that WOOD name makes sense.

A project has a root directory and all paths are relative to this project root. Project instance is initialized from {@link ProjectDescriptor}, but all descriptor properties have sensible default values, therefore descriptor is optional.

Project is a not constrained tree of directories and files, source files - layouts, styles, scripts, medias and variables, and descriptors. Directories hierarchy is indeed not constrained but there are naming conventions for component files - see {@link Component} class description.

It is recommended as good practice to have separated modules for user interface and back-end logic. Anyway, there are no constraints that impose this separation of concerns. It is, for example, allowed to embed WOOD project directories in a master Java project, perhaps Eclipse, e.g. a `lib` directory for both WOOD components and Java archives. Anyway, master project files must not use file extensions expected by this tool library and files name should obey syntax described by {@link Path} classes hierarchy. Finally, there is the option to exclude certain directories from WOOD builder processing.

### Project File System

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

