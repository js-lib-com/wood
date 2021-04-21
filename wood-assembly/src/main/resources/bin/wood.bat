@set home=%~dp0
@set home=%home:~0,-4%

@set "bin=%home%\bin\*"
@set "lib=%home%\lib\*"

@set "lock=%home%.lock"
@copy /y NUL %lock% >NUL

@java -cp "%bin%;%lib%" js.wood.cli.Main %*

@del %lock%
