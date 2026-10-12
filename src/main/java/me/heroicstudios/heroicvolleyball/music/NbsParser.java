package me.heroicstudios.heroicvolleyball.music;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NbsParser {

    private static final Charset CHARSET = Charset.forName("windows-1252");

    private NbsParser() {
    }

    public static NbsSong parse(File file) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(file.toPath())).order(ByteOrder.LITTLE_ENDIAN);

        int firstShort = u16(buffer.getShort());
        int version = 0;
        if (firstShort == 0) {
            version = u8(buffer.get());
        }

        if (version > 0) {
            u8(buffer.get());
        }
        if (version >= 3) {
            u16(buffer.getShort());
        }
        int songLayers = u16(buffer.getShort());
        String songName = readString(buffer);
        readString(buffer);
        readString(buffer);
        readString(buffer);

        double tempo = u16(buffer.getShort()) / 100.0;
        u8(buffer.get());
        u8(buffer.get());
        u8(buffer.get());
        buffer.getInt();
        buffer.getInt();
        buffer.getInt();
        buffer.getInt();
        buffer.getInt();
        readString(buffer);

        int loopStart = 0;
        if (version >= 4) {
            u8(buffer.get());
            u8(buffer.get());
            loopStart = u16(buffer.getShort());
        }

        Map<Integer, List<NbsNote>> notesByTick = new HashMap<>();
        int maxTick = -1;
        int tick = -1;
        while (true) {
            int outerJump = u16(buffer.getShort());
            if (outerJump == 0) {
                break;
            }
            tick += outerJump;

            int layer = -1;
            while (true) {
                int innerJump = u16(buffer.getShort());
                if (innerJump == 0) {
                    break;
                }
                layer += innerJump;

                int instrument = u8(buffer.get());
                int key = u8(buffer.get());
                int velocity = version >= 4 ? u8(buffer.get()) : 100;
                int panning = version >= 4 ? u8(buffer.get()) - 100 : 0;
                int pitch = version >= 4 ? buffer.getShort() : 0;

                NbsNote note = new NbsNote(tick, layer, instrument, key, velocity, panning, pitch);
                notesByTick.computeIfAbsent(tick, k -> new ArrayList<>()).add(note);
                if (tick > maxTick) {
                    maxTick = tick;
                }
            }
        }

        double[] layerVolumes = new double[Math.max(songLayers, 1)];
        Arrays.fill(layerVolumes, 1.0);
        for (int i = 0; i < songLayers; i++) {
            readString(buffer);
            if (version >= 4) {
                u8(buffer.get());
            }
            int volume = u8(buffer.get());
            if (version >= 2) {
                u8(buffer.get());
            }
            layerVolumes[i] = volume / 100.0;
        }

        int length = maxTick + 1;
        if (length <= 0) {
            length = 1;
        }
        return new NbsSong(songName, tempo <= 0 ? 10.0 : tempo, loopStart, length, notesByTick, layerVolumes);
    }

    private static String readString(ByteBuffer buffer) {
        int length = buffer.getInt();
        if (length <= 0) {
            return "";
        }
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, CHARSET);
    }

    private static int u8(byte value) {
        return value & 0xFF;
    }

    private static int u16(short value) {
        return value & 0xFFFF;
    }
}
