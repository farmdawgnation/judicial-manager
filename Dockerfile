FROM jetty:9.4.6

COPY target/scala-2.12/judicial-manager.war /var/lib/jetty/webapps/ROOT.war
