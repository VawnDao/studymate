package com.studymate.data.time

import com.studymate.domain.common.TimeProvider
import java.time.Instant
import javax.inject.Inject

internal class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Instant = Instant.now()
}
