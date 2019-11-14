package org.cloudfoundry.credhub.config

import org.cloudfoundry.credhub.CredhubTestApp
import org.cloudfoundry.credhub.utils.DatabaseProfileResolver
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(SpringRunner::class)
@ActiveProfiles(value = ["unit-test"], resolver = DatabaseProfileResolver::class)
@SpringBootTest(classes = [CredhubTestApp::class])
@Transactional
class EncryptionKeysConfigurationTest {

    @Autowired
    private lateinit var subject: EncryptionKeysConfiguration

    @Test
    fun fillsTheListOfKeysFromApplicationYml() {
        val keys = subject.providers!![0].keys
        assertThat(keys.size, equalTo(2))

        val firstKey = keys[0]
        val secondKey = keys[1]

        assertThat(firstKey.encryptionPassword, equalTo("opensesame"))
        assertThat(firstKey.isActive, equalTo(true))

        assertThat(secondKey.encryptionPassword, equalTo("correcthorsebatterystaple"))
        assertThat(secondKey.isActive, equalTo(false))
    }

    @Test
    fun fillsTheConfigurationObject() {
        val config = subject.providers!![0].configuration
        assertThat(config!!.host, equalTo("localhost"))
        assertThat(config.port, equalTo(50051))
    }
}
