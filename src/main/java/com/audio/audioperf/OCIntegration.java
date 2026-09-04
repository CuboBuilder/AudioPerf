package com.audio.audioperf;

import li.cil.oc.api.FileSystem;
import li.cil.oc.api.IMC;
import li.cil.oc.api.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.Callable;

public class OCIntegration {
    private static final String TAPE_LUA_CONTENT =
        "--[[ tape program, provides basic tape modification and access tools\n" +
        "Authors: Bizzycola and Vexatos\n" +
        "]]\n" +
        "local component = require(\"component\")\n" +
        "local fs = require(\"filesystem\")\n" +
        "local shell = require(\"shell\")\n" +
        "local term = require(\"term\")\n" +
        "\n" +
        "local args, options = shell.parse(...)\n" +
        "\n" +
        "if not component.isAvailable(\"tape_drive\") then\n" +
        "  io.stderr:write(\"This program requires a tape drive to run.\")\n" +
        "  return\n" +
        "end\n" +
        "\n" +
        "local function printUsage()\n" +
        "  print(\"Usage:\")\n" +
        "  print(\" - 'tape play' to start playing a tape\")\n" +
        "  print(\" - 'tape pause' to pause playing the tape\")\n" +
        "  print(\" - 'tape stop' to stop playing and rewind the tape\")\n" +
        "  print(\" - 'tape rewind' to rewind the tape\")\n" +
        "  print(\" - 'tape wipe' to wipe any data on the tape and erase it completely\")\n" +
        "  print(\" - 'tape label [name]' to label the tape, leave 'name' empty to get current label\")\n" +
        "  print(\" - 'tape speed <speed>' to set the playback speed. Needs to be between 0.25 and 2.0\")\n" +
        "  print(\" - 'tape volume <volume>' to set the volume of the tape. Needs to be between 0.0 and 1.0\")\n" +
        "  print(\" - 'tape write <path/of/audio/file>' to write to the tape from a file\")\n" +
        "  print(\" - 'tape write <URL>' to write from a URL\")\n" +
        "  print(\"Other options:\")\n" +
        "  print(\" '--address=<address>' to use a specific tape drive\")\n" +
        "  print(\" '--b=<bytes>' to specify the size of the chunks the program will write to a tape\")\n" +
        "  print(\" '--t=<timeout>' to specify a custom maximum timeout in seconds when writing from a URL\")\n" +
        "  print(\" '-y' to not ask for confirmation before starting to write\")\n" +
        "  return\n" +
        "end\n" +
        "\n" +
        "local function getTapeDrive()\n" +
        "  --Credits to gamax92 for this\n" +
        "  local tape\n" +
        "  if options.address then\n" +
        "    if type(options.address) ~= \"string\" then\n" +
        "      io.stderr:write(\"'address' may only be a string.\")\n" +
        "      return\n" +
        "    end\n" +
        "    local fulladdr = component.get(options.address)\n" +
        "    if fulladdr == nil then\n" +
        "      io.stderr:write(\"No component at this address.\")\n" +
        "      return\n" +
        "    end\n" +
        "    if component.type(fulladdr) ~= \"tape_drive\" then\n" +
        "      io.stderr:write(\"No tape drive at this address.\")\n" +
        "      return\n" +
        "    end\n" +
        "    tape = component.proxy(fulladdr)\n" +
        "  else\n" +
        "    tape = component.tape_drive\n" +
        "  end\n" +
        "  return tape\n" +
        "  --End of gamax92's part\n" +
        "end\n" +
        "\n" +
        "local tape = getTapeDrive()\n" +
        "\n" +
        "if not tape.isReady() then\n" +
        "  io.stderr:write(\"The tape drive does not contain a tape.\")\n" +
        "  return\n" +
        "end\n" +
        "\n" +
        "local function label(name)\n" +
        "  if not name then\n" +
        "    if tape.getLabel() == \"\" then\n" +
        "      print(\"Tape is currently not labeled.\")\n" +
        "      return\n" +
        "    end\n" +
        "    print(\"Tape is currently labeled: \" .. tape.getLabel())\n" +
        "    return\n" +
        "  end\n" +
        "  tape.setLabel(name)\n" +
        "  print(\"Tape label set to \" .. name)\n" +
        "end\n" +
        "\n" +
        "local function rewind()\n" +
        "  print(\"Rewound tape\")\n" +
        "  tape.seek(-tape.getSize())\n" +
        "end\n" +
        "\n" +
        "local function play()\n" +
        "  if tape.getState() == \"PLAYING\" then\n" +
        "    print(\"Tape is already playing\")\n" +
        "  else\n" +
        "    tape.play()\n" +
        "    print(\"Tape started\")\n" +
        "  end\n" +
        "end\n" +
        "\n" +
        "local function stop()\n" +
        "  if tape.getState() == \"STOPPED\" then\n" +
        "    print(\"Tape is already stopped\")\n" +
        "  else\n" +
        "    tape.stop()\n" +
        "    tape.seek(-tape.getSize())\n" +
        "    print(\"Tape stopped\")\n" +
        "  end\n" +
        "end\n" +
        "\n" +
        "local function pause()\n" +
        "  if tape.getState() == \"STOPPED\" then\n" +
        "    print(\"Tape is already paused\")\n" +
        "  else\n" +
        "    tape.stop()\n" +
        "    print(\"Tape paused\")\n" +
        "  end\n" +
        "end\n" +
        "\n" +
        "local function speed(sp)\n" +
        "  local s = tonumber(sp)\n" +
        "  if not s or s < 0.25 or s > 2 then\n" +
        "    io.stderr:write(\"Speed needs to be a number between 0.25 and 2.0\")\n" +
        "    return\n" +
        "  end\n" +
        "  tape.setSpeed(s)\n" +
        "  print(\"Playback speed set to \" .. sp)\n" +
        "end\n" +
        "\n" +
        "local function volume(vol)\n" +
        "  local v = tonumber(vol)\n" +
        "  if not v or v < 0 or v > 1 then\n" +
        "    io.stderr:write(\"Volume needs to be a number between 0.0 and 1.0\")\n" +
        "    return\n" +
        "  end\n" +
        "  tape.setVolume(v)\n" +
        "  print(\"Volume set to \" .. vol)\n" +
        "end\n" +
        "\n" +
        "local function confirm(msg)\n" +
        "  if not options.y then\n" +
        "    print(msg)\n" +
        "    print(\"Type `y` to confirm, `n` to cancel.\")\n" +
        "    repeat\n" +
        "      local response = io.read()\n" +
        "      if response and response:lower():sub(1, 1) == \"n\" then\n" +
        "        print(\"Canceled.\")\n" +
        "        return false\n" +
        "      end\n" +
        "    until response and response:lower():sub(1, 1) == \"y\"\n" +
        "  end\n" +
        "  return true\n" +
        "end\n" +
        "\n" +
        "local function wipe()\n" +
        "  if not confirm(\"Are you sure you want to wipe this tape?\") then return end\n" +
        "  local k = tape.getSize()\n" +
        "  tape.stop()\n" +
        "  tape.seek(-k)\n" +
        "  tape.stop() --Just making sure\n" +
        "  tape.seek(-90000)\n" +
        "  local s = string.rep(\"\\xAA\", 8192)\n" +
        "  for i = 1, k + 8191, 8192 do\n" +
        "    tape.write(s)\n" +
        "  end\n" +
        "  tape.seek(-k)\n" +
        "  tape.seek(-90000)\n" +
        "  print(\"Done.\")\n" +
        "end\n" +
        "\n" +
        "local function writeTape(path)\n" +
        "  local file, msg, _, y\n" +
        "  local block = 2048 --How much to read at a time\n" +
        "  if options.b then\n" +
        "    local nBlock = tonumber(options.b)\n" +
        "    if nBlock then\n" +
        "      print(\"Setting chunk size to \" .. options.b)\n" +
        "      block = nBlock\n" +
        "    else\n" +
        "      io.stderr:write(\"option --b is not a number.\\n\")\n" +
        "      return\n" +
        "    end\n" +
        "  end\n" +
        "  if not confirm(\"Are you sure you want to write to this tape?\") then return end\n" +
        "  tape.stop()\n" +
        "  tape.seek(-tape.getSize())\n" +
        "  tape.stop() --Just making sure\n" +
        "\n" +
        "  local bytery = 0 --For the progress indicator\n" +
        "  local filesize = tape.getSize()\n" +
        "\n" +
        "  if string.match(path, \"https?://.+\") then\n" +
        "\n" +
        "    if not component.isAvailable(\"internet\") then\n" +
        "      io.stderr:write(\"This command requires an internet card to run.\")\n" +
        "      return false\n" +
        "    end\n" +
        "\n" +
        "    local internet = component.internet\n" +
        "\n" +
        "    local function setupConnection(url)\n" +
        "\n" +
        "      local file, reason = internet.request(url)\n" +
        "\n" +
        "      if not file then\n" +
        "        io.stderr:write(\"error requesting data from URL: \" .. reason .. \"\\n\")\n" +
        "        return false\n" +
        "      end\n" +
        "\n" +
        "      local connected, reason = false, \"\"\n" +
        "      local timeout = 50\n" +
        "      if options.t then\n" +
        "        local nTimeout = tonumber(options.t)\n" +
        "        if nTimeout then\n" +
        "          print(\"Max timeout: \" .. options.t)\n" +
        "          timeout = nTimeout * 10\n" +
        "        else\n" +
        "          io.stderr:write(\"option --t is not a number. Defaulting to 5 seconds.\\n\")\n" +
        "        end\n" +
        "      end\n" +
        "      for i = 1, timeout do\n" +
        "        connected, reason = file.finishConnect()\n" +
        "        os.sleep(.1)\n" +
        "        if connected or connected == nil then\n" +
        "          break\n" +
        "        end\n" +
        "      end\n" +
        "      \n" +
        "      if connected == nil then\n" +
        "        io.stderr:write(\"Could not connect to server: \" .. reason)\n" +
        "        return false\n" +
        "      end\n" +
        "\n" +
        "      local status, message, header = file.response()\n" +
        "\n" +
        "      if status then\n" +
        "        status = string.format(\"%d\", status)\n" +
        "        print(\"Status: \" .. status .. \" \" .. message)\n" +
        "        if status:sub(1,1) == \"2\" then\n" +
        "          return true, {\n" +
        "            close = function(self, ...) return file.close(...) end,\n" +
        "            read = function(self, ...) return file.read(...) end,\n" +
        "          }, header\n" +
        "        end\n" +
        "        return false\n" +
        "      end\n" +
        "      io.stderr:write(\"no valid HTTP response - no response\")\n" +
        "      return false\n" +
        "    end\n" +
        "\n" +
        "    local success, header\n" +
        "    success, file, header = setupConnection(path)\n" +
        "    if not success then\n" +
        "      if file then\n" +
        "        file:close()\n" +
        "      end\n" +
        "      return\n" +
        "    end\n" +
        "\n" +
        "    print(\"Writing...\")\n" +
        "\n" +
        "    _, y = term.getCursor()\n" +
        "\n" +
        "    if header and header[\"Content-Length\"] and header[\"Content-Length\"][1] then\n" +
        "      filesize = tonumber(header[\"Content-Length\"][1])\n" +
        "    end\n" +
        "  else\n" +
        "    local path = shell.resolve(path)\n" +
        "    filesize = fs.size(path)\n" +
        "    print(\"Path: \" .. path)\n" +
        "    file, msg = io.open(path, \"rb\")\n" +
        "    if not file then\n" +
        "      io.stderr:write(\"Error: \" .. msg)\n" +
        "      return\n" +
        "    end\n" +
        "\n" +
        "    print(\"Writing...\")\n" +
        "\n" +
        "    _, y = term.getCursor()\n" +
        "  end\n" +
        "\n" +
        "  if filesize > tape.getSize() then\n" +
        "    term.setCursor(1, y)\n" +
        "    io.stderr:write(\"Warning: File is too large for tape, shortening file\\n\")\n" +
        "    _, y = term.getCursor()\n" +
        "    filesize = tape.getSize()\n" +
        "  end\n" +
        "\n" +
        "  --Displays long numbers with commas\n" +
        "  local function fancyNumber(n)\n" +
        "    return tostring(math.floor(n)):reverse():gsub(\"(%d%d%d)\", \"%1,\"):gsub(\"%D$\", \"\"):reverse()\n" +
        "  end\n" +
        "\n" +
        "  repeat\n" +
        "    local bytes = file:read(block)\n" +
        "    if bytes and #bytes > 0 then\n" +
        "      if not tape.isReady() then\n" +
        "        io.stderr:write(\"\\nError: Tape was removed during writing.\\n\")\n" +
        "        file:close()\n" +
        "        return\n" +
        "      end\n" +
        "      term.setCursor(1, y)\n" +
        "      bytery = bytery + #bytes\n" +
        "      local displaySize = math.min(bytery, filesize)\n" +
        "      term.write(string.format(\"Read %s of %s bytes... (%.2f %%)\", fancyNumber(displaySize), fancyNumber(filesize), 100 * displaySize / filesize))\n" +
        "      tape.write(bytes)\n" +
        "    end\n" +
        "  until not bytes or bytery > filesize\n" +
        "  file:close()\n" +
        "  tape.stop()\n" +
        "  tape.seek(-tape.getSize())\n" +
        "  tape.stop() --Just making sure\n" +
        "  print(\"\\nDone.\")\n" +
        "end\n" +
        "\n" +
        "if args[1] == \"play\" then\n" +
        "  play()\n" +
        "elseif args[1] == \"stop\" then\n" +
        "  stop()\n" +
        "elseif args[1] == \"pause\" then\n" +
        "  pause()\n" +
        "elseif args[1] == \"rewind\" then\n" +
        "  rewind()\n" +
        "elseif args[1] == \"label\" then\n" +
        "  label(args[2])\n" +
        "elseif args[1] == \"speed\" then\n" +
        "  speed(args[2])\n" +
        "elseif args[1] == \"volume\" then\n" +
        "  volume(args[2])\n" +
        "elseif args[1] == \"write\" then\n" +
        "  writeTape(args[2])\n" +
        "elseif args[1] == \"wipe\" then\n" +
        "  wipe()\n" +
        "else\n" +
        "  printUsage()\n" +
        "end\n";

