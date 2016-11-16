# awsflake

[![Build Status](https://travis-ci.org/ccampo133/awsflake.svg?branch=master)](https://travis-ci.org/ccampo133/awsflake)

A [Snowflake](https://blog.twitter.com/2010/announcing-snowflake)-like service for generating GUIDs within 
multi-regional AWS environments.

AWSFlake generates a 64-bit unique identifier composed of the following information:

 * **41 bits**: time since epoch, in millis (this gives us a max of ~69.7 years from the epoch)
 * **10 bits**: node ID; a manually assigned integer between 0 and 1023
 * **12 bits**: sequence number; allows multiple IDs to be generated within the same millisecond

The ID is presented as a standard 64 bit integer (long).

### Node ID

The node ID needs to be manually assigned at runtime to AWSFlake. If you are running a multi-server 
cluster, the node ID must be unique to each node. That is, no two nodes can have the same node ID.
This is to ensure that the IDs generated are truly unique across the cluster.

### Build and Run

To build AWSFlake, use the Gradle `build` task.

On Unix/Linux/OS X

    ./gradlew build

On Windows

    gradlew.bat build

You can then run the jar file (in the `build/libs` directory) as standalone:

    java -jar awsflake-<version>.jar

AWSFlake can also be built as a Docker image and run in a container (see "deployment" below).
It also accepts various environment variables (or Java system properties), and again, these
are detailed below.

### Deployment

Once deployed, AWSFlake requires no coordination between nodes. You can scale horizontally with up to
1023 total nodes to generate thousands of IDs per second. It is recommended that at least a 2 node
cluster is used, behind a round-robin load balancer.

AWSFlake can be built as a docker container and then deployed onto any of AWS's services 
that support docker. This includes ECS, Elastic Beanstalk, and plain-old EC2 instances.
If using Beanstalk or ECS, PLEASE ENSURE ONLY A SINGLE CONTAINER IS RUN ON EACH INSTANCE.
This is necessary to ensure ID uniqueness.

The AWSFlake docker image can be built with the following command:

    ./gradlew buildDocker
    
You can specify command line properties to the gradle task such as `-PdockerUseSudo=true`
as well. See `gradle.properties` for the list of supported properties.

Once you have the docker image built, you can run a container. The following environment 
variables are accepted as input:
 
  * SERVER_NODEID: the assigned node ID (0 - 1023). THIS IS REQUIRED.
  * SERVER_PORT: the port on which AWSFlake listens on (default: 8080)
  * SERVER_EPOCH: the epoch to calculate timestamps against. The more recent, the better. 
  Use the same value across all nodes in your AWSFlake cluster. (default: Jan 1 2016 00:00:00 UTC)
  
# API

To generate a new ID, simply run the service and send an HTTP request to the `/id` 
endpoint. Example using curl:

    $ curl -v -X GET "http://localhost:8080/id"
    *   Trying ::1...
    * Connected to localhost (::1) port 8080 (#0)
    > GET /id?minLength=13 HTTP/1.1
    > Host: localhost:8080
    > User-Agent: curl/7.43.0
    > Accept: */*
    >
    < HTTP/1.1 200 OK
    < Date: Mon, 14 Nov 2016 21:41:10 GMT
    < Content-Type: text/html;charset=utf-8
    < Transfer-Encoding: chunked
    < Server: Jetty(9.3.z-SNAPSHOT)
    <
    * Connection #0 to host localhost left intact
    2349816721408
    
The generated ID in the above example is `2349816721408`.
