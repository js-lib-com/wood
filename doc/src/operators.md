## Operators

WOOD operators are standard element attributes with predefined names recognized by WOOD tools. Operators are used to define relations between components, like inheritance and aggregation allowing build and preview tools to put together all components into pages. After processing, operators are removed from resulting page.

Clearly an operator should have a name and we should avoid name collision with standard HTML attribute names. On the other hand, in order to accommodate developer style there are couple naming strategies. See below table for supported naming strategy and `js.wood.NamingStrategy` class for details.

| Strategy      | Name                  | Description                                                  |
| ------------- | --------------------- | ------------------------------------------------------------ |
| __ATTR__      | Attribute name        | Simple attribute name. Because it does not use prefix it is prone to name collision but is simple to use. |
| __DATA_ATTR__ | Custom attribute name | Uses HTML custom attribute name, that is, prefixed with `data-`. This naming convention is a trade-off between simplicity to use and avoiding name collisions. |
| __XMLNS__     | XML name space        | This is default naming strategy and offer a clear separation for WOOD operator name space. Anyway, add complexity because name space should be declared with WOOD URI: `xmlns:wood="js-lib.com/wood"`. |

Default operators naming strategy is XML name space and this is also used in example from this document. Both build and preview have options to select desired naming strategy but selected naming convention is global per project.

Below are listed supported operators with short description. Following sections describe every operator in details. One can observe that with a limited number of operators WOOD is able to describe arbitrary complex component graphs.

| Operator          | Description                                                  | Operand                                                 | Example                                            |
| ----------------- | ------------------------------------------------------------ | ------------------------------------------------------- | -------------------------------------------------- |
| __wood:editable__ | Define editable area into templates.                         | Editable area name                                      | `<section wood:editable="section"></section>`      |
| __wood:template__ | Template link declaration contains both template path and editable name. | Reference to editable area                              | `<section wood:template="page#section">`           |
| __wood:compo__    | Child component link declaration.                            | Reference to component                                  | `<div wood:compo="list-view"></div>`               |
| __wood:param__    | Used in conjunction with `wood:compo` and `wood:template` to define layout parameters list. | Parameters list with syntax similar to inline CSS style | `<div ... wood:param="caption:Users List;"></div>` |



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

