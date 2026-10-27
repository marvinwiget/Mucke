package io.github.marvinwiget.mucke.sound;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.marvinwiget.mucke.interpreter.CompileResult;
import io.github.marvinwiget.mucke.interpreter.Interpreter;
import io.github.marvinwiget.mucke.music.NoteEvent;
import io.github.marvinwiget.mucke.music.PlaybackPlan;
import io.github.marvinwiget.mucke.music.TrackPlan;

// Run with java after compiling test sources; no audio device is needed.
public final class BeatSequencerTest {
    public static void main(String[] args) {
        sameLengthEditKeepsPhase();
        queuedCallsKeepTheirCurrentSegment();
        lengthEditWaitsForOldCycle();
        newTrackJoinsAndRemovedTrackStops();
        newTrackFollowsCurrentCycleAfterLengthEdit();
        differentPatternLengthsShareBarBeat();
        newestLengthEditWins();
        eightStepsIgnoreFourFourMeter();
        tempoChangeKeepsBeatNumber();
    }

    private static void sameLengthEditKeepsPhase() {
        BeatSequencer sequencer = new BeatSequencer();
        TrackPlan old = track("bass", 4, 0, -1,
            new NoteEvent(1, 60), new NoteEvent(3, 65));
        TrackPlan changed = track("bass", 4, 0, -1,
            new NoteEvent(1, 62), new NoteEvent(3, 67));
        sequencer.update(prepare(old), 0);
        for (int beat = 0; beat < 7; beat++) sequencer.atBeat(beat);

        sequencer.update(prepare(changed), 7);
        expectNotes(sequencer.atBeat(7), 67); // Step 3 changes on this beat.
        expectNotes(sequencer.atBeat(8));
        expectNotes(sequencer.atBeat(9), 62); // Edited step 1 returns here.
    }

    private static void lengthEditWaitsForOldCycle() {
        BeatSequencer sequencer = new BeatSequencer();
        TrackPlan old = track("lead", 4, 0, -1, new NoteEvent(3, 60));
        TrackPlan changed = track("lead", 8, 0, -1,
            new NoteEvent(0, 64), new NoteEvent(7, 69));
        sequencer.update(prepare(old), 0);
        for (int beat = 0; beat < 5; beat++) sequencer.atBeat(beat);

        sequencer.update(prepare(changed), 5);
        expectNotes(sequencer.atBeat(5));
        expectNotes(sequencer.atBeat(6));
        expectNotes(sequencer.atBeat(7), 60); // Finish the old four-step cycle.
        expectNotes(sequencer.atBeat(8), 64); // Start the new eight-step cycle.
        for (int beat = 9; beat < 15; beat++) sequencer.atBeat(beat);
        expectNotes(sequencer.atBeat(15), 69);
        expectNotes(sequencer.atBeat(16), 64);
    }

    private static void queuedCallsKeepTheirCurrentSegment() {
        BeatSequencer sequencer = new BeatSequencer();
        TrackPlan intro = track("bass", 4, 0, 1, new NoteEvent(0, 48));
        TrackPlan oldLoop = track("bass", 4, 4, -1, new NoteEvent(3, 50));
        TrackPlan newIntro = track("bass", 8, 0, 1, new NoteEvent(0, 48));
        TrackPlan newLoop = track("bass", 8, 8, -1,
            new NoteEvent(0, 60), new NoteEvent(7, 67));
        sequencer.update(prepare(intro, oldLoop), 0);
        for (int beat = 0; beat < 10; beat++) sequencer.atBeat(beat);

        sequencer.update(prepare(newIntro, newLoop), 10);
        expectNotes(sequencer.atBeat(10));
        expectNotes(sequencer.atBeat(11), 50);
        expectNotes(sequencer.atBeat(12), 60); // Loop changes at its old boundary.
        for (int beat = 13; beat < 19; beat++) sequencer.atBeat(beat);
        expectNotes(sequencer.atBeat(19), 67);
        expectNotes(sequencer.atBeat(20), 60);
    }

    private static void newTrackJoinsAndRemovedTrackStops() {
        BeatSequencer sequencer = new BeatSequencer();
        TrackPlan bass = track("bass", 4, 0, -1, new NoteEvent(3, 48));
        TrackPlan lead = track("lead", 4, 0, -1, new NoteEvent(3, 72));
        sequencer.update(prepare(bass), 0);
        for (int beat = 0; beat < 7; beat++) sequencer.atBeat(beat);

        sequencer.update(prepare(bass, lead), 7);
        expectNotes(sequencer.atBeat(7), 48, 72); // Lead joins shared step 3.
        sequencer.update(prepare(bass), 8);
        expectNotes(sequencer.atBeat(8));
        for (int beat = 9; beat < 11; beat++) sequencer.atBeat(beat);
        expectNotes(sequencer.atBeat(11), 48); // Removed lead stays stopped.
    }

    private static void newTrackFollowsCurrentCycleAfterLengthEdit() {
        BeatSequencer sequencer = new BeatSequencer();
        TrackPlan oldBass = track("bass", 3, 0, -1, new NoteEvent(0, 48));
        TrackPlan bass = track("bass", 4, 0, -1,
            new NoteEvent(0, 60), new NoteEvent(1, 61),
            new NoteEvent(2, 62), new NoteEvent(3, 63));
        TrackPlan lead = track("lead", 4, 0, -1,
            new NoteEvent(0, 70), new NoteEvent(1, 71),
            new NoteEvent(2, 72), new NoteEvent(3, 73));

        sequencer.update(prepare(oldBass), 0);
        sequencer.atBeat(0);
        sequencer.update(prepare(bass), 1);
        sequencer.atBeat(1);
        sequencer.atBeat(2); // Finish the old three-step cycle.
        expectNotes(sequencer.atBeat(3), 60);
        expectNotes(sequencer.atBeat(4), 61);
        expectNotes(sequencer.atBeat(5), 62); // Bass is on step 3 of 4.

        sequencer.update(prepare(bass, lead), 6);
        expectNotes(sequencer.atBeat(6), 63, 73); // Both play step 4.
        expectNotes(sequencer.atBeat(7), 60, 70); // Both return to step 1.
    }

