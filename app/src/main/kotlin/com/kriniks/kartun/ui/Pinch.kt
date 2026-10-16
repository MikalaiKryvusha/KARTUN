package com.kriniks.kartun.ui

import androidx.compose.ui.geometry.Offset

/**
 * One step of a two-finger gesture on a picture drawn with `graphicsLayer { scaleX = zoom; scaleY = zoom; translationX =
 * pan.x; translationY = pan.y }` (scaled about [centre], the layer's default origin): the point of the picture under the
 * fingers' [centroid] stays under them while they spread, and moves with them by [panChange]. Before it the zoom went
 * about the picture's centre, and the photo slid away from the fingers (the owner, chat 2026-10-16 ≈ 15:31: «словно центр
 * тяжести оказывается не между этими двумя пальцами, а в центре фото»; the same fix in KrinikCam f3ea840 - «вокруг
 * ЦЕНТРОИДА пальцев»). Returns the new zoom (within 1..[maxZoom]) and pan; the caller bounds the pan.
 * [TESTED: PinchTest - the point under the fingers stays put]
 */
fun pinchStep(zoom: Float, pan: Offset, centroid: Offset, centre: Offset, zoomChange: Float, panChange: Offset, maxZoom: Float): Pair<Float, Offset> {
    val next = (zoom * zoomChange).coerceIn(1f, maxZoom)
    val k = next / zoom
    // A screen point s shows the picture point c + (s - c - pan) / zoom; keeping it at s after the zoom gives this pan.
    val d = centroid - centre
    return next to (pan - d) * k + d + panChange
}
