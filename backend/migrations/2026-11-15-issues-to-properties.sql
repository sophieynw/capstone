-- Run against the existing MySQL schema with the backend stopped.
SET SESSION sql_mode = CONCAT_WS(',', NULLIF(@@SESSION.sql_mode, ''), 'STRICT_ALL_TABLES');

-- The nullable column allows existing issues to be backfilled before enforcing the new relationship.
SET @add_property_column = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE issue ADD COLUMN property_id BIGINT NULL', 'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'issue' AND column_name = 'property_id'
);
PREPARE add_property_column FROM @add_property_column;
EXECUTE add_property_column;
DEALLOCATE PREPARE add_property_column;

UPDATE issue AS i
JOIN cleaning AS c ON c.id = i.cleaning_id
SET i.property_id = c.property_id
WHERE i.property_id IS NULL;

ALTER TABLE issue MODIFY COLUMN property_id BIGINT NOT NULL;

SET @add_property_fk = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE issue ADD CONSTRAINT fk_issue_property FOREIGN KEY (property_id) REFERENCES property(id)',
        'SELECT 1')
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 'issue'
        AND column_name = 'property_id' AND referenced_table_name = 'property'
);
PREPARE add_property_fk FROM @add_property_fk;
EXECUTE add_property_fk;
DEALLOCATE PREPARE add_property_fk;

-- Hibernate generated the old foreign key name, so read it from this database.
SET @cleaning_fk = (
    SELECT constraint_name
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 'issue'
        AND column_name = 'cleaning_id' AND referenced_table_name = 'cleaning'
);
SET @drop_cleaning_link = CONCAT(
    'ALTER TABLE issue ',
    IF(@cleaning_fk IS NULL, '', CONCAT('DROP FOREIGN KEY `', REPLACE(@cleaning_fk, '`', '``'), '`, ')),
    'DROP COLUMN cleaning_id'
);
PREPARE drop_cleaning_link FROM @drop_cleaning_link;
EXECUTE drop_cleaning_link;
DEALLOCATE PREPARE drop_cleaning_link;
