package org.cloudfoundry.credhub.services

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.cloudfoundry.credhub.entities.EncryptedValue
import org.cloudfoundry.credhub.exceptions.KeyNotFoundException
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.nio.charset.StandardCharsets.UTF_8
import java.security.ProviderException
import java.util.UUID
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.crypto.IllegalBlockSizeException
import kotlin.concurrent.withLock

@RunWith(JUnit4::class)
class RetryingEncryptionServiceTest {

    private lateinit var subject: RetryingEncryptionService

    private lateinit var readLock: ReentrantReadWriteLock.ReadLock
    private lateinit var writeLock: ReentrantReadWriteLock.WriteLock
    private lateinit var encryptionService: InternalEncryptionService
    private lateinit var activeKeyUuid: UUID

    private lateinit var readWriteLock: ReentrantReadWriteLock
    private lateinit var keySet: EncryptionKeySet
    private lateinit var firstActiveKey: EncryptionKey
    private lateinit var secondActiveKey: EncryptionKey

    @Before
    fun beforeEach() {
        keySet = mock(EncryptionKeySet::class.java)
        encryptionService = mock(LunaEncryptionService::class.java)

        activeKeyUuid = UUID.randomUUID()
        firstActiveKey = mock(EncryptionKey::class.java)
        secondActiveKey = mock(EncryptionKey::class.java)

        subject = RetryingEncryptionService(keySet)

        val rwLock = ReentrantReadWriteLock()
        readLock = spy(rwLock.readLock())
        writeLock = spy(rwLock.writeLock())
        readWriteLock = spy(ReentrantReadWriteLock::class.java)
        `when`(readWriteLock.readLock()).thenReturn(readLock)
        `when`(readWriteLock.writeLock()).thenReturn(writeLock)
        subject.readWriteLock = readWriteLock
    }

    @Test
    @Throws(Exception::class)
    fun encrypt_shouldEncryptTheStringWithoutAttemptingToReconnect() {

        `when`(keySet.active)
            .thenReturn(firstActiveKey)

        val expectedEncryption = mock(EncryptedValue::class.java)
        `when`(firstActiveKey.encrypt("fake-plaintext"))
            .thenReturn(expectedEncryption)

        val encryptedValue = subject.encrypt("fake-plaintext")

        assertThat(encryptedValue, equalTo(expectedEncryption))

        verify(encryptionService, times(0))
            .reconnect(any(IllegalBlockSizeException::class.java))
        verify(keySet, times(0)).reload()
    }

    @Test
    @Throws(Exception::class)
    fun encrypt_whenThrowsAnError_retriesEncryptionFailure() {

        `when`(keySet.active)
            .thenReturn(firstActiveKey)

        `when`(firstActiveKey.encrypt("a value"))
            .thenThrow(ProviderException("function 'C_GenerateRandom' returns 0x30"))

        `when`(firstActiveKey.provider).thenReturn(encryptionService)

        try {
            subject.encrypt("a value")
            fail("Expected exception")
        } catch (e: ProviderException) {
            // expected
        }

        val inOrder = inOrder(firstActiveKey, encryptionService)
        inOrder.verify(firstActiveKey).encrypt(anyString())
        inOrder.verify(encryptionService).reconnect(any(ProviderException::class.java))
        inOrder.verify(firstActiveKey).encrypt(anyString())
    }

    @Test
    @Throws(Exception::class)
    fun encrypt_whenThrowsAnError_unlocksAfterExceptionAndLocksAgainBeforeEncrypting() {
        `when`(keySet.active)
            .thenReturn(firstActiveKey)


        `when`(firstActiveKey.encrypt("a value"))
            .thenThrow(ProviderException("function 'C_GenerateRandom' returns 0x30"))
        reset(writeLock)

        try {
            subject.encrypt("a value")
        } catch (e: ProviderException) {
            // expected
        }

        verify(readLock, times(2)).lock()
        verify(readLock, times(2)).unlock()

        verify(writeLock, times(1)).unlock()
        verify(writeLock, times(1)).lock()
    }

