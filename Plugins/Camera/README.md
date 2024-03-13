# 📷 Camera

**New features compared to Apollyon**
- [x] Small and more efficient than Apollyon.
- [x] `*_small.png` are really small (original / 2) and not just copied.
- [x] Creates a folder structure if it doesn't exist.
- [x] Report failures to Habbo.
- [x] Added a cooldown time for taking photos to prevent your hard drive from filling up.
- [x] Prevent exploits on thumbnails, not just photos.
- [x] No more FTP (no one used that).

[Download the latest compiled version here!](https://github.com/duckietm/Arcturus-Morningstar-Extended/blob/main/Plugins/Camera/Compiled/Camera-1.6.jar)

## Informations
All missing settings in the database will be created with this plugin after the first launch of your emulator.
### emulator_settings
- `camera.url`
- - The URL from which you can access the image (default: http://localhost/camera/)
- `imager.location.output.camera`
- - The location where to save the photo (default: C:/inetpub/wwwroot/public/camera/)
- `imager.location.output.thumbnail`
- - The location to save the thumbnail (default: C:/inetpub/wwwroot/public/camera/thumbnails/)
- `camera.price.points.publish`
- - The price of points for publishing this photo (default: 1)
- `camera.price.points.publish.type`
- - The type of points to publish (default: 5 = diamonds)
- `camera.publish.delay`
- - The time, in seconds, how long the user should wait to post a new photo (default: 180)
- `camera.price.credits`
- - The credits required to purchase this photo (default: 2)
- `camera.price.points`
- - The price of points to purchase this photo (default: 0 = no points required)
- `camera.price.points.type`
- - The type of points to buy this photo (default: 5 = diamonds)
- `camera.render.delay`
- - The time, in seconds, how long the user should wait to take a new photo (default: 5)
### emulator_texts
- `camera.permission`
- - default: "You don't have permission to use the camera!"
- `camera.wait`
- - default: "Please wait %seconds% seconds before making another picture."
- `camera.error.creation`
- - default: "Failed to create your picture. \*sadpanda\*"

Since this plugin doesn't use FTP like “Apollyon”, you can delete these values ​​in your database under `emulator_settings` if necessary.

## Comparison
Apollyon doesn't show you any errors like you don't have enough currency.

# Credits

- EntenKoeniq for releaseing the fixes and rewritten the plugin. (This camera plugin was written for https://habbo.sx/ and is now available to the community for free.)
- John
- Beny (Packet Hijacking)
- Ovflowd (Original Implimentation)
- Alejandro (Error Handling Help)
