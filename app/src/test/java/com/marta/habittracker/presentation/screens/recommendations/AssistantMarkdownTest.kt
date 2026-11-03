package com.marta.habittracker.presentation.screens.recommendations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantMarkdownTest {

    @Test
    fun `splits headings bullets a rule and numbered items`() {
        val source = """
            **Observaciones:**
            * Se registra un hábito con la etiqueta genérica "string".
            * El historial muestra 0 días completados.

            ---

            **Recomendaciones:**

            1. **Definir la acción con claridad:** Reemplazar el nombre genérico.
            2. **Establecer un periodo de inicio:** Fijar una meta corta.
        """.trimIndent()

        val blocks = parseAssistantMarkdown(source)

        assertEquals(
            listOf(
                AssistantBlock.Paragraph(listOf(TextRun("Observaciones:", bold = true))),
                AssistantBlock.Bullet(
                    listOf(TextRun("Se registra un hábito con la etiqueta genérica \"string\".", bold = false)),
                ),
                AssistantBlock.Bullet(
                    listOf(TextRun("El historial muestra 0 días completados.", bold = false)),
                ),
                AssistantBlock.Rule,
                AssistantBlock.Paragraph(listOf(TextRun("Recomendaciones:", bold = true))),
                AssistantBlock.Numbered(
                    index = "1",
                    runs = listOf(
                        TextRun("Definir la acción con claridad:", bold = true),
                        TextRun(" Reemplazar el nombre genérico.", bold = false),
                    ),
                ),
                AssistantBlock.Numbered(
                    index = "2",
                    runs = listOf(
                        TextRun("Establecer un periodo de inicio:", bold = true),
                        TextRun(" Fijar una meta corta.", bold = false),
                    ),
                ),
            ),
            blocks,
        )
        assertTrue(blocks.any { it.isStructural() })
        assertEquals(
            """
            Observaciones:
            • Se registra un hábito con la etiqueta genérica "string".
            • El historial muestra 0 días completados.

            Recomendaciones:
            1. Definir la acción con claridad: Reemplazar el nombre genérico.
            2. Establecer un periodo de inicio: Fijar una meta corta.
            """.trimIndent(),
            assistantPlainText(source),
        )
    }

    @Test
    fun `keeps a plain reply as one paragraph`() {
        val blocks = parseAssistantMarkdown("Walk after lunch.")

        assertEquals(
            listOf(AssistantBlock.Paragraph(listOf(TextRun("Walk after lunch.", bold = false)))),
            blocks,
        )
    }

    @Test
    fun `turns escaped newlines into lines when the text has none`() {
        val blocks = parseAssistantMarkdown("**Hola**\\n* uno")

        assertEquals(
            listOf(
                AssistantBlock.Paragraph(listOf(TextRun("Hola", bold = true))),
                AssistantBlock.Bullet(listOf(TextRun("uno", bold = false))),
            ),
            blocks,
        )
    }
}
