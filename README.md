# MIDI Synthesizer Pro

A MIDI synthesizer written in pure Java that plays and modifies MIDI files, and generates new music by applying mathematical transformations to melodies. It features a 16-channel interface, a dual-manual virtual keyboard, a mixer, a real-time note visualizer, and an interactive learning mode.

Built entirely with the standard Java libraries (Java Sound API and Swing), with no external dependencies.

## Features

* 16 independent MIDI channels, each with its own General MIDI instrument, volume, and effects
* Dual-manual virtual keyboard (6 octaves), playable from the computer keyboard, with two independent manuals mappable to different channels
* Mixer with per-channel volume, panning, and effects
* Real-time note visualizer, synchronized with playback through a transparent MIDI interceptor
* Recording of new compositions, with playback and export to `.mid`
* Mathematical transformations of melodies:

  * Transposition: `Ts(p) = p + s`
  * Inversion (mirror of pitches around a pivot): `Ic(p) = 2c - p`
  * Retrograde (reversal in time): `Rm(t) = m - t`
  * Pi-transposition: each note is shifted by a successive digit of pi
* Interactive learning mode, where notes fall toward the keyboard and the user presses them in time, with scoring and adjustable speed
* External MIDI keyboard support over USB (LPK25 only)

## Mathematical background

Each note is modeled as a pair `(p, t)` of pitch and time. Inversion and retrograde are involutions (applying them twice returns the original). The retrograde required careful handling of the Note On / Note Off pairing, since a naive reflection of time would swap the start and end of each note.

## Requirements

Java Development Kit (JDK) 17 or newer.

## How to run

Double-click `start.bat`, or from the command line:

```bat
javac -encoding UTF-8 -d build MidiSynthesizer.java
java -cp build MidiSynthesizer
```

A sample file, `Anonym\_Spanish\_Ballad.mid`, is included so you can try loading, playing, and transforming a melody right away.

## 

