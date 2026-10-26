package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class V17__add_appointment_pending_rejected_status extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement stmt = context.getConnection().createStatement()) {
            List<String> toDrop = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT con.conname " +
                    "FROM pg_catalog.pg_constraint con " +
                    "JOIN pg_catalog.pg_class rel ON rel.oid = con.conrelid " +
                    "JOIN pg_catalog.pg_namespace nsp ON nsp.oid = rel.relnamespace " +
                    "WHERE rel.relname = 'appointments' " +
                    "AND nsp.nspname = 'public' " +
                    "AND con.contype = 'c'")) {
                while (rs.next()) {
                    String name = rs.getString("conname");
                    if (name.toLowerCase().contains("appointment_type")) continue;
                    toDrop.add(name);
                }
            }

            for (String name : toDrop) {
                stmt.execute("ALTER TABLE appointments DROP CONSTRAINT IF EXISTS " + name);
            }

            stmt.execute("DO $$ BEGIN " +
                    "IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'appointments_status_check') THEN " +
                    "ALTER TABLE appointments ADD CONSTRAINT appointments_status_check " +
                    "CHECK (status IN ('PENDING_APPROVAL', 'CONFIRMED', 'REJECTED', " +
                    "'RESCHEDULED', 'COMPLETED', 'CANCELLED')); " +
                    "END IF; END $$");

            stmt.execute("ALTER TABLE appointments ALTER COLUMN status SET DEFAULT 'PENDING_APPROVAL'");
        }
    }
}
