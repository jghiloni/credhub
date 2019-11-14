package org.cloudfoundry.credhub.services

import com.google.common.collect.Lists.newArrayList
import org.cloudfoundry.credhub.data.EncryptedValueDataService
import org.cloudfoundry.credhub.entities.EncryptedValue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.data.domain.SliceImpl
import java.security.Key
import java.util.ArrayList
import java.util.UUID

@RunWith(JUnit4::class)
class EncryptionKeyRotatorTest {

    private lateinit var encryptedValueDataService: EncryptedValueDataService

    private lateinit var encryptedValue1: EncryptedValue
    private lateinit var encryptedValue2: EncryptedValue
    private lateinit var encryptedValue3: EncryptedValue
    private lateinit var encryptionKeyCanaryMapper: EncryptionKeyCanaryMapper
    private lateinit var oldUuid: UUID
    private lateinit var inactiveCanaries: List<UUID>
    private lateinit var keySet: EncryptionKeySet

    @Before
    fun beforeEach() {
        oldUuid = UUID.randomUUID()
        val activeUuid = UUID.randomUUID()

        encryptedValueDataService = mock(EncryptedValueDataService::class.java)
        keySet = EncryptionKeySet()
        keySet.add(EncryptionKey(mock(InternalEncryptionService::class.java), oldUuid, mock(Key::class.java), "key-name"))
        keySet.add(EncryptionKey(mock(InternalEncryptionService::class.java), activeUuid, mock(Key::class.java), "key-name"))
        keySet.setActive(activeUuid)

        encryptedValue1 = mock(EncryptedValue::class.java)
        encryptedValue2 = mock(EncryptedValue::class.java)
        encryptedValue3 = mock(EncryptedValue::class.java)

        encryptionKeyCanaryMapper = mock(EncryptionKeyCanaryMapper::class.java)
        inactiveCanaries = newArrayList(oldUuid)

        `when`(encryptedValueDataService.findByCanaryUuids(inactiveCanaries))
            .thenReturn(SliceImpl(listOf(encryptedValue1, encryptedValue2)))
            .thenReturn(SliceImpl(listOf(encryptedValue3)))
            .thenReturn(SliceImpl(ArrayList()))

        val encryptionKeyRotator = EncryptionKeyRotator(encryptedValueDataService,
            encryptionKeyCanaryMapper,
            keySet)

        encryptionKeyRotator.rotate()
    }

    @Test
    fun shouldRotateAllTheCredentialsThatWereEncryptedWithAnAvailableOldKey() {
        verify(encryptedValueDataService).rotate(encryptedValue1)
        verify(encryptedValueDataService).rotate(encryptedValue2)
        verify(encryptedValueDataService).rotate(encryptedValue3)
    }

    @Test
    fun deletesTheUnusedCanaries() {
        verify(encryptionKeyCanaryMapper).delete(inactiveCanaries)
    }

}
