-- Descrizioni mancanti per i comandi nel sistema Quick Commands
-- Da eseguire sulla tabella emulator_texts

INSERT INTO `emulator_texts` (`key`, `value`) VALUES
('commands.description.cmd_add_youtube_playlist', ':add_youtube <playlist_url>'),
('commands.description.cmd_dance', ':danceall <dance_id>'),
('commands.description.cmd_hidewired', ':hidewired'),
('commands.description.cmd_softkick', ':softkick <username>'),
('commands.description.cmd_subscription', ':subscription <username> <type> <days>'),
('commands.description.cmd_update_all', ':update_all'),
('commands.description.cmd_update_chat_bubbles', ':update_chatbubbles'),
('commands.description.cmd_update_youtube_playlists', ':update_youtube')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
