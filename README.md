# awsflake

[![Build Status](https://travis-ci.org/ccampo133/awsflake.svg?branch=master)](https://travis-ci.org/ccampo133/awsflake)

A [Snowflake](https://blog.twitter.com/2010/announcing-snowflake)-like service for generating GUIDs within 
multi-regional AWS environments.

AWSFlake generates a 77-bit unique identifier composed of the following information:

 * **41 bits**: time since epoch, in millis (this gives us a max of ~69.7 years from the epoch)
 * **5 bits**: region identifier (1-32, see "regions")
 * **16 bits**: last two octets of private IP (unique per VPC /16 netmask)
 * **15 bits**: sequence number; allows multiple IDs to be generated within the same milisecond

The ID is [base 62 encoded](https://www.kerstner.at/2012/07/shortening-strings-using-base-62-encoding/),
which is similar to base 64 encoding minus the non-alphanumeric characters (the charset [0-9A-Za-z]
is used).

### Regions
The region identifier is an integer, 0-31, that corresponds to an AWS region. This mapping is 
hardcoded currently and this mapping can be viewed in AWSRegion.kt. Currently there are only
12 AWS regions, but this code can support up to a total of 32.

### IP Address (IP)

The AWS EC2 instance private IP is used in the unique identifier generation algorithm. Since
largest CIDR block AWS hands out for individual VPCs is /24, that directly implies that the
last two octets of an instance's private IP can uniquely identify said instance within a
single VPC.

Combined with the region identifier, and you can uniquely identify an EC2 instance in a 
multi-region AWS cluster. THIS DOES NOT WORK FOR MULTI-VPC CLUSTERS WITHIN THE SAME VPC,
as the last octets of the IP address are only guaranteed to be unique within a single VPC.

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

Once deployed, AWSFlake requires no coordination between nodes. You can scale horizontally with
thousands of nodes to generate thousands of IDs per second. It is recommended that at least a 2 node
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
 
  * SERVER_PORT: the port on which AWSFlake listens on
  * SERVER_EPOCH: the epoch to calculate timestamps against. The more recent, the better. 
  Use the same value across all nodes in your AWSFlake cluster. 
  
  Additionally the following variables *may* be provided for debugging/testing only. If 
  omitted, they will be supplied by the Amazon EC2 metadata service automatically.
  
  * SERVER_REGION: the region of the node (ex: us-east-1)
  * SERVER_IP: the private IP address of the node

# API

To generate a new ID, simply run the service and send an HTTP request to the `/id` 
endpoint. The `minLength` query parameter can be provided to ensure IDs are returned
with a minimum number of characters. Example using curl:

    $ curl -v -X GET "http://localhost:8080/id?minLength=13"
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
    0aDuEi4sFesTo
    
The generated, base 62 encoded 13 character ID in the above example is 
`0aDuEi4sFesTo`.
