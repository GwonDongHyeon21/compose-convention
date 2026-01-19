package com.gwondh.composeconvention.annotation

import com.gwondh.composeconvention.annotation.IgnoreRules.Companion.IGNORE_RULES
import io.gitlab.arturbosch.detekt.rules.hasAnnotation
import org.jetbrains.kotlin.psi.KtNamedFunction

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class IgnoreRules {
    companion object {
        const val IGNORE_RULES = "IgnoreRules"
    }
}

fun KtNamedFunction.isIgnored() = hasAnnotation(IGNORE_RULES)