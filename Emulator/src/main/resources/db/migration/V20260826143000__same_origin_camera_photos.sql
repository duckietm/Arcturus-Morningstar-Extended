UPDATE emulator_settings
SET `value` = '/camera/'
WHERE `key` = 'camera.url'
  AND (`value` LIKE '%127.0.0.1%' OR `value` LIKE '%localhost%' OR `value` LIKE '%photo.bsshotel.it%');

UPDATE items
SET extra_data = REPLACE(
    REPLACE(
        REPLACE(extra_data, 'http://127.0.0.1:8080/camera/', '/camera/'),
        'http://localhost:8080/camera/', '/camera/'
    ),
    'https://photo.bsshotel.it//', '/camera/'
)
WHERE extra_data LIKE '%127.0.0.1:8080/camera/%'
   OR extra_data LIKE '%localhost:8080/camera/%'
   OR extra_data LIKE '%photo.bsshotel.it//%';
