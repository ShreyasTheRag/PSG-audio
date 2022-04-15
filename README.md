# PSG-audio
A Java utility to produce sounds in real time. It is inspired by the Sega Genesis console and its soundchips, the Yamaha YM2612 and the SN76489 PSG.
I designed a custom programming language to program the PSGs, which are instantiated through the PSG class. The commands are written in normal text files (.txt).
==COMMAND SYNTAX==
x y z [a][v]
  Used to generate a normal tone.
  x: the frequency of the tone in Hz
  y: the relative amplitude (loudness), which must be between 0 and 1
  z: the duration of the tone in ms
  a: enable attenuation (the tone gets softer over time)
  v: enable vibrato
  Example: 440 0.5 1000 av = Play a tone at 440 Hz and half loudness for 1 second, with vibrato and attenuation enabled
w[x] y z [a]
  Used to generate white noise.
  x: an integer. The default value is 1 and the pitch of the noise _decreases_ as x increases.
  y: the relative amplitude (loudness), which must be between 0 and 1
  z: the duration of the tone in ms
  a: enable attenuation (the sound gets softer over time)
  Example: w4 0.5 1000 a = Play white noise at level 4 and half loudness for 1 second, with attenuation enabled
If the PSG is set to percussion mode (using the setPercussion method):
x y z
  x: if x is 0, a kick drum is played. If x is 1, a snare drum is played.
  y: the relative amplitude (loudness), which must be between 0 and 1
  z: the duration of the tone in ms
  Example: 0 0.5 125 = Play a kick drum at half loudness for 1/8 of a second
