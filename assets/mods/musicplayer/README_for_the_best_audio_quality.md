## Sampling Rate

The basegame is build assuming the sampling rate of 48000 Hz.

Any audio files with lower sampling rate will be resampled on-the-fly by the game's audio engine,
but doing so may introduce artefacts, most notably fast periodic clicks, which may be audible in certain
circumstances. For the best results, please resample your audio files to 48000 Hz beforehand.


## Mono Incompatibility

The audio engine does not support monaural audio. Please convert your mono audio file to stereo beforehand.


## Gapless Playback

The basegame (and by the extension this music player) does support the Gapless Playback.

However, because of the inherent limitation of the MP3 format, the Gapless Playback is not achievable
without extensive hacks. If you do care, please convert your MP3 files into WAV or OGG format.


## SACD-Quality WAV File Incompatibility

The audio engine cannot resample an audio file with sampling rate greater than 48000 Hz, nor is capable
of reading anything that is not in 16-bit bit-depth.


## tl;dr

Stereo, 48 kHz, 16 bit, WAV or OGG.