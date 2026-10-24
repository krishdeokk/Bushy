package com.bushy.health

enum class LocalAiModel(
    val id: String,
    val displayName: String,
    val description: String,
    val sizeMb: Int,
    val ramRequirement: String,
    val isLocal: Boolean,
    val downloadUrl: String = ""
) {
    SMOLLM_360M(
        id = "smollm2_360m",
        displayName = "SmolLM2 360M",
        description = "Ultra-Fast On-Device Model • ~360 MB Download",
        sizeMb = 363,
        ramRequirement = "Fits easily on 4 GB RAM devices",
        isLocal = true,
        downloadUrl = "https://huggingface.co/onnx-community/SmolLM2-360M-Instruct-ONNX/resolve/main/onnx/model_quantized.onnx"
    ),
    QWEN_05B(
        id = "qwen25_05b",
        displayName = "Qwen 2.5 0.5B",
        description = "Recommended Balanced Model • ~512 MB Download",
        sizeMb = 512,
        ramRequirement = "Runs great on 4 GB+ RAM devices",
        isLocal = true,
        downloadUrl = "https://huggingface.co/onnx-community/Qwen2.5-0.5B-Instruct-ONNX/resolve/main/onnx/model_quantized.onnx"
    ),
    QWEN_VL_3B(
        id = "qwen2_vl_mobile_346m",
        displayName = "Qwen2-VL Mobile Vision",
        description = "Ultra-Fast On-Device Vision Model • ~346 MB Download",
        sizeMb = 346,
        ramRequirement = "Fits easily on 4 GB RAM devices",
        isLocal = true,
        downloadUrl = "https://huggingface.co/Xenova/vit-base-patch16-224/resolve/main/onnx/model.onnx"
    );

    companion object {
        fun fromId(id: String): LocalAiModel {
            return entries.find { it.id == id } ?: SMOLLM_360M
        }
    }
}

sealed class ModelDownloadState {
    object NotDownloaded : ModelDownloadState()
    data class Downloading(val progressPercent: Int) : ModelDownloadState()
    object Ready : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}
