package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonBasicSessionGenerator
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.abs
import kotlin.random.Random

/** Expectations below are copied from NUMCOMPSIM_COMPARE_SPEC.md, not from production validators. */
class ComparisonIndependentGenerationAuditTest {
    private enum class Severity { HARD, SOFT }
    private class Violation(val severity: Severity, message: String) : AssertionError(message)
    private data class Context(val level: ComparisonLevel, val seed: Int) {
        fun require(rule: Boolean, index: Int, name: String, severity: Severity = Severity.HARD) {
            if (!rule) throw Violation(severity, "level=$level seed=$seed index=$index rule=$name")
        }
    }

    @Test fun testLevelAcrossOneThousandSeeds() = audit(ComparisonLevel.TEST, 24, 8)
    @Test fun easyLevelAcrossOneThousandSeeds() = audit(ComparisonLevel.EASY, 45, 15)
    @Test fun normalLevelAcrossOneThousandSeeds() = audit(ComparisonLevel.NORMAL, 36, 12)
    @Test fun hardLevelAcrossOneThousandSeeds() = audit(ComparisonLevel.HARD, 30, 10)

    private fun audit(level: ComparisonLevel, size: Int, signsPerKind: Int) {
        val executor = Executors.newSingleThreadExecutor { task ->
            Thread(task, "comparison-audit-$level").apply { isDaemon = true }
        }
        var firstHard: String? = null
        var firstSoft: String? = null
        val signOrders = HashSet<List<ComparisonSign>>()
        val operationOrders = HashSet<List<String>>()
        try {
            for (seed in 0 until 1000) {
                val context = Context(level, seed)
                val future = executor.submit<List<ComparisonQuestion>> {
                    ComparisonBasicSessionGenerator.generate(level, Random(seed))
                }
                val questions = try {
                    future.get(60, TimeUnit.SECONDS)
                } catch (timeout: TimeoutException) {
                    future.cancel(true)
                    throw AssertionError("level=$level seed=$seed index=-1 rule=generation exceeded 60-second diagnostic limit", timeout)
                } catch (failure: ExecutionException) {
                    if (firstHard == null) firstHard =
                        "level=$level seed=$seed index=-1 rule=generation threw ${failure.cause}"
                    continue
                }
                try {
                    validateHard(context, questions, size, signsPerKind)
                } catch (failure: Violation) {
                    if (failure.severity == Severity.HARD && firstHard == null) firstHard = failure.message
                    if (failure.severity == Severity.SOFT && firstSoft == null) firstSoft = failure.message
                    continue
                }
                signOrders.add(questions.map { it.correctSign })
                operationOrders.add(questions.map { orderKey(it) })
                try {
                    validateSoft(context, questions)
                } catch (failure: Violation) {
                    if (firstSoft == null) firstSoft = failure.message
                }
            }
        } finally {
            executor.shutdownNow()
        }
        if (firstHard == null && firstSoft == null && signOrders.size < 2) firstSoft =
            "level=$level seed=0..999 index=-1 rule=sign order is identical across all seeds"
        if (firstHard == null && firstSoft == null && signOrders.isNotEmpty() &&
            signOrders.all { order -> order.indices.all { index -> order[index] == order[index % 3] } })
            firstSoft = "level=$level seed=0..999 index=-1 rule=every sign order is a fixed three-sign cycle"
        if (firstHard == null && firstSoft == null && level >= ComparisonLevel.NORMAL &&
            operationOrders.size < 2) firstSoft =
            "level=$level seed=0..999 index=-1 rule=operation order is identical across all seeds"
        if (firstHard != null || firstSoft != null) fail(
            listOfNotNull(firstHard?.let { "HARD: $it" }, firstSoft?.let { "SOFT: $it" }).joinToString("\n")
        )
    }

