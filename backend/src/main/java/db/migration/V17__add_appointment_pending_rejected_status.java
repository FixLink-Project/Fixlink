package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class V17__add_appointment_pending_rejected_status extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        DatabaseMetaData metaData = context.getConnection().getMetaData();
        String databaseProductName = metaData.getDatabaseProductName();
        boolean isPostgres = databaseProductName != null
                && databaseProductName.toLowerCase().contains("postgres");

        try (Statement stmt = context.getConnection().createStatement()) {
            List<String> toDrop = new ArrayList<>();

            if (isPostgres) {
                // PostgreSQL: truy vấn pg_constraint và pg_catalog để tìm chính xác các check constraint liên quan tới cột status
                String query = "SELECT con.conname "
                        + "FROM pg_catalog.pg_constraint con "
                        + "JOIN pg_catalog.pg_class rel ON rel.oid = con.conrelid "
                        + "JOIN pg_catalog.pg_namespace nsp ON nsp.oid = rel.relnamespace "
                        + "WHERE rel.relname = 'appointments' "
                        + "  AND con.contype = 'c' "
                        + "  AND pg_get_constraintdef(con.oid) LIKE '%status%'";

                try (ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        toDrop.add(rs.getString("conname"));
                    }
                }
            } else {
                // H2 (cho môi trường test/local): dùng information_schema.table_constraints
                String query = "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE LOWER(table_name) = 'appointments' AND constraint_type = 'CHECK'";

                try (ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        String name = rs.getString("constraint_name");
                        if (name.toLowerCase().contains("appointment_type")) {
                            continue;
                        }
                        toDrop.add(name);
                    }
                }
            }

            // Drop old CHECK constraints on status
            for (String name : toDrop) {
                stmt.execute("ALTER TABLE appointments DROP CONSTRAINT IF EXISTS " + name);
            }

            // Đảm bảo xóa appointments_status_check nếu còn sót
            stmt.execute("ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_status_check");

            // Add the new named CHECK with expanded status values
            stmt.execute("ALTER TABLE appointments ADD CONSTRAINT appointments_status_check "
                    + "CHECK (status IN ('PENDING_APPROVAL', 'CONFIRMED', 'REJECTED', "
                    + "'RESCHEDULED', 'COMPLETED', 'CANCELLED'))");

            // Update the default
            stmt.execute("ALTER TABLE appointments ALTER COLUMN status SET DEFAULT 'PENDING_APPROVAL'");
        }
    }
}
