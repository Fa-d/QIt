package dev.sadakat.qit.wear.application.usecase.storage

import dev.sadakat.qit.shared.domain.repository.DownloadRepository
import dev.sadakat.qit.shared.domain.valueobject.FileSize
import dev.sadakat.qit.wear.application.usecase.BaseUseCase
import javax.inject.Inject

/**
 * Use case for getting storage information
 */
class GetStorageInfoUseCase @Inject constructor(
    private val downloadRepository: DownloadRepository
) : BaseUseCase<StorageInfo>() {

    override suspend fun invoke(): Result<StorageInfo> {
        return try {
            val available = downloadRepository.getAvailableStorage()
            val used = downloadRepository.getTotalDownloadedSize()

            Result.success(
                StorageInfo(
                    availableSpace = available,
                    usedSpace = used
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class StorageInfo(
    val availableSpace: FileSize,
    val usedSpace: FileSize
) {
    val totalSpace: FileSize = availableSpace + usedSpace
    val usagePercentage: Float = if (totalSpace.bytes > 0) {
        (usedSpace.bytes.toFloat() / totalSpace.bytes.toFloat())
    } else {
        0f
    }
}
