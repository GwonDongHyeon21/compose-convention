package com.gwondh.composeconvention.rules

import com.gwondh.composeconvention.annotation.isIgnored
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
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

class ComposePreviewRule(config: Config) : Rule(config) {

    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Style,
        description = "Preview 함수 규칙을 검사합니다.",
        debt = Debt.FIVE_MINS
    )

    private var hasAnyPreview = false
    private val uiComposableList = mutableListOf<KtNamedFunction>()

    override fun visitKtFile(file: KtFile) {
        hasAnyPreview = false
        uiComposableList.clear()

        file.declarations.filterIsInstance<KtNamedFunction>().forEach { function ->
            if (function.hasAnnotation(PREVIEW)) {
                if (function.isIgnored()) return@forEach

                hasAnyPreview = true
                val previewFunctionName = function.nameAsSafeName.asString()

                // Preview는 private 함수
                if (!function.isPrivate()) {
                    report(
                        finding = CodeSmell(
                            issue = issue,
                            entity = Entity.from(function),
                            message = "Preview 함수 '$previewFunctionName'는 private이어야 합니다."
                        )
                    )
                }

                // 'Preview'로 시작하거나 끝나는 함수 이름
                if (!previewFunctionName.startsWith(PREVIEW) &&
                    !previewFunctionName.endsWith(PREVIEW)
                ) {
                    report(
                        finding = CodeSmell(
                            issue = issue,
                            entity = Entity.from(function),
                            message = "Preview 함수 '$previewFunctionName'의 이름은 'Preview'로 시작하거나 끝나야 합니다. (예: ${previewFunctionName}Preview)"
                        )
                    )
                }
            } else {
                // UI Composable 함수가 있는지 확인
                if (isUiComposable(function)) {
                    uiComposableList.add(function)
                }
            }
        }

        if (uiComposableList.isNotEmpty() && !hasAnyPreview) {
            report(
                CodeSmell(
                    issue = issue,
                    entity = Entity.from(file),
                    message = "이 파일에는 UI Composable이 존재하지만 Preview가 없습니다. (최소 1개 이상의 Preview 필요)"
                )
            )
        }

        super.visitKtFile(file)
    }
}