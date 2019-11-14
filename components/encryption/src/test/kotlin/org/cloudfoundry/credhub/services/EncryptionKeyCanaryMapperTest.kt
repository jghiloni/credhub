package org.cloudfoundry.credhub.services

import com.google.common.collect.Lists.newArrayList
import org.assertj.core.util.Lists
import org.cloudfoundry.credhub.config.EncryptionKeyMetadata
import org.cloudfoundry.credhub.config.EncryptionKeyProvider
import org.cloudfoundry.credhub.config.EncryptionKeysConfiguration
import org.cloudfoundry.credhub.config.ProviderType
import org.cloudfoundry.credhub.data.EncryptionKeyCanaryDataService
import org.cloudfoundry.credhub.entities.EncryptedValue
import org.cloudfoundry.credhub.entities.EncryptionKeyCanary
import org.cloudfoundry.credhub.services.EncryptionKeyCanaryMapper.CANARY_VALUE
import org.cloudfoundry.credhub.util.TimedRetry
import org.hamcrest.Matchers.arrayContainingInAnyOrder
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.core.IsCollectionContaining.hasItem
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.nio.charset.StandardCharsets.UTF_8
import java.security.Key
import java.util.ArrayList
import java.util.Arrays.asList
import java.util.UUID
import java.util.function.Supplier
import java.util.stream.Collectors
import javax.crypto.AEADBadTagException
import javax.crypto.IllegalBlockSizeException
import com.nhaarman.mockito_kotlin.any as any

@RunWith(JUnit4::class)
class EncryptionKeyCanaryMapperTest {

    @Rule @JvmField
    val exception: ExpectedException = ExpectedException.none()
    private lateinit var subject: EncryptionKeyCanaryMapper
    private lateinit var encryptionKeyCanaryDataService: EncryptionKeyCanaryDataService
    private lateinit var keySet: EncryptionKeySet
    private lateinit var encryptionService: InternalEncryptionService
    private lateinit var activeCanaryUuid: UUID
    private lateinit var existingCanaryUuid1: UUID
    private lateinit var existingCanaryUuid2: UUID
    private lateinit var unknownCanaryUuid: UUID
    private lateinit var activeKey: Key
    private lateinit var existingKey1: Key
    private lateinit var existingKey2: Key
    private lateinit var unknownKey: Key
    private lateinit var activeKeyProxy: KeyProxy
    private lateinit var existingKey1Proxy: KeyProxy
    private lateinit var existingKey2Proxy: KeyProxy
    private lateinit var activeKeyData: EncryptionKeyMetadata
    private lateinit var activeProvider: EncryptionKeyProvider
    private lateinit var existingKey1Data: EncryptionKeyMetadata
    private lateinit var existingKey2Data: EncryptionKeyMetadata
    private lateinit var activeKeyCanary: EncryptionKeyCanary
    private lateinit var existingKeyCanary1: EncryptionKeyCanary
    private lateinit var existingKeyCanary2: EncryptionKeyCanary
    private lateinit var unknownCanary: EncryptionKeyCanary
    private lateinit var timedRetry: TimedRetry
    private lateinit var encryptionKeysConfiguration: EncryptionKeysConfiguration
    private lateinit var providerFactory: EncryptionProviderFactory

    private fun <T> any(type: Class<T>): T = Mockito.any<T>(type)

