package net.crepe.utils

import com.hypixel.hytale.math.raycast.RaycastAABB
import com.hypixel.hytale.math.shape.Box
import com.hypixel.hytale.math.vector.Transform
import com.hypixel.hytale.math.vector.Vector3d
import com.hypixel.hytale.math.vector.Vector3i

class MathUtils {
    companion object {
        fun boxHit(boundingBox: Box, detailedBoxes: Array<Box>, targetBoxes: List<Box>, boxPos: Vector3i, ray: Transform): Int? {
            val hitDist = RaycastAABB.intersect(
                boundingBox.min.x + boxPos.x, boundingBox.min.y + boxPos.y, boundingBox.min.z + boxPos.z,
                boundingBox.max.x + boxPos.x, boundingBox.max.y + boxPos.y, boundingBox.max.z + boxPos.z,
                ray.position.x, ray.position.y, ray.position.z,
                ray.direction.x, ray.direction.y, ray.direction.z
            )

            if (hitDist == Double.POSITIVE_INFINITY) return null

            val boxesHitDist = detailedBoxes.map {
                RaycastAABB.intersect(
                    it.min.x + boxPos.x, it.min.y + boxPos.y, it.min.z + boxPos.z,
                    it.max.x + boxPos.x, it.max.y + boxPos.y, it.max.z + boxPos.z,
                    ray.position.x, ray.position.y, ray.position.z,
                    ray.direction.x, ray.direction.y, ray.direction.z
                )
            }
            
            val minDist = boxesHitDist.min()
            targetBoxes.forEach { 
                val targetBoxIndex = detailedBoxes.indexOf(it)
                if (minDist == boxesHitDist[targetBoxIndex]) {
                    return targetBoxIndex
                }
            }
            
            return null
        }
    }
}

fun Vector3d.isWithinBox(center: Vector3d, radius: Double): Boolean {
    return this.x >= center.x - radius && this.x <= center.x + radius &&
           this.y >= center.y - radius && this.y <= center.y + radius &&
           this.z >= center.z - radius && this.z <= center.z + radius
}