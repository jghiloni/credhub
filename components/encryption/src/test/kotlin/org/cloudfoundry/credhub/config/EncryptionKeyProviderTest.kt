package org.cloudfoundry.credhub.config

import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EncryptionKeyProviderTest {

    @Test
    fun initializesWithAnEmptyButAppendableKeyList() {
        val provider = EncryptionKeyProvider()
        assertThat(provider.keys, `is`(emptyList()))
        (provider.keys as MutableList).add(EncryptionKeyMetadata())
        assertThat(provider.keys, hasSize(1))
    }
}
