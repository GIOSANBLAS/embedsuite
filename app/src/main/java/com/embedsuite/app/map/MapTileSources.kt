package com.embedsuite.app.map

import org.osmdroid.tileprovider.tilesource.XYTileSource

val DarkMapTileSource = XYTileSource(
    "CartoDB Dark",
    0, 20, 256, ".png",
    arrayOf("https://basemaps.cartocdn.com/dark_all/")
)
