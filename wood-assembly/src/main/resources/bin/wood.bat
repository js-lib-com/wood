@set home=%~dp0
@set home=%home:~0,-4%

@set "bin=%home%\bin\*"
@set "lib=%home%\lib\*"
@set "logs=%home%\logs\"

@set "lock=%home%.lock"
@copy /y NUL %lock% >NUL

@java -cp "%bin%;%lib%" -DLOGS_DIR=%logs% js.wood.cli.Main %*

@del %lock%
