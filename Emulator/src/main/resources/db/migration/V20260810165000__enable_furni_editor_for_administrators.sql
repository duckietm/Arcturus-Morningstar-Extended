-- The Nitro furni editor and every matching packet handler are protected by
-- acc_catalogfurni. Administrators previously had acc_anyroomowner but not
-- acc_catalogfurni, which exposed an Edit Furni button that could never open.
UPDATE `permissions`
SET `acc_catalogfurni` = '1'
WHERE `level` >= 7
  AND `acc_catalogfurni` <> '1';
