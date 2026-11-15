package ca.sheridancollege.restfulhousekeeping.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoMetadataMigrationTests {

    @Test
    void movesExistingMetadataAndCleaningBytesWithoutRecreatingDeletedPhotosOnRestart() {
        var dataSource = new SingleConnectionDataSource("jdbc:h2:mem:photo_migration;MODE=MySQL", "sa", "", true);
        try {
            var jdbc = new JdbcTemplate(dataSource);
            var transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
            var migration = new PhotoMetadataMigration(jdbc);
            jdbc.execute("""
                    create table photo (
                        id bigint auto_increment primary key, cleaning_id bigint,
                        file_name varchar(255), file_type varchar(255), image_kit_url varchar(2048),
                        image_kit_file_id varchar(255), image_data blob
                    )
                    """);
            jdbc.execute("create table cleaning_photo as select * from photo where 1 = 0");
            jdbc.execute("""
                    create table _user (id bigint primary key, profile_picture_path varchar(255),
                        profile_picture_file_id varchar(255), profile_photo_id bigint references photo(id))
                    """);
            jdbc.execute("""
                    create table property (id bigint primary key, property_picture_path varchar(255),
                        property_picture_file_id varchar(255), property_photo_id bigint references photo(id))
                    """);
            byte[] legacyImage = {1, 2, 3};
            jdbc.update("insert into cleaning_photo values (12, 3, 'cleaning.jpg', 'image/jpeg', null, null, ?)",
                    legacyImage);
            jdbc.update("insert into _user values (1, 'https://example.com/profile.jpg', null, null)");
            jdbc.update("insert into property values (1, 'https://example.com/property.jpg', 'property-file', null)");

            transaction.executeWithoutResult(status -> migration.run(new DefaultApplicationArguments()));

            assertThat(jdbc.queryForObject("select count(*) from photo", Long.class)).isEqualTo(3);
            assertThat(jdbc.queryForObject("select image_data from photo where id = 12", byte[].class))
                    .containsExactly(legacyImage);
            assertThat(jdbc.queryForObject("select cleaning_id from photo where id = 12", Long.class)).isEqualTo(3);
            assertThat(jdbc.queryForObject("""
                    select p.image_kit_url from photo p join _user u on p.id = u.profile_photo_id
                    """, String.class)).isEqualTo("https://example.com/profile.jpg");
            assertThat(jdbc.queryForObject("""
                    select p.image_kit_file_id from photo p join property pr on p.id = pr.property_photo_id
                    """, String.class)).isEqualTo("property-file");

            jdbc.update("delete from photo where id = 12");
            jdbc.update("update _user set profile_photo_id = null");
            jdbc.update("delete from photo where image_kit_url = 'https://example.com/profile.jpg'");
            transaction.executeWithoutResult(status -> migration.run(new DefaultApplicationArguments()));

            assertThat(jdbc.queryForObject("select count(*) from photo", Long.class)).isEqualTo(1);
        } finally {
            dataSource.destroy();
        }
    }
}
