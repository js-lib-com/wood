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

