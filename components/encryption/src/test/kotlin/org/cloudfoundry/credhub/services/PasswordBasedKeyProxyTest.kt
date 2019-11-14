package org.cloudfoundry.credhub.services

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.ArrayUtils.toPrimitive
import org.bouncycastle.util.encoders.Hex
import org.cloudfoundry.credhub.constants.EncryptionConstants
import org.cloudfoundry.credhub.entities.EncryptionKeyCanary
import org.cloudfoundry.credhub.services.EncryptionKeyCanaryMapper.CANARY_VALUE
import org.cloudfoundry.credhub.utils.TestPasswordKeyProxyFactory
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.not
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.nio.charset.StandardCharsets.UTF_8
import java.security.Key
import java.security.SecureRandom
import java.util.Collections

@RunWith(JUnit4::class)
class PasswordBasedKeyProxyTest {
    private lateinit var subject: PasswordBasedKeyProxy
    private lateinit var password: String

    private lateinit var encryptionService: PasswordEncryptionService

    @Before
    @Throws(Exception::class)
    fun beforeEach() {
        password = "abcdefghijklmnopqrst"
        encryptionService = PasswordEncryptionService(TestPasswordKeyProxyFactory())
        subject = PasswordBasedKeyProxy(password, 1, encryptionService)
    }

    @Test
    fun deriveKey_returnstheExpectedKey() {
        val knownRandomNumber = "7034522dc85138530e44b38d0569ca67"
        val knownGeneratedKey = "09cafa70264eaa47dcf0678dfd03aa73d24044df47b0381c17ebe0ed4e2f3d91"

        val salt = Hex.decode(knownRandomNumber) // gen'dp originally from SecureRandom..

        val derivedKey = subject.deriveKey(Collections.unmodifiableList(listOf(*ArrayUtils.toObject(salt))))

        val hexOutput = Hex.toHexString(derivedKey.encoded)

        assertThat(hexOutput, equalTo(knownGeneratedKey))
        assertThat(derivedKey.encoded.size, equalTo(32))
    }

    @Test
    @Throws(Exception::class)
    fun matchesCanary_whenCanaryMatches_setsTheKey() {
        // Generate a key from the password and a new salt
        val oldProxy = PasswordBasedKeyProxy(password, 1, encryptionService)
        val derivedKey = oldProxy.deriveKey()
        val salt = oldProxy.salt

        // Create a canary whose value is encrypted with this key
        val encryptedCanaryValue = encryptionService.encrypt(null, derivedKey, CANARY_VALUE)
        val canary = EncryptionKeyCanary()
        canary.encryptedCanaryValue = encryptedCanaryValue.encryptedValue
        canary.nonce = encryptedCanaryValue.nonce
        arrayOfNulls<Byte>(salt.size)
        canary.salt = toPrimitive(salt.toTypedArray())

        val match = subject.matchesCanary(canary)
        assertTrue(match)
        assertThat(subject.key, equalTo(derivedKey))
    }

    @Test
    @Throws(Exception::class)
    fun matchesCanary_whenCanaryDoesNotMatch_doesNotAffectTheKey() {
        // Create a canary whose value cannot be decrypted by any key
        val canary = EncryptionKeyCanary()
        canary.salt = ByteArray(EncryptionConstants.SALT_SIZE)
        canary.nonce = ByteArray(EncryptionConstants.NONCE_SIZE)
        canary.encryptedCanaryValue = ByteArray(32)

        // Set some well-known but bogus key into the subject
        val bogusKey = mock(Key::class.java)
        subject.key = bogusKey
        val match = subject.matchesCanary(canary)

        assertFalse(match)
        assertThat(subject.key, equalTo(bogusKey))
    }

    @Test
    fun matchesCanary_whenCanaryDoesNotContainSalt_returnsFalse() {
        val canary = EncryptionKeyCanary()
        canary.salt = null
        assertFalse(subject.matchesCanary(canary))
    }

    @Test
    fun matchesCanary_whenCanaryHasEmptySalt_returnsFalse() {
        val canary = EncryptionKeyCanary()
        canary.salt = "".toByteArray(UTF_8)
        assertFalse(subject.matchesCanary(canary))
    }

    @Test
    fun getKey_whenNoKeyHasBeenSet_derivesNewKeyAndSalt() {
        subject = PasswordBasedKeyProxy("some password", 1, encryptionService)
        assertNull(subject.salt)
        assertNotNull(subject.key)
        assertNotNull(subject.salt)
    }

    @Test
    fun generateSalt_returnsSaltOfAtLeastSizeOfHashFunctionOutput() {
        subject = PasswordBasedKeyProxy("some password", 1, encryptionService)
        assertThat(subject.generateSalt().size, greaterThanOrEqualTo(48))
    }

    @Test
    fun generateSalt_usesCorrectSecureRandom() {
        val mockEncryptionService = mock(InternalEncryptionService::class.java)
        `when`(mockEncryptionService.secureRandom).thenReturn(SecureRandom())

        subject = PasswordBasedKeyProxy("some password", 1, mockEncryptionService)
        subject.generateSalt()

        verify(mockEncryptionService).secureRandom
    }
}
