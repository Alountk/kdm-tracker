package io.kdmtracker.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * A server-side account. [passwordHash] holds an already-hashed value only; the hashing and
 * password policy live in Task 4 (JWT auth), not here.
 */
@Entity
@Table(name = "users")
class UserEntity(
    @field:Column(unique = true)
    var username: String = "",
    var passwordHash: String = "",
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
)
