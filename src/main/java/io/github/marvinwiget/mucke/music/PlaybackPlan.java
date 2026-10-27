package io.github.marvinwiget.mucke.music;

import java.util.List;

public final class PlaybackPlan {
    public final double bpm;
    public final long meterTop;
    public final long meterBottom;
    public final List<TrackPlan> tracks;

    public PlaybackPlan(double bpm, long meterTop, long meterBottom,
                        List<TrackPlan> tracks) {
        this.bpm = bpm;
        this.meterTop = meterTop;
        this.meterBottom = meterBottom;
        this.tracks = List.copyOf(tracks);
    }
}