package net.rebel459.music_and_melody.mixin.client;

import com.mojang.blaze3d.audio.SoundBuffer;
import net.minecraft.Util;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.rebel459.music_and_melody.client.util.DirectSoundFiles;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {

	@Shadow
	@Final
	private Map<ResourceLocation, CompletableFuture<SoundBuffer>> cache;

	@Inject(method = "getCompleteBuffer", at = @At("HEAD"), cancellable = true)
	private void getCompleteDirectBuffer(
			ResourceLocation location,
			CallbackInfoReturnable<CompletableFuture<SoundBuffer>> cir
	) {
		DirectSoundFiles.get(location).ifPresent(path -> {
			CompletableFuture<SoundBuffer> future = this.cache.computeIfAbsent(location, ignored ->
					CompletableFuture.supplyAsync(() -> {
						try (
								InputStream inputStream = Files.newInputStream(path);
								FiniteAudioStream audioStream = new JOrbisAudioStream(inputStream)
						) {
							ByteBuffer data = audioStream.readAll();
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
			ResourceLocation location,
			boolean looping,
			CallbackInfoReturnable<CompletableFuture<AudioStream>> cir
	) {
		DirectSoundFiles.get(location).ifPresent(path -> {
			CompletableFuture<AudioStream> future = CompletableFuture.supplyAsync(() -> {
				try {
					InputStream inputStream = Files.newInputStream(path);
					return looping
							? new LoopingAudioStream(JOrbisAudioStream::new, inputStream)
							: new JOrbisAudioStream(inputStream);
				} catch (IOException exception) {
					throw new CompletionException(exception);
				}
			}, Util.nonCriticalIoPool());

			cir.setReturnValue(future);
		});
	}
}