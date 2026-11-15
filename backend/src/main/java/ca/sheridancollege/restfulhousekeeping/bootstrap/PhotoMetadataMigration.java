package ca.sheridancollege.restfulhousekeeping.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;

@Component
@Order(0)
@RequiredArgsConstructor
public class PhotoMetadataMigration implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Preserve cleaning photo IDs because they are part of the existing API URLs.
        if (hasColumn("cleaning_photo", "id")) {
            jdbc.update("""
                    insert into photo (id, cleaning_id, file_name, file_type, image_kit_url, image_kit_file_id, image_data)
                    select id, cleaning_id, file_name, file_type, image_kit_url, image_kit_file_id, image_data
                    from cleaning_photo
                    """);
            jdbc.update("delete from cleaning_photo");
        }

        migratePictures("_user", "profile_picture_path", "profile_picture_file_id", "profile_photo_id");
        migratePictures("property", "property_picture_path", "property_picture_file_id", "property_photo_id");
    }

    private void migratePictures(String ownerTable, String pathColumn, String fileIdColumn, String photoIdColumn) {
        if (!hasColumn(ownerTable, pathColumn)) {
            return;
        }

        var pictures = jdbc.query("""
                select id, %s, %s from %s where %s is not null and %s is null
                """.formatted(pathColumn, fileIdColumn, ownerTable, pathColumn, photoIdColumn),
                (row, index) -> new LegacyPicture(row.getLong(1), row.getString(2), row.getString(3)));

        for (LegacyPicture picture : pictures) {
            GeneratedKeyHolder key = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement insert = connection.prepareStatement(
                        "insert into photo (image_kit_url, image_kit_file_id) values (?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                insert.setString(1, picture.url());
                insert.setString(2, picture.fileId());
                return insert;
            }, key);

            jdbc.update("""
                    update %s set %s = ?, %s = null, %s = null where id = ?
                    """.formatted(ownerTable, photoIdColumn, pathColumn, fileIdColumn),
                    Objects.requireNonNull(key.getKey()).longValue(), picture.ownerId());
        }
    }

    private boolean hasColumn(String table, String column) {
        return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            var metadata = connection.getMetaData();
            String tablePattern = metadata.storesUpperCaseIdentifiers() ? table.toUpperCase(Locale.ROOT) : table;
            try (var columns = metadata.getColumns(connection.getCatalog(), connection.getSchema(), tablePattern, "%")) {
                while (columns.next()) {
                    if (table.equalsIgnoreCase(columns.getString("TABLE_NAME"))
                            && column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
                        return true;
                    }
                }
                return false;
            }
        }));
    }

    private record LegacyPicture(long ownerId, String url, String fileId) {
    }
}
