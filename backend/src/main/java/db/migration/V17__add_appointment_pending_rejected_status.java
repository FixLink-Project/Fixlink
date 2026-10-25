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
            // Collect all CHECK constraint names on the appointments table
            List<String> toDrop = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT constraint_name FROM information_schema.table_constraints " +
                    "WHERE LOWER(table_name) = 'appointments' AND constraint_type = 'CHECK'")) {
                while (rs.next()) {
                    String name = rs.getString("constraint_name");
                    if (name.toLowerCase().contains("appointment_type")) continue;
                    toDrop.add(name);
                }
            }

            // Drop the old CHECK constraints on status
            for (String name : toDrop) {
                stmt.execute("ALTER TABLE appointments DROP CONSTRAINT " + name);
            }

            // Add the new named CHECK with expanded status values
            stmt.execute("ALTER TABLE appointments ADD CONSTRAINT appointments_status_check " +
                    "CHECK (status IN ('PENDING_APPROVAL', 'CONFIRMED', 'REJECTED', " +
                    "'RESCHEDULED', 'COMPLETED', 'CANCELLED'))");

            // Update the default
            stmt.execute("ALTER TABLE appointments ALTER COLUMN status SET DEFAULT 'PENDING_APPROVAL'");
        }
    }
}