    private fun validateHard(c: Context, questions: List<ComparisonQuestion>, size: Int, perSign: Int) {
        c.require(questions.size == size, -1, "session size expected=$size actual=${questions.size}")
        val signs = mutableMapOf<ComparisonSign, Int>()
        val operations = mutableMapOf<ComparisonOperation, Int>()
        val groups = mutableMapOf<String, Int>()
        val distances = IntArray(3)
        val keys = HashSet<String>()
        var complexComputations = 0
        questions.forEachIndexed { index, question ->
            c.require(question.level == c.level, index, "question level")
            val left = directValue(c, index, question.left)
            val right = directValue(c, index, question.right)
            val expectedSign = when {
                left < right -> ComparisonSign.LESS
                left > right -> ComparisonSign.GREATER
                else -> ComparisonSign.EQUAL
            }
            c.require(question.correctSign == expectedSign, index, "mathematical sign: $left ? $right")
            signs[expectedSign] = (signs[expectedSign] ?: 0) + 1
            val key = independentKey(question)
            c.require(keys.add(key), index, "canonical duplicate key=$key")
            val distance = abs(left - right)
            when (c.level) {
                ComparisonLevel.TEST -> {
                    c.require(question.left is ComparisonSide.Number && question.right is ComparisonSide.Number,
                        index, "TEST number/number")
                    c.require(left in 0L..10L && right in 0L..10L, index, "TEST range 0..10")
                }
                ComparisonLevel.EASY -> {
                    c.require(question.left is ComparisonSide.Number && question.right is ComparisonSide.Number,
                        index, "EASY number/number")
                    c.require(left in 0L..100L && right in 0L..100L, index, "EASY range 0..100")
                    if (distance > 0) distances[checkDistance(c, index, distance, 5, 30, 99)]++
                }
                ComparisonLevel.NORMAL -> {
                    val expression = (question.left as? ComparisonSide.Expression)
                        ?: (question.right as? ComparisonSide.Expression)
                    val number = (question.left as? ComparisonSide.Number)
                        ?: (question.right as? ComparisonSide.Number)
                    c.require(expression != null && number != null, index, "NORMAL expression/number")
                    val e = expression!!
                    val n = number!!
                    c.require((question.left is ComparisonSide.Expression) !=
                        (question.right is ComparisonSide.Expression), index, "NORMAL one expression")
                    validateNormalExpression(c, index, e, directValue(c, index, e))
                    val numberRange = if (e.operation == ComparisonOperation.DIVISION) 2..18 else 2..100
                    c.require(n.value in numberRange, index, "NORMAL number range")
                    operations[e.operation] = (operations[e.operation] ?: 0) + 1
                    if (isComplex(c.level, e)) complexComputations++
                    if (distance > 0) distances[when (e.operation) {
                        ComparisonOperation.ADDITION, ComparisonOperation.SUBTRACTION ->
                            checkDistance(c, index, distance, 5, 15, 35)
                        ComparisonOperation.MULTIPLICATION -> checkDistance(c, index, distance, 4, 12, 25)
                        ComparisonOperation.DIVISION -> checkDistance(c, index, distance, 2, 5, 9)
                    }]++
                }
                ComparisonLevel.HARD -> {
                    c.require(question.left is ComparisonSide.Expression &&
                        question.right is ComparisonSide.Expression, index, "HARD expression/expression")
                    val a = question.left as ComparisonSide.Expression
                    val b = question.right as ComparisonSide.Expression
                    val arithmeticA = isArithmetic(a.operation)
                    val arithmeticB = isArithmetic(b.operation)
                    val group = when {
                        arithmeticA && arithmeticB -> "ARITHMETIC"
                        !arithmeticA && !arithmeticB -> "TABULAR"
                        else -> "MIXED"
                    }
                    groups[group] = (groups[group] ?: 0) + 1
                    validateHardExpression(c, index, a, directValue(c, index, a))
                    validateHardExpression(c, index, b, directValue(c, index, b))
                    if (group == "ARITHMETIC") for (e in listOf(a, b)) {
                        val value = directValue(c, index, e)
                        if (e.operation == ComparisonOperation.ADDITION) c.require(
                            (e.firstOperand in 100..999 || e.secondOperand in 100..999) && value in 100L..1000L,
                            index, "HARD arithmetic addition lower bounds"
                        ) else c.require(e.firstOperand in 100..1000 && value in 100L..999L,
                            index, "HARD arithmetic subtraction lower bounds")
                    }
                    if (group == "MIXED") {
                        val arithmetic = if (arithmeticA) a else b
                        val tabular = if (arithmeticA) b else a
                        val limit = if (tabular.operation == ComparisonOperation.MULTIPLICATION) 100L else 20L
                        c.require(directValue(c, index, arithmetic) in 2L..limit,
                            index, "HARD mixed arithmetic result 2..$limit")
                    }
                    complexComputations += listOf(a, b).count { isComplex(c.level, it) }
                    if (distance > 0) distances[when (group) {
                        "ARITHMETIC" -> checkDistance(c, index, distance, 10, 50, 200)
                        "TABULAR" -> checkDistance(c, index, distance, 5, 20, 80)
                        else -> if (a.operation == ComparisonOperation.MULTIPLICATION ||
                            b.operation == ComparisonOperation.MULTIPLICATION)
                            checkDistance(c, index, distance, 5, 15, 40)
                        else checkDistance(c, index, distance, 2, 6, 18)
                    }]++
                }
            }
        }
        for (sign in ComparisonSign.entries) c.require(signs[sign] == perSign, -1,
            "sign quota $sign expected=$perSign actual=${signs[sign] ?: 0}")
        if (c.level == ComparisonLevel.NORMAL) {
            c.require(complexComputations == 22, -1,
                "NORMAL complexity quota 22/36 actual=$complexComputations")
            for (operation in ComparisonOperation.entries) c.require(operations[operation] == 9, -1,
                "NORMAL operation quota $operation expected=9 actual=${operations[operation] ?: 0}", Severity.SOFT)
        }
        if (c.level == ComparisonLevel.HARD) {
            c.require(complexComputations == 42, -1,
                "HARD complexity quota 42/60 actual=$complexComputations")
            for (group in listOf("ARITHMETIC", "TABULAR", "MIXED")) c.require(groups[group] == 10, -1,
                "HARD group quota $group expected=10 actual=${groups[group] ?: 0}", Severity.SOFT)
        }
        val expectedDistances = when (c.level) {
            ComparisonLevel.TEST -> null
            ComparisonLevel.EASY -> Triple(9, setOf(10, 11), setOf(10, 11))
            ComparisonLevel.NORMAL -> Triple(7, setOf(8, 9), setOf(8, 9))
            ComparisonLevel.HARD -> Triple(6, setOf(7), setOf(7))
        }
        if (expectedDistances != null) c.require(
            distances[0] == expectedDistances.first &&
                distances[1] in expectedDistances.second &&
                distances[2] in expectedDistances.third &&
                abs(distances[1] - distances[2]) <= 1 &&
                distances.sum() == size - perSign,
            -1, "distance quotas actual=${distances.toList()}", Severity.SOFT
        )
    }

