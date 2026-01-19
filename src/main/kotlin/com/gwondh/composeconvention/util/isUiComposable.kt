package com.gwondh.composeconvention.util

import com.gwondh.composeconvention.util.String.COMPOSABLE
import io.gitlab.arturbosch.detekt.rules.hasAnnotation
import org.jetbrains.kotlin.psi.KtNamedFunction

fun isUiComposable(function: KtNamedFunction): Boolean {
    if (!function.hasAnnotation(COMPOSABLE)) return false
    if (function.typeReference != null && function.typeReference?.text != "Unit") return false
    return function.nameAsSafeName.toString().firstOrNull()?.isUpperCase() == true
}