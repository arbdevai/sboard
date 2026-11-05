package com.sahalos.sboard.core

import com.sahalos.sboard.model.KeyModel
import kotlin.math.sqrt

data class KeyBoundingBox(
    val key: KeyModel,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float = (left + right) / 2f
    val centerY: Float = (top + bottom) / 2f
}

object HitboxEngine {

    fun resolveKeyAt(
        touchX: Float,
        touchY: Float,
        keyBoxes: List<KeyBoundingBox>,
        previousChar: Char? = null
    ): KeyModel? {
        if (keyBoxes.isEmpty()) return null

        // 1. Direct hit inside bounding box
        val directHits = keyBoxes.filter { box ->
            touchX >= box.left && touchX <= box.right &&
            touchY >= box.top && touchY <= box.bottom
        }

        if (directHits.size == 1) {
            val hit = directHits.first()
            val dx = touchX - hit.centerX
            val dy = touchY - hit.centerY
            val dist = sqrt(dx * dx + dy * dy)
            val halfWidth = (hit.right - hit.left) / 2f

            // If well inside center zone (within 70% of radius), accept directly
            if (dist < halfWidth * 0.7f) {
                return hit.key
            }
        }

        // 2. Spatial Euclidean Distance + Bigram Scoring for border / fat-finger touches
        var bestCandidate: KeyModel? = null
        var highestScore = -Float.MAX_VALUE

        for (box in keyBoxes) {
            val dx = touchX - box.centerX
            val dy = touchY - box.centerY
            val distance = sqrt(dx * dx + dy * dy)

            // Euclidean distance penalty (closer is better)
            val distancePenalty = -distance

            // Bigram boost if candidate is a single character
            val bigramBoost = if (box.key.primaryLabel.length == 1 && previousChar != null) {
                BigramEngine.getTransitionBonus(previousChar, box.key.primaryLabel[0]) * 15f
            } else {
                0f
            }

            val score = distancePenalty + bigramBoost
            if (score > highestScore) {
                highestScore = score
                bestCandidate = box.key
            }
        }

        return bestCandidate
    }
}
