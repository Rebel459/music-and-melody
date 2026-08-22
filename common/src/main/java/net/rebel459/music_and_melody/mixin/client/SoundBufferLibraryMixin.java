package net.rebel459.music_and_melody.mixin.client;

import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.rebel459.music_and_melody.client.util.DirectSoundFiles;
import net.rebel459.music_and_melody.client.util.ExternalAudioStreams;
import net.rebel459.music_and_melody.client.util.PlaylistHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import javax.sound.sampled.AudioFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {

	@Shadow
	@Final
	private Map<Identifier, CompletableFuture<SoundBuffer>> cache;

	@Inject(method = "getCompleteBuffer", at = @At("HEAD"), cancellable = true)
	private void getCompleteDirectBuffer(
			Identifier location,
			CallbackInfoReturnable<CompletableFuture<SoundBuffer>> cir
	) {
		DirectSoundFiles.get(location).ifPresent(path -> {
			CompletableFuture<SoundBuffer> future = this.cache.computeIfAbsent(location, ignored ->
					CompletableFuture.supplyAsync(() -> {
						try (AudioStream audioStream = openDirectStream(path, false)) {
							ByteBuffer data = ExternalAudioStreams.readAll(audioStream);
							return new SoundBuffer(data, audioStream.getFormat());
						} catch (IOException exception) {
							throw new CompletionException(exception);
						}
					}, Util.nonCriticalIoPool())
			);

			cir.setReturnValue(future);
		});
	}

	@Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
	private void music_and_melody$getDirectStream(
			Identifier location,
			boolean looping,
			CallbackInfoReturnable<CompletableFuture<AudioStream>> cir
	) {
		DirectSoundFiles.get(location).ifPresent(path -> {
			CompletableFuture<AudioStream> future = CompletableFuture.supplyAsync(() -> {
				try {
					AudioStream stream = openDirectStream(path, looping);
					skipToRequestedOffset(stream, PlaylistHelper.consumePendingSeekMillis(location));
					return stream;
				} catch (IOException exception) {
					throw new CompletionException(exception);
				}
			}, Util.nonCriticalIoPool());

			cir.setReturnValue(future);
		});
	}

	@Inject(method = "getStream", at = @At("RETURN"), cancellable = true)
	private void music_and_melody$seekResourceStream(
			Identifier location,
			boolean looping,
			CallbackInfoReturnable<CompletableFuture<AudioStream>> cir
	) {
		long offset = PlaylistHelper.consumePendingSeekMillis(location);
		if (offset <= 0L) return;
		cir.setReturnValue(cir.getReturnValue().thenApply(stream -> {
			try {
				skipToRequestedOffset(stream, offset);
				return stream;
			} catch (IOException exception) {
				try {
					stream.close();
				} catch (IOException ignored) {
				}
				throw new CompletionException(exception);
			}
		}));
	}

	private static void skipToRequestedOffset(AudioStream stream, long offsetMillis) throws IOException {
		if (offsetMillis <= 0L) return;
		AudioFormat format = stream.getFormat();
		int frameSize = Math.max(1, format.getFrameSize());
		float frameRate = format.getFrameRate() > 0.0F ? format.getFrameRate() : format.getSampleRate();
		if (frameRate <= 0.0F) return;
		long remaining = Math.max(0L, Math.round(frameRate * frameSize * offsetMillis / 1000.0D));
		while (remaining > 0L) {
			ByteBuffer data = stream.read((int) Math.min(16_384L, remaining));
			int read = data.remaining();
			if (read <= 0) break;
			remaining -= read;
		}
	}

	private static AudioStream openDirectStream(java.nio.file.Path path, boolean looping) throws IOException {
		if (ExternalAudioStreams.isSupported(path) && !path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".ogg")) {
			return looping ? ExternalAudioStreams.openLooping(path) : ExternalAudioStreams.open(path);
		}
		InputStream inputStream = Files.newInputStream(path);
		return looping
				? new LoopingAudioStream(JOrbisAudioStream::new, inputStream)
				: new JOrbisAudioStream(inputStream);
	}
}
