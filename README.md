# awsflake

[![Download](https://api.bintray.com/packages/ccampo133/public/awsflake/images/download.svg)](https://bintray.com/ccampo133/public/awsflake/_latestVersion)
[![](https://github.com/ccampo133/awsflake/workflows/Build%20master/badge.svg)](https://github.com/{owner}/{repo}/actions) 

A [Snowflake](https://blog.twitter.com/2010/announcing-snowflake)-like service for generating GUIDs within 
multi-regional AWS environments.

## Background

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

## Usage

### As a Library

Perhaps the simplest usage of `awsflake` is as a library. You can use `awsflake-core` as a typical Kotlin or Java library 
(Groovy, Scala, and other JVM languages _should_ also be supported theoretically, but this has not been tested yet).

**Gradle**

```groovy
implementation 'me.ccampo:awsflake-core:2.0.0'
```

**Maven**

```xml
<dependency>
  <groupId>me.ccampo</groupId>
  <artifactId>awsflake-core</artifactId>
  <version>2.0.0</version>
</dependency>
```

The API is simple - instantiate an `IDGenerator` with the provided `IDGenerator.Builder` class, and then use that 
to start generating IDs.

```kotlin
val epoch = Instant.parse("2016-01-01T00:00:00Z")
val region = AWSRegion.US_EAST_1
val ip = "10.0.128.255"

val generator = IDGenerator.Builder()
        .epoch(epoch)
        .region(reg)
        .ip(ip)
        .build()

val id = generator.nextId()
```

See the source and KDocs for more detailed usage information.

### As a Microservice

`awsflake` can also packaged as a microservice, powered by [Micronaut](https://micronaut.io/).

In the `server` directory, there is a README with additonal information, but in general, the most
basic use case is to build a docker image of the service, and then call it via http. There is a
`build-docker.sh` script to build the image. Additionally, you can simply build the project, and a
fat-jar will be produced which can be executed directly. For example:

```
# In the service/build/libs directory...

$ java -jar awsflake-server-2.0.0-all.jar
15:43:58.950 [main] INFO  io.micronaut.runtime.Micronaut - Startup completed in 4775ms. Server Running: http://localhost:8080

# And then to generate IDs..

$ curl http://localhost:8080/id
57NO5HEVlVCa
```

## Deployment

Once deployed, AWSFlake requires no coordination between nodes. You can scale horizontally with
thousands of nodes to generate thousands of IDs per second. It is recommended that at least a 2 node
cluster is used, behind a round-robin load balancer.

AWSFlake can be built as a docker container and then deployed onto any of AWS's services 
that support docker. This includes ECS, Elastic Beanstalk, and plain-old EC2 instances.
If using Beanstalk or ECS, PLEASE ENSURE ONLY A SINGLE CONTAINER IS RUN ON EACH INSTANCE.
This is necessary to ensure ID uniqueness.

Once you have the docker image built, you can run a container. The following environment 
variables are accepted as input:
 
  * MICRONAUT_SERVER_PORT: the port on which AWSFlake listens on
  
Additionally the following variables *may* be provided for debugging/testing only. If 
omitted, they will be supplied by the Amazon EC2 metadata service automatically.
  
  * AWSFLAKE_REGION: the region where this is deployed. This is usually not necessary and
  the service will default to the region where the code is running, using AWS's EC2 metadata
  service.
  * AWSFLAKE_IP: the IPv4 address of the node. Again, this is also optional and will default
  to the IP returned by the EC2 metadata service.
  * AWSFLAKE_EPOCH: the epoch to calculate timestamps against. The more recent, the better. 
  Use the same value across all nodes in your AWSFlake cluster. Defaults to Jan 1 2020.

### API

To generate a new ID, simply run the service and send an HTTP request to the `/id` 
endpoint. The `minLength` query parameter can be provided to ensure IDs are returned
with a minimum number of characters. Example using curl:

    $ curl -X GET "http://localhost:8080/id?minLength=13"
    0aDuEi4sFesTo
    
The generated, base 62 encoded 13 character ID in the above example is 
`0aDuEi4sFesTo`.

## Development

Requires Java 8

### To Build

macOS or *nix:

    ./gradlew build

Windows:    

    gradlew.bat build    

Additionally, each sub-module can be built in the same fashion.

#### Publishing to Bintray

    ./gradlew -PbintrayUser="..." -PbintrayKey="..." clean build dokkaJar bintrayUpload
