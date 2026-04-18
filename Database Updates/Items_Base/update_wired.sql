UPDATE items_base SET customparams = 'is_invisible' WHERE  public_name like 'tile_stackmagic%';
UPDATE items_base SET customparams = 'is_invisible' WHERE  public_name like 'tile_walkmagic%';
UPDATE items_base SET customparams = 'is_invisible' WHERE  public_name like 'room_invisible_block%';
UPDATE items_base SET customparams = 'is_invisible' WHERE  public_name = 'room_invisible_sit_tile';
UPDATE items_base SET customparams = 'is_invisible' WHERE  public_name = 'room_invisible_click_tile';

UPDATE `items_base` SET `interaction_type` = 'wf_conf_invis_control' WHERE `public_name` = 'conf_invis_control';
UPDATE `items_base` SET `interaction_type` = 'wf_conf_handitem_block' WHERE `public_name` = 'conf_handitem_block';
UPDATE `items_base` SET `interaction_type` = 'wf_conf_wired_disable' WHERE `public_name` = 'conf_wired_disable';
UPDATE `items_base` SET `interaction_type` = 'wf_conf_queue_speed' WHERE `public_name` = 'conf_queue_speed';
UPDATE `items_base` SET `interaction_type` = 'wf_conf_area_hide' WHERE `public_name` = 'conf_area_hide';