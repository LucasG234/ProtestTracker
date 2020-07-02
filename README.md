# Protest Tracker

## Table of Contents
1. [Overview](#Overview)
1. [Product Spec](#Product-Spec)
1. [Wireframes](#Wireframes)
2. [Schema](#Schema)

## Overview
### Description
Android app which allows users to stay informed about local protests and easily join them. Protesters can post information about locations and breakouts of looting or brutality with accompanying photo or videos. Other users can access their local information to easily join protests and avoid hotspots of violence.

### App Evaluation
[Evaluation of your app across the following attributes]
- **Category:** Social Networking
- **Mobile:** Mobile is essential to allow users to continue posting and receiving information actively while protesting or traveling to a protest. The app can also provide video and audio recording options to catch unexpected events.
- **Story:** Allows existing protesters to stay informed and avoid danger and trouble while making it easier for new users to begin demonstrating for the things they care about.
- **Market:** Any person who attends protests, is interested in attending a protest you hasn't before, or wants to view information about protesting locally or globally.
- **Habit:** Protesters will use this app consistently before and during a protest to find and post new information.
- **Scope:** V1 would allow text and photo posts with geographical integration. V2 would make posts naturally disapear as they become outdated (with users voting to extend or shorten the time). V3 would add more integration with user profiles and history. V4 would allow video and audio to be added into posts.

## Product Spec

### 1. User Stories (Required and Optional)

**Required Must-have Stories**

* Users can view all local posts about protests in their home view
* Users can sign into accounts to post information about protests
* Users can create new accounts while staying on the app
* Users can make text posts giving information about current protests
    * Posts can contain images sourced from the camera roll or taken immediately from the camera 
    * Posts are tied to the geographical location where they were made
* Users can go to a map view to see geographical information about local and global protesting
* App uses gesture recognition for some navigation
* App contains animation
* App utilizes a third party library for visual polish

**Optional Nice-to-have Stories**

* Ability to connect acounts to social media to make sharing effortless
* Users can enable always-on recording to ensure they catch evidence of any important events
* Posts can be clicked on to view additional detail
* Users can comment on posts
* Posts automatically disapear based on time elapsed
    * Users can flag posts as "unhelpful" or "irrelevant" to make them disapear qucker
    * Users can vote to extend the lifetime of posts which continue to be relevant
* Users can become "community leaders" whose posts are prioritized because of a proven history of truthfulness and usefulness
* Users their own profile page with their post history
* Users can view other user's profiles
* Posts can also include audio and/or video

### 2. Screen Archetypes

* Login Screen
   * Users can sign into accounts to post information about protests
* Registration Screen
   * Users can create new accounts while staying on the app
* Home Stream
    * Users can view all local posts about protests in their home view
* Creation Screen
    * Users can make text posts giving information about current protests
* Map View
    * Users can go to a map view to see geographical information about local and global protesting

### 3. Navigation

**Tab Navigation** (Tab to Screen)

* Home Stream
* Map View
* Creation Screen

**Flow Navigation** (Screen to Screen)

* Login Screen
   ==> Home
* Registration Screen
   ==> Home
* Home Stream
    ==> None, but future version likely involves navigation to post detail screen
* Creation Screen
   ==> Home (After you finish posting)
* Map View
   ==> None, but future version likely involves navigation to post detail screen
<!---
## Wireframes
[Add picture of your hand sketched wireframes in this section]
<img src="YOUR_WIREFRAME_IMAGE_URL" width=600>

### [BONUS] Digital Wireframes & Mockups

### [BONUS] Interactive Prototype

## Schema 
[This section will be completed in Unit 9]
### Models
[Add table of models]
### Networking
- [Add list of network requests by screen ]
- [Create basic snippets for each Parse network request]
- [OPTIONAL: List endpoints if using existing API such as Yelp]
--->
