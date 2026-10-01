package com.glide.shared.api

import kotlinx.serialization.json.Json

/**
 * The one JSON configuration used on both ends of the wire (backend and Android).
 *
 * - `ignoreUnknownKeys`: an older app version must keep working when the backend adds a field.
 * - `encodeDefaults`: the backend always sends every field, so clients never guess defaults.
 * - `explicitNulls = false`: null fields are omitted rather than sent as `null`.
 */
val ApiJson: Json =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
