Who is using JRuby?
====================

Sharing experiences and learning from other users is essential. We are
frequently asked who is using a particular feature of JRuby so people can get in
contact with other users to share experiences and best practices. People
also often want to know if product/platform X supports JRuby.
While the [JRuby Matrix community](https://matrix.to/#/#jruby:matrix.org) allows
users to get in touch, it can be challenging to find this information quickly.

The following is a directory of adopters to help identify users of individual
features. The users themselves directly maintain the list.

Adding yourself as a user
-------------------------

If you are using JRuby or it is integrated into your product, service, or
platform, please consider adding yourself as a user with a quick
description of your use case by [opening a pull request to this file](https://github.com/jruby/jruby/blob/master/USERS.md)
and adding a section describing your usage of JRuby:

If you are open to others contacting you about your use of JRuby, add your
Matrix nickname or contact info as well.

    Name: Name of user (company)
    Desc: Description
    Usage: Usage of features
    Since: How long have you used JRuby (optional)
    Link: Link with further information (optional)
    Contact: Contacts available for questions (optional)

Example entry:

    * Name: The JRuby Project
      Desc: The project that brings you JRuby
      Usage: JRuby uses JRuby to build JRuby via our Maven toolchain
      Since: 2009
      Link: https://github.com/jruby/jruby
      Contact: @headius, @enebo

Requirements to be listed
-------------------------

 * You must represent the user listed. Do *NOT* add entries on behalf of
   other users.
 * There is no minimum deployment size but we request to list permanent
   production deployments only, i.e., no demo or trial deployments. Commercial
   use is not required. A well-done home lab setup can be equally
   interesting as a large-scale commercial deployment.

Users (Alphabetically)
----------------------



### SubstituteAlert Inc.

**Desc:** We make SubAlert, the top rated substitute teacher app in the U.S. and Canada. We're an officially-approved 3rd party app for substitutes whose school districts use [Frontline Education](https://www.frontlineeducation.com/)'s software for teacher absence management. Thousands of subs rely on us for quick notifications of new jobs at their district, one-click booking of their jobs from their lock screen, and for conveniences like syncing their work calendar to Google Calender or the iPhone Calendar app

**Usage:** Ok, so for each of our thousands of subs, we need to hit an API endpoint to get a list of their available jobs once per minute right? How do you do this with stock Ruby, where you can only make use of one processor at a time? Well, you've got to split your users into groups and assign them roughly equally to a bunch of worker threads that can each use a single core. With users cancelling and new users creating accounts, you'll have to coordinate which worker gets which worker to keep the load balanced, etc. Does each worker lock up and sit idle while you are waiting on a network response from the API? Not sure about the latest C-Ruby, but earlier C-Rubies did for sure. Do i need multiple workers per core then to make full use of each core, or to use C-Ruby multiplexed threads inside of each worker? What about restarting all the workers when a new version is out, you've got to write something to coordinate that as well, and as well as monitor each worker that freezes up or crashes, etc. What a mess. It is so, so much more straightforward with the real threads provided by **JRuby**. I just have one giant, multithreaded background job checker process, and I don't need to worry about *ANY* of that crazy stuff. If you're comfortable with threads, thats the obvious, easy solution.<br> 
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;In addition, being able to pull in Java libraries can be really useful. For example, the official Firebase library to be able to send push notifications to Android (among a lot of other functionality), is not available in Ruby, but is in Java. Not a problem for us, just list it as a requirment from maven and get to work, thanks to JRuby. In the past when Apple switched to using HTTP/2 for its notification channels, there was no solid Ruby library available at the time. Not a problem for us, we just grabbed the most popular enterprise-grade Apple push notification library from the Java world.<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Not to mention, responsiveness to issues from the maintainers is amazing, they are so helpful in the rare cases you run into issues, or if you have requests, or if you just need help with anything! We've been really happy using JRuby for a bit over 12 years at the time of this writing! 🎉

**Since:** 2013

**Links:** [https://www.subalert.com](https://www.subalert.com), [iPhone App](https://apps.apple.com/us/app/subalert-for-frontline-ed/id557785741), [Android App](https://play.google.com/store/apps/details?id=com.substitutealert)

**Contact:** @mohamedhafez83 
