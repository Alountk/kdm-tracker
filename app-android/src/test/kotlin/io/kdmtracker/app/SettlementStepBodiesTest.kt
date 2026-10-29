package io.kdmtracker.app

import io.kdmtracker.app.ui.ArchiveStepCopy
import io.kdmtracker.app.ui.DeathCountStepCopy
import io.kdmtracker.app.ui.EndPhaseStepCopy
import io.kdmtracker.app.ui.GainEndeavorsStepCopy
import io.kdmtracker.app.ui.MilestonesStepCopy
import io.kdmtracker.app.ui.PrepareDepartingStepCopy
import io.kdmtracker.app.ui.SetUpStepCopy
import io.kdmtracker.app.ui.SpecialShowdownStepCopy
import io.kdmtracker.app.ui.TimelineStepCopy
import io.kdmtracker.app.ui.deathsRowSubtitle
import io.kdmtracker.app.ui.milestoneFlavor
import io.kdmtracker.core.domain.Core16Milestones
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the step bodies' verbatim design copy (the cached Stitch HTML). The checkbox rows, checklists
 * and codex quotes are static Spanish the render cannot derive from state, so they live in copy
 * objects and are checked here against the design source — the pin that fails if the copy drifts.
 * Dynamic copy (status lines, subtitles, flavor paragraphs) derives in the model tests instead.
 */
class SettlementStepBodiesTest {
    @Test
    fun theStep1CheckboxCopyMatchesTheDesign() {
        assertEquals("Hoja de Asentamiento inicializada", SetUpStepCopy.CHECKBOX_TITLE)
        // The design bolds `El Cúmulo`; the app renders the name from state in the same slot.
        assertEquals("Bautizado formalmente como El Cúmulo", SetUpStepCopy.checkboxSubtitle("El Cúmulo"))
    }

    @Test
    fun theStep1CodexQuoteIsTheDesignNarrative() {
        assertEquals(
            "\"Tras abatir al León Blanco, los supervivientes deambulan por la oscuridad perpetua, " +
                "atraídos por un suave resplandor que brilla cálido en el horizonte.\"",
            SetUpStepCopy.CODEX_QUOTE,
        )
    }

    @Test
    fun theStep3TokenTrayHintMatchesTheDesign() {
        assertEquals("Toca una ficha para alternar su consumo", GainEndeavorsStepCopy.HINT)
    }

    @Test
    fun theStep4StoryCopyMatchesTheDesign() {
        assertEquals("Primer Día (First Day)", TimelineStepCopy.STORY_TITLE)
        assertEquals(
            "Los supervivientes contemplan el brillo y descubren la palabra hablada.",
            TimelineStepCopy.STORY_SUBTITLE,
        )
        assertEquals("Obligatorio", TimelineStepCopy.STORY_CHIP)
        assertEquals("Próximo año: Aparición de Carroñeros", TimelineStepCopy.NEXT_YEAR)
    }

    @Test
    fun theStep4ChecklistRowsMatchTheDesign() {
        assertEquals("Añadir innovación: ", TimelineStepCopy.CHECK_INNOVATION_PREFIX)
        assertEquals("Lenguaje", TimelineStepCopy.CHECK_INNOVATION_HIGHLIGHT)
        assertEquals("Añadir localización: ", TimelineStepCopy.CHECK_LOCATION_PREFIX)
        assertEquals("Cúmulo de Linternas", TimelineStepCopy.CHECK_LOCATION_HIGHLIGHT)
        assertEquals("Crear mazo de Innovaciones del Asentamiento", TimelineStepCopy.CHECK_DECK_ROW)
    }

    @Test
    fun theStep5DeathRowCopyMatchesTheDesign() {
        assertEquals("Bajas en la Primera Historia", DeathCountStepCopy.TITLE)
        assertEquals(
            "Ningún superviviente pereció ante el León Blanco.",
            deathsRowSubtitle(0),
        )
        assertEquals(
            "Marca una casilla vacía por cada caído en combate o por eventos de historia " +
                "durante la partida.",
            DeathCountStepCopy.NOTE,
        )
    }

    @Test
    fun theStep6MilestoneFlavorMatchesTheDesign() {
        assertEquals(
            "Se activa automáticamente cuando se registre la primera muerte en el " +
                "asentamiento. El grupo deberá elegir una filosofía funeraria.",
            milestoneFlavor(Core16Milestones.PRINCIPLE_DEATH),
        )
        assertEquals(
            "Se activará cuando el primer niño nazca en el asentamiento.",
            milestoneFlavor(Core16Milestones.PRINCIPLE_SOCIETY),
        )
    }

    @Test
    fun theStep6CodexQuoteIsTheDesignNarrative() {
        assertEquals(
            "\"Después de derrotar al León Blanco, los supervivientes deambulan por la " +
                "oscuridad, atraídos por un suave resplandor que brilla cálido en el horizonte.\"",
            MilestonesStepCopy.CODEX_QUOTE,
        )
        assertEquals("Códice de Supervivencia • Prólogo", MilestonesStepCopy.CODEX_LABEL)
    }

