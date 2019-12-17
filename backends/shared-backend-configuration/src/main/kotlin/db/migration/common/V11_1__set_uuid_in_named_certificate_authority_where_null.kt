package db.migration.common

import org.flywaydb.core.api.migration.spring.SpringJdbcMigration
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class V11_1__set_uuid_in_named_certificate_authority_where_null : SpringJdbcMigration {
    @Throws(Exception::class)
    override fun migrate(jdbcTemplate: JdbcTemplate) {
        val nullUuidRecords = jdbcTemplate.queryForList(
            "select id from named_certificate_authority where uuid is null",
            Long::class.java)
        for (record in nullUuidRecords) {
            jdbcTemplate.update(
                "update named_certificate_authority set uuid = ? where id = ?",
                UUID.randomUUID().toString(), record)
        }
    }
}