    private static void differentPatternLengthsShareBarBeat() {
        for (int bassLength : new int[] {4, 8}) {
            int leadLength = bassLength == 4 ? 8 : 4;
            BeatSequencer sequencer = new BeatSequencer();
            TrackPlan oldBass = track("bass", 3, 0, -1, new NoteEvent(0, 48));
            TrackPlan bass = track("bass", bassLength, 0, -1,
                everyStep(bassLength, 60));
            TrackPlan lead = track("lead", leadLength, 0, -1,
                everyStep(leadLength, 70));

            sequencer.update(prepare(oldBass), 0);
            sequencer.atBeat(0);
            sequencer.update(prepare(bass), 1);
            for (int beat = 1; beat < 6; beat++) sequencer.atBeat(beat);

            sequencer.update(prepare(bass, lead), 6);
            expectNotes(sequencer.atBeat(6), 63, 73); // Both on beat 4 of the bar.
            expectNotes(sequencer.atBeat(7),
                bassLength == 4 ? 60 : 64, leadLength == 4 ? 70 : 74);
            for (int beat = 8; beat < 14; beat++) sequencer.atBeat(beat);
            expectNotes(sequencer.atBeat(14), 63, 73); // Both keep their phase.
        }
    }

    private static void newestLengthEditWins() {
        BeatSequencer sequencer = new BeatSequencer();
        TrackPlan old = track("bass", 4, 0, -1, new NoteEvent(0, 48));
        TrackPlan firstEdit = track("bass", 8, 0, -1, new NoteEvent(0, 60));
        TrackPlan latestEdit = track("bass", 6, 0, -1,
            new NoteEvent(0, 62), new NoteEvent(5, 67));
        sequencer.update(prepare(old), 0);
        for (int beat = 0; beat < 5; beat++) sequencer.atBeat(beat);

        sequencer.update(prepare(firstEdit), 5);
        sequencer.atBeat(5);
        sequencer.update(prepare(latestEdit), 6);
        sequencer.atBeat(6);
        sequencer.atBeat(7);
        expectNotes(sequencer.atBeat(8), 62);
        for (int beat = 9; beat < 13; beat++) sequencer.atBeat(beat);
        expectNotes(sequencer.atBeat(13), 67);
        expectNotes(sequencer.atBeat(14), 62);
    }

    private static void eightStepsIgnoreFourFourMeter() {
        String source = """
            config {
                bpm: 120
                meter: 4/4
                init lead with (
                    sample: "lead.wav",
                    pattern: [E4, E3, G3, E5, E2, rest, rest, A3]
                )
            }
            main {
                lead.loop()
            }
            """;
        CompileResult result = new Interpreter().compile(source, Path.of("."));
        check(result.isValid(), result.getDiagnostics().toString());
        TrackPlan track = result.getPlan().tracks.get(0);
        check(track.patternBeats == 8, "Pattern must last eight beats in 4/4.");

        BeatSequencer sequencer = new BeatSequencer();
        sequencer.update(prepare(track), 0);
        expectNotes(sequencer.atBeat(0), 64);
        for (int beat = 1; beat < 4; beat++) sequencer.atBeat(beat);
        expectNotes(sequencer.atBeat(4), 40); // Step 5 is beat 1 of bar 2.
        expectNotes(sequencer.atBeat(5));
        expectNotes(sequencer.atBeat(6));
        expectNotes(sequencer.atBeat(7), 57);
        expectNotes(sequencer.atBeat(8), 64);
    }

    private static void tempoChangeKeepsBeatNumber() {
        Transport clock = new Transport(120);
        long frame = clock.frameAtBeat(7);
        clock.changeTempoAt(frame, 7, 60);
        check(clock.frameAtBeat(8) == frame + 48_000,
            "Beat 8 must follow beat 7 at the new tempo.");
    }

    private static TrackPlan track(String name, int length, long start,
                                   long repeats, NoteEvent... notes) {
        return new TrackPlan(name, Path.of("unused.wav"), 60, 1, 1,
            start, repeats, length, List.of(notes));
    }

    private static NoteEvent[] everyStep(int length, int firstMidi) {
        NoteEvent[] notes = new NoteEvent[length];
        for (int step = 0; step < length; step++) {
            notes[step] = new NoteEvent(step, firstMidi + step);
        }
        return notes;
    }

    private static LivePlayer.PreparedPlan prepare(TrackPlan... tracks) {
        List<LivePlayer.PreparedTrack> prepared = new ArrayList<>();
        for (TrackPlan track : tracks) {
            prepared.add(new LivePlayer.PreparedTrack(track, new float[] {0}));
        }
        return new LivePlayer.PreparedPlan(
            new PlaybackPlan(120, 4, 4, List.of(tracks)), prepared);
    }

    private static void expectNotes(List<BeatSequencer.Trigger> actual, int... notes) {
        check(actual.size() == notes.length,
            "Expected " + notes.length + " notes but got " + actual.size());
        for (int i = 0; i < notes.length; i++) {
            check(actual.get(i).note.midiNote == notes[i],
                "Wrong note at position " + i);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
