package com.studymate.domain.srs

import com.studymate.domain.model.ReviewRating
import com.studymate.domain.model.ReviewSchedule
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sm2SchedulerTest {

    private val now: Instant = Instant.parse("2026-11-14T08:00:00Z")
    private val newCard = ReviewSchedule.newCard(now)

    @Test
    fun `new card rated good is due in one day`() {
        val next = Sm2Scheduler.next(newCard, ReviewRating.GOOD, now)

        assertEquals(1, next.reps)
        assertEquals(1, next.intervalDays)
        assertEquals(now.plus(Duration.ofDays(1)), next.dueAt)
        assertEquals(ReviewSchedule.DEFAULT_EASE, next.ease, 1e-9)
    }

    @Test
    fun `again resets reps, counts a lapse and relearns after ten minutes`() {
        val learned = newCard.copy(reps = 3, intervalDays = 8, ease = 2.5)

        val next = Sm2Scheduler.next(learned, ReviewRating.AGAIN, now)

        assertEquals(0, next.reps)
        assertEquals(0, next.intervalDays)
        assertEquals(1, next.lapses)
        assertEquals(now.plus(Sm2Scheduler.RELEARN_DELAY), next.dueAt)
        assertTrue(next.ease < learned.ease)
    }

    @Test
    fun `hard lowers ease but still schedules forward`() {
        val next = Sm2Scheduler.next(newCard, ReviewRating.HARD, now)

        assertEquals(1, next.intervalDays)
        assertEquals(2.36, next.ease, 1e-9)
    }

    @Test
    fun `three good reviews grow interval 1 then 3 then interval times ease`() {
        val first = Sm2Scheduler.next(newCard, ReviewRating.GOOD, now)
        val second = Sm2Scheduler.next(first, ReviewRating.GOOD, now)
        val third = Sm2Scheduler.next(second, ReviewRating.GOOD, now)

        assertEquals(1, first.intervalDays)
        assertEquals(3, second.intervalDays)
        assertEquals(8, third.intervalDays) // round(3 * 2.5)
    }

    @Test
    fun `easy adds a bonus over good`() {
        val good = Sm2Scheduler.next(newCard, ReviewRating.GOOD, now)
        val easy = Sm2Scheduler.next(newCard, ReviewRating.EASY, now)

        assertTrue(easy.intervalDays > good.intervalDays)
        assertTrue(easy.ease > good.ease)
    }

    @Test
    fun `ease never drops below the minimum`() {
        var schedule = newCard.copy(ease = 1.35)
        repeat(5) { schedule = Sm2Scheduler.next(schedule, ReviewRating.AGAIN, now) }

        assertEquals(Sm2Scheduler.MIN_EASE, schedule.ease, 1e-9)
    }

    @Test
    fun `preview returns one duration per rating`() {
        val preview = Sm2Scheduler.preview(newCard, now)

        assertEquals(ReviewRating.entries.toSet(), preview.keys)
        assertEquals(Duration.ofMinutes(10), preview[ReviewRating.AGAIN])
        assertEquals(Duration.ofDays(1), preview[ReviewRating.GOOD])
    }
}
