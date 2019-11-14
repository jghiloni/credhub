package org.cloudfoundry.credhub.services

import com.nhaarman.mockito_kotlin.mock
import org.cloudfoundry.credhub.config.EncryptionKeyMetadata
import org.cloudfoundry.credhub.entities.EncryptionKeyCanary
import org.cloudfoundry.credhub.exceptions.IncorrectKeyException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import java.nio.charset.StandardCharsets.UTF_8
import javax.crypto.AEADBadTagException
import javax.crypto.IllegalBlockSizeException

@RunWith(JUnit4::class)
class ExternalKeyProxyTest {
    private lateinit var subject: ExternalKeyProxy
    private lateinit var encryptionProvider: EncryptionProvider
    private lateinit var encryptionKeyMetadata: EncryptionKeyMetadata
    private lateinit var encryptionKeyCanary: EncryptionKeyCanary

    @Before
    @Throws(Exception::class)
    fun setUp() {
        encryptionProvider = mock()
        encryptionKeyMetadata = mock()
        encryptionKeyCanary = mock()
    }

    @Test
    @Throws(Exception::class)
    fun matchesCanary_shouldReturnTrue_IfTheCanaryDecryptsToTheCanaryValue() {
        `when`(encryptionKeyCanary.encryptedCanaryValue).thenReturn("value".toByteArray(UTF_8))
        `when`(encryptionKeyCanary.nonce).thenReturn("nonce".toByteArray(UTF_8))
        `when`(encryptionKeyMetadata.encryptionKeyName).thenReturn("name")
        `when`(encryptionProvider.decrypt(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(EncryptionKeyCanaryMapper.CANARY_VALUE)

        subject = ExternalKeyProxy(encryptionKeyMetadata, encryptionProvider)
        assertTrue(subject.matchesCanary(encryptionKeyCanary))

        val argument = ArgumentCaptor.forClass(EncryptionKey::class.java)
        verify(encryptionProvider).decrypt(argument.capture(), ArgumentMatchers.eq("value".toByteArray(UTF_8)), ArgumentMatchers.eq("nonce".toByteArray(UTF_8)))
        assertEquals(encryptionProvider, argument.value.provider)
        assertEquals("name", argument.value.encryptionKeyName)
    }

    @Test
    @Throws(Exception::class)
    fun matchesCanary_shouldReturnFalse_IfTheCanaryDoesNotDecryptToTheCanaryValue() {
        `when`(encryptionProvider.decrypt(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn("garbage")

        subject = ExternalKeyProxy(encryptionKeyMetadata, encryptionProvider)
        assertFalse(subject.matchesCanary(encryptionKeyCanary))
    }

    @Test
    @Throws(Exception::class)
    fun matchesCanary_shouldReturnTrue_IfTheCanaryDecryptsToTheDeprecatedCanaryValue() {
        `when`(encryptionProvider.decrypt(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(EncryptionKeyCanaryMapper.DEPRECATED_CANARY_VALUE)

        subject = ExternalKeyProxy(encryptionKeyMetadata, encryptionProvider)
        assertTrue(subject.matchesCanary(encryptionKeyCanary))
    }

    @Test
    @Throws(Exception::class)
    fun matchesCanary_shouldReturnFalse_IfTheInternalKeyWasWrong() {
        `when`(encryptionProvider.decrypt(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenThrow(AEADBadTagException::class.java)

        subject = ExternalKeyProxy(encryptionKeyMetadata, encryptionProvider)
        assertFalse(subject.matchesCanary(encryptionKeyCanary))
    }

    @Test
    @Throws(Exception::class)
    fun matchesCanary_shouldReturnFalseIfInputDataCouldNotBeProccessed_AndC_DecryptReturns_0x40() {
        `when`(encryptionProvider.decrypt(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenThrow(IllegalBlockSizeException("returns 0x40"))

        subject = ExternalKeyProxy(encryptionKeyMetadata, encryptionProvider)
        assertFalse(subject.matchesCanary(encryptionKeyCanary))
    }

    @Test
    @Throws(Exception::class)
    fun matchesCanary_shouldThrowIncorrectKeyException_IfHSMKeyWasWrong() {
        `when`(encryptionProvider.decrypt(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenThrow(IllegalBlockSizeException("something bad happened"))

        subject = ExternalKeyProxy(encryptionKeyMetadata, encryptionProvider)
        try {
            subject.matchesCanary(encryptionKeyCanary)
            fail("Expected IncorrectKeyException, got none")
        } catch (e: IncorrectKeyException) {
        } catch (e: RuntimeException) {
            fail("Wrong exception. Expected IncorrectKeyException but got " + e.javaClass.toString())
        }

    }

    @Test
    @Throws(Exception::class)
    fun matchesCanary_shouldThrowIncorrectKeyException_IfExceptionIsThrown() {
        `when`(encryptionProvider.decrypt(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenThrow(Exception("something bad happened"))

        subject = ExternalKeyProxy(encryptionKeyMetadata, encryptionProvider)
        try {
            subject.matchesCanary(encryptionKeyCanary)
            fail("Expected IncorrectKeyException, got none")
        } catch (e: IncorrectKeyException) {
        } catch (e: RuntimeException) {
            fail("Wrong exception. Expected IncorrectKeyException but got " + e.javaClass.toString())
        }

    }
}