    @Test
    fun theStep8QuarryCardCopyMatchesTheDesign() {
        assertEquals("Nivel 1", PrepareDepartingStepCopy.QUARRY_LEVEL)
        assertEquals("Bestia Blanca", PrepareDepartingStepCopy.QUARRY_KICKER)
        assertEquals("León Blanco", PrepareDepartingStepCopy.QUARRY_NAME)
        assertEquals("Prólogo / Presa Estándar", PrepareDepartingStepCopy.QUARRY_SUBTITLE)
        assertEquals("Vida / IA", PrepareDepartingStepCopy.QUARRY_STAT_LIFE_LABEL)
        assertEquals("Dureza", PrepareDepartingStepCopy.QUARRY_STAT_TOUGH_LABEL)
    }

    @Test
    fun theStep8ArmorCalloutCopyMatchesTheDesign() {
        assertEquals("Ajuste de Armaduras y Equipo", PrepareDepartingStepCopy.ARMOR_TITLE)
        assertEquals(
            "Asegúrate de que todas las bonificaciones de equipo, fichas y armadura adquiridas están " +
                "reflejadas en las 5 localizaciones de impacto de cada hoja de superviviente.",
            PrepareDepartingStepCopy.ARMOR_PARAGRAPH,
        )
        assertEquals("Cintura +1 (Tela)", PrepareDepartingStepCopy.ARMOR_CHIP_WAIST)
        assertEquals("Puntos de Supervivencia", PrepareDepartingStepCopy.ARMOR_CHIP_SURVIVAL)
    }

    @Test
    fun theStep8CampaignRuleBoxMatchesTheDesign() {
        assertEquals("Regla Oficial de Campaña", PrepareDepartingStepCopy.CAMPAIGN_RULE_LABEL)
        assertEquals(
            "\"Sáltate el paso 9 (Enfrentamiento Especial) en la primera fase de asentamiento.\"",
            PrepareDepartingStepCopy.CAMPAIGN_RULE_QUOTE,
        )
        assertEquals("Reglamento KDM pág. 35", PrepareDepartingStepCopy.CAMPAIGN_RULE_SOURCE)
    }

    @Test
    fun theStep9ExemptionCopyMatchesTheDesign() {
        assertEquals("Nemesis / Evento", SpecialShowdownStepCopy.META_CHIP)
        assertEquals("Regla de Año Linterna 1", SpecialShowdownStepCopy.LABEL)
        assertEquals("Sáltate el Paso 9", SpecialShowdownStepCopy.HEADLINE)
        assertEquals("Automático", SpecialShowdownStepCopy.CHIP)
        assertEquals(
            "Durante la primera fase de asentamiento, los supervivientes aún no conocen las bestias " +
                "némesis. Este paso queda ignorado por orden del códice.",
            SpecialShowdownStepCopy.PARAGRAPH,
        )
    }

    @Test
    fun theStep9LaterYearsBlockMatchesTheDesign() {
        assertEquals("Efecto en Años Posteriores", SpecialShowdownStepCopy.INNER_LABEL)
        // The design bolds `Paso 5`; the render splits the sentence around the highlight.
        assertEquals(
            "Tras resolver un combate especial, regresa de inmediato al Paso 5. Cura a estos " +
                "combatientes y retira fichas temporales. No computan como supervivientes que regresan.",
            SpecialShowdownStepCopy.INNER_PREFIX +
                SpecialShowdownStepCopy.INNER_HIGHLIGHT +
                SpecialShowdownStepCopy.INNER_SUFFIX,
        )
    }

    @Test
    fun theStep10StoredGearCopyMatchesTheDesign() {
        assertEquals("Cúmulo", ArchiveStepCopy.META_CHIP)
        assertEquals("Equipo Almacenado", ArchiveStepCopy.GEAR_HEADER)
        assertEquals("Añadir Ítem", ArchiveStepCopy.ADD_ITEM)
    }

    @Test
    fun theStep11ChecklistRowsMatchTheDesign() {
        assertEquals(
            listOf(
                "Limpieza de mesa completada" to
                    "Cartas de innovación recogidas y recursos no gastados asegurados.",
                "Hojas de registro actualizadas" to
                    "Equipo adquirido registrado en las fichas de los supervivientes que parten.",
                "Grupo de Caza Designado" to
                    "4 supervivientes listos con sus cuadrículas de equipo ajustadas.",
            ),
            EndPhaseStepCopy.CHECKLIST,
        )
    }

    @Test
    fun theStep11ClosingCopyMatchesTheDesign() {
        assertEquals("Cierre", EndPhaseStepCopy.META_CHIP)
        assertEquals("Fin de la Fase de Asentamiento", EndPhaseStepCopy.OVERLAY)
        // The design bolds `Primera Historia`; the render splits the note around the highlight.
        assertEquals(
            "Los jugadores han concluido la Primera Historia. La campaña avanza hacia las tinieblas " +
                "de la estepa de rostros.",
            EndPhaseStepCopy.NOTE_PREFIX + EndPhaseStepCopy.NOTE_HIGHLIGHT + EndPhaseStepCopy.NOTE_SUFFIX,
        )
    }
}
