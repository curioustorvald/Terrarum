
for %%I in (*.png) do (
	"C:\Program Files\ImageMagick-7.0.4-Q16\magick" convert "+compress" "-matte" "%%~I" "%%~nI.bmp"
	"C:\Program Files\ImageMagick-7.0.4-Q16\magick" convert "+compress" "-matte" "%%~nI.bmp" "%%~nI.tga"
)