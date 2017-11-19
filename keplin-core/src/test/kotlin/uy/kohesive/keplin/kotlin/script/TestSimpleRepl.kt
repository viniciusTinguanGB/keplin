@file:Suppress("DEPRECATION")

package uy.kohesive.keplin.kotlin.script

import org.jetbrains.kotlin.cli.common.repl.LineId
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class TestSimpleRepl {
    @Test
    fun testBasicScript() {
        SimplifiedRepl().use { repl ->
            val line1 = repl.nextCodeLine("val x = 10")
            val checkResult1 = repl.check(line1)

            assertEquals(LineId(line1), checkResult1.codeLine)
            assertTrue(checkResult1.isComplete)

            val compileResult1 = repl.compile(line1)

            assertEquals(LineId(line1), compileResult1.codeLine)
            assertFalse(compileResult1.compilerData.hasResult)

            val evalResult1 = repl.eval(compileResult1)

            assertEquals(LineId(line1), evalResult1.codeLine)
            assertEquals(Unit, evalResult1.resultValue)
        }
    }

    @Test
    fun testAtomicCompileAndEval() {
        SimplifiedRepl().use { repl ->
            repl.compileAndEval(repl.nextCodeLine("val x = 10"))
            val evalResult = repl.compileAndEval("x")
            assertEquals(10, evalResult.resultValue)
        }
    }

    @Test
    fun testResettingHistory() {
        SimplifiedRepl().use { repl ->
            val line1 = repl.nextCodeLine("val x = 10")
            repl.eval(repl.compile(line1))

            val line2 = repl.nextCodeLine("val y = x + 10")
            repl.eval(repl.compile(line2))

            val line3 = repl.nextCodeLine("val x = 30")
            repl.eval(repl.compile(line3))

            val line4 = repl.nextCodeLine("x")
            val evalResult4 = repl.eval(repl.compile(line4))

            assertEquals(30, evalResult4.resultValue)

            val line5 = repl.nextCodeLine("println(\"value of X is \$x\")")
            repl.eval(repl.compile(line5))

            val line6 = repl.nextCodeLine("val x = 1000\nprintln(\"value of X is \$x\")")
            repl.eval(repl.compile(line6))

            try {
                val removedLines = repl.resetToLine(line2)
                assertEquals(listOf(line3, line4, line5, line6).map { LineId(it) }, removedLines)

                val newLine3 = repl.nextCodeLine("x")

                val newCompileResult3 = repl.compile(newLine3)
                val newEvalResult3 = repl.eval(newCompileResult3)

                assertEquals(10, newEvalResult3.resultValue)

                val newLine4 = repl.nextCodeLine("x + 10")
                val newCompileResult4 = repl.compile(newLine4)
                val newEvalResult4 = repl.eval(newCompileResult4)

                assertEquals(20, newEvalResult4.resultValue)

                val newLine5 = repl.nextCodeLine("val x = 99\nprintln(x)")
                repl.eval(repl.compile(newLine5))

                val newLine6 = repl.nextCodeLine("x")
                val newCompileResult6 = repl.compile(newLine6)
                val newEvalResult6 = repl.eval(newCompileResult6)

                assertEquals(99, newEvalResult6.resultValue)

                val removedNewLines = repl.resetToLine(line2)
                assertEquals(listOf(newLine3, newLine4, newLine5, newLine6).map { LineId(it) }, removedNewLines)

                val finalLine3 = repl.nextCodeLine("x")
                val finalCompileResult3 = repl.compile(finalLine3)
                val finalEvalResult3 = repl.eval(finalCompileResult3)
                assertEquals(10, finalEvalResult3.resultValue)
            } catch (ex: Exception) {
                ex.printStackTrace()
                throw ex
            }
        }
    }

    @Test
    fun testCompileAllFirstEvalAllLast() {
        SimplifiedRepl().use { repl ->
            val line1 = repl.compile(repl.nextCodeLine("""val x = 1"""))
            val line2 = repl.compile(repl.nextCodeLine("""val y = 2"""))
            val line3 = repl.compile(repl.nextCodeLine("""x+y"""))

            repl.eval(line1)
            repl.eval(line2)
            val result = repl.eval(line3)
            assertEquals(3, result.resultValue)
        }
    }

    @Test(expected = ReplException::class)
    fun testCompileInOrderThenEvalOutOfOrderError() {
        SimplifiedRepl().use { repl ->
            val line1 = repl.compile(repl.nextCodeLine("""val x = 1"""))
            val line2 = repl.compile(repl.nextCodeLine("""val y = 2"""))
            val line3 = repl.compile(repl.nextCodeLine("""x+y"""))

            repl.eval(line1)
            repl.eval(line3)
            repl.eval(line2)
        }
    }

    @Test
    fun testOneShotUnitEval() {
        SimplifiedRepl().use { repl ->
            repl.compileAndEval("""println("hello world")""")
        }
    }

    @Test
    fun testOneShotAssignEval() {
        SimplifiedRepl().use { repl ->
            repl.compileAndEval("""val x = 10""")
        }
    }

    @Test
    fun testBasicClasspath() {
        SimplifiedRepl().use { repl ->
            repl.compileAndEval("""val x = 10""")
            val classpath1 = repl.currentEvalClassPath
            repl.compileAndEval("""val y = 10""")
            val classpath2 = repl.currentEvalClassPath
            assertEquals(classpath1, classpath2)
            repl.compileAndEval("""val z = 10""")
            val classpath3 = repl.currentEvalClassPath
            assertEquals(classpath2, classpath3)
        }
    }
}

