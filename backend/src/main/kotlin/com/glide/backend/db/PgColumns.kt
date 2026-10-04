package com.glide.backend.db

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** PostgreSQL `jsonb`, written and read as a JSON string. Our values come from kotlinx.serialization, never from users. */
class JsonbColumnType : ColumnType<String>() {
    override fun sqlType() = "jsonb"

    override fun valueFromDB(value: Any): String = value.toString()

    // The driver sends parameters as text; the cast makes PostgreSQL accept it for a jsonb column.
    override fun parameterMarker(value: String?) = "CAST(? AS jsonb)"
}

/** PostgreSQL `timestamptz`, as an [OffsetDateTime] in UTC. */
class TimestamptzColumnType : ColumnType<OffsetDateTime>() {
    override fun sqlType() = "timestamptz"

    override fun valueFromDB(value: Any): OffsetDateTime =
        when (value) {
            is OffsetDateTime -> value
            is Timestamp -> value.toInstant().atOffset(ZoneOffset.UTC)
            else -> OffsetDateTime.parse(value.toString())
        }
}

fun Table.jsonb(name: String): Column<String> = registerColumn(name, JsonbColumnType())

fun Table.timestamptz(name: String): Column<OffsetDateTime> = registerColumn(name, TimestamptzColumnType())

/** The database's own clock (`now()`), so every timestamp in one transaction is the same and none comes from the app. */
object DbNow : Expression<OffsetDateTime>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append("now()")
    }
}
