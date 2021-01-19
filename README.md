# OpenStreetMap bus-route maintenance using GPS data 

A [project](https://www.cst.cam.ac.uk/teaching/part-ii/projects) for Part II of the Computer Science Tripos at Cambridge.

## Introduction

Bus-routes are constantly changing as new bus services are created, old bus services are retired, and active bus services are updated. The current route map system is poorly equipped to reflect this dynamic environment, consisting of an outdated and static map being replicated at each stop, thereby requiring an unrealistic effort to update. A new route map is desperately needed; one that can be found at a single location and is easily modifiable.

[OpenStreetMap](https://www.openstreetmap.org) is an example of such a route map system, maintained by a community of volunteer editors. However, without being a regular user of the buses in a particular area, it is difficult for editors to create such a map. It is even harder for editors to identify when a mapped bus-route is no longer accurate.

These difficulties can be overcome with the help of GPS trackers present in today's buses. The information extracted from this GPS data would not only allow for automatic detection of invalid sections of OpenStreetMap, but could also be used to calculate the modifications required to make such sections valid. The goal of this project is to create a tool that can provide this functionality.

## Installation

The latest release of this plug-in can be found in the [releases](https://github.com/archwheeler/bus-route-maintenance/releases) section.

This plug-in is not in the official list of JOSM plug-ins and must therefore be [installed manually](https://wiki.openstreetmap.org/wiki/JOSM/Plugins#Manually_install_JOSM_plugins).

Make sure to enable the plug-in after installing it: Edit > Preferences > Plugins.