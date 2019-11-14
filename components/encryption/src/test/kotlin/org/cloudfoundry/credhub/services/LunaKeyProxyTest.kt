package org.cloudfoundry.credhub.services

import org.assertj.core.api.Java6Assertions.fail
import org.cloudfoundry.credhub.config.EncryptionKeyMetadata
import org.cloudfoundry.credhub.entities.EncryptionKeyCanary
import org.cloudfoundry.credhub.services.EncryptionKeyCanaryMapper.CANARY_VALUE
import org.cloudfoundry.credhub.services.EncryptionKeyCanaryMapper.DEPRECATED_CANARY_VALUE
import org.cloudfoundry.credhub.utils.TestPasswordKeyProxyFactory
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import java.security.Key
import javax.crypto.AEADBadTagException

@RunWith(JUnit4::class)
class LunaKeyProxyTest {
    private lateinit var subject: LunaKeyProxy
    private lateinit var encryptionKey: Key
    private lateinit var canary: EncryptionKeyCanary
    private lateinit var deprecatedCanary: EncryptionKeyCanary

    @Before
    @Throws(Exception::class)
    fun beforeEach() {
        val encryptionService = PasswordEncryptionService(
            TestPasswordKeyProxyFactory()
        )
        val keyMetadata = EncryptionKeyMetadata()
        keyMetadata.encryptionPassword = "p@ssword"

        encryptionKey = encryptionService.createKeyProxy(keyMetadata).key
        canary = EncryptionKeyCanary()
        val encryptionData = encryptionService.encrypt(null, encryptionKey, CANARY_VALUE)
        canary.encryptedCanaryValue = encryptionData.encryptedValue
        canary.nonce = encryptionData.nonce

        deprecatedCanary = EncryptionKeyCanary()
        val deprecatedEncryptionData = encryptionService
            .encrypt(null, encryptionKey, DEPRECATED_CANARY_VALUE)
        deprecatedCanary.encryptedCanaryValue = deprecatedEncryptionData.encryptedValue
        deprecatedCanary.nonce = deprecatedEncryptionData.nonce
    }

    @Test
    @Throws(Exception::class)
    fun isMatchingCanary_whenCanaryMatches_returnsTrue() {
        subject = LunaKeyProxy(encryptionKey, PasswordEncryptionService(TestPasswordKeyProxyFactory()))

        assertThat(subject.matchesCanary(canary), equalTo(true))
    }

    @Test
    @Throws(Exception::class)
    fun isMatchingCanary_usingOldCanaryValue_returnsTrue() {
        subject = LunaKeyProxy(encryptionKey, PasswordEncryptionService(TestPasswordKeyProxyFactory()))

        assertThat(subject.matchesCanary(deprecatedCanary), equalTo(true))
    }

    @Test
    @Throws(Exception::class)
    fun isMatchingCanary_whenDecryptThrowsAEADBadTagException_returnsFalse() {
        subject = LunaKeyProxy(encryptionKey,
            object : PasswordEncryptionService(TestPasswordKeyProxyFactory()) {
                @Throws(Exception::class)
                override fun decrypt(key: Key?, encryptedValue: ByteArray?, nonce: ByteArray?): String {
                    throw AEADBadTagException()
                }
            })

        assertThat(subject.matchesCanary(mock(EncryptionKeyCanary::class.java)), equalTo(false))
    }

    @Test
    @Throws(Exception::class)
    fun isMatchingCanary_whenDecryptThrowsExceptionWithCauseIndicatingTheKeyIsIncorrect_returnsFalse() {
        subject = LunaKeyProxy(encryptionKey,
            object : PasswordEncryptionService(TestPasswordKeyProxyFactory()) {
                override fun decrypt(key: Key?, encryptedValue: ByteArray?, nonce: ByteArray?): String {
                    throw RuntimeException(RuntimeException("returns 0x40 (CKR_ENCRYPTED_DATA_INVALID)"))
                }
            })

        assertThat(subject.matchesCanary(mock(EncryptionKeyCanary::class.java)), equalTo(false))
    }

    @Test
    @Throws(Exception::class)
    fun isMatchingCanary_whenDecryptThrowsExceptionWithOtherCause_throwsRuntimeException() {
        subject = LunaKeyProxy(encryptionKey,
            object : PasswordEncryptionService(TestPasswordKeyProxyFactory()) {
                override fun decrypt(key: Key?, encryptedValue: ByteArray?, nonce: ByteArray?): String {
                    throw RuntimeException(RuntimeException("some message that isn't 0x40..."))
                }
            })

        try {
            subject.matchesCanary(mock(EncryptionKeyCanary::class.java))
            fail("Expected to get RuntimeException")
        } catch (e: RuntimeException) {
            assertThat(e.cause!!.cause!!.message, equalTo("some message that isn't 0x40..."))
        }

    }

    @Test
    @Throws(Exception::class)
    fun isMatchingCanary_WhenDecryptThrowsExceptionWithNoCause_throwsRuntimeException() {
        subject = LunaKeyProxy(encryptionKey,
            object : PasswordEncryptionService(TestPasswordKeyProxyFactory()) {
                override fun decrypt(key: Key?, encryptedValue: ByteArray?, nonce: ByteArray?): String {
                    throw RuntimeException("test message")
                }
            })

        try {
            subject.matchesCanary(mock(EncryptionKeyCanary::class.java))
            fail("Expected to get RuntimeException")
        } catch (e: RuntimeException) {
            assertThat(e.cause!!.message, equalTo("test message"))
        }

    }

    //  @Test(expected = RuntimeException.class)
    //  public void isMatchingCanary_whenDecryptThrows
}