    private fun validateNormalExpression(c: Context, index: Int, e: ComparisonSide.Expression, value: Long) {
        when (e.operation) {
            ComparisonOperation.ADDITION -> c.require(value in 20L..100L, index, "NORMAL + result 20..100")
            ComparisonOperation.SUBTRACTION -> c.require(e.firstOperand <= 100 && value >= 2,
                index, "NORMAL - minuend/result")
            ComparisonOperation.MULTIPLICATION -> c.require(e.firstOperand in 2..9 &&
                e.secondOperand in 2..9, index, "NORMAL x factors 2..9")
            ComparisonOperation.DIVISION -> c.require(e.secondOperand in 2..9 && value in 2L..9L,
                index, "NORMAL division divisor/quotient 2..9")
        }
    }

    private fun validateHardExpression(c: Context, index: Int, e: ComparisonSide.Expression, value: Long) {
        when (e.operation) {
            ComparisonOperation.ADDITION -> c.require(value <= 1000L, index, "HARD + result <=1000")
            ComparisonOperation.SUBTRACTION -> c.require(e.firstOperand <= 1000 && value > 0,
                index, "HARD - minuend/result")
            ComparisonOperation.MULTIPLICATION -> c.require(e.firstOperand in 2..10 &&
                e.secondOperand in 2..10, index, "HARD x factors 2..10")
            ComparisonOperation.DIVISION -> c.require(e.secondOperand in 2..10 && value in 2L..10L,
                index, "HARD division divisor/quotient 2..10")
        }
    }

    private fun directValue(c: Context, index: Int, side: ComparisonSide): Long = when (side) {
        is ComparisonSide.Number -> side.value.toLong()
        is ComparisonSide.Expression -> {
            val a = side.firstOperand.toLong()
            val b = side.secondOperand.toLong()
            c.require(a > 0 && b > 0, index, "positive operands")
            val value = when (side.operation) {
                ComparisonOperation.ADDITION -> a + b
                ComparisonOperation.SUBTRACTION -> a - b
                ComparisonOperation.MULTIPLICATION -> a * b
                ComparisonOperation.DIVISION -> {
                    c.require(a % b == 0L, index, "exact division")
                    a / b
                }
            }
            c.require(value > 0, index, "positive expression result")
            c.require(side.value.toLong() == value, index, "stored expression value expected=$value")
            value
        }
    }

