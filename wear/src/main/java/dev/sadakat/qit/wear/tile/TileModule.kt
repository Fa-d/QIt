package dev.sadakat.qit.wear.tile

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface TileModule {
    @Binds
    fun bindTileUpdates(updates: SystemTileUpdates): TileUpdates
}
