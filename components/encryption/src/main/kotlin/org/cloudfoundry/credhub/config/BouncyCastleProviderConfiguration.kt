package org.cloudfoundry.credhub.config

import java.security.Security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

@Configuration
class BouncyCastleProviderConfiguration {
    @Bean
    fun bouncyCastleProvider(): BouncyCastleFipsProvider {
        val bouncyCastleProvider = BouncyCastleFipsProvider()
        Security.addProvider(bouncyCastleProvider)
        return bouncyCastleProvider
    }

    @Bean
    fun jcaContentSignerBuilder(jceProvider: BouncyCastleFipsProvider): JcaContentSignerBuilder {
        return JcaContentSignerBuilder("SHA256withRSA").setProvider(jceProvider)
    }

    @Bean
    fun jcaX509CertificateConverter(jceProvider: BouncyCastleFipsProvider): JcaX509CertificateConverter {
        return JcaX509CertificateConverter().setProvider(jceProvider)
    }
}
