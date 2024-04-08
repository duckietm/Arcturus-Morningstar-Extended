# javascript-plugin
Allows 2-way communication between some javascript running in your client and your emulator using the FlashExternalInterface

## How do I use it?
You can use the following Vue application which will work out of the box with this plugin: https://github.com/dank074/youtube-overlay
Just include the scripts and add a div with id=app, and you are set to go

## I don't wanna use that Vue trash, how do I use it with my own javascript?

Receiving a message:
```js
FlashExternalInterface.openHabblet = function(a,b){console.log("recieved " + a)}
```

Sending a message:
```js
document.querySelector('object, embed').openroom(JSON.stringify({"header": "test", "data": {"name": "Efrain"}}))
```
## Built in features
- [x] Arrowkey walking
- [x] Youtube jukebox (use interaction `yt_jukebox` on any furniture)
- [x] Slots Machine (use interaction `slots_machine` on any furniture)
- [x] :youtube command ```ALTER TABLE permissions
    ADD COLUMN cmd_youtube enum('0','1') DEFAULT '1';```
