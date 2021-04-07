## Inheritance

OOP inheritance is emulated using templates with editable elements. An editable is an element with `wood:editable` operator; attribute value is that editable name. A component with layout with editable elements is called template. An editable element define an insertion point where content layout will be injected by WOOD tool. Content element is the root of content layout and will literally replace editable element from template. A content element is an element that has `wood:template` operator that contains the path to named editable element. In resulting page, template styles are included before content style so that content component can overwrite template styles. Variables are replaced by their defined values at layout and style files reading, on the fly.

Because a template layout file can contain multiple editable elements the path referring to an editable element should include both component path and editable name, e.g. 'template/page#body' . See below simplified rule and {@link js.wood.EditablePath} for details about path syntax.

```
    editable-path = component-path '#' editable-name
```

Below ASCII diagram describe component inheritance via template mechanism. For brevity only one editable is figured but a template may have a not limited number of editable elements. A component that uses a template but does not define content for all template editable elements become a template on its turn. This way one can create an arbitrary large inheritance chain; a sub-template can define content for any super-template from hierarchy and can include its own editable elements.

![](inheritance.svg)

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

![](dialogs.svg)

Is obvious we will have a template and three content components. Template `res/compo/form-dialog` has an editable form element. Content components, `res/user/create-dialog`, `res/user/edit-dialog` and `res/user/password-dialog` implements specific form fields. Content components use `wood:template="res/compo/form-dialog#form"` operator to identify template and its editable area.



![](dialogs-hierarchy.svg)

To customize dialog caption and submit button label there are `wood:param` operators. For example, on user creation dialog it is `wood:param="caption:CREATE USER;btn:Create"`. Parameters names need to match those declared on template: `@param\caption`, respective `@param\btn`. For other two dialogs parameters handling is similar.

