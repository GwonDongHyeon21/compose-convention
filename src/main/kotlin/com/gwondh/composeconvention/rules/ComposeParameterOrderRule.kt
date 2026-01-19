package com.gwondh.composeconvention.rules

import com.gwondh.composeconvention.util.String.COMPOSABLE
import com.gwondh.composeconvention.util.String.MODIFIER
import com.gwondh.composeconvention.util.String.PREVIEW
import com.gwondh.composeconvention.util.isUiComposable
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.rules.hasAnnotation
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
        description = "Modifier 파라미터 규칙과 파라미터 순서 규칙을 검사합니다.",
        debt = Debt.FIVE_MINS
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (!function.hasAnnotation("Composable")) return

        val parameters = function.valueParameters

        checkModifierRules(function, parameters)
        checkParametersOrder(parameters)
    }

    // Modifier 관련 검사
    private fun checkModifierRules(function: KtNamedFunction, parameters: List<KtParameter>) {
        val modifierParameter = parameters.find { it.name == MODIFIER }

        // UI Composable 함수 Modifier 존재 검사 (Preview 함수는 제외)
        if (modifierParameter == null) {
            if (isUiComposable(function) && !function.hasAnnotation(PREVIEW)) {
                report(
                    finding = CodeSmell(
                        issue = issue,
                        entity = Entity.from(function.nameIdentifier ?: function),
                        message = "UI Composable 함수 '${function.name}'는 '$MODIFIER' 파라미터를 필수로 가져야 합니다."
                    )
                )
            }
            return
        }

        // Modifier 기본값 검사
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

        // Modifier 위치 검사
        val firstOptionalParameter = parameters.firstOrNull { it.hasDefaultValue() }
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
    private fun checkParametersOrder(parameters: List<KtParameter>) {
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
}