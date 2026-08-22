package net.rebel459.music_and_melody.client.util;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.FloatSampleSource;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import org.jflac.FLACDecoder;
import org.jflac.frame.Frame;
import org.jflac.metadata.StreamInfo;
import org.jflac.util.ByteData;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class AudioStreams {

    private AudioStreams() {}

    public static boolean isSupported(Path path) {
        return switch (extension(path)) {
            case "ogg", "mp3", "flac", "wav" -> true;
            default -> false;
        };
    }

    public static AudioStream open(Path path) throws IOException {
        return switch (extension(path)) {
            case "mp3" -> new Mp3(Files.newInputStream(path));
            case "flac" -> new Flac(Files.newInputStream(path));
            case "wav" -> new Wav(Files.newInputStream(path));
            default -> throw new IOException("No external decoder for '" + path + "'.");
        };
    }

    public static AudioStream openLooping(Path path) throws IOException {
        return new Looping(path, open(path));
    }

    public static ByteBuffer readAll(AudioStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while (true) {
            ByteBuffer block = stream.read(16_384);
            if (!block.hasRemaining()) break;
            byte[] bytes = new byte[block.remaining()];
            block.get(bytes);
            output.write(bytes);
        }
        return directBuffer(output.toByteArray());
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    // LWJGL/OpenAL receives these buffers directly, so a heap ByteBuffer is not usable
    private static ByteBuffer directBuffer(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder());
        buffer.put(bytes);
        return buffer.flip();
    }

    private abstract static class Buffered implements AudioStream {
        protected ByteBuffer remaining = ByteBuffer.allocate(0);

        @Override
        public final ByteBuffer read(int requested) throws IOException {
            if (requested <= 0) return ByteBuffer.allocate(0);
            ByteArrayOutputStream output = new ByteArrayOutputStream(requested);
            while (output.size() < requested) {
                if (!remaining.hasRemaining()) {
                    remaining = nextBlock();
                    if (!remaining.hasRemaining()) break;
                }
                int count = Math.min(requested - output.size(), remaining.remaining());
                byte[] bytes = new byte[count];
                remaining.get(bytes);
                output.write(bytes);
            }
            return directBuffer(output.toByteArray());
        }

        protected abstract ByteBuffer nextBlock() throws IOException;
    }

    private static final class Mp3 implements FloatSampleSource {
        private final InputStream input;
        private final Bitstream bits;
        private final Decoder decoder = new Decoder();
        private AudioFormat format;
        private SampleBuffer pending;

        private Mp3(InputStream input) throws IOException {
            this.input = input;
            this.bits = new Bitstream(input);
            this.pending = decodeFrame();
            if (pending == null || format == null) throw new IOException("MP3 contains no audio frames.");
        }

        @Override
        public AudioFormat getFormat() {
            return format;
        }

        @Override
        public boolean readChunk(FloatConsumer consumer) throws IOException {
            SampleBuffer samples = pending;
            if (samples == null) samples = decodeFrame();
            if (samples == null) return false;
            short[] source = samples.getBuffer();
            for (int i = 0; i < samples.getBufferLength(); i++) {
                consumer.accept(source[i] / 32768.0F);
            }
            pending = null;
            return true;
        }

        private SampleBuffer decodeFrame() throws IOException {
            try {
                Header header = bits.readFrame();
                if (header == null) return null;
                SampleBuffer samples = (SampleBuffer) decoder.decodeFrame(header, bits);
                bits.closeFrame();
                format = new AudioFormat(samples.getSampleFrequency(), 16, samples.getChannelCount(), true, false);
                return samples;
            } catch (Exception exception) {
                throw new IOException("Could not decode MP3 audio.", exception);
            }
        }

        @Override
        public void close() throws IOException {
            try {
                bits.close();
            } catch (Exception exception) {
                throw new IOException("Could not close MP3 audio.", exception);
            } finally {
                input.close();
            }
        }
    }

    private static final class Flac implements FloatSampleSource {
        private final InputStream input;
        private final FLACDecoder decoder;
        private final StreamInfo info;
        private final AudioFormat format;

        private Flac(InputStream input) throws IOException {
            this.input = input;
            this.decoder = new FLACDecoder(input);
            this.info = decoder.readStreamInfo();
            decoder.readMetadata(info);
            if (info.getChannels() < 1 || info.getChannels() > 2) throw new IOException("Only mono and stereo FLAC files are supported.");
            this.format = new AudioFormat(info.getSampleRate(), 16, info.getChannels(), true, false);
        }

        @Override
        public AudioFormat getFormat() {
            return format;
        }

        @Override
        public boolean readChunk(FloatConsumer consumer) throws IOException {
            Frame frame = decoder.readNextFrame();
            if (frame == null) return false;
            ByteData data = decoder.decodeFrame(frame, null);
            emitSamples(data, consumer);
            return true;
        }

        private void emitSamples(ByteData data, FloatConsumer consumer) throws IOException {
            byte[] source = data.getData();
            int length = data.getLen();
            int sampleBytes = (info.getBitsPerSample() + 7) / 8;
            if (sampleBytes < 1 || sampleBytes > 3 || length % sampleBytes != 0) {
                throw new IOException("Unsupported FLAC sample format.");
            }
            for (int sourceIndex = 0; sourceIndex < length; sourceIndex += sampleBytes) {
                int value;
                int maximum;
                if (sampleBytes == 1) {
                    value = (source[sourceIndex] & 0xFF) - 128;
                    maximum = 128;
                } else if (sampleBytes == 2) {
                    value = (short) ((source[sourceIndex] & 0xFF) | (source[sourceIndex + 1] & 0xFF) << 8);
                    maximum = 32_768;
                } else {
                    value = (source[sourceIndex] & 0xFF)
                            | (source[sourceIndex + 1] & 0xFF) << 8
                            | (source[sourceIndex + 2] & 0xFF) << 16;
                    if ((value & 0x80_0000) != 0) value |= 0xFF00_0000;
                    maximum = 8_388_608;
                }
                consumer.accept(value / (float) maximum);
            }
        }

        @Override
        public void close() throws IOException {
            input.close();
        }
    }

    private static final class Wav extends Buffered {
        private final AudioInputStream input;
        private final AudioFormat format;

        private Wav(InputStream input) throws IOException {
            try {
                this.input = AudioSystem.getAudioInputStream(input);
            } catch (UnsupportedAudioFileException exception) {
                throw new IOException("Unsupported WAV audio.", exception);
            }
            AudioFormat source = this.input.getFormat();
            if (!AudioFormat.Encoding.PCM_SIGNED.equals(source.getEncoding())
                    || source.getChannels() < 1 || source.getChannels() > 2
                    || source.getSampleSizeInBits() != 16 || source.isBigEndian()) {
                throw new IOException("WAV files must be little-endian 16-bit mono or stereo PCM.");
            }
            this.format = new AudioFormat(source.getSampleRate(), 16, source.getChannels(), true, false);
        }

        @Override
        public AudioFormat getFormat() {
            return format;
        }

        @Override
        protected ByteBuffer nextBlock() throws IOException {
            byte[] bytes = input.readNBytes(16_384);
            return directBuffer(bytes);
        }

        @Override
        public void close() throws IOException {
            input.close();
        }
    }

    private static final class Looping implements AudioStream {
        private final Path path;
        private AudioStream stream;
        private final AudioFormat format;

        private Looping(Path path, AudioStream stream) {
            this.path = path;
            this.stream = stream;
            this.format = stream.getFormat();
        }

        @Override
        public AudioFormat getFormat() {
            return format;
        }

        @Override
        public ByteBuffer read(int requested) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream(requested);
            while (output.size() < requested) {
                ByteBuffer block = stream.read(requested - output.size());
                if (!block.hasRemaining()) {
                    stream.close();
                    stream = open(path);
                    if (!stream.getFormat().matches(format)) throw new IOException("Looping stream format changed.");
                    continue;
                }
                byte[] bytes = new byte[block.remaining()];
                block.get(bytes);
                output.write(bytes);
            }
            return directBuffer(output.toByteArray());
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }
}
