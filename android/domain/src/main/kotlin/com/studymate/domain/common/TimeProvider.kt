package com.studymate.domain.common

import java.time.Instant
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Nguồn thời gian, tách ra để test được logic phụ thuộc "bây giờ". */
fun interface TimeProvider {
    fun now(): Instant
}

/** Phát thời điểm hiện tại ngay lập tức, rồi lặp lại sau mỗi [period]. */
fun TimeProvider.ticks(period: Duration): Flow<Instant> = flow {
    while (true) {
        emit(now())
        delay(period)
    }
}
