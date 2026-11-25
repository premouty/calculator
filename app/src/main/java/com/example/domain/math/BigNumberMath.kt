package com.example.domain.math

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.ln as kotlinLn
import kotlin.math.sqrt as kotlinSqrt

enum class MathErrorType {
    DIVISION_BY_ZERO,
    DOMAIN_ERROR,
    SYNTAX_ERROR,
    OVERFLOW
}

class MathEvaluationException(val errorType: MathErrorType) : Exception(errorType.name)

object BigNumberMath {
    // 64 significant digits of working precision for transcendental/fractional ops
    val WORK_MC = MathContext(64, RoundingMode.HALF_UP)
    // Display rounding precision for non-terminating / scientific results
    val DISPLAY_MC = MathContext(16, RoundingMode.HALF_UP)

    private const val MAX_EXACT_DIGITS = 5000
    private const val MAX_FACTORIAL = 2000
    private const val MAX_EXP_INPUT = 23000.0

    val PI: BigDecimal = BigDecimal(
        "3.1415926535897932384626433832795028841971693993751058209749445923"
    )
    val TWO_PI: BigDecimal = PI.multiply(BigDecimal(2), WORK_MC)
    val HALF_PI: BigDecimal = PI.divide(BigDecimal(2), WORK_MC)
    val E: BigDecimal = BigDecimal(
        "2.7182818284590452353602874713526624977572470936999595749669676277"
    )
    private val LN_2: BigDecimal = BigDecimal(
        "0.6931471805599453094172321214581765680755001343602552541206800095"
    )
    private val LN_10: BigDecimal = BigDecimal(
        "2.3025850929940456840179914546843642076011014886287729760333279010"
    )
    private val DEG_TO_RAD: BigDecimal = PI.divide(BigDecimal(180), WORK_MC)
    private val RAD_TO_DEG: BigDecimal = BigDecimal(180).divide(PI, WORK_MC)
    private val EPSILON_SNAP: BigDecimal = BigDecimal("1E-50")

    fun add(a: BigDecimal, b: BigDecimal): BigDecimal {
        val result = a.add(b)
        return trimIfHuge(result)
    }

    fun subtract(a: BigDecimal, b: BigDecimal): BigDecimal {
        val result = a.subtract(b)
        return trimIfHuge(result)
    }

    fun multiply(a: BigDecimal, b: BigDecimal): BigDecimal {
        val estDigits = a.precision() + b.precision()
        val result = if (estDigits > MAX_EXACT_DIGITS) {
            a.multiply(b, WORK_MC)
        } else {
            a.multiply(b)
        }
        return cleanZero(result)
    }

    fun divide(a: BigDecimal, b: BigDecimal): BigDecimal {
        if (b.compareTo(BigDecimal.ZERO) == 0) {
            throw MathEvaluationException(MathErrorType.DIVISION_BY_ZERO)
        }
        val result = try {
            a.divide(b)
        } catch (_: ArithmeticException) {
            a.divide(b, WORK_MC)
        }
        return cleanZero(result)
    }

    fun factorial(n: BigDecimal): BigDecimal {
        val stripped = cleanZero(n)
        if (stripped.compareTo(BigDecimal.ZERO) < 0 || stripped.scale() > 0) {
            throw MathEvaluationException(MathErrorType.DOMAIN_ERROR)
        }
        val intVal = try {
            stripped.intValueExact()
        } catch (_: ArithmeticException) {
            throw MathEvaluationException(MathErrorType.OVERFLOW)
        }
        if (intVal > MAX_FACTORIAL) {
            throw MathEvaluationException(MathErrorType.OVERFLOW)
        }
        var acc = BigInteger.ONE
        for (i in 2..intVal) {
            acc = acc.multiply(BigInteger.valueOf(i.toLong()))
        }
        return BigDecimal(acc)
    }

    fun sqrt(x: BigDecimal): BigDecimal {
        val cmp = x.compareTo(BigDecimal.ZERO)
        if (cmp < 0) throw MathEvaluationException(MathErrorType.DOMAIN_ERROR)
        if (cmp == 0) return BigDecimal.ZERO

        // Initial estimate built from mantissa + scale so arbitrarily huge/tiny numbers work
        val norm = normalizeToMantissa(x) // x = m * 10^exp, m in [1, 10)
        val exp = norm.second
        val evenExp = if (exp % 2 == 0) exp else exp - 1
        val adjustedMantissa = if (exp % 2 == 0) norm.first else norm.first.multiply(BigDecimal.TEN)
        val sqrtMantissa = kotlinSqrt(adjustedMantissa.toDouble())
        var guess = BigDecimal.valueOf(sqrtMantissa).movePointRight(evenExp / 2)
        val two = BigDecimal(2)

        for (i in 0 until 15) {
            val prev = guess
            guess = guess.add(x.divide(guess, WORK_MC), WORK_MC).divide(two, WORK_MC)
            if (guess.subtract(prev).abs().compareTo(EPSILON_SNAP.multiply(guess.abs())) <= 0 && i > 4) {
                break
            }
        }
        return cleanZero(guess.round(WORK_MC))
    }

