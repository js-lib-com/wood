@set home=%~dp0
@set home=%home:~0,-4%

@set "bin=%home%\bin\*"
@set "lib=%home%\lib\*"

@java -cp "%bin%;%lib%" js.wood.cli.Main %*
