package uv.nova.matchmaker.core

import kotlin.test.Test
import kotlin.test.assertEquals

internal class ClusteringTest {

    @Test
    fun vectorsForEachIndexed() {
        val buf = VectorBuffer(2, 4)
        buf.forEachIndexed { i, v ->
            v[0] = i.toDouble()
            v[1] = (i * i).toDouble()
        }

        buf.seek(0)
        assertEquals(0.0, buf[0], EPSILON)
        assertEquals(0.0, buf[1], EPSILON)

        buf.seek(1)
        assertEquals(1.0, buf[0], EPSILON)
        assertEquals(1.0, buf[1], EPSILON)

        buf.seek(2)
        assertEquals(2.0, buf[0], EPSILON)
        assertEquals(4.0, buf[1], EPSILON)

        buf.seek(3)
        assertEquals(3.0, buf[0], EPSILON)
        assertEquals(9.0, buf[1], EPSILON)
    }

    @Test
    fun kmeans_1() {
        val points = VectorBuffer(2, 1)
        points.put(0, 1.0, 4.0)

        val labels = kmeans(1, points)
        assertEquals(1, labels.size)
        assertEquals(0, labels[0])
    }

    @Test
    fun kmeans_2() {
        val points = VectorBuffer(2, 10)
        points.put(0, 1.0, 1.0)
        points.put(1, 2.0, 1.0)
        points.put(2, 1.0, 2.0)
        points.put(3, 2.0, 2.0)
        points.put(4, 1.5, 1.5)

        points.put(5, 10.0, 10.0)
        points.put(6, 11.0, 10.0)
        points.put(7, 10.0, 11.0)
        points.put(8, 11.0, 11.0)
        points.put(9, 10.5, 10.5)

        val labels = kmeans(2, points)
        assertEquals(2, labels.size)
        assertEquals(0, labels[0])
        assertEquals(0, labels[1])
        assertEquals(0, labels[2])
        assertEquals(0, labels[3])
        assertEquals(0, labels[4])
        assertEquals(1, labels[5])
        assertEquals(1, labels[6])
        assertEquals(1, labels[7])
        assertEquals(1, labels[8])
        assertEquals(1, labels[9])

    }

    private fun VectorBuffer.put(i: Int, x: Double, y: Double) {
        this.seek(i)
        this[0] = x
        this[1] = y
    }

}
