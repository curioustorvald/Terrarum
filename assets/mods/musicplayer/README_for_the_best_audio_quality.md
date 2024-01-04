## Sampling Rate

The audio engine of the basegame has the fixed sampling rate of 48000 Hz.

Any audio files with lower sampling rate will be resampled on-the-fly by the game's audio engine,
but doing so may introduce artefacts, most notably fast periodic clicks, which is only audible in certain
artificial cases such as high-pitched monotonic sine wave. If you are concerned about the artefacts,
please resample your audio files to 48000 Hz beforehand. This is easily achievable using free software
such as [Audacity](https://www.audacityteam.org/) or [FFmpeg](https://ffmpeg.org/download.html).


## Mono Incompatibility

The audio engine does not read monaural audio. Please convert your mono audio files to stereo beforehand.


## Gapless Playback

The basegame (and by extension this music player) does support the Gapless Playback.

However, because of the inherent limitation of the MP3 format, the Gapless Playback is not achievable
without extensive hacks. If you do care, please convert your MP3 files into WAV or OGG format.


## SACD-Quality WAV File Incompatibility

The audio engine cannot resample an audio file with sampling rate greater than 48000 Hz, nor is capable
of reading anything that is not in 16-bit bit-depth.


## tl;dr

Stereo, 48 kHz, 16 bit, WAV or OGG.