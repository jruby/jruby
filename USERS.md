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

```markdown
### Name of user (company)

* **Desc**: Description
* **Usage**: Usage of features
* **Since**: How long have you used JRuby (optional)
* **Link**: Link with further information (optional)
* **Contact**: Contacts available for questions (optional)
```

Example entry:

```markdown
### The JRuby Project

* **Desc**: The project that brings you JRuby
* **Usage**: JRuby uses JRuby to build JRuby via our Maven toolchain
* **Since**: 2009
* **Link**: https://github.com/jruby/jruby
* **Contact**: @headius, @enebo
```

Requirements to be listed
-------------------------

 * You must represent the user listed. Do *NOT* add entries on behalf of
   other users.
 * There is no minimum deployment size but we request to list permanent
   production deployments only, i.e., no demo or trial deployments. Commercial
   use is not required. A well-done home lab setup can be equally
   interesting as a large-scale commercial deployment.

Users (most recent first)
----------------------

### AsciidoctorJ

* **Desc:** AsciidoctorJ is a Java port of the Asciidoctor document processor,
a fast and open source text processor and publishing toolchain for converting
AsciiDoc content to HTML 5, PDF and other formats That is implemented in Ruby.
* **Usage:** JRuby is used to run the original Asciidoctor processor inside the JVM. JRuby allows to provide Java APIs and SPIs that make the port feel like a native Java library.
* **Since:** 2014
* **Links:** [asciidoctor.org](https://asciidoctor.org), [asciidoctorj](https://github.com/asciidoctor/asciidoctorj), [docs](https://docs.asciidoctor.org/asciidoctorj/latest/)
* **Contact:** [@robertpanzer](https://github.com/robertpanzer)

### SubstituteAlert Inc.

Additional content in the [JRuby Success Stories](https://github.com/jruby/jruby/wiki/SuccessStories#substitutealert-inc) wiki page.

* **Desc:** We make SubAlert, the top rated substitute teacher app in the U.S. and Canada.
* **Usage:** JRuby is used for scaling this highly parallel application in ways that CRuby can't match. Java integration with the Firebase library allows sending Android notifications, even though there's no official support for Ruby. The JRuby team has been amazingly responsive and we are really happy using JRuby for over 12 years at the time of this writing! 🎉
* **Since:** 2013
* **Links:** [https://www.subalert.com](https://www.subalert.com), [iPhone App](https://apps.apple.com/us/app/subalert-for-frontline-ed/id557785741), [Android App](https://play.google.com/store/apps/details?id=com.substitutealert)
* **Contact:** @mohamedhafez83 (matrix)

### GoCD (Go Continuous Delivery)

* **Desc:** GoCD is an open-source self-hostable continuous delivery / integration automation
server, automating the software build-test-release cycle to enable frequent and reliable
software builds & deployments. It helps teams visualise and streamline complex development workflows 
by allowing teams to visualize their entire delivery process through "pipelines as code"
ensuring that software can be released at any time.
* **Usage:** While originally used more widely within GoCD, JRuby/Rails/[Jruby Rack](https://github.com/jruby/jruby-rack) is still used 
via [Jetty](https://jetty.org/) for server-side rendering of various interfaces for users to interact with their pipeline job runs 
and interoperate with the rest of the Java-based server.
* **Since:** 2007
* **Links:** [gocd.org](https://www.gocd.org/), [source](https://github.com/gocd/gocd), [docs](https://docs.gocd.org/current/)
* **Contact:** [@chadlwilson](https://github.com/chadlwilson)
