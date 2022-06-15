package codes.draeger.demo.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity(name = "example")
@Table(name = "example")
class ExampleEntity(
    @Id
    val id: String = UUID.randomUUID().toString(),

    @Column(name = "name", nullable = false, unique = true)
    var name: String,

    @Column(name = "age", nullable = false)
    val age: Int,


)

interface ExampleRepository: JpaRepository<ExampleEntity, String>
