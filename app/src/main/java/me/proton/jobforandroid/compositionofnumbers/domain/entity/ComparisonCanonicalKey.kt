package me.proton.jobforandroid.compositionofnumbers.domain.entity

/** Structural identity of a comparison, independent of side order and correct sign. */
object ComparisonCanonicalKey {
    fun of(question: ComparisonQuestion): String {
        val left = sideKey(question.left)
        val right = sideKey(question.right)
        return if (left <= right) "$left|$right" else "$right|$left"
    }

    private fun sideKey(side: ComparisonSide): String = when (side) {
        is ComparisonSide.Number -> "N:${side.value}"
        is ComparisonSide.Expression -> {
            val first = side.firstOperand
            val second = side.secondOperand
            val commutative = side.operation == ComparisonOperation.ADDITION ||
                side.operation == ComparisonOperation.MULTIPLICATION
            val a = if (commutative) minOf(first, second) else first
            val b = if (commutative) maxOf(first, second) else second
            "E:${side.operation.name}:$a:$b"
        }
    }
}
