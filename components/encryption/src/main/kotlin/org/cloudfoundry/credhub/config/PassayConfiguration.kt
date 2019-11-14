package org.cloudfoundry.credhub.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import org.cloudfoundry.credhub.services.RandomNumberGenerator
import org.passay.PasswordGenerator

@Configuration
class PassayConfiguration {

    @Bean
    fun passwordGenerator(randomNumberGenerator: RandomNumberGenerator): PasswordGenerator {
        return PasswordGenerator(randomNumberGenerator.secureRandom)
    }

}