    @Before
    @Throws(Exception::class)
    fun beforeEach() {
        encryptionKeyCanaryDataService = mock(EncryptionKeyCanaryDataService::class.java)
        encryptionService = mock(InternalEncryptionService::class.java)
        encryptionKeysConfiguration = mock(EncryptionKeysConfiguration::class.java)
        keySet = EncryptionKeySet()
        providerFactory = mock(EncryptionProviderFactory::class.java)

        activeCanaryUuid = UUID.randomUUID()
        existingCanaryUuid1 = UUID.randomUUID()
        existingCanaryUuid2 = UUID.randomUUID()
        unknownCanaryUuid = UUID.randomUUID()

        activeKeyData = EncryptionKeyMetadata()
        activeKeyData.encryptionPassword = "this-is-active"
        activeKeyData.isActive = true

        existingKey1Data = EncryptionKeyMetadata()
        existingKey1Data.encryptionPassword = "existing-key-1"
        existingKey1Data.isActive = false

        existingKey2Data = EncryptionKeyMetadata()
        existingKey2Data.encryptionPassword = "existing-key-2"
        existingKey2Data.isActive = false

        activeProvider = EncryptionKeyProvider()
        activeProvider.providerName = "int"
        activeProvider.providerType = ProviderType.INTERNAL
        activeProvider.keys = listOf(activeKeyData, existingKey1Data, existingKey2Data)

        activeKey = mock(Key::class.java, "active key")
        existingKey1 = mock(Key::class.java, "key 1")
        existingKey2 = mock(Key::class.java, "key 2")
        unknownKey = mock(Key::class.java, "key 3")
        activeKeyProxy = mock(KeyProxy::class.java)
        existingKey1Proxy = mock(KeyProxy::class.java)
        existingKey2Proxy = mock(KeyProxy::class.java)

        activeKeyCanary = createEncryptionCanary(activeCanaryUuid, "fake-active-encrypted-value",
            "fake-active-nonce", activeKey)
        existingKeyCanary1 = createEncryptionCanary(existingCanaryUuid1,
            "fake-existing-encrypted-value1", "fake-existing-nonce1", existingKey1)
        existingKeyCanary2 = createEncryptionCanary(existingCanaryUuid2,
            "fake-existing-encrypted-value2", "fake-existing-nonce2", existingKey2)
        unknownCanary = createEncryptionCanary(unknownCanaryUuid, "fake-existing-encrypted-value3",
            "fake-existing-nonce3", unknownKey)

        val providers = ArrayList<EncryptionKeyProvider>()
        val provider = EncryptionKeyProvider()
        val keys = newArrayList(
            existingKey1Data,
            activeKeyData,
            existingKey2Data
        )
        provider.keys = keys
        providers.add(provider)

        `when`(encryptionService.encrypt(null, activeKey, CANARY_VALUE))
            .thenReturn(
                EncryptedValue(null,
                    "fake-encrypted-value",
                    "fake-nonce"
                )
            )
        `when`(encryptionKeysConfiguration.providers).thenReturn(providers)
        `when`(providerFactory.getEncryptionService(activeProvider)).thenReturn(encryptionService)

        `when`(encryptionService.createKeyProxy(eq(activeKeyData))).thenReturn(activeKeyProxy)
        `when`(encryptionService.createKeyProxy(eq(existingKey1Data))).thenReturn(existingKey1Proxy)
        `when`(encryptionService.createKeyProxy(eq(existingKey2Data))).thenReturn(existingKey2Proxy)

        `when`(activeKeyProxy.matchesCanary(eq(activeKeyCanary))).thenReturn(true)
        `when`(existingKey1Proxy.matchesCanary(eq(existingKeyCanary1))).thenReturn(true)
        `when`(existingKey2Proxy.matchesCanary(eq(existingKeyCanary2))).thenReturn(true)
        `when`(activeKeyProxy.key).thenReturn(activeKey)
        `when`(existingKey1Proxy.key).thenReturn(existingKey1)
        `when`(existingKey2Proxy.key).thenReturn(existingKey2)

        `when`(encryptionKeyCanaryDataService.findAll())
            .thenReturn(ArrayList(asArrayList(existingKeyCanary1, activeKeyCanary, existingKeyCanary2)))

        timedRetry = mock(TimedRetry::class.java)
        `when`(timedRetry.retryEverySecondUntil(anyLong(), any()))
            .thenAnswer { answer ->
                val retryableOperation = answer.getArgument<Supplier<Boolean>>(1)
                for (i in 0..9) {
                    if (retryableOperation.get()) {
                        return@thenAnswer true
                    }
                }
                false
            }
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_shouldCreateTheKeys() {
        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(true)
        `when`(encryptionKeysConfiguration.providers).thenReturn(listOf(activeProvider))
        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)
        subject.mapUuidsToKeys(keySet)

        val keys = keySet.keys
        assertThat(keys.size, equalTo(3))
        assertThat(keys.stream().map { it.key }.collect(Collectors.toList()), containsInAnyOrder(
            activeKey, existingKey1, existingKey2
        ))
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_shouldContainAReferenceToActiveKey() {
        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(true)
        `when`(encryptionKeysConfiguration.providers).thenReturn(listOf(activeProvider))

        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)
        subject.mapUuidsToKeys(keySet)

        assertThat(keySet.keys, hasItem(keySet.active))
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_whenTheActiveKeyIsTheOnlyKey_andThereAreNoCanariesInTheDatabase_andKeyCreationIsEnabled_createsAndSavesACanaryToTheDatabase() {
        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(true)
        `when`(encryptionKeysConfiguration.providers!![0].keys).thenReturn(listOf(activeKeyData))
        `when`(encryptionKeysConfiguration.providers).thenReturn(listOf(activeProvider))
        val canaries = newArrayList<EncryptionKeyCanary>()
        `when`(encryptionKeyCanaryDataService.findAll()).thenReturn(canaries)

        `when`(encryptionKeyCanaryDataService.save(any(EncryptionKeyCanary::class.java)))
            .thenAnswer {
                canaries.add(activeKeyCanary)
                activeKeyCanary
            }
        `when`(encryptionService.encrypt(any(EncryptionKey::class.java), eq(CANARY_VALUE))).thenReturn(EncryptedValue(null,
            "fake-encrypted-value",
            "fake-nonce"))

        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)
        subject.mapUuidsToKeys(keySet)

        assertCanaryValueWasEncryptedAndSavedToDatabase()
        assertThat(keySet.get(activeCanaryUuid).key, equalTo(activeKey))
        assertThat(keySet.active.uuid, equalTo(activeCanaryUuid))
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_whenTheActiveKeyIsTheOnlyKey_andThereAreNoCanariesInTheDatabase_andKeyCreationIsDisabled_waitsForAnotherProcessToPutACanaryToTheDatabase() {
        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(false)
        `when`(encryptionKeysConfiguration.providers!![0].keys).thenReturn(listOf(activeKeyData))
        `when`(encryptionKeysConfiguration.providers).thenReturn(listOf(activeProvider))

        val noCanaries = newArrayList<EncryptionKeyCanary>()
        val oneCanary = Lists.newArrayList(activeKeyCanary)
        `when`(encryptionKeyCanaryDataService.findAll())
            .thenReturn(noCanaries)
            .thenReturn(noCanaries)
            .thenReturn(oneCanary)
        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)
        subject.mapUuidsToKeys(keySet)

        verify(encryptionKeyCanaryDataService, never()).save(any())
        verify(timedRetry).retryEverySecondUntil(eq(600L), any())
        assertThat(keySet.active.uuid, equalTo(activeCanaryUuid))
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_whenKeyCreationIsDisabled_AndNoKeyIsEverCreated_ThrowsAnException() {
        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(false)
        `when`(encryptionKeysConfiguration.providers!![0].keys).thenReturn(listOf(activeKeyData))
        `when`(encryptionKeysConfiguration.providers).thenReturn(listOf(activeProvider))
        `when`(encryptionKeyCanaryDataService.findAll()).thenReturn(newArrayList())
        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)

        exception.expectMessage("Timed out waiting for active key canary to be created")
        subject.mapUuidsToKeys(keySet)
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_whenTheActiveKeyIsTheOnlyKey_whenThereIsNoMatchingCanaryInTheDatabase_whenDecryptingWithTheWrongKeyRaisesAnInternalException_itShouldCreateACanaryForTheKey() {
        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(true)
        `when`(encryptionKeysConfiguration.providers!![0].keys).thenReturn(listOf(activeKeyData))
        `when`(encryptionKeysConfiguration.providers).thenReturn(listOf(activeProvider))
        val nonMatchingCanary = EncryptionKeyCanary()

        nonMatchingCanary.uuid = UUID.randomUUID()
        nonMatchingCanary.encryptedCanaryValue = "fake-non-matching-encrypted-value".toByteArray(UTF_8)
        nonMatchingCanary.nonce = "fake-non-matching-nonce".toByteArray(UTF_8)

        `when`(encryptionKeyCanaryDataService.findAll())
            .thenReturn(asArrayList(nonMatchingCanary))

        `when`(encryptionService
            .decrypt(activeKey, nonMatchingCanary.encryptedCanaryValue,
                nonMatchingCanary.nonce))
            .thenThrow(AEADBadTagException())
        `when`(encryptionKeyCanaryDataService.save(any(EncryptionKeyCanary::class.java)))
            .thenReturn(activeKeyCanary)
        `when`(encryptionService.encrypt(any(EncryptionKey::class.java), eq(CANARY_VALUE))).thenReturn(EncryptedValue(null,
            "fake-encrypted-value",
            "fake-nonce"))

        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)

        subject.mapUuidsToKeys(keySet)

        assertCanaryValueWasEncryptedAndSavedToDatabase()
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_whenTheActiveKeyIsTheOnlyKey_whenThereIsNoMatchingCanaryInTheDatabase_whenDecryptingWithTheWrongKeyRaisesAnHSMException_itShouldCreateACanaryForTheKey() {
        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(true)
        `when`(encryptionKeysConfiguration.providers!![0].keys).thenReturn(listOf(activeKeyData))
        `when`(encryptionKeysConfiguration.providers).thenReturn(listOf(activeProvider))
        val nonMatchingCanary = EncryptionKeyCanary()

        nonMatchingCanary.uuid = UUID.randomUUID()
        nonMatchingCanary.encryptedCanaryValue = "fake-non-matching-encrypted-value".toByteArray(UTF_8)
        nonMatchingCanary.nonce = "fake-non-matching-nonce".toByteArray(UTF_8)

        `when`(encryptionKeyCanaryDataService.findAll())
            .thenReturn(asArrayList(nonMatchingCanary))

        `when`(encryptionService
            .decrypt(activeKey, nonMatchingCanary.encryptedCanaryValue,
                nonMatchingCanary.nonce))
            .thenThrow(IllegalBlockSizeException(
                "Could not process input data: function 'C_Decrypt' returns 0x40"))
        `when`(encryptionKeyCanaryDataService.save(any(EncryptionKeyCanary::class.java)))
            .thenReturn(activeKeyCanary)
        `when`(encryptionService.encrypt(any(EncryptionKey::class.java), eq(CANARY_VALUE))).thenReturn(EncryptedValue(null,
            "fake-encrypted-value",
            "fake-nonce"))

        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)
        subject.mapUuidsToKeys(keySet)

        assertCanaryValueWasEncryptedAndSavedToDatabase()
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_whenThereIsNoActiveKey() {
        val keys = asList(existingKey1Data, existingKey2Data)
        activeProvider.keys = keys
        val providers = asList(activeProvider)

        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(true)
        `when`(encryptionKeysConfiguration.providers).thenReturn(providers)
        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)

        exception.expectMessage("No active key was found")
        subject.mapUuidsToKeys(keySet)
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_whenTheActiveKeyIsTheOnlyKey_whenThereIsNoMatchingCanaryInTheDatabase_whenDecryptingWithTheWrongKeyRaisesAnHSMException_throwsTheException() {
        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(true)
        `when`(encryptionKeysConfiguration.providers!![0].keys).thenReturn(listOf(activeKeyData))
        `when`(encryptionKeysConfiguration.providers).thenReturn(listOf(activeProvider))
        val nonMatchingCanary = EncryptionKeyCanary()

        nonMatchingCanary.uuid = UUID.randomUUID()
        nonMatchingCanary.encryptedCanaryValue = "fake-non-matching-encrypted-value".toByteArray(UTF_8)
        nonMatchingCanary.nonce = "fake-non-matching-nonce".toByteArray(UTF_8)

        `when`(encryptionKeyCanaryDataService.findAll())
            .thenReturn(asArrayList(nonMatchingCanary))

        `when`(activeKeyProxy.matchesCanary(nonMatchingCanary))
            .thenThrow(RuntimeException(IllegalBlockSizeException(
                "I don't know what 0x41 means and neither do you")))
        `when`(encryptionKeyCanaryDataService.save(any(EncryptionKeyCanary::class.java)))
            .thenReturn(activeKeyCanary)

        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)

        exception.expectMessage("javax.crypto.IllegalBlockSizeException: I don't know what 0x41 means and neither do you")
        subject.mapUuidsToKeys(keySet)
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_whenTheActiveKeyIsTheOnlyKey_whenThereIsNoMatchingCanaryInTheDatabase_whenDecryptingWithTheWrongKeyReturnsAnIncorrectCanaryValue_createsACanaryForTheKey() {
        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(true)
        `when`(encryptionKeysConfiguration.providers!![0].keys).thenReturn(listOf(activeKeyData))
        `when`(encryptionKeysConfiguration.providers).thenReturn(listOf(activeProvider))
        val nonMatchingCanary = EncryptionKeyCanary()

        nonMatchingCanary.uuid = UUID.randomUUID()
        nonMatchingCanary.encryptedCanaryValue = "fake-non-matching-encrypted-value".toByteArray(UTF_8)
        nonMatchingCanary.nonce = "fake-non-matching-nonce".toByteArray(UTF_8)

        `when`(encryptionKeyCanaryDataService.findAll())
            .thenReturn(asArrayList(nonMatchingCanary))

        `when`(encryptionService.decrypt(activeKey, nonMatchingCanary.encryptedCanaryValue,
            nonMatchingCanary.nonce))
            .thenReturn("different-canary-value")
        `when`(encryptionKeyCanaryDataService.save(any(EncryptionKeyCanary::class.java)))
            .thenReturn(activeKeyCanary)
        `when`(encryptionService.encrypt(any(EncryptionKey::class.java), eq(CANARY_VALUE))).thenReturn(EncryptedValue(null,
            "fake-encrypted-value",
            "fake-nonce"))

        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)

        subject.mapUuidsToKeys(keySet)

        assertCanaryValueWasEncryptedAndSavedToDatabase()
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_whenTheActiveKeyIsTheOnlyKey_whenThereIsAMatchingCanaryInTheDatabase_shouldMapTheKeyToTheMatchingCanary() {
        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(true)
        `when`(encryptionKeysConfiguration.providers!![0].keys).thenReturn(listOf(activeKeyData))
        `when`(encryptionKeysConfiguration.providers).thenReturn(listOf(activeProvider))
        `when`(encryptionKeyCanaryDataService.findAll()).thenReturn(asArrayList(activeKeyCanary))
        `when`(encryptionService
            .decrypt(activeKey, activeKeyCanary.encryptedCanaryValue,
                activeKeyCanary.nonce))
            .thenReturn(CANARY_VALUE)

        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)

        subject.mapUuidsToKeys(keySet)

        assertThat(keySet.get(activeCanaryUuid).key, equalTo(activeKey))
        verify(encryptionService, times(0))
            .encrypt(eq(activeCanaryUuid), eq(activeKey), any(String::class.java))
        assertThat(keySet.active.uuid, equalTo(activeCanaryUuid))
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_whenThereAreMultipleKeys_andMatchingCanariesForEveryKey_itShouldReturnAMapBetweenMatchingCanariesAndKeys() {

        activeProvider.keys = asList(existingKey1Data, activeKeyData, existingKey2Data)
        val providers = asList(activeProvider)

        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(true)
        `when`(encryptionKeysConfiguration.providers).thenReturn(providers)

        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)
        subject.mapUuidsToKeys(keySet)

        assertThat(keySet.get(activeCanaryUuid).key, equalTo(activeKey))
        assertThat(keySet.get(existingCanaryUuid1).key, equalTo(existingKey1))
        assertThat(keySet.get(existingCanaryUuid2).key, equalTo(existingKey2))
        assertThat<Array<Any>>(keySet.inactiveUuids.toTypedArray(),
            arrayContainingInAnyOrder(existingCanaryUuid1, existingCanaryUuid2))
        assertThat(keySet.active.uuid, equalTo(activeCanaryUuid))
    }

