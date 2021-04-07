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
    

