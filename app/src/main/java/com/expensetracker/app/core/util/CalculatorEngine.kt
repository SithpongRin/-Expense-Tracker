package com.expensetracker.app.core.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Stack

/**
 * Robust, safe arithmetic expression evaluator designed for financial entry.
 * Uses exact [BigDecimal] arithmetic. Rejects malformed expressions and division by zero.
 */
class CalculatorEngine(
    private val mathContext: MathContext = MathContext(16, RoundingMode.HALF_EVEN)
) {

    sealed class Token {
        data class Number(val value: BigDecimal) : Token()
        sealed class Operator(val precedence: Int) : Token() {
            object Add : Operator(1)
            object Subtract : Operator(1)
            object Multiply : Operator(2)
            object Divide : Operator(2)
        }
        object OpenParen : Token()
        object CloseParen : Token()
    }

    /**
     * Evaluates a mathematical expression string and returns the resulting [BigDecimal].
     * Throws [IllegalArgumentException] on malformed input or division by zero.
     */
    fun evaluate(expression: String): BigDecimal {
        val sanitized = sanitize(expression)
        if (sanitized.isBlank()) {
            return BigDecimal.ZERO
        }

        val tokens = tokenize(sanitized)
        if (tokens.isEmpty()) {
            return BigDecimal.ZERO
        }

        val rpn = toReversePolishNotation(tokens)
        return evaluateRpn(rpn)
    }

    private fun sanitize(expr: String): String {
        return expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(",", "")
            .trim()
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        var expectingUnary = true

        while (i < expr.length) {
            val c = expr[i]

            when {
                c.isWhitespace() -> {
                    i++
                }
                c == '(' -> {
                    tokens.add(Token.OpenParen)
                    expectingUnary = true
                    i++
                }
                c == ')' -> {
                    tokens.add(Token.CloseParen)
                    expectingUnary = false
                    i++
                }
                c == '+' -> {
                    if (expectingUnary) {
                        // Unary plus is a no-op
                        i++
                    } else {
                        tokens.add(Token.Operator.Add)
                        expectingUnary = true
                        i++
                    }
                }
                c == '-' -> {
                    if (expectingUnary) {
                        // Unary minus: parse negative number
                        i++
                        val (numStr, nextIdx) = readNumberString(expr, i)
                        if (numStr.isEmpty()) {
                            throw IllegalArgumentException("Invalid negative number syntax")
                        }
                        tokens.add(Token.Number(BigDecimal("-$numStr")))
                        expectingUnary = false
                        i = nextIdx
                    } else {
                        tokens.add(Token.Operator.Subtract)
                        expectingUnary = true
                        i++
                    }
                }
                c == '*' -> {
                    if (expectingUnary) throw IllegalArgumentException("Unexpected operator '*'")
                    tokens.add(Token.Operator.Multiply)
                    expectingUnary = true
                    i++
                }
                c == '/' -> {
                    if (expectingUnary) throw IllegalArgumentException("Unexpected operator '/'")
                    tokens.add(Token.Operator.Divide)
                    expectingUnary = true
                    i++
                }
                c.isDigit() || c == '.' -> {
                    val (numStr, nextIdx) = readNumberString(expr, i)
                    try {
                        tokens.add(Token.Number(BigDecimal(numStr)))
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid number format: $numStr", e)
                    }
                    expectingUnary = false
                    i = nextIdx
                }
                else -> {
                    throw IllegalArgumentException("Unsupported character in expression: '$c'")
                }
            }
        }

        if (expectingUnary && tokens.isNotEmpty() && tokens.last() !is Token.CloseParen) {
            throw IllegalArgumentException("Expression terminates with an incomplete operator")
        }

        return tokens
    }

    private fun readNumberString(expr: String, startIdx: Int): Pair<String, Int> {
        val sb = StringBuilder()
        var dotSeen = false
        var i = startIdx

        while (i < expr.length) {
            val c = expr[i]
            if (c.isDigit()) {
                sb.append(c)
                i++
            } else if (c == '.') {
                if (dotSeen) {
                    throw IllegalArgumentException("Multiple decimal points in a single number")
                }
                dotSeen = true
                sb.append(c)
                i++
            } else {
                break
            }
        }
        return Pair(sb.toString(), i)
    }

    private fun toReversePolishNotation(tokens: List<Token>): List<Token> {
        val outputQueue = mutableListOf<Token>()
        val opStack = Stack<Token>()

        for (token in tokens) {
            when (token) {
                is Token.Number -> outputQueue.add(token)
                is Token.Operator -> {
                    while (opStack.isNotEmpty() && opStack.peek() is Token.Operator) {
                        val top = opStack.peek() as Token.Operator
                        if (top.precedence >= token.precedence) {
                            outputQueue.add(opStack.pop())
                        } else {
                            break
                        }
                    }
                    opStack.push(token)
                }
                is Token.OpenParen -> opStack.push(token)
                is Token.CloseParen -> {
                    var foundOpen = false
                    while (opStack.isNotEmpty()) {
                        val top = opStack.pop()
                        if (top is Token.OpenParen) {
                            foundOpen = true
                            break
                        } else {
                            outputQueue.add(top)
                        }
                    }
                    if (!foundOpen) {
                        throw IllegalArgumentException("Mismatched parentheses: missing '('")
                    }
                }
            }
        }

        while (opStack.isNotEmpty()) {
            val top = opStack.pop()
            if (top is Token.OpenParen || top is Token.CloseParen) {
                throw IllegalArgumentException("Mismatched parentheses")
            }
            outputQueue.add(top)
        }

        return outputQueue
    }

    private fun evaluateRpn(rpn: List<Token>): BigDecimal {
        val stack = Stack<BigDecimal>()

        for (token in rpn) {
            when (token) {
                is Token.Number -> stack.push(token.value)
                is Token.Operator -> {
                    if (stack.size < 2) {
                        throw IllegalArgumentException("Malformed expression: insufficient operands")
                    }
                    val right = stack.pop()
                    val left = stack.pop()

                    val result = when (token) {
                        is Token.Operator.Add -> left.add(right, mathContext)
                        is Token.Operator.Subtract -> left.subtract(right, mathContext)
                        is Token.Operator.Multiply -> left.multiply(right, mathContext)
                        is Token.Operator.Divide -> {
                            if (right.compareTo(BigDecimal.ZERO) == 0) {
                                throw ArithmeticException("Division by zero")
                            }
                            left.divide(right, mathContext)
                        }
                    }
                    stack.push(result)
                }
                else -> throw IllegalStateException("Unexpected token in RPN evaluation")
            }
        }

        if (stack.size != 1) {
            throw IllegalArgumentException("Malformed expression: too many operands")
        }

        return stack.pop().setScale(4, RoundingMode.HALF_EVEN).stripTrailingZeros()
    }
}
