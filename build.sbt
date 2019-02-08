name := "ActorTests"

version := "1.0"

scalaVersion := "2.12.8"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies +=
	"com.typesafe.akka" %% "akka-actor" % "2.5.20"

libraryDependencies += "com.typesafe.akka" % "akka-remote" % "2.0.1"
