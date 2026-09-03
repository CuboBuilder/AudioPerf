# AudioPerf

A port of the classic Computronics audio peripherals for **Minecraft 1.21.1 (NeoForge)**, designed to work with the modern **OpenComputers** mod.

Turn your OpenComputers network into a full audio workstation: record and play DFPWM tapes, run them through a network of audio cables, and pump the sound out through speakers.

## Features

- **Tape Drive** — the heart of the system. Insert a tape and control it from OpenComputers (or from its built-in GUI) to play, pause, stop, rewind, fast-forward, seek, read and write data.
- **Audio Cable** — connects tape drives to speakers. Audio packets propagate through connected cables and out to every speaker attached to the network.
- **Speaker** — plays the audio it receives at its own position, so you can place sound anywhere.
- **Tapes** — 10 tape variants with different capacities (2 to 128 minutes). Tapes store raw DFPWM audio data and persist to disk per-world.
- **DFPWM audio** — standard DFPWM1a codec, compatible with tools like [music.madefor.cc](https://github.com/SquidDev-CC/music.madefor.cc). Audio is streamed to clients and played back through OpenAL.
- **Creative tab** — all blocks and every tape variant are available in a dedicated `AudioPerf` creative tab.
- **GUI** — the tape drive has a simple inventory-style GUI for inserting tapes and controlling playback, with rewind / play / stop / fast-forward buttons.

## Requirements

| Dependency  | Version        |
|-------------|----------------|
| Minecraft   | 1.21.1         |
| NeoForge    | 21.1.235+      |
| OpenComputers | 1.9.4+        |

The mod depends on OpenComputers and will not load without it.

## OpenComputers integration

The tape drive registers as a `tape_drive` component. Connect it to your network with an adapter or cable and use it from Lua:

```lua
local component = require("component")
local tape = component.tape_drive

tape.isReady()        -- is there a tape inserted?
tape.getSize()        -- tape capacity in bytes
tape.getPosition()    -- current position in bytes
tape.getLabel()       -- tape label
tape.setLabel("My mix")
tape.play()           -- start playing
tape.stop()           -- stop
tape.seek(1024)       -- seek (negative to rewind)
tape.read()           -- read one byte
tape.write(0xAA)      -- write a byte
tape.setSpeed(1.0)    -- playback speed (0.25 .. 2.0)
tape.setVolume(1.0)   -- volume (0.0 .. 1.0)
tape.getState()       -- "STOPPED", "PLAYING", "REWINDING" or "FORWARDING"
```

A full-featured `tape` program (play, stop, rewind, wipe, label, write from a file or URL) ships with the original Computronics and can be found in the Computronics sources under `assets/computronics/loot/tape/usr/bin/tape.lua`.

## Writing audio to a tape

1. Convert your audio to DFPWM, e.g. with [music.madefor.cc](https://github.com/SquidDev-CC/music.madefor.cc).
2. Place the resulting file somewhere readable by the computer (floppy disk, HTTP server, etc.).
3. Insert a tape into the tape drive and run:

   ```
   tape write /path/to/audio.dfpwm
   ```

4. Then `tape play` and enjoy.

Tape data is saved per-world under the `.minecraft/<world>/audio_perf/` directory.

## Building from source

```bash
./gradlew build
```

The built jar will be in `build/libs/`.

## Notes on development

This mod receives a **low update frequency** — it is maintained in spare time and only gets updated when there is something important to fix or add. That said, **pull requests are greatly appreciated**. If you find a bug, want a feature, or have improvements, feel free to open an issue or a PR.

## Credits

- [Computronics](https://github.com/Vexatos/Computronics)  — the original mod this is ported from.
- [OpenComputers](https://github.com/PC-Logix/OpenComputers) — the mod this integrates with.
- [music.madefor.cc](https://github.com/SquidDev-CC/music.madefor.cc) — a handy DFPWM converter.

## TODO
- [x] "tape" program diskette
- [ ] proper audio cable texture
- [ ] improve tape player GUI sync
- [ ] fix "tape speed" command

## License

WTFPL — Do What The Fuck You Want To Public License, Version 2.
See [LICENSE](LICENSE).
