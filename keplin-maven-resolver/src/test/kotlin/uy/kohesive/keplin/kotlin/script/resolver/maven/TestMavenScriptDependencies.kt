package uy.kohesive.keplin.kotlin.script.resolver.maven

import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.junit.Assert
import org.junit.Test
import uy.kohesive.keplin.kotlin.script.SimplifiedRepl
import uy.kohesive.keplin.kotlin.script.resolver.AnnotationTriggeredScriptDefinition
import uy.kohesive.keplin.kotlin.script.resolver.JarFileScriptDependenciesResolver
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import kotlin.reflect.KClass
import kotlin.script.templates.standard.ScriptTemplateWithArgs
import kotlin.test.assertEquals
import kotlin.test.fail

class TestMavenScriptDependencies {
    private val EMPTY_SCRIPT_ARGS: Array<out Any?> = arrayOf(emptyArray<String>())
    private val EMPTY_SCRIPT_ARGS_TYPES: Array<out KClass<out Any>> = arrayOf(Array<String>::class)

    fun makeEngine(): SimplifiedRepl = SimplifiedRepl(scriptDefinition = AnnotationTriggeredScriptDefinition(
            "varargTemplateWithMavenResolving",
            ScriptTemplateWithArgs::class,
            ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES),
            listOf(JarFileScriptDependenciesResolver(), MavenScriptDependenciesResolver()))
    )

    @Test
    fun testWithMavenDependencies() {
        makeEngine().use { repl ->

            repl.compileAndEval("""println("just a blank eval so default classpath items are added")""")

            val classpath1 = repl.currentEvalClassPath

            repl.compileAndEval("""
                    @file:DependsOnMaven("junit:junit:4.12")
                    import org.junit.Assert.*

                    assertTrue(true)
            """)

            val classpath2 = repl.currentEvalClassPath
            val diff = classpath2 - classpath1
            assertEquals(2, diff.size)
            Assert.assertTrue(diff.all { it.name.contains("junit-4.12.jar") || it.name.contains("hamcrest-core-1.3.jar") })

            repl.compileAndEval("""assertEquals("123", "123")""")

            val classpath3 = repl.currentEvalClassPath
            assertEquals(classpath2, classpath3)
        }
    }

    @Test
    fun testWithMavenDependencies2() {
        makeEngine().use { repl ->
            repl.compileAndEval("""
                    @file:DependsOnMaven("uy.klutter:klutter-config-typesafe-jdk6:1.20.1")
            """)
            repl.compileAndEval("""
                    import uy.klutter.config.typesafe.*

                    val refCfg = ReferenceConfig()
                    val appCfg = ApplicationConfig()
            """)
        }
    }

    @Test
    fun testResolveLibWithExtensionFunctions() {
        makeEngine().use { repl ->
            repl.compileAndEval("""@file:DependsOnMaven("uy.klutter:klutter-core-jdk6:1.20.1")""")
            repl.compileAndEval("""@file:DependsOnMaven("uy.klutter:klutter-core-jdk8:1.20.1")""")
            repl.compileAndEval("""
                                   import uy.klutter.core.jdk.*
                                   import uy.klutter.core.jdk8.*
                                """)
            val result = repl.compileAndEval("""10.minimum(100).maximum(50)""")
            repl.compileAndEval("""println(utcNow().toIsoString())""")
            assertEquals(50, result.resultValue)
        }
    }

    @Test
    fun testMavenWithAFewThreads() {
        makeEngine().use { repl ->
            val countdown = CountDownLatch(3)
            val errors = ConcurrentLinkedQueue<Exception>()
            val results = ConcurrentHashMap<String, Any?>()

            fun runScriptThread(name: String, codeLines: List<String>) {
                Thread({
                    try {
                        repl.compileAndEval("""println("Thread $name is running...")""")
                        val evalResults = codeLines.map { repl.compileAndEval(it) }
                        results.put(name, evalResults.last().resultValue)
                    } catch (ex: Exception) {
                        errors.add(ex)
                    } finally {
                        countdown.countDown()
                    }
                }, name).start()
            }

            runScriptThread("junit 1", listOf(
                    """
                        @file:DependsOnMaven("junit:junit:4.12")
                        org.junit.Assert.assertTrue(true)
                    """,
                    """org.junit.Assert.assertEquals("123", "123")"""
            ))
            runScriptThread("junit 2", listOf(
                    """@file:DependsOnMaven("junit:junit:4.12")""",
                    """org.junit.Assert.assertTrue(true)""",
                    """org.junit.Assert.assertEquals("123", "123")"""
            ))
            runScriptThread("junit 3", listOf(
                    """
                        @file:DependsOnMaven("junit:junit:4.12")
                        org.junit.Assert.assertTrue(true)
                        org.junit.Assert.assertEquals("123", "123")
                    """
            ))

            countdown.await()
            if (errors.isNotEmpty()) {
                errors.forEach { println("ERROR: $it") }
                fail("Test failed due to compiler errors during scripting")
            }
        }
    }

}