    @Test
    @Throws(Exception::class)
    fun mapUuidsToKeys_whenThereAreMultipleKeys_andCanariesForKeysWeDontHave_itShouldNotBeIncluded() {
        `when`(encryptionKeysConfiguration.isKeyCreationEnabled).thenReturn(true)
        `when`(encryptionKeysConfiguration.providers).thenReturn(listOf(activeProvider))
        `when`(encryptionKeyCanaryDataService.findAll())
            .thenReturn(asArrayList(unknownCanary, activeKeyCanary))

        subject = EncryptionKeyCanaryMapper(encryptionKeyCanaryDataService,
            encryptionKeysConfiguration, timedRetry, providerFactory)
        subject.mapUuidsToKeys(keySet)

        assertEquals(keySet.get(activeCanaryUuid).key, activeKey)
        assertNull(keySet.get(unknownCanaryUuid))
        assertEquals(keySet.active.uuid, activeCanaryUuid)
        assertEquals(keySet.inactiveUuids.size, 0)
    }

    private fun asArrayList(vararg canaries: EncryptionKeyCanary): List<EncryptionKeyCanary> {
        val list = ArrayList<EncryptionKeyCanary>()
        for (canary in canaries) {
            list.add(canary)
        }
        return list
    }

    @Throws(Exception::class)
    private fun assertCanaryValueWasEncryptedAndSavedToDatabase() {
        val argumentCaptor = ArgumentCaptor
            .forClass(EncryptionKeyCanary::class.java)
        verify(encryptionKeyCanaryDataService).save(argumentCaptor.capture())

        val encryptionKeyCanary = argumentCaptor.value
        assertThat(encryptionKeyCanary.encryptedCanaryValue,
            equalTo("fake-encrypted-value".toByteArray(UTF_8)))
        assertThat(encryptionKeyCanary.nonce, equalTo("fake-nonce".toByteArray(UTF_8)))
        verify(encryptionService, times(1)).encrypt(any(EncryptionKey::class.java), eq(CANARY_VALUE))
    }

    @Throws(Exception::class)
    private fun createEncryptionCanary(canaryUuid: UUID, encryptedValue: String,
                                       nonce: String, encryptionKey: Key?): EncryptionKeyCanary {
        val encryptionKeyCanary = EncryptionKeyCanary()
        encryptionKeyCanary.uuid = canaryUuid
        encryptionKeyCanary.encryptedCanaryValue = encryptedValue.toByteArray(UTF_8)
        encryptionKeyCanary.nonce = nonce.toByteArray(UTF_8)
        `when`(encryptionService.decrypt(encryptionKey, encryptedValue.toByteArray(UTF_8), nonce.toByteArray(UTF_8)))
            .thenReturn(CANARY_VALUE)
        return encryptionKeyCanary
    }
}