    @Test
    @Throws(Exception::class)
    fun encryption_whenThrowsAnError_createsNewKeysForUUIDs() {
        `when`(keySet.active)
            .thenReturn(firstActiveKey)

        `when`(firstActiveKey.encrypt(anyString()))
            .thenThrow(ProviderException("function 'C_GenerateRandom' returns 0x30"))

        `when`(firstActiveKey.provider).thenReturn(encryptionService)

        try {
            subject.encrypt("a value")
            fail("Expected exception")
        } catch (e: ProviderException) {
            // expected
        }

        verify(keySet).reload()
    }

    @Test
    @Throws(Exception::class)
    fun encryption_whenTheOperationSucceedsOnlyAfterReconnection_shouldReturnTheEncryptedString() {
        val expectedEncryption = mock(EncryptedValue::class.java)

        `when`(keySet.active)
            .thenReturn(firstActiveKey)
            .thenReturn(secondActiveKey)

        `when`(firstActiveKey.encrypt("fake-plaintext"))
            .thenThrow(IllegalBlockSizeException("test exception"))
        `when`(firstActiveKey.provider).thenReturn(encryptionService)
        `when`(secondActiveKey.encrypt("fake-plaintext"))
            .thenReturn(expectedEncryption)
        `when`(secondActiveKey.provider).thenReturn(encryptionService)

        assertThat(subject.encrypt("fake-plaintext"), equalTo(expectedEncryption))


        verify(encryptionService, times(1))
            .reconnect(any(IllegalBlockSizeException::class.java))
        verify(keySet, times(1)).reload()
    }

    @Test
    @Throws(Exception::class)
    fun encryption_encryptionLocks_acquiresALunaUsageReadLock() {
        reset(writeLock)

        `when`(keySet.active)
            .thenReturn(firstActiveKey)

        subject.encrypt("a value")
        verify(readLock, times(1)).lock()
        verify(readLock, times(1)).unlock()

        verify(writeLock, times(0)).unlock()
        verify(writeLock, times(0)).lock()
    }

    @Test
    @Throws(Exception::class)
    fun whenUsingTwoThreads_wontRetryTwice() {
        val lock = ReentrantLock()
        val firstThread = object : Thread("first") {
            override fun run() {
                try {
                    subject.encrypt("a value 1")
                } catch (e: Exception) {
                    //do nothing
                }

            }
        }
        val secondThread = object : Thread("second") {
            override fun run() {
                try {
                    subject.encrypt("a value 2")
                } catch (e: Exception) {
                    //do nothing
                }

            }
        }

        subject = RacingRetryingEncryptionServiceForTest(firstThread, secondThread, lock)
        `when`(keySet.active)
            .thenReturn(firstActiveKey)
        `when`(firstActiveKey.encrypt(anyString()))
            .thenThrow(ProviderException("function 'C_GenerateRandom' returns 0x30"))
        `when`(firstActiveKey.provider).thenReturn(encryptionService)

        firstThread.start()

        firstThread.join()
        secondThread.join()

        verify(keySet, times(1)).reload()
    }

    @Test
    @Throws(Exception::class)
    fun decrypt_shouldReturnTheDecryptedStringWithoutAttemptionToReconnect() {

        `when`(keySet.get(activeKeyUuid))
            .thenReturn(firstActiveKey)
        `when`(firstActiveKey.decrypt("fake-encrypted-value".toByteArray(UTF_8), "fake-nonce".toByteArray(UTF_8)))
            .thenReturn("fake-plaintext")


        assertThat(
            subject.decrypt(EncryptedValue(activeKeyUuid, "fake-encrypted-value".toByteArray(UTF_8), "fake-nonce".toByteArray(UTF_8))),
            equalTo("fake-plaintext"))

        verify(encryptionService, times(0)).reconnect(any(IllegalBlockSizeException::class.java))
        verify(keySet, times(0)).reload()
    }

