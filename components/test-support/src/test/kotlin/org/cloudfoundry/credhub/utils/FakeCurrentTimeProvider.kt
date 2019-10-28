package org.cloudfoundry.credhub.utils

import java.time.Instant
import java.time.temporal.TemporalAccessor
import java.util.Optional

import org.cloudfoundry.credhub.util.CurrentTimeProvider

class FakeCurrentTimeProvider : CurrentTimeProvider() {

    private var timeMillis: Long = 0

    override val instant: Instant
        get() = throw UnsupportedOperationException("not yet implemented")

    fun setCurrentTimeMillis(timeMillis: Long) {
        this.timeMillis = timeMillis
    }

    override fun getNow(): Optional<TemporalAccessor> {
        throw UnsupportedOperationException("not yet implemented")
    }

    override fun currentTimeMillis(): Long {
        return timeMillis
    }

    @Throws(InterruptedException::class)
    override fun sleep(sleepTimeInMillis: Long) {
        timeMillis += sleepTimeInMillis
    }
}
