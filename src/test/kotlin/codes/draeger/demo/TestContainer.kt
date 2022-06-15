package codes.draeger.demo

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@ContextConfiguration(initializers = [TestContainer.Companion.Initializer::class])
open class TestContainer {

    companion object {
        val sqlContainer: CustomSQLContainer = CustomSQLContainer()

        class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
                sqlContainer.start()

                TestPropertyValues.of(
                    "spring.datasource.url=${sqlContainer.jdbcUrl}",
                    "spring.datasource.username=${sqlContainer.username}",
                    "spring.datasource.password=${sqlContainer.password}",
                ).applyTo(configurableApplicationContext.environment)
            }
        }
    }
}

class CustomSQLContainer(
    version: String = "12.4-alpine",
) : PostgreSQLContainer<CustomSQLContainer>(DockerImageName.parse("postgres:$version")) {
    init {
        withDatabaseName("testing")
        withUsername("testing")
        withPassword("testing")
        withCommand("postgres -N 500")
    }
}
