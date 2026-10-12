package me.heroicstudios.heroicvolleyball.music;

import java.util.List;
import java.util.Map;

public final class NbsSong {

    private final String name;
    private final double tempo;
    private final int loopStart;
    private final int length;
    private final Map<Integer, List<NbsNote>> notesByTick;
    private final double[] layerVolumes;

    public NbsSong(String name, double tempo, int loopStart, int length,
                   Map<Integer, List<NbsNote>> notesByTick, double[] layerVolumes) {
        this.name = name;
        this.tempo = tempo;
        this.loopStart = loopStart;
        this.length = length;
        this.notesByTick = notesByTick;
        this.layerVolumes = layerVolumes;
    }

    public String name() {
        return name;
    }

    public double tempo() {
        return tempo;
    }

    public int loopStart() {
        return loopStart;
    }

    public int length() {
        return length;
    }

    public List<NbsNote> notesAt(int tick) {
        return notesByTick.get(tick);
    }

    public double layerVolume(int layer) {
        if (layer < 0 || layer >= layerVolumes.length) {
            return 1.0;
        }
        return layerVolumes[layer];
    }
}
