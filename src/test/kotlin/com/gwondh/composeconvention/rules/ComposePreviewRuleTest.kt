package com.gwondh.composeconvention.rules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposePreviewRuleTest {

    private val rule = ComposePreviewRule(Config.empty)

    // =========================================================================
    // 1. 성공 케이스 (Golden Path)
    // =========================================================================

    @Test
    fun `UI Composable이 있고 올바른 Private Preview가 있으면 통과해야 한다`() {
        val code = """
            @Composable
            fun MyScreen() {}

            @Composable
            @Preview
            private fun MyScreenPreview() { 
                MyScreen()
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `Preview 함수 이름이 Preview로 시작해도(Prefix) 통과해야 한다`() {
        val code = """
            @Composable
            fun MyComponent() {}

            @Composable
            @Preview
            private fun PreviewMyComponent() { 
                MyComponent()
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `UI Composable이 아닌 함수(Helper, 반환값 존재)만 있다면 Preview가 없어도 통과해야 한다`() {
        val code = """
            @Composable
            fun calculateState(): Int { // Helper Composable (Not UI)
                return 0
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(0, findings.size)
    }

    // =========================================================================
    // 2. 파일 레벨 위반 (Preview 누락)
    // =========================================================================

    @Test
    fun `UI Composable이 있는데 Preview가 하나도 없으면 파일 에러가 발생해야 한다`() {
        val code = """
            @Composable
            fun LoginScreen() { // UI Composable
                Text("Login")
            }
            // Preview 없음
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("Preview가 없습니다"))
    }

    @Test
    fun `UI Composable이 여러 개여도 Preview가 없으면 파일 에러는 1개만 발생해야 한다`() {
        val code = """
            @Composable
            fun Header() {}

            @Composable
            fun Footer() {}
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size) // 함수마다가 아니라 파일 단위 1개
        assertTrue(findings[0].message.contains("최소 1개 이상의 Preview 필요"))
    }

    // =========================================================================
    // 3. 함수 레벨 위반 (스타일 규칙)
    // =========================================================================

    @Test
    fun `Preview 함수가 private이 아니면 에러여야 한다`() {
        val code = """
            @Composable
            fun MyScreen() {}

            @Composable
            @Preview
            fun MyScreenPreview() { // Public (위반)
                MyScreen()
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("private이어야 합니다"))
    }

    @Test
    fun `Preview 함수 이름이 규칙(PreviewXX or XXPreview)을 어기면 에러여야 한다`() {
        val code = """
            @Composable
            fun MyScreen() {}

            @Composable
            @Preview
            private fun ShowMyScreen() { // 'Preview' 없음 (위반)
                MyScreen()
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("이름은 'Preview'로 시작하거나 끝나야 합니다"))
    }

    // =========================================================================
    // 4. Ignore(무시) 로직 검증 (return@forEach 동작 확인)
    // =========================================================================

    @Test
    fun `Ignore 처리된 Preview는 스타일 규칙(Public, 이름)을 어겨도 에러가 나지 않아야 한다`() {
        val code = """
            @Composable
            fun MyScreen() {}

            @Composable
            @Preview
            @IgnoreRules // 무시됨
            fun PublicAndBadName() { // Public + Bad Name 이지만 무시
                MyScreen()
            }
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
        assertTrue(findings.none { it.message.contains("private") })
        assertTrue(findings.none { it.message.contains("이름은 'Preview'") })
    }

    @Test
    fun `Ignore 처리된 Preview만 있다면, 유효한 Preview가 없는 것으로 간주되어 파일 에러가 발생해야 한다`() {
        val code = """
            @Composable
            fun MyScreen() {} // UI Composable 있음

            @Composable
            @Preview
            @IgnoreRules // 이 프리뷰는 없는 셈 침
            private fun IgnoredPreview() {}
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("Preview가 없습니다"))
    }
}