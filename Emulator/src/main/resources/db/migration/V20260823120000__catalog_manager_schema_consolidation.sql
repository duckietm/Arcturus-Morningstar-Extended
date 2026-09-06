-- Catalog Manager edits the physical live catalog directly.
--
-- LOCAL FORCE-MIGRATION VARIANT:
-- The upstream migration aggregates every historical catalog change entry into
-- one JSON_ARRAYAGG per history group. MariaDB 12.3 repeatedly aborts that
-- conversion with error 1260 ("Row ... was cut by json_arrayagg()") even with
-- group_concat_max_len raised to its maximum.
--
-- Preserve the history groups/revisions/metadata, but do not aggregate the old
-- per-entry before/after payloads. The original database backup retains the full
-- legacy audit tables if historical forensic data is ever required.

CREATE TABLE `catalog_manager_state` (
  `singleton_id` tinyint NOT NULL,
  `revision` bigint NOT NULL DEFAULT 0,
  `updated_at` timestamp(3) NOT NULL DEFAULT current_timestamp(3)
    ON UPDATE current_timestamp(3),
  PRIMARY KEY (`singleton_id`),
  CONSTRAINT `chk_catalog_manager_state_singleton`
    CHECK (`singleton_id` = 1),
  CONSTRAINT `chk_catalog_manager_state_revision`
    CHECK (`revision` >= 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `catalog_manager_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `revision` bigint NOT NULL,
  `actor_id` int NOT NULL,
  `operation_id` varchar(96) DEFAULT NULL,
  `request_fingerprint` char(64) DEFAULT NULL,
  `summary` varchar(255) NOT NULL,
  `source` enum('UI','JSONC','SQL','RESTORE','UNDO') NOT NULL,
  `changes_json` longtext NOT NULL,
  `created_at` timestamp(3) NOT NULL DEFAULT current_timestamp(3),

  PRIMARY KEY (`id`),

  UNIQUE KEY `uq_catalog_manager_history_revision` (`revision`),

  UNIQUE KEY `uq_catalog_manager_history_operation`
    (`actor_id`,`operation_id`),

  KEY `idx_catalog_manager_history_created`
    (`created_at`,`id`),

  CONSTRAINT `chk_catalog_manager_history_changes_json`
    CHECK (json_valid(`changes_json`))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Preserve one history row per old catalog change group.
--
-- We deliberately do NOT use JSON_ARRAYAGG() here. Old detailed entry payloads
-- remain available in the pre-migration database backup.
INSERT INTO `catalog_manager_history`
(
  `id`,
  `revision`,
  `actor_id`,
  `operation_id`,
  `request_fingerprint`,
  `summary`,
  `source`,
  `changes_json`,
  `created_at`
)
SELECT
  groups.`id`,
  groups.`id`,
  groups.`actor_id`,

  (
    SELECT operations.`operation_id`
    FROM `catalog_operations` operations
    WHERE operations.`history_group_id` = groups.`id`
    ORDER BY operations.`created_at` DESC
    LIMIT 1
  ),

  (
    SELECT operations.`request_fingerprint`
    FROM `catalog_operations` operations
    WHERE operations.`history_group_id` = groups.`id`
    ORDER BY operations.`created_at` DESC
    LIMIT 1
  ),

  groups.`summary`,
  groups.`source`,

  JSON_ARRAY(),

  groups.`created_at`

FROM `catalog_change_groups` groups
ORDER BY groups.`id`;

-- Preserve the highest logical catalog revision.
INSERT INTO `catalog_manager_state`
(
  `singleton_id`,
  `revision`,
  `updated_at`
)
SELECT
  1,

  GREATEST(
    COALESCE(
      (SELECT MAX(`id`)
       FROM `catalog_manager_history`),
      0
    ),

    COALESCE(
      (SELECT MAX(`revision`)
       FROM `catalog_versions`),
      0
    )
  ),

  COALESCE(
    (
      SELECT `updated_at`
      FROM `catalog_runtime_state`
      WHERE `singleton_id` = 1
    ),
    current_timestamp(3)
  );

DROP TABLE `catalog_edit_locks`;
DROP TABLE `catalog_operations`;
DROP TABLE `catalog_change_entries`;
DROP TABLE `catalog_change_groups`;
DROP TABLE `catalog_runtime_state`;
DROP TABLE `catalog_version_offers`;
DROP TABLE `catalog_version_pages`;
DROP TABLE `catalog_versions`;
