# Judicial Manager

This is an application built to assist in running mock trial competitions. This application was
originally designed for South Carolina's Youth in Government Program, but could be applicable
to other programs as well.

This application isn't what I'd consider the best example of quality. There are significant
rough edges and very little automated testing in the webapp layer. Here be dragons, so please
be aware.

## features

This application features the following features:

* Ability to populate and manage teams and judges within a competition
* Ability to schedule matches in a competition
* Automated suggested scheduling based on one of three algorithms:
  * Randomized matching: matches are totally random
  * Opportunity matching: matches with teams whose scores are slightly apart
  * Challenge matching: matches with teams whose scores are very close together
* Ensures various scheduling rules are preserved in the automated algorithm

## Requirements

This application is written in [Scala][scala] using the [Lift Framework][lift]. To be able to
build and run it you'll need to make sure you have a few things on your machine:

* [sbt][sbt] 0.13+ - `brew install sbt` if you're on Mac
* [sass][sass] 3.4+ - `gem install sass`
* [Docker][docker] and Docker Compose

Once both of these are installed and available on your `PATH`, you'll be able to clone the
repository and get rolling.

To bring up the different services that the Judicial Manager depends on:

```
$ docker-compose up -d
```

Once the services are up, then you can start the application:

```
$ sbt
> jetty:start
```

You'll get a slew of output and - eventually - http://localhost:8080 will be running the
Judicial Manager.

[scala]: https://scala-lang.org
[lift]: https://liftweb.net
[sbt]: http://www.scala-sbt.org
[sass]: http://sass-lang.com
[docker]: http://docker.com

## Running

During the first run, an admin user will be created. In development mode, the credentials will
be:

* Email: admin@admin.com
* Password: admin

In production, the admin account described above will be created, but with a random password that
is printed to the log when it first runs. Switch to production mode by setting the run.mode
property.

## About the Authors

This application was designed by [Liz Shinn][liz] and developed by [Matt Farmer][me].

[liz]: http://lizshinndesign.com
[me]: https://farmdawgnation.com
