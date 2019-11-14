package org.cloudfoundry.credhub.services

import com.nhaarman.mockito_kotlin.any
import org.cloudfoundry.credhub.config.EncryptionKeyMetadata
import org.cloudfoundry.credhub.util.TimedRetry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.util.function.Supplier
import javax.crypto.SecretKey

@RunWith(JUnit4::class)
class LunaEncryptionServiceTest {

    private lateinit var subject: LunaEncryptionService
    private lateinit var connection: LunaConnection
    private lateinit var aesKey: SecretKey
    private lateinit var timedRetry: TimedRetry


    @Before
    @Throws(Exception::class)
    fun setUp() {
        connection = mock(LunaConnection::class.java)
        aesKey = mock(SecretKey::class.java)
        `when`(connection.generateKey()).thenReturn(aesKey)
        `when`(connection.getKey("fake_key_name")).thenReturn(aesKey)

        timedRetry = mock(TimedRetry::class.java)
        `when`(timedRetry.retryEverySecondUntil(anyLong(), any()))
            .thenAnswer { answer ->
                val retryingOperation = answer.getArgument<Supplier<Boolean>>(1)
                for (i in 0..9) {
                    if (retryingOperation.get()) {
                        return@thenAnswer true
                    }
                }
                false
            }
    }

    @Test
    @Throws(Exception::class)
    fun createKeyProxy_createsKeyIfNoKeyExists() {
        setupNoKeyExists()

        val keyMetadata = EncryptionKeyMetadata()
        keyMetadata.encryptionKeyName = "fake_key_name"

        subject = LunaEncryptionService(connection, true, timedRetry)

        assertEquals(subject.createKeyProxy(keyMetadata).key, aesKey)
        verify(connection).setKeyEntry("fake_key_name", aesKey)
    }

    @Test
    @Throws(Exception::class)
    fun createKeyProxy_getsKeyIfKeyExists() {
        setupKeyExists()

        val keyMetadata = EncryptionKeyMetadata()
        keyMetadata.encryptionKeyName = "fake_key_name"

        subject = LunaEncryptionService(connection, true, timedRetry)

        assertEquals(subject.createKeyProxy(keyMetadata).key, aesKey)
        verify(connection, never()).setKeyEntry("fake_key_name", aesKey)
    }

    @Test
    @Throws(Exception::class)
    fun createKeyProxy_waitsForKeyIfCreationIsDisabled() {
        setupAnotherProcessCreatesKey()

        val keyMetadata = EncryptionKeyMetadata()
        keyMetadata.encryptionKeyName = "fake_key_name"

        subject = LunaEncryptionService(connection, false, timedRetry)

        assertEquals(subject.createKeyProxy(keyMetadata).key, aesKey)
        verify(connection, never()).generateKey()
    }

    @Throws(Exception::class)
    private fun setupNoKeyExists() {
        `when`(connection.containsAlias("fake_key_name")).thenReturn(false)
    }

    @Throws(Exception::class)
    private fun setupAnotherProcessCreatesKey() {
        `when`(connection.containsAlias("fake_key_name"))
            .thenReturn(false)
            .thenReturn(true)
    }

    @Throws(Exception::class)
    private fun setupKeyExists() {
        `when`(connection.containsAlias("fake_key_name")).thenReturn(true)
    }
}