    fun ln(x: BigDecimal): BigDecimal {
        if (x.compareTo(BigDecimal.ZERO) <= 0) {
            throw MathEvaluationException(MathErrorType.DOMAIN_ERROR)
        }
        if (x.compareTo(BigDecimal.ONE) == 0) return BigDecimal.ZERO

        // x = m * 10^k with m in [1, 10)
        val (mInitial, kInitial) = normalizeToMantissa(x)
        var m = mInitial
        var k10 = kInitial
        // Bring m closer to 1 for fast convergence: divide by 2 until m <= 2
        var k2 = 0
        while (m > BigDecimal(2)) {
            m = m.divide(BigDecimal(2), WORK_MC)
            k2++
        }
        // Now m in [1, 2], so y = (m - 1)/(m + 1) has |y| <= 1/3
        val y = m.subtract(BigDecimal.ONE, WORK_MC)
            .divide(m.add(BigDecimal.ONE, WORK_MC), WORK_MC)
        val y2 = y.multiply(y, WORK_MC)
        var term = y
        var sum = y
        var denom = 1L
        for (i in 1..60) {
            denom += 2
            term = term.multiply(y2, WORK_MC)
            val delta = term.divide(BigDecimal.valueOf(denom), WORK_MC)
            sum = sum.add(delta, WORK_MC)
            if (delta.abs().compareTo(EPSILON_SNAP) <= 0) break
        }
        val lnM = sum.multiply(BigDecimal(2), WORK_MC)
        val result = lnM
            .add(LN_2.multiply(BigDecimal.valueOf(k2.toLong()), WORK_MC), WORK_MC)
            .add(LN_10.multiply(BigDecimal.valueOf(k10.toLong()), WORK_MC), WORK_MC)
        return cleanZero(result)
    }

    fun log10(x: BigDecimal): BigDecimal {
        if (x.compareTo(BigDecimal.ZERO) <= 0) {
            throw MathEvaluationException(MathErrorType.DOMAIN_ERROR)
        }
        // Check exact powers of 10 cleanly
        val norm = normalizeToMantissa(x)
        if (norm.first.compareTo(BigDecimal.ONE) == 0) {
            return BigDecimal.valueOf(norm.second.toLong())
        }
        return cleanZero(ln(x).divide(LN_10, WORK_MC))
    }

