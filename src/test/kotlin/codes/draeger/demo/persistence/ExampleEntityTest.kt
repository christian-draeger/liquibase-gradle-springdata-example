package codes.draeger.demo.persistence

import codes.draeger.demo.TestContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import strikt.api.expectThat
import strikt.assertions.isNotEmpty

@SpringBootTest(properties = [
    "spring.liquibase.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create"
])
internal class WithoutLiquibaseTest(
    @Autowired private val exampleRepository: ExampleRepository
): TestContainer() {

    @BeforeEach
    internal fun setUp() {
        exampleRepository.save(ExampleEntity(name = "chris", age = 34))
    }

    @Test
    internal fun `will start application without liquibase and generate schema using hibernate ddl`() {
        expectThat(exampleRepository.findAll()).isNotEmpty()
    }
}

@SpringBootTest(properties = [
    "spring.liquibase.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
])
internal class WithLiquibaseTest(
    @Autowired private val exampleRepository: ExampleRepository
): TestContainer() {

    @BeforeEach
    internal fun setUp() {
        exampleRepository.save(ExampleEntity(name = "chris", age = 34))
    }

    @Test
    internal fun `will start application without liquibase and generate schema using hibernate ddl`() {
        expectThat(exampleRepository.findAll()).isNotEmpty()
    }
}
