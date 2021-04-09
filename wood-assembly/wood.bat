@set "home=D:\java\wood-1.0"
@set "bin=%home%\bin\*"
@set "lib=%home%\lib\*"

@java -cp "%bin%;%lib%" -DWOOD_HOME=%home% js.wood.cli.Main %1 %2 %3 %4 %5 %6 %7 %8 %9
