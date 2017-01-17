package uy.kohesive.keplin.kotlin.core.scripting

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import kotlin.reflect.KClass

interface ScriptTemplateEmptyArgsProvider {
    val defaultEmptyArgs: ScriptArgsWithTypes?
}

open class KotlinScriptDefinitionEx(template: KClass<out Any>,
                                    override val defaultEmptyArgs: ScriptArgsWithTypes?,
                                    val defaultImports: List<String> = emptyList())
    : KotlinScriptDefinition(template), ScriptTemplateEmptyArgsProvider {
    class EmptyDependencies() : KotlinScriptExternalDependencies
    class DefaultImports(val defaultImports: List<String>, val base: KotlinScriptExternalDependencies) : KotlinScriptExternalDependencies by base {
        override val imports: List<String> get() = (defaultImports + base.imports).distinct()
    }

    override fun <TF : Any> getDependenciesFor(file: TF, project: Project, previousDependencies: KotlinScriptExternalDependencies?): KotlinScriptExternalDependencies? {
        val base = super.getDependenciesFor(file, project, previousDependencies)
        return if (previousDependencies == null && defaultImports.isNotEmpty()) DefaultImports(defaultImports, base ?: EmptyDependencies())
        else base
    }
}

class SimpleScriptTemplateEmptyArgsProvider(override val defaultEmptyArgs: ScriptArgsWithTypes? = null) : ScriptTemplateEmptyArgsProvider

class ScriptArgsWithTypes(val scriptArgs: Array<out Any?>, val scriptArgsTypes: Array<out KClass<out Any>>)