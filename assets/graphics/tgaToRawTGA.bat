
for %%I in (*.tga) do (
	"C:\Program Files\ImageMagick-7.0.4-Q16\magick" convert "-matte" "%%~I" "%%~nI.bmp"
	"C:\Program Files\ImageMagick-7.0.4-Q16\magick" convert "+compress" "%%~nI.bmp" "%%~nI.tga"
)