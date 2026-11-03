
package com.backendreliabilitylab

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.containers.GenericContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	fun postgresContainer(): PostgreSQLContainer {
		return PostgreSQLContainer(DockerImageName.parse("postgres:17"))
	}

	@Bean
	fun redisContainer(): GenericContainer<*> {
		return GenericContainer(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379)
	}

	@Bean
	fun redisProperties(
		redisContainer: GenericContainer<*>
	): DynamicPropertyRegistrar {
		return DynamicPropertyRegistrar { registry ->
			registry.add("spring.data.redis.host") {
				redisContainer.host
			}
			registry.add("spring.data.redis.port") {
				redisContainer.getMappedPort(6379)
			}
		}
	}
}
