SET basefilename=%~d1%~p1%~n1
SET inputextension=%~x1
rem inputextension should be dot-psd
rem color space must be Lab16

IF "%inputextension%" NEQ ".psd" goto fail

convert %1 -colorspace sRGB -write mpr:temp -background black -alpha Remove mpr:temp -compose Copy_Opacity -composite "%basefilename%.tga"

exit

:fail
echo "File not PSD"
pause
exit /b 1