    fun exp(x: BigDecimal): BigDecimal {
        if (x.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE
        val approxDouble = x.toDouble()
        if (approxDouble.isInfinite() || abs(approxDouble) > MAX_EXP_INPUT) {
            if (x.signum() < 0) return BigDecimal.ZERO
            throw MathEvaluationException(MathErrorType.OVERFLOW)
        }
        // x = k * ln(2) + r, |r| <= ln(2)/2
        val k = x.divide(LN_2, MathContext(16, RoundingMode.HALF_UP))
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
        val r = x.subtract(LN_2.multiply(BigDecimal.valueOf(k.toLong()), WORK_MC), WORK_MC)

        var term = BigDecimal.ONE
        var sum = BigDecimal.ONE
        for (i in 1..50) {
            term = term.multiply(r, WORK_MC).divide(BigDecimal.valueOf(i.toLong()), WORK_MC)
            sum = sum.add(term, WORK_MC)
            if (term.abs().compareTo(EPSILON_SNAP) <= 0) break
        }

        val scaleFactor = if (k >= 0) {
            BigDecimal(2).pow(k, WORK_MC)
        } else {
            BigDecimal.ONE.divide(BigDecimal(2).pow(-k, WORK_MC), WORK_MC)
        }
        return cleanZero(sum.multiply(scaleFactor, WORK_MC))
    }

    fun pow(base: BigDecimal, exponent: BigDecimal): BigDecimal {
        val b = cleanZero(base)
        val e = cleanZero(exponent)
        if (e.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE
        if (b.compareTo(BigDecimal.ZERO) == 0) {
            if (e.signum() < 0) throw MathEvaluationException(MathErrorType.DIVISION_BY_ZERO)
            return BigDecimal.ZERO
        }

        // Exact integer exponent fast path
        if (e.scale() <= 0) {
            val expInt = try {
                e.intValueExact()
            } catch (_: ArithmeticException) {
                null
            }
            if (expInt != null && abs(expInt) <= 2000) {
                if (expInt > 0) {
                    val estDigits = b.precision().toLong() * expInt
                    return if (estDigits <= MAX_EXACT_DIGITS) {
                        cleanZero(b.pow(expInt))
                    } else {
                        // Use WORK_MC for huge powers like 9^1500
                        cleanZero(b.pow(expInt, WORK_MC))
                    }
                } else {
                    val posPow = b.pow(-expInt, WORK_MC)
                    return divide(BigDecimal.ONE, posPow)
                }
            }
        }

        if (b.signum() < 0) {
            throw MathEvaluationException(MathErrorType.DOMAIN_ERROR)
        }
        val lnBase = ln(b)
        val prod = e.multiply(lnBase, WORK_MC)
        return exp(prod)
    }

    fun sin(angle: BigDecimal, isDegrees: Boolean): BigDecimal {
        if (isDegrees) {
            val mod360 = normalizeDegrees(angle)
            if (mod360.compareTo(BigDecimal.ZERO) == 0 ||
                mod360.compareTo(BigDecimal(180)) == 0 ||
                mod360.compareTo(BigDecimal(-180)) == 0
            ) return BigDecimal.ZERO
            if (mod360.compareTo(BigDecimal(90)) == 0 ||
                mod360.compareTo(BigDecimal(-270)) == 0
            ) return BigDecimal.ONE
            if (mod360.compareTo(BigDecimal(270)) == 0 ||
                mod360.compareTo(BigDecimal(-90)) == 0
            ) return BigDecimal(-1)
            return sinRad(mod360.multiply(DEG_TO_RAD, WORK_MC))
        }
        return sinRad(angle)
    }

    fun cos(angle: BigDecimal, isDegrees: Boolean): BigDecimal {
        if (isDegrees) {
            val mod360 = normalizeDegrees(angle)
            if (mod360.compareTo(BigDecimal(90)) == 0 ||
                mod360.compareTo(BigDecimal(270)) == 0 ||
                mod360.compareTo(BigDecimal(-90)) == 0 ||
                mod360.compareTo(BigDecimal(-270)) == 0
            ) return BigDecimal.ZERO
            if (mod360.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ONE
            if (mod360.compareTo(BigDecimal(180)) == 0 ||
                mod360.compareTo(BigDecimal(-180)) == 0
            ) return BigDecimal(-1)
            return cosRad(mod360.multiply(DEG_TO_RAD, WORK_MC))
        }
        return cosRad(angle)
    }

    fun tan(angle: BigDecimal, isDegrees: Boolean): BigDecimal {
        val c = cos(angle, isDegrees)
        if (c.abs().compareTo(BigDecimal("1E-55")) <= 0) {
            throw MathEvaluationException(MathErrorType.DOMAIN_ERROR)
        }
        val s = sin(angle, isDegrees)
        return cleanZero(s.divide(c, WORK_MC))
    }

    fun asin(x: BigDecimal, isDegrees: Boolean): BigDecimal {
        val cmpOne = x.abs().compareTo(BigDecimal.ONE)
        if (cmpOne > 0) throw MathEvaluationException(MathErrorType.DOMAIN_ERROR)
        val rad = if (cmpOne == 0) {
            if (x.signum() >= 0) HALF_PI else HALF_PI.negate()
        } else {
            val denom = sqrt(BigDecimal.ONE.subtract(x.multiply(x, WORK_MC), WORK_MC))
            atanRad(x.divide(denom, WORK_MC))
        }
        return cleanZero(if (isDegrees) rad.multiply(RAD_TO_DEG, WORK_MC) else rad)
    }

    fun acos(x: BigDecimal, isDegrees: Boolean): BigDecimal {
        val cmpOne = x.abs().compareTo(BigDecimal.ONE)
        if (cmpOne > 0) throw MathEvaluationException(MathErrorType.DOMAIN_ERROR)
        val rad = HALF_PI.subtract(asin(x, isDegrees = false), WORK_MC)
        return cleanZero(if (isDegrees) rad.multiply(RAD_TO_DEG, WORK_MC) else rad)
    }

    fun atan(x: BigDecimal, isDegrees: Boolean): BigDecimal {
        val rad = atanRad(x)
        return cleanZero(if (isDegrees) rad.multiply(RAD_TO_DEG, WORK_MC) else rad)
    }

    private fun normalizeDegrees(deg: BigDecimal): BigDecimal {
        val rem = deg.remainder(BigDecimal(360))
        return cleanZero(if (rem < BigDecimal.ZERO) rem.add(BigDecimal(360)) else rem)
    }

    private fun sinRad(x: BigDecimal): BigDecimal {
        var r = x.remainder(TWO_PI, WORK_MC)
        if (r > PI) r = r.subtract(TWO_PI, WORK_MC)
        if (r < PI.negate()) r = r.add(TWO_PI, WORK_MC)

        // Snap near-zero values (like sin(pi))
        if (r.abs().compareTo(EPSILON_SNAP) <= 0) return BigDecimal.ZERO

        val r2 = r.multiply(r, WORK_MC)
        var term = r
        var sum = r
        for (i in 1..45) {
            val d1 = BigDecimal.valueOf((2L * i))
            val d2 = BigDecimal.valueOf((2L * i + 1))
            term = term.multiply(r2, WORK_MC).negate()
                .divide(d1.multiply(d2, WORK_MC), WORK_MC)
            sum = sum.add(term, WORK_MC)
            if (term.abs().compareTo(EPSILON_SNAP) <= 0) break
        }
        return if (sum.abs().compareTo(BigDecimal("1E-52")) <= 0) BigDecimal.ZERO else cleanZero(sum)
    }

    private fun cosRad(x: BigDecimal): BigDecimal {
        var r = x.remainder(TWO_PI, WORK_MC)
        if (r > PI) r = r.subtract(TWO_PI, WORK_MC)
        if (r < PI.negate()) r = r.add(TWO_PI, WORK_MC)

        val r2 = r.multiply(r, WORK_MC)
        var term = BigDecimal.ONE
        var sum = BigDecimal.ONE
        for (i in 1..45) {
            val d1 = BigDecimal.valueOf((2L * i - 1))
            val d2 = BigDecimal.valueOf((2L * i))
            term = term.multiply(r2, WORK_MC).negate()
                .divide(d1.multiply(d2, WORK_MC), WORK_MC)
            sum = sum.add(term, WORK_MC)
            if (term.abs().compareTo(EPSILON_SNAP) <= 0) break
        }
        return if (sum.abs().compareTo(BigDecimal("1E-52")) <= 0) BigDecimal.ZERO else cleanZero(sum)
    }

    private fun atanRad(x: BigDecimal): BigDecimal {
        if (x.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        val sign = x.signum()
        val absX = x.abs()
        val inverted = absX > BigDecimal.ONE
        var u = if (inverted) BigDecimal.ONE.divide(absX, WORK_MC) else absX

        // Half-angle reduction twice: atan(u) = 2 * atan(u / (1 + sqrt(1 + u^2)))
        repeat(2) {
            val root = sqrt(BigDecimal.ONE.add(u.multiply(u, WORK_MC), WORK_MC))
            u = u.divide(BigDecimal.ONE.add(root, WORK_MC), WORK_MC)
        }

        // Now u <= tan(pi/16) ~ 0.1989, series converges rapidly
        val u2 = u.multiply(u, WORK_MC)
        var term = u
        var sum = u
        var denom = 1L
        for (i in 1..50) {
            denom += 2
            term = term.multiply(u2, WORK_MC).negate()
            val delta = term.divide(BigDecimal.valueOf(denom), WORK_MC)
            sum = sum.add(delta, WORK_MC)
            if (delta.abs().compareTo(EPSILON_SNAP) <= 0) break
        }
        var res = sum.multiply(BigDecimal(4), WORK_MC)
        if (inverted) {
            res = HALF_PI.subtract(res, WORK_MC)
        }
        return if (sign < 0) res.negate() else res
    }

    private fun normalizeToMantissa(x: BigDecimal): Pair<BigDecimal, Int> {
        val s = cleanZero(x.abs())
        val exp = s.precision() - s.scale() - 1
        val mantissa = s.movePointLeft(exp)
        return mantissa to exp
    }

    private fun trimIfHuge(v: BigDecimal): BigDecimal {
        return if (v.precision() > MAX_EXACT_DIGITS) {
            cleanZero(v.round(WORK_MC))
        } else {
            cleanZero(v)
        }
    }

    fun cleanZero(v: BigDecimal): BigDecimal {
        if (v.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        return v.stripTrailingZeros()
    }
}
