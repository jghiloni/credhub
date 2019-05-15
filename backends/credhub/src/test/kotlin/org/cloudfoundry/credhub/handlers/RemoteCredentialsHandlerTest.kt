package org.cloudfoundry.credhub.handlers

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteCredentialsHandlerTest {

    // TODO: remove test
    // TODO: either delete test class or make work on mac
    @Test
    fun alwaysPassingTest() {
        return
    }

//    private val CREDENTIAL_NAME = "/test/credential"
//    private val USER = "test-user"
//
//    private val userContextHolder = mock<UserContextHolder>(UserContextHolder::class.java)!!
//    private val objectMapper = ObjectMapper()
//    private var client = mock<RemoteBackendClient>(RemoteBackendClient::class.java)!!
//    private lateinit var subjectWithAcls: RemoteCredentialsHandler
//    private lateinit var uuid: String
//    private lateinit var versionCreatedAt: String
//
//    @Before
//    fun beforeEach() {
//        objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
//
//        subjectWithAcls = RemoteCredentialsHandler(
//            userContextHolder,
//            objectMapper,
//            client)
//
//        val userContext = mock(UserContext::class.java)
//        `when`(userContext.actor).thenReturn(USER)
//        `when`(userContextHolder.userContext).thenReturn(userContext)
//
//        uuid = UUID.randomUUID().toString()
//        versionCreatedAt = Instant.now().toString()
//    }
//
//    @Test
//    fun getCurrentCredentialVersion_withValueCredential_returnsCorrectDataReponse() {
//        val type = "value"
//        val value = "test-value"
//
//        val response = GetResponse.newBuilder().setName(CREDENTIAL_NAME)
//            .setType(type).setData(ByteString.copyFromUtf8(value))
//            .setId(uuid).setVersionCreatedAt(versionCreatedAt).build()
//        `when`(client.getByNameRequest(CREDENTIAL_NAME, USER)).thenReturn(response)
//
//        val result = subjectWithAcls.getCurrentCredentialVersions(CREDENTIAL_NAME)
//        assertEquals(result.data.size, 1)
//        assertEquals(result.data[0].type, type)
//        assertEquals(result.data[0].uuid, uuid)
//        assertEquals(result.data[0].versionCreatedAt, versionCreatedAt)
//        assertThat(result.data[0].value).isInstanceOf(StringCredentialValue::class.java)
//    }
//
//    @Test
//    fun getCurrentCredentialVersion_withJsonCredential_returnsCorrectDataResponse() {
//        val type = "json"
//        val value = """
//            {"key": "value"}
//        """.trimIndent()
//
//        val response = GetResponse.newBuilder().setName(CREDENTIAL_NAME)
//            .setType(type).setData(ByteString.copyFromUtf8(value))
//            .setId(uuid).setVersionCreatedAt(versionCreatedAt).build()
//        `when`(client.getByNameRequest(CREDENTIAL_NAME, USER)).thenReturn(response)
//
//        val result = subjectWithAcls.getCurrentCredentialVersions(CREDENTIAL_NAME)
//        assertEquals(result.data.size, 1)
//        assertEquals(result.data[0].type, type)
//        assertEquals(result.data[0].uuid, uuid)
//        assertEquals(result.data[0].versionCreatedAt, versionCreatedAt)
//        assertThat(result.data[0].value).isInstanceOf(JsonCredentialValue::class.java)
//    }
//
//    @Test
//    fun getCurrentCredentialVersion_withCertificateCredential_returnsCorrectDataResponse() {
//        val type = "certificate"
//        val value = """
//            {
//                "ca": "-----BEGIN CERTIFICATE-----\nca\n-----END CERTIFICATE-----\n",
//                "certificate": "-----BEGIN CERTIFICATE-----\ncertificate\n-----END CERTIFICATE-----\n",
//                "private_key": "-----BEGIN RSA PRIVATE KEY-----\nfake-key\n-----END RSA PRIVATE KEY-----\n"
//            }
//        """.trimIndent()
//
//        val response = GetResponse.newBuilder().setName(CREDENTIAL_NAME)
//            .setType(type).setData(ByteString.copyFromUtf8(value))
//            .setId(uuid).setVersionCreatedAt(versionCreatedAt)
//            .setId(uuid).setVersionCreatedAt(versionCreatedAt).build()
//        `when`(client.getByNameRequest(CREDENTIAL_NAME, USER)).thenReturn(response)
//
//        val result = subjectWithAcls.getCurrentCredentialVersions(CREDENTIAL_NAME)
//        assertEquals(result.data.size, 1)
//        assertEquals(result.data[0].type, type)
//        assertEquals(result.data[0].uuid, uuid)
//        assertEquals(result.data[0].versionCreatedAt, versionCreatedAt)
//        assertThat(result.data[0].value).isInstanceOf(CertificateCredentialValue::class.java)
//    }
//
//    @Test
//    fun getCurrentCredentialVersion_withPasswordCredential_returnsCorrectDataResponse() {
//        val type = "password"
//        val value = "some-password"
//
//        val response = GetResponse.newBuilder().setName(CREDENTIAL_NAME)
//            .setType(type).setData(ByteString.copyFromUtf8(value))
//            .setId(uuid).setVersionCreatedAt(versionCreatedAt).build()
//        `when`(client.getByNameRequest(CREDENTIAL_NAME, USER)).thenReturn(response)
//
//        val result = subjectWithAcls.getCurrentCredentialVersions(CREDENTIAL_NAME)
//        assertEquals(result.data.size, 1)
//        assertEquals(result.data[0].type, type)
//        assertEquals(result.data[0].uuid, uuid)
//        assertEquals(result.data[0].versionCreatedAt, versionCreatedAt)
//        assertThat(result.data[0].value).isInstanceOf(StringCredentialValue::class.java)
//    }
//
//    @Test
//    fun getCurrentCredentialVersion_withUserCredential_returnsCorrectDataResponse() {
//        val type = "user"
//        val value = """
//            {
//                "username": "some-user",
//                "password": "some-password"
//            }
//        """.trimIndent()
//
//        val response = GetResponse.newBuilder().setName(CREDENTIAL_NAME)
//            .setType(type).setData(ByteString.copyFromUtf8(value))
//            .setId(uuid).setVersionCreatedAt(versionCreatedAt).build()
//        `when`(client.getByNameRequest(CREDENTIAL_NAME, USER)).thenReturn(response)
//
//        val result = subjectWithAcls.getCurrentCredentialVersions(CREDENTIAL_NAME)
//        assertEquals(result.data.size, 1)
//        assertEquals(result.data[0].type, type)
//        assertEquals(result.data[0].uuid, uuid)
//        assertEquals(result.data[0].versionCreatedAt, versionCreatedAt)
//        assertThat(result.data[0].value).isInstanceOf(UserCredentialValue::class.java)
//    }
//
//    @Test
//    fun getCurrentCredentialVersion_withRsaCredential_returnsCorrectDataResponse() {
//        val type = "rsa"
//        val value = """
//            {
//                "public_key": "-----BEGIN PUBLIC KEY-----\npublic-key\n-----END PUBLIC KEY-----\n",
//                "private_key": "-----BEGIN RSA PRIVATE KEY-----\nfake-key\n-----END RSA PRIVATE KEY-----\n"
//            }
//        """.trimIndent()
//
//        val response = GetResponse.newBuilder().setName(CREDENTIAL_NAME)
//            .setType(type).setData(ByteString.copyFromUtf8(value))
//            .setId(uuid).setVersionCreatedAt(versionCreatedAt).build()
//        `when`(client.getByNameRequest(CREDENTIAL_NAME, USER)).thenReturn(response)
//
//        val result = subjectWithAcls.getCurrentCredentialVersions(CREDENTIAL_NAME)
//        assertEquals(result.data.size, 1)
//        assertEquals(result.data[0].type, type)
//        assertEquals(result.data[0].uuid, uuid)
//        assertEquals(result.data[0].versionCreatedAt, versionCreatedAt)
//        assertThat(result.data[0].value).isInstanceOf(RsaCredentialValue::class.java)
//    }
//
//    @Test
//    fun getCurrentCredentialVersion_withSshCredential_returnsCorrectDataResponse() {
//        val type = "ssh"
//        val value = """
//            {
//                "public_key": "-----BEGIN PUBLIC KEY-----\npublic-key\n-----END PUBLIC KEY-----\n",
//                "private_key": "-----BEGIN RSA PRIVATE KEY-----\nfake-key\n-----END RSA PRIVATE KEY-----\n"
//            }
//        """.trimIndent()
//
//        val response = GetResponse.newBuilder().setName(CREDENTIAL_NAME)
//            .setType(type).setData(ByteString.copyFromUtf8(value))
//            .setId(uuid).setVersionCreatedAt(versionCreatedAt).build()
//        `when`(client.getByNameRequest(CREDENTIAL_NAME, USER)).thenReturn(response)
//
//        val result = subjectWithAcls.getCurrentCredentialVersions(CREDENTIAL_NAME)
//        assertEquals(result.data.size, 1)
//        assertEquals(result.data[0].type, type)
//        assertEquals(result.data[0].uuid, uuid)
//        assertEquals(result.data[0].versionCreatedAt, versionCreatedAt)
//        assertThat(result.data[0].value).isInstanceOf(SshCredentialValue::class.java)
//    }
}
