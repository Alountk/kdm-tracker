package io.kdmtracker

import io.kdmtracker.domain.HuntMember
import io.kdmtracker.domain.HuntRecord
import io.kdmtracker.domain.HuntStatus
import io.kdmtracker.domain.Settlement
import io.kdmtracker.domain.SettlementPhase
import io.kdmtracker.domain.Survivor
import io.kdmtracker.repository.HuntMemberRepository
import io.kdmtracker.repository.HuntRecordRepository
import io.kdmtracker.repository.SettlementRepository
import io.kdmtracker.repository.SurvivorRepository
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class HuntPhaseFlowTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var settlementRepository: SettlementRepository

    @Autowired
    private lateinit var survivorRepository: SurvivorRepository

    @Autowired
    private lateinit var huntRecordRepository: HuntRecordRepository

    @Autowired
    private lateinit var huntMemberRepository: HuntMemberRepository

    @Test
    fun huntStartValidationRejectsBlankQuarryLevelAndPartyMistakes() {
        val id = newSettlement("Hunt Validation")
        val alive = (1..5).map { survivor("Val ${'A' + (it - 1)}", id).id!! }
        val fallen = survivor("Val Dead", id, alive = false).id!!

        // Blank quarry.
        postStart(id, quarry = "   ", members = listOf(alive[0]), expectedStatus = 200)
            .andExpect(content().string(containsString("Choose a quarry")))
        assertTrue(huntRecordRepository.findAllBySettlementId(id).isEmpty(), "no hunt on a rejected start")

        // Level bounds.
        postStart(id, level = 4, members = listOf(alive[0]), expectedStatus = 200)
            .andExpect(content().string(containsString("Monster level must be 1, 2 or 3.")))
        postStart(id, level = 0, members = listOf(alive[0]), expectedStatus = 200)
            .andExpect(content().string(containsString("Monster level must be 1, 2 or 3.")))

        // Party size: zero, five (the 5th member), and a member who is not alive.
        postStart(id, members = emptyList(), expectedStatus = 200)
            .andExpect(content().string(containsString("Pick at least 1 survivor for the hunt party.")))
        postStart(id, members = alive, expectedStatus = 200)
            .andExpect(content().string(containsString("A hunt party can have at most 4 survivors.")))
        postStart(id, members = listOf(fallen), expectedStatus = 200)
            .andExpect(content().string(containsString("unique, alive survivors")))

        assertTrue(huntRecordRepository.findAllBySettlementId(id).isEmpty())
        assertEquals(SettlementPhase.SETTLEMENT, phaseOf(id), "rejected starts never flip the phase")

        // One ACTIVE hunt per settlement: the second start is rejected in code.
        postStart(id, members = alive.take(2)).andExpect(status().is3xxRedirection)
        assertEquals(1, huntRecordRepository.findAllBySettlementId(id).size)
        postStart(id, quarry = "Antelope", members = alive.take(2), expectedStatus = 200)
            .andExpect(content().string(containsString("already has an active hunt")))
        assertEquals(1, huntRecordRepository.findAllBySettlementId(id).size, "still exactly one hunt")
        assertEquals(2, huntMemberRepository.findAllByHuntIdIn(listOf(activeHuntOf(id).id!!)).size)
        // The rejected start re-rendered with the error above the board (the error itself is
        // request-scoped); a fresh GET shows the untouched active hunt board again.
        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Hunting White Lion")))
            .andExpect(content().string(not(containsString("already has an active hunt"))))
    }

    @Test
    fun huntStartCreatesRecordPartyAndSwitchesPhase() {
        val id = newSettlement("Hunt Start", lanternYear = 3, currentStep = 5)
        val first = survivor("Start Ann", id).id!!
        val second = survivor("Start Bob", id).id!!

        postStart(id, quarry = "  White Lion  ", level = 2, members = listOf(first, second))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))

        val hunt = activeHuntOf(id)
        assertEquals("White Lion", hunt.quarry, "quarry is trimmed")
        assertEquals(2, hunt.level)
        assertEquals(1, hunt.currentSpace)
        assertEquals(HuntStatus.ACTIVE, hunt.status)
        assertEquals(3, hunt.lanternYear, "the hunt copies the settlement's lantern year")
        assertEquals(SettlementPhase.HUNT, phaseOf(id), "defensive: a SETTLEMENT phase flips to HUNT")
        assertEquals(5, settlementRepository.findById(id).orElseThrow().currentStep, "the step is untouched")

        val members = huntMemberRepository.findAllByHuntIdOrderBySeat(hunt.id!!)
        assertEquals(listOf(first, second), members.map { it.survivorId }, "party keeps the posted order")
        assertEquals(listOf(0, 1), members.map { it.seat })

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Hunting White Lion")))
            .andExpect(content().string(containsString("Lvl 2")))
            .andExpect(content().string(containsString("Space 1/13")))
            .andExpect(content().string(containsString("Started LY 3")))
            .andExpect(content().string(containsString("Start Ann")))
            .andExpect(content().string(containsString("Start Bob")))
            .andExpect(content().string(not(containsString("Start the hunt"))))
    }

    @Test
    fun huntBoardAdvanceAddsOneSpaceAndClampsAtThirteen() {
        val id = newSettlement("Hunt Advance")
        survivor("Adv Ann", id)
        survivor("Adv Bob", id)
        startHunt(id, members = survivorIds(id))

        advance(id)
        assertEquals(2, activeHuntOf(id).currentSpace, "advance moves exactly one space")

        setSpace(id, 12)
        advance(id)
        assertEquals(13, activeHuntOf(id).currentSpace, "12 advances to Starvation")

        advance(id)
        assertEquals(13, activeHuntOf(id).currentSpace, "space 13 is a no-op (clamped)")
    }

    @Test
    fun huntBoardMarkersAndBannersReactToTheCurrentSpace() {
        val id = newSettlement("Hunt Spaces")
        survivor("Space Ann", id)
        survivor("Space Bob", id)
        startHunt(id, members = survivorIds(id))

        setSpace(id, 6)
        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Space 7: Overwhelming Darkness")))
            .andExpect(content().string(containsString("Space 13: Starvation")))
            .andExpect(content().string(not(containsString("story event triggers on first entry"))))
            .andExpect(content().string(not(containsString("Starvation: remove 1d5 resources"))))

        setSpace(id, 7)
        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Overwhelming Darkness story event triggers on first entry")))

        setSpace(id, 13)
        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Space 13/13")))
            .andExpect(
                content()
                    .string(containsString("Starvation: remove 1d5 resources from storage")),
            ).andExpect(content().string(containsString("/settlements/$id/resources")))
    }

    @Test
    fun huntNotesSaveStaysOnTheBoard() {
        val id = newSettlement("Hunt Notes")
        survivor("Notes Ann", id)
        survivor("Notes Bob", id)
        startHunt(id, members = survivorIds(id))

        mockMvc
            .perform(post("/settlements/$id/hunt/notes").param("text", "d100 roll: 87 — a trap snaps shut"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        assertEquals("d100 roll: 87 — a trap snaps shut", activeHuntOf(id).notes)
        assertEquals(SettlementPhase.HUNT, phaseOf(id), "saving notes stays in the hunt phase")

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("d100 roll: 87 — a trap snaps shut")))
    }

    @Test
    fun foundOutcomeShowsShowdownAndResolveReturnsToSettlementWithHistory() {
        val id = newSettlement("Found Flow", currentStep = 8)
        survivor("Found Ann", id)
        survivor("Found Bob", id)
        startHunt(id, members = survivorIds(id))

        mockMvc
            .perform(post("/settlements/$id/hunt/found"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        assertEquals(SettlementPhase.SHOWDOWN, phaseOf(id))
        assertEquals(HuntStatus.QUARRY_FOUND, activeStatusOf(id))

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Showdown in progress")))
            .andExpect(content().string(containsString("White Lion · Lvl 1")))
            .andExpect(content().string(containsString("The monster acts first")))
            .andExpect(content().string(containsString("Resolve aftermath → return to settlement")))
            .andExpect(content().string(not(containsString("survivors skip their first turn"))))

        mockMvc
            .perform(post("/settlements/$id/hunt/resolve"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        val settled = settlementRepository.findById(id).orElseThrow()
        assertEquals(SettlementPhase.SETTLEMENT, settled.phase, "the aftermath opens a new settlement phase")
        assertEquals(1, settled.currentStep)
        assertEquals(HuntStatus.QUARRY_FOUND, activeStatusOf(id), "the hunt keeps its outcome in the history")

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Notes for this step")))
            .andExpect(content().string(not(containsString("Showdown in progress"))))

        // Re-entering the hunt phase shows the preserved history row with its outcome label.
        startHunt(id, quarry = "Antelope", members = survivorIds(id))
        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("LY 1 · White Lion · Lvl 1 · quarry found")))
    }

    @Test
    fun ambushOutcomeRendersTheSkipFirstTurnReminder() {
        val id = newSettlement("Ambush Flow")
        survivor("Amb Ann", id)
        survivor("Amb Bob", id)
        startHunt(id, members = survivorIds(id))

        mockMvc
            .perform(post("/settlements/$id/hunt/ambush"))
            .andExpect(status().is3xxRedirection)
        assertEquals(SettlementPhase.SHOWDOWN, phaseOf(id))
        assertEquals(HuntStatus.AMBUSH, activeStatusOf(id))

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Showdown in progress")))
            .andExpect(content().string(containsString("The monster acts first")))
            .andExpect(content().string(containsString("survivors skip their first turn")))
    }

    @Test
    fun failedAndRunAwayBeginANewSettlementPhaseAtStepOne() {
        val id = newSettlement("Failed Flow", currentStep = 6)
        survivor("Fail Ann", id)
        survivor("Fail Bob", id)
        startHunt(id, members = survivorIds(id))

        mockMvc
            .perform(post("/settlements/$id/hunt/failed"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/settlements/$id"))
        val failed = settlementRepository.findById(id).orElseThrow()
        assertEquals(SettlementPhase.SETTLEMENT, failed.phase, "hunt failed → new settlement phase")
        assertEquals(1, failed.currentStep)
        assertEquals(HuntStatus.FAILED, activeStatusOf(id))

        // The FAILED history row offers the fallen-survivors link once the hunt panel renders again.
        setPhase(id, SettlementPhase.HUNT)
        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("LY 1 · White Lion · Lvl 1 · hunt failed")))
            .andExpect(content().string(containsString("mark fallen survivors as not alive")))

        startHunt(id, members = survivorIds(id))
        mockMvc
            .perform(post("/settlements/$id/hunt/run-away"))
            .andExpect(status().is3xxRedirection)
        val runAway = settlementRepository.findById(id).orElseThrow()
        assertEquals(SettlementPhase.SETTLEMENT, runAway.phase, "run away → new settlement phase")
        assertEquals(1, runAway.currentStep)
        assertEquals(HuntStatus.RUN_AWAY, activeStatusOf(id))

        setPhase(id, SettlementPhase.HUNT)
        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("LY 1 · White Lion · Lvl 1 · ran away")))
    }

    @Test
    fun huntHistoryRendersNewestFirstWithOutcomeLabelsAndALimitOfTen() {
        val id = newSettlement("Hunt History", phase = SettlementPhase.HUNT)
        (1..12).forEach { i ->
            val status =
                when (i % 3) {
                    0 -> HuntStatus.FAILED
                    1 -> HuntStatus.QUARRY_FOUND
                    else -> HuntStatus.RUN_AWAY
                }
            huntRecordRepository.save(
                HuntRecord(
                    settlementId = id,
                    quarry = "HistQuarry%02d".format(i),
                    level = ((i - 1) % 3) + 1,
                    status = status,
                    lanternYear = i,
                ),
            )
        }
        // An ACTIVE hunt renders as the board header, never as a history row.
        huntRecordRepository.save(HuntRecord(settlementId = id, quarry = "HistActive", status = HuntStatus.ACTIVE))

        val page =
            mockMvc
                .perform(get("/settlements/$id"))
                .andExpect(status().isOk)
                .andExpect(content().string(containsString("Hunt history")))
                .andReturn()
                .response
                .contentAsString

        assertTrue(
            page.contains("LY 12 · HistQuarry12 · Lvl 3 · hunt failed"),
            "label format: LY · quarry · Lvl · outcome",
        )
        assertTrue(page.contains("LY 11 · HistQuarry11 · Lvl 2 · ran away"))
        assertTrue(
            page.indexOf("HistQuarry12") < page.indexOf("HistQuarry11"),
            "history lists the newest hunt first",
        )
        assertTrue(page.contains("HistQuarry03"), "the 10 newest rows are shown")
        assertFalse(page.contains("HistQuarry01"), "older than 10 rows are dropped")
        assertFalse(page.contains("HistQuarry02"), "older than 10 rows are dropped")
        assertFalse(page.contains("· HistActive ·"), "the ACTIVE hunt is not part of the history")
    }

    @Test
    fun settlementDeletePurgesHuntRecordsAndMembers() {
        val id = newSettlement("Doomed Hunt")
        val survivorId = survivor("Doomed Hunter", id).id!!
        val hunt = huntRecordRepository.save(HuntRecord(settlementId = id, quarry = "Doomed Lion"))
        huntMemberRepository.save(HuntMember(huntId = hunt.id!!, survivorId = survivorId, seat = 0))
        val kept = newSettlement("Kept Hunt")
        val keptHunt = huntRecordRepository.save(HuntRecord(settlementId = kept, quarry = "Kept Lion"))
        huntMemberRepository.save(HuntMember(huntId = keptHunt.id!!, survivorId = survivorId, seat = 0))

        mockMvc
            .perform(post("/settlements/$id/delete"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))

        assertTrue(huntRecordRepository.findAllBySettlementId(id).isEmpty(), "hunt records purged")
        assertTrue(
            huntMemberRepository.findAllByHuntIdIn(listOf(hunt.id!!)).isEmpty(),
            "hunt members purged",
        )
        assertEquals(1, huntRecordRepository.findAllBySettlementId(kept).size, "other settlements keep their hunts")
        assertEquals(1, huntMemberRepository.findAllByHuntIdIn(listOf(keptHunt.id!!)).size)
    }

    @Test
    fun importReplaceAllWipesTheHuntTables() {
        val id = newSettlement("Wipe Me")
        val survivorId = survivor("Wipe Hunter", id).id!!
        val hunt = huntRecordRepository.save(HuntRecord(settlementId = id, quarry = "Wipe Lion"))
        huntMemberRepository.save(HuntMember(huntId = hunt.id!!, survivorId = survivorId, seat = 0))

        // v2 file without hunts (export stays schema v2) — the replace-all must still wipe
        // both hunt tables so no orphaned rows survive the import. The payload re-seeds the
        // basics that ResourceFlowTests expects to exist after any successful import.
        val payload =
            """
            {"schemaVersion":2,"settlements":[
              {"name":"Imported Hold","phase":"SETTLEMENT","currentStep":1,
               "survivors":[],"innovations":[],"log":[],"resources":[]},
              {"name":"New Settlement","lanternYear":1,"notes":"","phase":"SETTLEMENT","currentStep":1,
               "survivors":[],"innovations":[],"log":[],
               "resources":[{"name":"Bone","category":"BASIC","quantity":0},
                            {"name":"Hide","category":"BASIC","quantity":0},
                            {"name":"Sinew","category":"BASIC","quantity":0},
                            {"name":"Amber","category":"SPECIAL","quantity":0},
                            {"name":"Cloth","category":"SPECIAL","quantity":0},
                            {"name":"Iron","category":"SPECIAL","quantity":0},
                            {"name":"Pigment","category":"SPECIAL","quantity":0},
                            {"name":"Scrap Iron","category":"SPECIAL","quantity":0}]}]}
            """.trimIndent()

        mockMvc
            .perform(
                multipart("/api/import")
                    .file(MockMultipartFile("file", "hunts.json", "application/json", payload.toByteArray())),
            ).andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/"))

        assertEquals(0L, huntRecordRepository.count(), "hunt records wiped by replace-all")
        assertEquals(0L, huntMemberRepository.count(), "hunt members wiped by replace-all")
        assertFalse(settlementRepository.findAll().any { it.name == "Wipe Me" })
    }

    @Test
    fun settlementPhaseRendersTheStepCockpitWithoutAnyHuntPanel() {
        val id = newSettlement("No Hunt Panel", currentStep = 4)
        // Even with a hunt record lying around, a SETTLEMENT phase renders the step cockpit only.
        val stray = huntRecordRepository.save(HuntRecord(settlementId = id, quarry = "Stray Lion"))

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Notes for this step")))
            .andExpect(content().string(containsString("Settlement profile")))
            .andExpect(content().string(containsString("Steps progress")))
            .andExpect(content().string(not(containsString("Start the hunt"))))
            .andExpect(content().string(not(containsString("Hunt history"))))
            .andExpect(content().string(not(containsString("Showdown in progress"))))
            .andExpect(content().string(not(containsString("hunt/advance"))))
            .andExpect(content().string(not(containsString("Resume settlement phase"))))

        huntRecordRepository.delete(stray)
    }

    @Test
    fun huntSidebarShowsTheQuarryAndWithholdsResumeDuringHunt() {
        val id = newSettlement("Hunt Sidebar", currentStep = 7, phase = SettlementPhase.HUNT)
        survivor("Side Ann", id)
        survivor("Side Bob", id)
        startHunt(id, quarry = "Phoenix", level = 3, members = survivorIds(id))

        mockMvc
            .perform(get("/settlements/$id"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("Hunt in progress")))
            .andExpect(content().string(containsString("Phoenix · Lvl 3")))
            .andExpect(content().string(containsString("7/11")))
            // Escape hatch: during HUNT the outcome buttons are the exit, so no Resume offer.
            .andExpect(content().string(not(containsString("Resume settlement phase"))))
            .andExpect(content().string(containsString("Found the quarry")))
            .andExpect(content().string(containsString("class=\"side-rail paused\"")))
    }

    // ---- helpers ----

    private fun newSettlement(
        name: String,
        phase: SettlementPhase = SettlementPhase.SETTLEMENT,
        currentStep: Int = 1,
        lanternYear: Int = 1,
    ): Long =
        settlementRepository
            .save(Settlement(name = name, phase = phase, currentStep = currentStep, lanternYear = lanternYear))
            .id!!

    private fun survivor(
        name: String,
        settlementId: Long,
        alive: Boolean = true,
    ): Survivor = survivorRepository.save(Survivor(name = name, alive = alive, settlementId = settlementId))

    private fun survivorIds(id: Long): List<Long> =
        survivorRepository
            .findAllBySettlementIdOrderByAliveDescNameAsc(id)
            .filter { it.alive }
            .mapNotNull { it.id }

    private fun postStart(
        id: Long,
        quarry: String = "White Lion",
        level: Int = 1,
        members: List<Long>,
        expectedStatus: Int = 302,
    ): ResultActions {
        val request =
            post("/settlements/$id/hunt/start")
                .param("quarry", quarry)
                .param("level", level.toString())
        members.forEach { request.param("members", it.toString()) }
        val actions = mockMvc.perform(request)
        return if (expectedStatus == 302) {
            actions.andExpect(status().is3xxRedirection).andExpect(redirectedUrl("/settlements/$id"))
        } else {
            actions.andExpect(status().isOk)
        }
    }

    private fun startHunt(
        id: Long,
        quarry: String = "White Lion",
        level: Int = 1,
        members: List<Long>,
    ) {
        require(members.isNotEmpty()) { "startHunt needs party members" }
        postStart(id, quarry = quarry, level = level, members = members)
    }

    private fun advance(id: Long) {
        mockMvc
            .perform(post("/settlements/$id/hunt/advance"))
            .andExpect(status().is3xxRedirection)
    }

    private fun setSpace(
        id: Long,
        space: Int,
    ) {
        val hunt = activeHuntOf(id)
        hunt.currentSpace = space
        huntRecordRepository.save(hunt)
    }

    private fun setPhase(
        id: Long,
        phase: SettlementPhase,
    ) {
        val settlement = settlementRepository.findById(id).orElseThrow()
        settlement.phase = phase
        settlementRepository.save(settlement)
    }

    private fun phaseOf(id: Long): SettlementPhase = settlementRepository.findById(id).orElseThrow().phase

    private fun activeHuntOf(id: Long): HuntRecord =
        huntRecordRepository.findBySettlementIdAndStatus(id, HuntStatus.ACTIVE)
            ?: error("no ACTIVE hunt for settlement $id")

    /** Outcomes are terminal, so the newest hunt row carries the status under test. */
    private fun activeStatusOf(id: Long): HuntStatus =
        huntRecordRepository.findAllBySettlementIdOrderByIdDesc(id).first().status
}
