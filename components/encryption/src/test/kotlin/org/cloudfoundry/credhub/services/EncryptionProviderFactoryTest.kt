package org.cloudfoundry.credhub.services

import org.cloudfoundry.credhub.config.EncryptionKeyProvider
import org.cloudfoundry.credhub.config.EncryptionKeysConfiguration
import org.cloudfoundry.credhub.config.ProviderType
import org.cloudfoundry.credhub.util.TimedRetry
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.sameInstance
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

@RunWith(JUnit4::class)
class EncryptionProviderFactoryTest {
    @Mock
    private lateinit var provider: EncryptionKeyProvider

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Throws(Exception::class)
    fun getEncryptionService_whenEncryptionServiceIsAlreadyInitialized() {
        val subject = EncryptionProviderFactory(
            mock(EncryptionKeysConfiguration::class.java),
            mock(TimedRetry::class.java),
            mock(PasswordKeyProxyFactory::class.java)
        )

        `when`(provider.providerType).thenReturn(ProviderType.INTERNAL)

        val internal = subject.getEncryptionService(provider) as InternalEncryptionService
        val internalAgain = subject.getEncryptionService(provider) as InternalEncryptionService
        assertThat(internal, sameInstance(internalAgain))
        assertThat(internal, instanceOf(PasswordEncryptionService::class.java))
    }


}
