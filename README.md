# ResourceDownloader
Utility in Java 8+ to download files from internet or any public available url.

###### Update on 10-Aug-2020<br>
 - Update config file and dispose method
 - Update logger and download timer
 
## Usage<br>
`javac ResourceDownloader <url> <destination>`<br>
where
 * \<url> - can be direct source url like http url or can be txt file containing one url each line to download<br>
 * \<destination> - folder where download files will be stored.<br>

This program also provides UI that gives option to put url and destination folder.
Once download starts, UI shows the progress of each file and internet speed.

Configurations and Urls will be auto-stored to config files and will be reloaded on start up.

Attaching screen shot:
![Image of Yaktocat](https://github.com/svermaji/ResourceDownloader/blob/master/rd.png) 

Attaching screen shot with progress bar changes:
![Image of Yaktocat](https://github.com/svermaji/ResourceDownloader/blob/master/rd-progress-bars.png) 

Attaching screen shot with progress bar and text area changes:
![Image of Yaktocat](https://github.com/svermaji/ResourceDownloader/blob/master/rd-table-and-textarea.png) 
