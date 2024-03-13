### WEBSOCKET SETTINGS

INSERT INTO `emulator_settings` (`key`, `value`) VALUES  ('websockets.whitelist', 'localhost'); # Change this to the url of the websocket Expl. ws.mydomain.com
INSERT INTO `emulator_settings` (`key`, `value`) VALUES  ('ws.nitro.host', '0.0.0.0'); # Best is this to leave it at 0.0.0.0
INSERT INTO `emulator_settings` (`key`, `value`) VALUES  ('ws.nitro.ip.header', ''); # When useing a proxy change the header : X-Forwarded-For when using a proxy server or CF-Connecting-IP if behind Cloudflare.
INSERT INTO `emulator_settings` (`key`, `value`) VALUES  ('ws.nitro.port', '2096'); # set the port of the websocket, cloudflare ports : 443 / 2053 / 2083 / 2087 / 2096 / 8443