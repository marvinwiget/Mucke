package io.github.marvinwiget.mucke.sound;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class SampleLibrary {
    private final Map<Path, float[]> cache = new HashMap<>();

    public float[] load(Path path) throws IOException, UnsupportedAudioFileException {
        Path file = path.toAbsolutePath().normalize();
        float[] cached = cache.get(file);
        if (cached != null) return cached;

        AudioFormat target = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, Transport.SAMPLE_RATE,
            16, 1, 2, Transport.SAMPLE_RATE, false);

        try (AudioInputStream input = AudioSystem.getAudioInputStream(file.toFile())) {
            if (!AudioSystem.isConversionSupported(target, input.getFormat())) {
                throw new UnsupportedAudioFileException(
                    "Cannot convert sample to 48 kHz mono PCM: " + file);
            }

            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(target, input)) {
                byte[] bytes = pcm.readAllBytes();
                float[] samples = new float[bytes.length / 2];
                for (int i = 0; i < samples.length; i++) {
                    int low = bytes[2 * i] & 0xff;
                    int high = bytes[2 * i + 1];
                    samples[i] = (short) ((high << 8) | low) / 32768f;
                }
                cache.put(file, samples);
                return samples;
            }
        }
    }
}