    public static void registerTapeFloppy() {
        if (!ModList.get().isLoaded("opencomputers")) return;
        try {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("audio_perf", "loot/tape");

            Callable<li.cil.oc.api.fs.FileSystem> factory = () -> {
                // Try to load from OC resource system first
                li.cil.oc.api.fs.FileSystem fs = FileSystem.fromResource(loc);
                if (fs != null) {
                    AudioPerf.LOGGER.info("Loaded tape filesystem via fromResource");
                    return FileSystem.asReadOnly(fs);
                }
                // Try via fromClass reflection (OC's internal method)
                try {
                    java.lang.reflect.Method fromClass = li.cil.oc.api.FileSystem.class.getMethod("fromClass", Class.class, String.class, String.class);
                    fs = (li.cil.oc.api.fs.FileSystem) fromClass.invoke(null, AudioPerf.class, "audio_perf", "loot/tape");
                    if (fs != null) {
                        AudioPerf.LOGGER.info("Loaded tape filesystem via fromClass fallback");
                        return FileSystem.asReadOnly(fs);
                    }
                } catch (Exception e) {
                    AudioPerf.LOGGER.warn("fromClass fallback failed", e);
                }
                // Ultimate fallback: memory filesystem with embedded content
                AudioPerf.LOGGER.info("Creating memory filesystem with embedded tape.lua");
                byte[] content = TAPE_LUA_CONTENT.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                long capacity = content.length + 1024;
                li.cil.oc.api.fs.FileSystem memFs = FileSystem.fromMemory(capacity);
                memFs.makeDirectory("/usr");
                memFs.makeDirectory("/usr/bin");
                int handle = memFs.open("/usr/bin/tape.lua", li.cil.oc.api.fs.Mode.Write);
                li.cil.oc.api.fs.Handle h = memFs.getHandle(handle);
                h.write(content);
                h.close();
                // Verify
                String[] rootFiles = memFs.list("/");
                AudioPerf.LOGGER.info("Memory FS root contents: {}", String.join(", ", rootFiles));
                int readHandle = memFs.open("/usr/bin/tape.lua", li.cil.oc.api.fs.Mode.Read);
                li.cil.oc.api.fs.Handle rh = memFs.getHandle(readHandle);
                byte[] readContent = new byte[content.length];
                int bytesRead = rh.read(readContent);
                rh.close();
                if (bytesRead == content.length) {
                    AudioPerf.LOGGER.info("Memory FS verification successful");
                } else {
                    AudioPerf.LOGGER.warn("Memory FS verification failed: read {} bytes, expected {}", bytesRead, content.length);
                }
                AudioPerf.LOGGER.info("Created memory filesystem with tape.lua");
                return FileSystem.asReadOnly(memFs);
            };
            Items.registerFloppy("tape", loc, DyeColor.WHITE, factory, true);
            IMC.registerProgramDiskLabel("tape", "tape", "Lua 5.2", "Lua 5.3", "LuaJ");
            AudioPerf.LOGGER.info("Registered tape program diskette");
        } catch (Exception e) {
            AudioPerf.LOGGER.warn("Failed to register tape floppy", e);
        }
    }
}