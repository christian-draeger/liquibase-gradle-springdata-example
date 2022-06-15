import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("org.springframework.boot") version "2.7.0"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("plugin.jpa") version "1.6.21"
    id("org.liquibase.gradle") version "2.1.1"
    id("com.avast.gradle.docker-compose") version "0.14.3"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    runtimeOnly("org.postgresql:postgresql:42.3.4")
    implementation("org.liquibase:liquibase-core:4.11.0")
    liquibaseRuntime("org.liquibase:liquibase-core:4.11.0")
    liquibaseRuntime("org.liquibase.ext:liquibase-hibernate5:4.10.0") // needed to support hibernate diff from entity
    liquibaseRuntime(sourceSets.getByName("main").output) // necessary for Hibernate to find your entity classes
    liquibaseRuntime(sourceSets.getByName("main").compileClasspath)
    liquibaseRuntime(sourceSets.getByName("main").runtimeClasspath)
    liquibaseRuntime("org.yaml:snakeyaml:1.30") // needed to support generation in yaml format
    liquibaseRuntime("info.picocli:picocli:4.6.3") // needed dependency on the liquibase classpath
    liquibaseRuntime("org.postgresql:postgresql:42.3.4")
    liquibaseRuntime("ch.qos.logback:logback-core:1.2.11") // logging bridge
    liquibaseRuntime("ch.qos.logback:logback-classic:1.2.11")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers:1.17.2")
    testImplementation("org.testcontainers:postgresql:1.17.2")
    testImplementation("io.strikt:strikt-core:0.34.1")
}


dockerCompose {
    useComposeFiles = listOf("docker-compose.yml")
    startedServices = listOf("database")
}

liquibase {
    activities.register("main") {
        arguments = mapOf(
            "classpath" to "src/main/resources",
            "changeLogFile" to "src/main/resources/db/changelog/db.changelog-master.yaml",

            // previous state (target DB)
            "driver" to "org.postgresql.Driver",
            "url" to "jdbc:postgresql://localhost:5432/liquibase-demo",
            "username" to "liquibase-demo",
            "password" to "liquibase-demo",
            "logLevel" to "INFO",

            // actual state (hibernate model)
            "referenceUrl" to "hibernate:spring:codes.draeger?dialect=org.hibernate.dialect.PostgreSQL10Dialect", // needs to match the base package of your projects entities
            "referenceDriver" to "liquibase.ext.hibernate.database.connection.HibernateDriver",
        )
    }
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    composeUp { dependsOn(composeDown) }

    bootRun {
        dependsOn(composeUp)
    }

    val bootRunWithoutLiquibase by creating(BootRun::class) {
        group = "Application"
        description = "Starts app and creates db schema by using hibernate ddl"
        bootRun.configure {
            args("--spring.liquibase.enabled=false", "--spring.jpa.hibernate.ddl-auto=create")
        }
        finalizedBy(bootRun)
    }

    diffChangeLog {
        dependsOn(bootRunWithoutLiquibase)
    }
}
