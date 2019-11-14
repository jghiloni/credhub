package org.cloudfoundry.credhub.services

import org.cloudfoundry.credhub.config.EncryptionKeyMetadata
import org.cloudfoundry.credhub.utils.TestPasswordKeyProxyFactory
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PasswordEncryptionServiceTest {
    @Test
    @Throws(Exception::class)
    fun createsPasswordBasedKeyProxy() {
        val subject = PasswordEncryptionService(TestPasswordKeyProxyFactory())

        val keyMetadata = EncryptionKeyMetadata()
        keyMetadata.encryptionPassword = "foobar"

        val keyProxy = subject.createKeyProxy(keyMetadata)
        assertThat(keyProxy, instanceOf(PasswordBasedKeyProxy::class.java))
    }
}
