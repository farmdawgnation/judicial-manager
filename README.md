# Judicial Manager

This is an application built to assist in running mock trial competitions. This application was
originally designed for South Carolina's Youth in Government Program, but could be applicable
to other programs as well.

We're still in development, but once things have stabalized we'll include a run-through of the
available features here.

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

## About the Authors

This application was designed by [Liz Shinn][liz] and developed by [Matt Farmer][me].

[liz]: http://lizshinndesign.com
[me]: https://farmdawgnation.com
