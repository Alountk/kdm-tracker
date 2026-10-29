package io.kdmtracker.core.rules

import io.kdmtracker.core.domain.StepType

/**
 * The 11 official Settlement Phase steps in rulebook order.
 *
 * Content condensed from `static/prototypes/research/lantern-year-steps.md` (§2 step list,
 * §5 UX classification) and copied here as plain Spanish constants — the markdown is a
 * research artifact and is never parsed at runtime. The titles are the design's canonical
 * Spanish names so the carousel, the step frame and the catalog all agree.
 */
object SettlementStepCatalog {
    const val TOTAL = 11

    val ALL: List<SettlementStep> =
        listOf(
            SettlementStep(
                1,
                "Preparación",
                "Preparar el tablero, las hojas de registro, los mazos, las localizaciones y los principios; " +
                    "el mazo de innovaciones debe contener exactamente las consecuencias desbloqueadas.",
                StepType.BOOKKEEPING,
            ),
            SettlementStep(
                2,
                "Supervivientes Regresan",
                "Curar las heridas y limpiar los tokens de los que regresan (supervivencia y locura no se " +
                    "rellenan); luego aplicar los efectos de regreso de los principios y los eventos.",
                StepType.DECISION,
            ),
            SettlementStep(
                3,
                "Ganar Esfuerzos",
                "Cada superviviente que sobrevive y regresa de la cacería genera 1 esfuerzo (*). " +
                    "Se extinguen al concluir esta fase si no son consumidos.",
                StepType.BOOKKEEPING,
            ),
            SettlementStep(
                4,
                "Actualizar Línea de Tiempo",
                "Avanzar el año linterna, robar y resolver el evento de asentamiento; luego disparar los " +
                    "eventos de historia de este año (incluida la elección de némesis).",
                StepType.BRANCHING,
            ),
            SettlementStep(
                5,
                "Actualizar Contador de Muertes",
                "Registrar cada muerte; la primera actualización del contador dispara el evento de " +
                    "historia Principio: Muerte.",
                StepType.BRANCHING,
            ),
            SettlementStep(
                6,
                "Comprobar Hitos",
                "Marcar los hitos alcanzados en cualquier orden y resolver sus eventos de historia " +
                    "(principios, Caballero Encapuchado, Fin de la Partida).",
                StepType.BRANCHING,
            ),
            SettlementStep(
                7,
                "Desarrollo",
                "Fabricar, gastar esfuerzos, innovar y construir — libre y repetible; " +
                    "cerrar el paso antes de la partida.",
                StepType.DECISION,
            ),
            SettlementStep(
                8,
                "Preparar Supervivientes",
                "Selecciona una presa de la Lista de Presas y cuatro supervivientes con el valor " +
                    "para adentrarse en la oscuridad infinita.",
                StepType.DECISION,
            ),
            SettlementStep(
                9,
                "Enfrentamiento Especial",
                "Solo cuando la línea de tiempo lo indique: enfrentarlo y volver al paso 5 " +
                    "para actualizar el contador de muertes.",
                StepType.BRANCHING,
            ),
            SettlementStep(
                10,
                "Anotar y Almacenar",
                "Guarda cualquier recurso no invertido y equipo sin asignar en el almacén del asentamiento " +
                    "para futuras innovaciones o cacerías.",
                StepType.BOOKKEEPING,
            ),
            SettlementStep(
                11,
                "El Fin del Principio",
                "Descartar los esfuerzos no gastados y confirmar: el asentamiento pasa a la Fase de Caza.",
                StepType.BOOKKEEPING,
            ),
        )

    fun byNumber(number: Int): SettlementStep = ALL.first { it.number == number }
}
