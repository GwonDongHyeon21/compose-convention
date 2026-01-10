package com.gwondh.composeconvention.rules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedFunction

class DefaultArgumentLastRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Style,
        description = "",
        debt = Debt.FIVE_MINS
    )

    override fun visitNamedFunction(function: KtNamedFunction) {}
}