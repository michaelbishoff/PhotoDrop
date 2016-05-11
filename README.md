# Photo Drop
------------------------------

Drop photos at your location. View photos near you.

### Design
Jimena Del Castillo <jimenadelcastillo@gmail.com>  
Tim Shattuck <tshattuck@gmail.com>

### Programming
[Siqi Lin](https://github.com/linsiqi1) <linsiqi1@umbc.edu>  
[Lun-Chung Yuan](https://github.com/yuandondon) <lun1@umbc.edu>  
Xiaoxia Zheng <xiaoxia1@umbc.edu>  
[Michael Bishoff](https://github.com/michaelbishoff) <bishoff1@umbc.edu>  
  
[Video Demo](https://www.youtube.com/watch?v=epph75WTc5Q)
[Screenshots](https://goo.gl/photos/dXPCKkVtbTdWRHyN9)

### The Idea
    Users can take a photo and drop the photo at their location anonymously. Anyone near the photo can open the parachute and view the photo. Anyone can see the locations of all the photos that have been taken on a map. Photos with more likes will have a brighter color parachute. Users can also see photos being dropped live if they have the map open and someone else takes a photo. This entices users to go to places that have a lot of photos, places that have good photos, or places that have photos being dropped as they're using the app. Our app makes photo sharing a real world experience because you have to physically go to where the photo was taken and live through someone else's eyes.

### Walkthrough
    When a user opens the app, they are presented with a login/signup page. If the user has an account, it will log them in. If they user does not, it will make them an account and log them in. After login, the screen moves to the map of photos, indicated by the parachutes. Clicking on a parachute within the viewable range, indicated by the blue circle on the map or a red parachute, will open the photo. The user can then Like the photo, Comment on it, or Flag it as inappropriate. Comments on the photo are also anonymous and live; so, as a user is looking at the comments, they can see new comments on the photo appear instantly. Every user has a Profile page, which consists of all of the photos that they have taken. Clicking on a photo in the Profile page will open the photo the same way as clicking on a parachute. In the Settings page of the app, users can change their password and log out.

### Optimizations
    We store the photos that the user has taken on their device. This allows us to load the photo from storage, rather than accessing the database (Firebase), if the user would like to see a photo that they have taken. This speeds up the app significantly because photos have a large file size.
    We store whether the user has liked or flagged a photo previously in SharedPreferences so that when they open a photo, the app will indicated that they have previously liked or flagged the photo.
    The optimizations explained above (loading photos faster and indicating liked/flagged photos faster) can be achieved with database accesses, but these optimizations make the app significantly faster. If a user logs-in from another device, other than the one they took the photos on, then the photos will be loaded from the database.
    We also made the photos accessible by other photo apps. From another photo app, the user's photos will be in a Photodrop folder.

## Contributing
1. `git clone https://github.com/michaelbishoff/photodrop.git`
2. `cd PhotoDrop`
3. Now you need to change the API Key in app\res\values\google_maps_api.xml to be the API Key for Your package if you change the package name. [Follow these steps](https://developers.google.com/maps/documentation/android/start#get-key)