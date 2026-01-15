package com.gwondh.composeconvention.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter

// 파라미터 순서 정의
private enum class ParamState {
    REQUIRED_DATA,  // 필수 데이터
    REQUIRED_EVENT, // 필수 이벤트
    REQUIRED_SLOT,  // 필수 컴포저블
    OPTIONAL_DATA,  // 선택 데이터
    OPTIONAL_EVENT, // 선택 이벤트
    OPTIONAL_SLOT   // 선택 컴포저블
}

class ComposeParameterOrderRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Style,
        description = "Compose 파라미터 순서: [필수] 데이터 -> [필수] 이벤트 -> [필수] 슬롯 -> [Modifier] -> [선택] 데이터 -> [선택] 이벤트 -> [선택] 슬롯 -> 예외 슬롯",
        debt = Debt.FIVE_MINS
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.hasAnnotation("Composable")) return

        val parameters = function.valueParameters
        if (parameters.isEmpty()) return

        checkModifierOrder(function, parameters)
        checkParametersOrder(function, parameters)
    }

    // Modifier 위치 검사
    private fun checkModifierOrder(function: KtNamedFunction, parameters: List<KtParameter>) {
        val modifierParameter = parameters.find { it.name == MODIFIER } ?: return
        val firstOptionalParameter = parameters.firstOrNull { it.hasDefaultValue() }

        if (!modifierParameter.hasDefaultValue()) {
            report(
                finding = CodeSmell(
                    issue = issue,
                    entity = Entity.from(modifierParameter),
                    message = "'$MODIFIER'는 기본값을 가져야 합니다."
                )
            )
            return
        }
        if (firstOptionalParameter != null && firstOptionalParameter != modifierParameter) {
            report(
                finding = CodeSmell(
                    issue = issue,
                    entity = Entity.from(modifierParameter),
                    message = "'$MODIFIER'는 선택적 파라미터 그룹의 가장 첫 번째에 와야 합니다."
                )
            )
        }
    }

    // Modifier를 고려하지 않은 전체 파라미터 순서 검사
    private fun checkParametersOrder(function: KtNamedFunction, parameters: List<KtParameter>) {
        // 마지막이 Composable Slot인 경우에는 순서 검사에서 제외
        val lastComposableSlot = parameters.lastOrNull()?.takeIf { isComposableSlot(it) }
        val filteredParameters =
            if (lastComposableSlot != null) parameters.dropLast(1) else parameters

        var state = ParamState.REQUIRED_DATA
        for (parameter in filteredParameters) {
            val isOptional = parameter.hasDefaultValue()
            val isComposableSlot = isComposableSlot(parameter)
            val isEventSlot = isEventSlot(parameter)

            val currentState = when {
                !isOptional && !isComposableSlot && !isEventSlot -> ParamState.REQUIRED_DATA
                !isOptional && isEventSlot -> ParamState.REQUIRED_EVENT
                !isOptional -> ParamState.REQUIRED_SLOT
                isOptional && !isComposableSlot && !isEventSlot -> ParamState.OPTIONAL_DATA
                isOptional && isEventSlot -> ParamState.OPTIONAL_EVENT
                else -> ParamState.OPTIONAL_SLOT
            }

            if (currentState < state) {
                report(
                    finding = CodeSmell(
                        issue = issue,
                        entity = Entity.from(parameter),
                        message = getErrorMessage(
                            expected = state,
                            actual = currentState
                        )
                    )
                )
            }
            state = currentState
        }
    }

    private fun isComposableSlot(parameter: KtParameter): Boolean {
        val typeRef = parameter.typeReference ?: return false
        return (typeRef.typeElement is KtFunctionType) && typeRef.text.contains(COMPOSABLE)
    }

    private fun isEventSlot(parameter: KtParameter): Boolean {
        val typeRef = parameter.typeReference ?: return false
        return (typeRef.typeElement is KtFunctionType) && !typeRef.text.contains(COMPOSABLE)
    }

    private fun getErrorMessage(expected: ParamState, actual: ParamState): String {
        return "순서 위반: ${expected.name} 뒤에 ${actual.name}가 올 수 없습니다."
    }

    private fun KtNamedFunction.hasAnnotation(name: String): Boolean =
        annotationEntries.any { it.shortName?.asString() == name }

    companion object {
        const val MODIFIER = "modifier"
        const val COMPOSABLE = "@Composable"
    }
}