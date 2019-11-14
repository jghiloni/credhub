package org.cloudfoundry.credhub.config

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.sha256WithRSAEncryption
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.security.KeyPairGenerator

@RunWith(SpringRunner::class)
@ContextConfiguration(classes = [BouncyCastleProviderConfiguration::class])
class BouncyCastleProviderConfigurationTest {

    @Autowired
    internal var jcaContentSignerBuilder: JcaContentSignerBuilder? = null

    private lateinit var generator: KeyPairGenerator

    @Before
    @Throws(Exception::class)
    fun beforeEach() {
        generator = KeyPairGenerator
            .getInstance("RSA", BouncyCastleFipsProvider.PROVIDER_NAME)
        generator.initialize(1024)
    }

    @Test
    @Throws(Exception::class)
    fun jcaContentSignerBuilder() {
        val key = generator.generateKeyPair().private

        val signer = jcaContentSignerBuilder!!.build(key)

        assertThat(signer.algorithmIdentifier.algorithm, equalTo(sha256WithRSAEncryption))
    }
}
