package io.github.marvinwiget.mucke.sound;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

// One MIDI channel per synth track, so each chord can end on the next beat.
final class SynthPlayer implements AutoCloseable {
    private List<MidiChannel> channels = List.of();
    private List<List<Integer>> sounding = List.of();
    private volatile Synthesizer synth;
    private volatile boolean closed;

    synchronized void prepare(int trackCount) throws MidiUnavailableException {
        if (trackCount == 0) return;
        if (closed) throw new MidiUnavailableException("Synthesizer is closed.");

        if (synth == null) {
            Synthesizer opened = MidiSystem.getSynthesizer();
            try {
                opened.open();
                if (opened.getAvailableInstruments().length == 0) {
                    throw new MidiUnavailableException("Synthesizer has no instruments.");
                }

                List<MidiChannel> available = new ArrayList<>();
                MidiChannel[] devices = opened.getChannels();
                for (int index = 0; index < devices.length; index++) {
                    if (index != 9 && devices[index] != null) {
                        available.add(devices[index]);
                    }
                }
                if (trackCount > available.size()) {
                    throw new MidiUnavailableException("Too many synth tracks.");
                }
                if (closed) throw new MidiUnavailableException("Synthesizer is closed.");

                List<List<Integer>> active = new ArrayList<>();
                for (int index = 0; index < available.size(); index++) {
                    active.add(new ArrayList<>());
                }
                channels = List.copyOf(available);
                sounding = active;
                synth = opened;
            } catch (MidiUnavailableException | RuntimeException error) {
                opened.close();
                throw error;
            }
        } else if (trackCount > channels.size()) {
            throw new MidiUnavailableException("Too many synth tracks.");
        }
    }

    void endBeat() {
        if (synth == null) return;
        for (int index = 0; index < channels.size(); index++) {
            MidiChannel channel = channels.get(index);
            for (int note : sounding.get(index)) channel.noteOff(note);
            sounding.get(index).clear();
        }
    }

    void play(int channelIndex, int program, int transpose, List<Integer> notes) {
        if (notes.isEmpty()) return;
        MidiChannel channel = channels.get(channelIndex);
        channel.programChange(program);
        for (int note : notes) {
            int actual = note + transpose;
            channel.noteOn(actual, 96);
            sounding.get(channelIndex).add(actual);
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (synth == null) return;
        endBeat();
        channels.forEach(MidiChannel::allSoundOff);
        synth.close();
        synth = null;
    }
}