    private fun checkDistance(c: Context, index: Int, distance: Long,
                              close: Int, medium: Int, far: Int): Int {
        c.require(distance in 1L..far.toLong(), index,
            "distance $distance outside 1..$far (close=$close medium=$medium)")
        return when {
            distance <= close -> 0
            distance <= medium -> 1
            else -> 2
        }
    }

    private fun independentKey(question: ComparisonQuestion): String =
        listOf(question.left, question.right).map { side ->
            when (side) {
                is ComparisonSide.Number -> "N:${side.value}"
                is ComparisonSide.Expression -> {
                    val commutative = side.operation == ComparisonOperation.ADDITION ||
                        side.operation == ComparisonOperation.MULTIPLICATION
                    val a = if (commutative) minOf(side.firstOperand, side.secondOperand) else side.firstOperand
                    val b = if (commutative) maxOf(side.firstOperand, side.secondOperand) else side.secondOperand
                    "E:${side.operation}:$a:$b"
                }
            }
        }.sorted().joinToString("|")

    private fun isArithmetic(operation: ComparisonOperation) =
        operation == ComparisonOperation.ADDITION || operation == ComparisonOperation.SUBTRACTION

    private fun isComplex(level: ComparisonLevel, e: ComparisonSide.Expression): Boolean {
        val a = e.firstOperand
        val b = e.secondOperand
        val high = if (level == ComparisonLevel.NORMAL) 9 else 10
        return when (e.operation) {
            ComparisonOperation.ADDITION -> if (level == ComparisonLevel.NORMAL) a % 10 + b % 10 >= 10
                else anyDigitCarry(a, b)
            ComparisonOperation.SUBTRACTION -> if (level == ComparisonLevel.NORMAL) a % 10 < b % 10
                else anyDigitBorrow(a, b)
            ComparisonOperation.MULTIPLICATION -> a in 6..high && b in 6..high
            ComparisonOperation.DIVISION -> b in 6..high && (a / b) in 6..high
        }
    }

    private fun anyDigitCarry(first: Int, second: Int): Boolean {
        var a = first
        var b = second
        while (a > 0 || b > 0) {
            if (a % 10 + b % 10 >= 10) return true
            a /= 10
            b /= 10
        }
        return false
    }

    private fun anyDigitBorrow(first: Int, second: Int): Boolean {
        var a = first
        var b = second
        while (a > 0 || b > 0) {
            if (a % 10 < b % 10) return true
            a /= 10
            b /= 10
        }
        return false
    }

    private fun orderKey(question: ComparisonQuestion): String = when (question.level) {
        ComparisonLevel.NORMAL -> listOf(question.left, question.right)
            .filterIsInstance<ComparisonSide.Expression>().single().operation.name
        ComparisonLevel.HARD -> listOf(question.left, question.right)
            .filterIsInstance<ComparisonSide.Expression>().map { it.operation.name }.sorted().joinToString("+")
        else -> question.correctSign.name
    }

    private fun validateSoft(c: Context, questions: List<ComparisonQuestion>) {
        if (c.level == ComparisonLevel.EASY) c.require(
            questions.count { it.left.value in 10..99 && it.right.value in 10..99 } > questions.size / 2,
            -1, "EASY two-digit majority", Severity.SOFT
        )
        if (c.level != ComparisonLevel.NORMAL && c.level != ComparisonLevel.HARD) return
        val order = questions.map(::orderKey)
        var run = 1
        for (index in 1 until order.size) {
            run = if (order[index] == order[index - 1]) run + 1 else 1
            c.require(run <= 3, index, "operation/pair series $run of ${order[index]}", Severity.SOFT)
        }
        if (c.level == ComparisonLevel.HARD) c.require(
            ComparisonOperation.entries.all { operation -> questions.any { question ->
                (question.left as ComparisonSide.Expression).operation == operation ||
                    (question.right as ComparisonSide.Expression).operation == operation
            } }, -1, "HARD all four operations represented", Severity.SOFT
        )
    }
}
