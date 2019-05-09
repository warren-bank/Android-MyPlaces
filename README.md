#### [MyPlaces](https://github.com/warren-bank/Android-MyPlaces)

Android app that reads standard GPX and KML file formats as organized collections of geo bookmarks.

#### Features:

* choose a directory
  * see list of GPX and KML files in the chosen directory
* click on file
  * see list of all geolocation points in the chosen file
  * sort:
    * sequential, ordered as the points occur in the file
    * alphabetic
    * by distance
      * refresh button will recalculate distance from current position, and update the sort order
* click on geolocation point
  * open in external mapping/navigation app (ex: [Google Maps](https://play.google.com/store/apps/details?id=com.google.android.apps.maps), [OsmAnd](https://play.google.com/store/apps/details?id=net.osmand.plus))

#### Use Cases:

* sequential sort order can be used to plan a travel itinerary
* distance sort order can be used to sightsee without any planning
* alphabetic sort order can be used to bookmark favorite places

#### Credits:

* uses [Android-DirectoryChooser](https://github.com/passy/Android-DirectoryChooser) by [Pascal‏‏ Harti‏g](https://github.com/passy) with [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0) license
  * the Activity that allows the user to choose a directory
* uses [jOOX](https://github.com/jOOQ/jOOX) by [jOOQ](https://github.com/jOOQ) with [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0) license
  * an XML parser with an API very similar to jQuery that is used to extract data from GPX and KML files

#### Legal:

* copyright: [Warren Bank](https://github.com/warren-bank)
* license: [GPL-2.0](https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt)
