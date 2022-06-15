Liquibase via Gradle Demo 
=========================

This is an example project to show-case how to set up liquibase using the [liquibase-gradle-plugin](https://github.com/liquibase/liquibase-gradle-plugin) in a spring-boot project.
The Example project is completely written in Kotlin, uses [spring-data-jpa](https://spring.io/projects/spring-data-jpa) (and thereby hibernate under the hood) and a Postgres database.

Its aim is to provide a minimal step-by-step guide that demonstrates what is needed to be able to generate an initial schema as well as generate liquibase changelog diffs automatically based on your JPA entities.

> Hint: you already have a spring-boot project that uses JPA --> jump directly over to step 3 

## Step 1
Generate a new spring-boot project that uses spring-data-jpa using [spring initializr](https://start.spring.io).

![step1-1](assets/step1-1-create-new-project.png) ![step1-2](assets/step1-2-add-data-jpa-dependency.png)

## Step 2
Adding a database and an entity to the project.
In this example we will spin up a Postgres database using [docker compose](docker-compose.yml), but it will also work with any other database that is supported by spring-data-jpa and liquibase.

### Step 2.1
Add postgres driver as a runtime dependency to your projects build.gradle.kts.

![step2-1](assets/step2-1-add-postgres-driver.png)

### Step 2.2
Add docker-compose file to be able to conveniently spin up a postgres database on your local machine.

<img height="350" src="assets/step2-2-add-docker-compose-file.png" />

### Step 2.3
Connect Spring Boot app with database (TODO: few words about driver and ddl)

<img height="200" src="assets/step2-3-wire-database-with-spring-app.png" />

### Step 2.4
Add an Entity.
> Hint: since we use the jpa option `spring.jpa.hibernate.ddl-auto: create` in our [application.yaml](src/main/resources/application.yaml) Hibernate first drops existing tables, then creates new tables based on the Entities (classes annotated with `@Entity`) of our project!
> Accordingly this property will influence how the schema tool management will manipulate the database schema at startup.

<img height="200" src="assets/step2-4-add-an-entity.png" />

A few words about JPA Entities and how to use them correctly in Kotlin or why you should always use classes - **NEVER use `data class` as an Entity!**

| **Kotlin Data Class**                                                                                                                                                                                                                                           | **Hibernate Requirements for Entity classes**                                                                                                           |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| According to the [official kotlin docs](https://kotlinlang.org/docs/data-classes.html) a Data classes cannot be open (is final / immutable)                                                                                                                     | using auto-generated IDs means an entity class needs to be mutable to work properly                                                                     |
| will generate `equals` and `hashCode` functions based on the properties declared in the primary constructor on instantiation                                                                                                                                    | needs to provide useful implementations for `equals` and `hashCode`. The `hashCode`s entity identifier may be set after the object has been constructed |
| will create getters by using `val` properties (which means those values are final)                                                                                                                                                                              | persistent instance variables of the entity should not be final                                                                                         |
| You cannot extend a Data class or make it abstract. This limitation is not Kotlin’s fault. There is no way to generate the correct value-based `equals()` without violating the Liskov Principle. That’s why Kotlin doesn’t allow inheritance for Data classes. | may extend non-entity classes **as well as entity classes**                                                                                             |
| will only have a primary no-args constructor if all parameters have a default value                                                                                                                                                                             | must have a public or protected (or package-private) no-argument constructor                                                                            |
| equals(), hashCode() and toString() implementations use all fields from the primary constructor (regardless of its annotations, e.g. an association to another entity with `FetchType.LAZY`)                                                                    | calling properties with `FetchType.LAZY` on every `toString()` call results in unwanted requests to the DB or a LazyInitializationException             |

The official spring guide summarizes this dilemma as follows: "don’t use data classes with val properties because **JPA is not designed to work with immutable classes or the methods generated automatically by data classes**." 
  * BTW: other spring-data flavors such as Spring Data MongoDB, Spring Data JDBC, etc. support such constructs, you are free to use data classes there

Further reading: [video webinar](https://www.youtube.com/watch?v=a_6V8xwiv04), [blog post](https://www.jpa-buddy.com/blog/best-practices-and-common-pitfalls/), [blog post](https://kotlinexpertise.com/hibernate-with-kotlin-spring-boot/), [blog post](https://dzone.com/articles/kotlin-data-classes-and-jpa), 

### Step 2.5
Verify if spring boot app will connect to database and hibernate auto-generates a database schema
* start postgres database with following command executed from your projects root directory: `docker compose up`
* run spring-boot app `./gradlew bootRun`
* connect to database using any database client (i am using intellij here)
    * <img height="400" src="assets/step2-5-1-connect-to-database.png" />
* check if schema was automatically generated by hibernate dll
    * <img height="400" src="assets/step2-5-2-see-schema.png" />

## Step 3
Add liquibase to your project to enable Database migration 

### Step 3.1 - add liquibase to spring application classpath
add liquibase dependency to your build.gradle.kts
```kotlin
dependencies {
    // ...
    implementation("org.liquibase:liquibase-core:4.11.0")
}
```

The default directory where spring expects to find a liquibase migration script is `src/main/resources/db/changelog/db.changelog-master.yaml`.
It is possible to change the changelog files destination using the following property in our [application.yaml](src/main/resources/application.yaml): `liquibase.change-log=classpath:liquibase-changeLog.xml`.
But we want to stick to the defaults here.

Whenever spring finds liquibase in the applications classpath (which it should because we added the dependency) it wants to run Liquibase migrations automatically on startup.

Since we have not created a liquibase changelog file yet the application will fail to start with following error:
```text
***************************
APPLICATION FAILED TO START
***************************

Description:

Liquibase failed to start because no changelog could be found at 'classpath:/db/changelog/db.changelog-master.yaml'.
```

We could now provide a 'hand written' liquibase changelog file and would be good to go. But come on, **Instead of writing the changeLog file manually and error-prone, we can use the Liquibase Gradle plugin to generate one and save ourselves a lot of work.**

### Step 3.2 - Including the plugin
Add [liquibase gradle plugin](https://github.com/liquibase/liquibase-gradle-plugin#usage) to [build.gradle.kts](build.gradle.kts).

```kotlin
plugins {
  // ...
  id("org.liquibase.gradle") version "2.1.1"
}
```

### Step 3.3 - Setting up the classpath
The plugin will need to be able to find Liquibase on the classpath when it runs a task, and Liquibase will need to be able to find database drivers, changelog parsers, etc. in the classpath. This is done by adding liquibaseRuntime dependencies to the dependencies block in the [build.gradle.kts](build.gradle.kts) file.

```kotlin
dependencies {
  // ...
  liquibaseRuntime("org.liquibase:liquibase-core:4.11.0")
  liquibaseRuntime("org.postgresql:postgresql:42.3.4")
  liquibaseRuntime("org.liquibase.ext:liquibase-hibernate5:4.10.0") // needed to support hibernate diff from entity
  liquibaseRuntime(sourceSets.getByName("main").output) // necessary for Hibernate to find your entity classes
  liquibaseRuntime(sourceSets.getByName("main").compileClasspath)
  liquibaseRuntime(sourceSets.getByName("main").runtimeClasspath)
  liquibaseRuntime("org.yaml:snakeyaml:1.30") // needed to support generation in yaml format
  liquibaseRuntime("info.picocli:picocli:4.6.3") // needed dependency on the liquibase classpath
}
```
### Step 3.4 - Configuring the plugin
Parameters for Liquibase commands are configured in the liquibase block inside the build.gradle file. This block contains a series of, "activities", each defining a series of Liquibase parameters.
In our example we only define a "main" activity. If you need some more advanced setup, see the [liquibase gradle plugin docs](https://github.com/liquibase/liquibase-gradle-plugin#3-configuring-the-plugin).

```kotlin
liquibase {
    activities.register("main") {
        arguments = mapOf(
            "classpath" to "src/main/resources",
            "changeLogFile" to "src/main/resources/db/changelog/db.changelog-master.yaml",
            "logLevel" to "INFO",

            // previous state (target DB)
            "driver" to "org.postgresql.Driver",
            "url" to "jdbc:postgresql://localhost:5432/liquibase-demo",
            "password" to "liquibase-demo",
            "username" to "liquibase-demo",

            // actual state (hibernate model)
            "referenceUrl" to "hibernate:spring:codes.draeger", // needs to match the base package of your projects entities
            "referenceDriver" to "liquibase.ext.hibernate.database.connection.HibernateDriver",
        )
    }
}
```

Background:

* changeLogFile: the main Liquibase Changelog file
* url: the connection Url for your database (Postgres in this case)
* username & password: for your database (in this case defined at docker-compose.yaml)
* driver: the Postgres driver, as url uses a Postgres Url
* referenceUrl: Hibernate Url pointing to your spring project (codes.draeger in this case)
* referenceDriver: Hibernate driver as referenceUrl


> Run `./gradlew tasks` to list all the task (including the ones that have been added by the liquibase plugin) of your project.

## Step 4
generate liquibase changelogs based on your projects' entity classes.

### Step 4.1
generate initial liquibase changelog file from your current schema.

```
 mkdir -p src/main/resources/db/changelog
./gradlew generateChangelog
```

### Step 4.2
Whenever you made changes to your entities like adding a field/column, changing relations or adding constrains run the following command to generate an additional changelog:

```
./gradlew diffChangelog
```
