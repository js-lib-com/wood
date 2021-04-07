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
    