package com.expensetracker.app.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UpdateRepositoryTest {

    @Test
    fun `version comparison identifies newer versions accurately`() {
        assertThat(UpdateRepository.isVersionNewer("1.0.1", "1.0.0")).isTrue()
        assertThat(UpdateRepository.isVersionNewer("1.1.0", "1.0.9")).isTrue()
        assertThat(UpdateRepository.isVersionNewer("2.0.0", "1.9.9")).isTrue()

        assertThat(UpdateRepository.isVersionNewer("1.0.0", "1.0.0")).isFalse()
        assertThat(UpdateRepository.isVersionNewer("0.9.0", "1.0.0")).isFalse()
        assertThat(UpdateRepository.isVersionNewer("", "1.0.0")).isFalse()
    }
}
