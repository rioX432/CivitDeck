package com.riox432.civitdeck.domain.model

enum class BaseModel(val apiValue: String, val displayName: String) {
    SD15("SD 1.5", "SD 1.5"),
    SDXL10("SDXL 1.0", "SDXL"),
    Pony("Pony", "Pony"),
    Flux1D("Flux.1 D", "Flux.1 D"),
    Flux1S("Flux.1 S", "Flux.1 S"),
    SD21("SD 2.1", "SD 2.1"),
    SVD("SVD", "SVD"),
}
