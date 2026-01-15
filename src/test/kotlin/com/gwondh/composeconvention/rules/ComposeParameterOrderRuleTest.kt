package com.gwondh.composeconvention.rules

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ComposeParameterOrderRuleTest {

    private val rule = ComposeParameterOrderRule(Config.empty)

    // =========================================================================
    // 1. 성공 케이스 (Golden Path)
    // =========================================================================

    @Test
    fun `모든 규칙을 준수한 완벽한 순서는 통과해야 한다`() {
        val code = """
            @Composable
            fun PerfectComponent(
                id: String,                  // 1. [필수] 데이터
                onClick: () -> Unit,         // 2. [필수] 이벤트
                onLongClick: () -> Unit,     // 2. [필수] 이벤트 (여러 개 가능)
                modifier: Modifier = Modifier, // 3. [Modifier] (선택 그룹의 1등)
                color: Color = Color.Red,    // 4. [선택] 데이터
                onDismiss: () -> Unit = {},  // 5. [선택] 이벤트
                content: @Composable () -> Unit = {} // 6. [선택] 슬롯
            ) {}
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(0, findings.size)
    }

    @Test
    fun `마지막 파라미터가 필수 Composable Slot이면 선택 인자 뒤에 와도 된다 (Trailing Lambda)`() {
        val code = """
            @Composable
            fun SlotComponent(
                text: String = "",           // [선택] 데이터
                content: @Composable () -> Unit // [필수] 슬롯 (하지만 맨 뒤라 허용됨)
            ) {}
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(0, findings.size)
    }

    // =========================================================================
    // 2. Modifier 관련 위반 케이스
    // =========================================================================

    @Test
    fun `Modifier에 기본값이 없으면 에러여야 한다`() {
        val code = """
            @Composable
            fun NoDefaultModifier(
                modifier: Modifier // 기본값 없음!
            ) {}
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("기본값"))
    }

    @Test
    fun `Modifier가 다른 선택 파라미터보다 뒤에 오면 에러여야 한다`() {
        val code = """
            @Composable
            fun WrongModifierOrder(
                color: Color = Red,            // 선택 데이터
                modifier: Modifier = Modifier  // Modifier가 선택 데이터 뒤에 옴 (위반)
            ) {}
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("'modifier'는 선택적 파라미터 그룹의 가장 첫 번째"))
    }

    // =========================================================================
    // 3. 타입별 순서 위반 (Data -> Event -> Slot)
    // =========================================================================

    @Test
    fun `필수 그룹에서 Event가 Data보다 먼저 오면 에러여야 한다`() {
        val code = """
            @Composable
            fun EventBeforeData(
                onClick: () -> Unit,  // [필수] 이벤트
                text: String          // [필수] 데이터 (순서 위반)
            ) {}
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
        assertTrue(findings[0].message.contains("순서 위반"))
    }

    @Test
    fun `필수 그룹에서 Slot이 Event보다 먼저 오면 에러여야 한다`() {
        val code = """
            @Composable
            fun SlotBeforeEvent(
                content: @Composable () -> Unit, // [필수] 슬롯
                onClick: () -> Unit              // [필수] 이벤트 (순서 위반)
            ) {}
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `선택 그룹에서 Event가 Data보다 먼저 오면 에러여야 한다`() {
        val code = """
            @Composable
            fun OptEventBeforeOptData(
                modifier: Modifier = Modifier,
                onClick: () -> Unit = {}, // [선택] 이벤트
                color: Color = Red        // [선택] 데이터 (순서 위반)
            ) {}
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
    }

    // =========================================================================
    // 4. 필수 vs 선택 순서 위반
    // =========================================================================

    @Test
    fun `필수 파라미터가 선택 파라미터 뒤에 오면 에러여야 한다`() {
        val code = """
            @Composable
            fun RequiredAfterOptional(
                a: Int = 1, // [선택]
                b: Int      // [필수] (위반)
            ) {}
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `필수 이벤트가 선택 데이터 뒤에 오면 에러여야 한다 (Trailing Lambda 아님)`() {
        // onClick은 @Composable이 아니므로 Trailing Lambda 예외 대상이 아님
        val code = """
            @Composable
            fun RequiredEventAfterOptional(
                text: String = "",   // [선택]
                onClick: () -> Unit  // [필수] 이벤트 (위반! 맨 뒤여도 이벤트는 안 봐줌)
            ) {}
        """.trimIndent()

        val findings = rule.compileAndLint(code)
        assertEquals(1, findings.size)
    }

    // =========================================================================
    // 5. 무시 조건 (Ignored Cases)
    // =========================================================================

    @Test
    fun `Composable 어노테이션이 없으면 검사하지 않아야 한다`() {
        val code = """
            fun NormalFunction(
                a: Int = 1,
                b: Int // Composable이 아니면 코틀린 문법상 에러일 뿐, Lint가 잡을 건 아님 (또는 정상일 수 있음)
            ) {}
        """.trimIndent()

        // Composable이 아니면 룰이 작동하지 않으므로 finding은 0이어야 함
        val findings = rule.compileAndLint(code)
        assertEquals(0, findings.size)
    }
}