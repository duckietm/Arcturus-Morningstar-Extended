-- Furni Editor/BSS imports keep the authoritative asset classname in item_name.
-- Only normalize classnames that Polaris actually registers as WIRED interactions;
-- decorative wf_* assets (wires, plates, gates, etc.) retain their own behavior.
UPDATE items_base
SET interaction_type = item_name
WHERE interaction_type IN ('', 'default')
  AND item_name IN (
    'wf_act_alert',
    'wf_act_bot_follow_avatar',
    'wf_act_bot_talk',
    'wf_act_furni_to_furni',
    'wf_act_give_score',
    'wf_act_give_userbadge',
    'wf_act_teleport_to',
    'wf_cnd_has_handitem',
    'wf_trg_bot_reached_avtr',
    'wf_trg_periodically',
    'wf_trg_score_achieved',
    'wf_xtra_anim_time',
    'wf_xtra_filter_users',
    'wf_xtra_mov_carry_users',
    'wf_xtra_mov_physics'
  );
