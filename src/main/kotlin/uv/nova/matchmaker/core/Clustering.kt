package uv.nova.matchmaker.core

import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random


const val EPSILON = 1E-9

interface Vector {
    val dims: Int
    operator fun get(index: Int): Double
    operator fun set(index: Int, value: Double)
    operator fun plusAssign(rhs: Vector)
    operator fun divAssign(n: Double)
}

class VectorBuffer(
    override val dims: Int,
    val size: Int
): Vector {
    private val buffer = DoubleArray(dims * size)
    private var offset = 0

    override operator fun get(index: Int): Double = buffer[offset + index]

    override operator fun set(index: Int, value: Double) {
        buffer[offset + index] = value
    }

    override fun plusAssign(rhs: Vector) {
        for (i in 0 until dims) {
            this[i] += rhs[i]
        }
    }

    override fun divAssign(n: Double) {
        for (i in 0 until dims) {
            this[i] /= n
        }
    }

    fun seek(index: Int) {
        offset = index * dims
    }

    fun fill(value: Double) {
        buffer.fill(value)
    }
}

fun VectorBuffer.forEach(block: (v: Vector) -> Unit) {
    for (i in 0 until size) {
        seek(i)
        block(this)
    }
}

fun VectorBuffer.forEachIndexed(block: (index: Int, v: Vector) -> Unit) {
    for (i in 0 until size) {
        seek(i)
        block(i, this)
    }
}

inline fun VectorBuffer.forEachDim(crossinline block: (dim: Int, v: Vector) -> Unit) {
    for (i in 0 until this.dims) {
        block(i, this)
    }
}

interface ClusteringStrategy {
    val maxIterations: Int
    fun distance(v1: Vector, v2: Vector): Double
}

val DefaultClustering = object : ClusteringStrategy {
    override val maxIterations = 100

    override fun distance(v1: Vector, v2: Vector): Double {
        assert(v1.dims == v2.dims)

        var sqsum = 0.0
        for (i in 0 until min(v1.dims, v2.dims)) {
            val d = v1[i] - v2[i]
            sqsum += d * d
        }
        return sqrt(sqsum)
    }
}

fun kmeans(k: Int, points: VectorBuffer, strategy: ClusteringStrategy = DefaultClustering): IntArray {
    val labels = IntArray(points.size)
    val clusterSize = IntArray(k)
    val clusterCenters = VectorBuffer(points.dims, k)
    initCenters(clusterCenters, points)

    for (iteration in 0 until strategy.maxIterations) {
        var changed = 0
        // assign to clusters
        points.forEachIndexed { i, p ->
            var label = 0
            clusterCenters.seek(0)
            var minDist = strategy.distance(clusterCenters, p)
            for (j in 1 until k) {
                clusterCenters.seek(j)
                val dist = strategy.distance(clusterCenters, p)
                if (dist < minDist) {
                    minDist = dist
                    label = j
                }
            }

            if (labels[i] != label) changed += 1
            labels[i] = label
        }

        if (changed == 0) break

        // update centroids
        clusterCenters.fill(0.0)
        clusterSize.fill(0)
        points.forEachIndexed { i, p ->
            val label = labels[i]
            clusterSize[label] += 1

            clusterCenters.seek(label)
            clusterCenters += p
        }
        for (i in 0 until clusterCenters.size) {
            clusterCenters.seek(i)
            clusterCenters /= clusterSize[i].toDouble()
        }
    }

    return labels
}

private fun initCenters(centers: VectorBuffer, points: VectorBuffer) {
    val minMax = VectorBuffer(2, points.dims)

    points.seek(0)
    points.forEachDim { i, v ->
        minMax.seek(i)
        minMax[0] = v[i]
        minMax[1] = v[i]
    }

    for (j in 1 until points.size) {
        points.seek(j)
        points.forEachDim { i, v ->
            minMax.seek(i)
            if (points[i] < minMax[0]) minMax[0] = v[i]
            if (points[i] > minMax[1]) minMax[1] = v[i]
        }
    }

    centers.forEach { c ->
        centers.forEachDim { i, _ ->
            minMax.seek(i)
            c[i] = Random.nextDouble(minMax[0] - 1.0, minMax[1] + 1.0)
        }
    }
}
