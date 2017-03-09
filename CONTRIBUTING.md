### Contributing code ###

* Writing tests
* Code review
* Guidelines
    - Well-documented. (comments, people, comments!)


### Contributing translations ###

* Writing text
	- You will need to fiddle with .json files in ./res/locales/<Language code>
	- Do not use double-dash (```--```), use proper em-dash. Em-dashes must not prepend and append any spaces.
	- Languages with ligatures: please do use them. (e.g. coeur -> cœur)
	- Japanese: please refrain from using double-hypen (```゠```), use middle dot (```・```)
* Languagus with apparent grammatical gender
	- Any gender discrimination should *not* exist in this game, so please choose vocabularies that is gender-neutral. If such behaviour is not possible in the target language, please use male gender, but try your best to avoid the situation.

Note: Right-to-left languages (arabic, hebrew, etc.) are not supported.


### Contributing artworks ###

* RGB we mean is always sRGB, _pure white_ we say is always D65 (6 500 K daylight). If you have a monitor calibration device, this is our desired target. (calibration software must be _DisplayCal 3_)
* Master file for the picture (e.g. psd) can be either RGB/8 or Lab/16.
* Every exported audio must be in OGG/Vorbis, with quality ```10```.
* When exporting the image, if the image contains semitransparent colours, export it as TGA; if not, export it as PNG.

        Instruction for exporting the image as TGA
        
        1. In Photoshop, File > Export > Quick Export as PNG
        2. Save the PNG somewhere
        3. Open up the PNG with Paint.NET/GIMP
        4. Export the image as TGA
        5. Include the converted PNG to the project

### Game font ###

The font for the game is managed on [this GitHub repository](https://github.com/minjaesong/Terrarum-sans-bitmap). Make your contribution there and file a pull request on this project.