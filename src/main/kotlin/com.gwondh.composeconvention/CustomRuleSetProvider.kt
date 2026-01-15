package com.gwondh.composeconvention

import com.gwondh.composeconvention.rules.ComposeParameterOrderRule
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class CustomRuleSetProvider : RuleSetProvider {

    override val ruleSetId: String = "custom-rules"

    override fun instance(config: Config): RuleSet {
        return RuleSet(
            id = ruleSetId,
            rules = listOf(ComposeParameterOrderRule(config))
        )
    }
}