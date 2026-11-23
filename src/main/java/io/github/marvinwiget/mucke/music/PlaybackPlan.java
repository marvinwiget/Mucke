package io.github.marvinwiget.mucke.music;

import java.util.List;
import java.util.Map;

public final class PlaybackPlan {
    public final double bpm;
    public final long meterTop;
    public final long meterBottom;
    public final List<TrackPlan> tracks;
    public final Map<Integer, String> slots;

    public PlaybackPlan(double bpm, long meterTop, long meterBottom,
                        List<TrackPlan> tracks) {
        this(bpm, meterTop, meterBottom, tracks, Map.of());
    }

    public PlaybackPlan(double bpm, long meterTop, long meterBottom,
                        List<TrackPlan> tracks, Map<Integer, String> slots) {
        this.bpm = bpm;
        this.meterTop = meterTop;
        this.meterBottom = meterBottom;
        this.tracks = List.copyOf(tracks);
        this.slots = Map.copyOf(slots);
    }
}
