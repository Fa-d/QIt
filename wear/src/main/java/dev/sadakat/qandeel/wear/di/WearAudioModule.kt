package dev.sadakat.qandeel.wear.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sadakat.qandeel.wear.audio.AudioManagerStreamVolume
import dev.sadakat.qandeel.wear.audio.StreamVolume

/** Binds the watch's audio adapters to their ports. */
@Module
@InstallIn(SingletonComponent::class)
abstract class WearAudioModule {

    @Binds
    abstract fun bindStreamVolume(impl: AudioManagerStreamVolume): StreamVolume
}