    @Test
    @Throws(Exception::class)
    fun decrypt_whenThrowsAnError_retriesDecryptionFailure() {

        `when`(keySet.get(activeKeyUuid))
            .thenReturn(firstActiveKey)

        `when`(keySet.active)
            .thenReturn(firstActiveKey)

        `when`(firstActiveKey.decrypt(any(ByteArray::class.java), any(ByteArray::class.java)))
            .thenThrow(ProviderException("function 'C_GenerateRandom' returns 0x30"))

        `when`(firstActiveKey.provider).thenReturn(encryptionService)

        try {
            subject.decrypt(EncryptedValue(activeKeyUuid, "an encrypted value".toByteArray(UTF_8), "a nonce".toByteArray(UTF_8)))
            fail("Expected exception")
        } catch (e: ProviderException) {
            // expected
        }

        val inOrder = inOrder(firstActiveKey, encryptionService)
        inOrder.verify(firstActiveKey).decrypt(any(ByteArray::class.java), any(ByteArray::class.java))
        inOrder.verify(encryptionService).reconnect(any(ProviderException::class.java))
        inOrder.verify(firstActiveKey)
            .decrypt(any(ByteArray::class.java), any(ByteArray::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun decrypt_whenThrowsErrors_unlocksAfterExceptionAndLocksAgainBeforeEncrypting() {

        `when`(keySet.get(activeKeyUuid))
            .thenReturn(firstActiveKey)


        `when`(keySet.active)
            .thenReturn(firstActiveKey)

        `when`(firstActiveKey.decrypt(any(ByteArray::class.java), any(ByteArray::class.java)))
            .thenThrow(ProviderException("function 'C_GenerateRandom' returns 0x30"))
        reset(writeLock)

        try {
            subject.decrypt(EncryptedValue(activeKeyUuid, "an encrypted value".toByteArray(UTF_8), "a nonce".toByteArray(UTF_8)))
        } catch (e: ProviderException) {
            // expected
        }

        verify(readLock, times(2)).lock()
        verify(readLock, times(2)).unlock()

        verify(writeLock, times(1)).lock()
        verify(writeLock, times(1)).unlock()
    }

    @Test
    @Throws(Exception::class)
    fun decrypt_locksAndUnlocksTheReconnectLockWhenLoginError() {
        `when`(keySet.get(activeKeyUuid))
            .thenReturn(firstActiveKey)

        `when`(firstActiveKey.decrypt(any(ByteArray::class.java), any(ByteArray::class.java)))
            .thenThrow(ProviderException("function 'C_GenerateRandom' returns 0x30"))
        reset(writeLock)
        doThrow(RuntimeException()).`when`(encryptionService)
            .reconnect(any(Exception::class.java))

        try {
            subject.decrypt(EncryptedValue(activeKeyUuid, "an encrypted value".toByteArray(UTF_8), "a nonce".toByteArray(UTF_8)))
        } catch (e: IllegalBlockSizeException) {
            // expected
        } catch (e: RuntimeException) {
        }

        verify(readLock, times(2)).lock()
        verify(readLock, times(2)).unlock()

        verify(writeLock, times(1)).lock()
        verify(writeLock, times(1)).unlock()
    }

    @Test
    @Throws(Exception::class)
    fun decrypt_whenTheOperationSucceedsOnlyAfterReconnection() {

        `when`(keySet.get(activeKeyUuid))
            .thenReturn(firstActiveKey)
            .thenReturn(secondActiveKey)

        `when`(keySet.active)
            .thenReturn(firstActiveKey)

        `when`(firstActiveKey
            .decrypt("fake-encrypted-value".toByteArray(UTF_8), "fake-nonce".toByteArray(UTF_8)))
            .thenThrow(IllegalBlockSizeException("test exception"))
        `when`(firstActiveKey.provider).thenReturn(encryptionService)

        `when`(secondActiveKey
            .decrypt("fake-encrypted-value".toByteArray(UTF_8), "fake-nonce".toByteArray(UTF_8)))
            .thenReturn("fake-plaintext")
        `when`(secondActiveKey.provider).thenReturn(encryptionService)

        assertThat(subject
            .decrypt(EncryptedValue(activeKeyUuid, "fake-encrypted-value".toByteArray(UTF_8), "fake-nonce".toByteArray(UTF_8))),
            equalTo("fake-plaintext"))

        verify(encryptionService, times(1))
            .reconnect(any(IllegalBlockSizeException::class.java))
        verify(keySet, times(1)).reload()
    }

    @Test(expected = KeyNotFoundException::class)
    @Throws(Exception::class)
    fun decrypt_whenTheEncryptionKeyCannotBeFound_throwsAnException() {
        val fakeUuid = UUID.randomUUID()
        reset(encryptionService)
        `when`(keySet.get(fakeUuid)).thenReturn(null)
        subject.decrypt(EncryptedValue(fakeUuid, "something we cant read".toByteArray(UTF_8), "nonce".toByteArray(UTF_8)))
    }

    @Test
    @Throws(Exception::class)
    fun decryptionLocks_acquiresALunaUsageReadLock() {

        `when`(keySet.get(activeKeyUuid))
            .thenReturn(firstActiveKey)

        subject.decrypt(EncryptedValue(activeKeyUuid, "an encrypted value".toByteArray(UTF_8), "a nonce".toByteArray(UTF_8)))
        verify(readLock, times(1)).lock()
        verify(readLock, times(1)).unlock()

        verify(writeLock, times(0)).lock()
        verify(writeLock, times(0)).unlock()
    }

    @Test
    @Throws(Exception::class)
    fun usingTwoThread_wontRetryTwice() {
        val lock = ReentrantLock()
        val firstThread = object : Thread("first") {
            @SuppressFBWarnings(value = ["DE_MIGHT_IGNORE"], justification = "Exception is a no-op in this test")
            override fun run() {
                try {
                    subject.decrypt(EncryptedValue(activeKeyUuid, "a value 1".toByteArray(UTF_8), "nonce".toByteArray(UTF_8)))
                } catch (e: Exception) {
                    //do nothing
                }

            }
        }
        val secondThread = object : Thread("second") {
            @SuppressFBWarnings(value = ["DE_MIGHT_IGNORE"], justification = "Exception is a no-op in this test")
            override fun run() {
                try {
                    subject.decrypt(EncryptedValue(activeKeyUuid, "a value 2".toByteArray(UTF_8), "nonce".toByteArray(UTF_8)))
                } catch (e: Exception) {
                    //do nothing
                }

            }
        }

        subject = RacingRetryingEncryptionServiceForTest(firstThread, secondThread, lock)

        `when`(keySet.get(activeKeyUuid))
            .thenReturn(firstActiveKey)

        `when`(keySet.active)
            .thenReturn(firstActiveKey)


        `when`(firstActiveKey.decrypt(any(ByteArray::class.java), any(ByteArray::class.java)))
            .thenThrow(ProviderException("function 'C_GenerateRandom' returns 0x30"))

        `when`(firstActiveKey.provider).thenReturn(encryptionService)

        firstThread.start()

        firstThread.join()
        secondThread.join()

        verify(keySet, times(1)).reload()
    }

    private inner class RacingRetryingEncryptionServiceForTest internal constructor(
        private val firstThread: Thread, private val secondThread: Thread, private val lock: ReentrantLock) :
        RetryingEncryptionService(this@RetryingEncryptionServiceTest.keySet) {

        val condition: Condition = lock.newCondition()
        @SuppressFBWarnings(value = ["UW_UNCOND_WAIT", "WA_NOT_IN_LOOP", "REC_CATCH_EXCEPTION"], justification = "We want to force the first thread to wait, and we don't care about exceptions.")
        public override fun setNeedsReconnectFlag() {
            try {
                if (Thread.currentThread() == firstThread) {
                    secondThread.start()
                    lock.withLock {
                        condition.await() // pause the first thread
                    }
                    Thread.sleep(10) // give thread two a chance to get all the way through the retry
                } else {
                    lock.withLock {
                        condition.signal() // unpause the first thread
                    }
                }
            } catch (e: Exception) {
                //do nothing
            }

            /* give thread one a chance to set the needsRetry flag
      after thread two finishes. sets us up for reconnecting twice */
            super.setNeedsReconnectFlag()
        }
    }
}

