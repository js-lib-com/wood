## Encapsulation and Privacy

Encapsulation is achieved by structuring project user interface in self-containing components. A component is fully defined and possess all resources it needs. A direct side effect is that an component is unit testing friendly, see <a href="#testing">Unit Testing</a> section. Also component acts as a name space. For example, an image from a component may have the same name as an image from another component.

Build and preview processes search for variables and media files only inside component directory. This ensure one can freely change resources from a component without the risk of breaking other components. Anyway, if one really needs global resources there is project 'asset' directory just for that.



## Process

WOOD supplies two major services: preview and build. Preview is used with a standard web browser and a Tomcat runtime. There is a preview Servlet that redirect browser requests to project source files. Developer change source files and press F5 in browser to see results. Build process prepares site files for production deployment. Note that build outcome are standard web user interface files that can be integrated into any web project.

Source files reside in a project with a predefined files layout, see below section. WOOD tool works only on its own files and ignores all others. This way an WOOD project can be part of a larger, master project - perhaps an Eclipse project. WOOD tools is designed for web user interfaces; server side coding is not in scope. Although WOOD is written in Java and operates using Tomcat, there is no formal restriction regarding the language used for server side logic.
  

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

