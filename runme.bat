@echo off
cls

cd /D "%~dp0"

set inputfile=
set outputfile=alior_%date:~-4,4%%date:~-7,2%%date:~-10,2%.STA
set archivefile=archive_%date:~-4,4%%date:~-7,2%%date:~-10,2%.STA

FOR %%F IN (input\*.mt940) DO (
	set inputfile=%%~nxF
	goto begin
)

if "%inputfile%" == "" goto error

:begin

java parserBgz.bgzMT940 input\%inputfile% input\%outputfile%

copy /y input\%inputfile% archiwum\%archivefile%

del /q input\%inputfile%

goto end

:error

echo Brak lub niepoprawny plik mt940

:end

pause