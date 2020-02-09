package me.ccampo.awsflake.core

enum class AWSRegion(val region: String) {
    // US East (N. Virginia)
    US_EAST_1("us-east-1"),

    // US East (Ohio)
    US_EAST_2("us-east-2"),

    // US West (N. California)
    US_WEST_1("us-west-1"),

    // US West (Oregon)
    US_WEST_2("us-west-2"),

    // EU (Ireland)
    EU_WEST_1("eu-west-1"),

    // EU (Frankfurt)
    EU_CENTRAL_1("eu-central-1"),

    // Asia Pacific (Tokyo)
    AP_NORTHEAST_1("ap-northeast-1"),

    // Asia Pacific (Seoul)
    AP_NORTHEAST_2("ap-northeast-2"),

    // Asia Pacific (Singapore)
    AP_SOUTHEAST_1("ap-southeast-1"),

    // Asia Pacific (Sydney)
    AP_SOUTHEAST_2("ap-southeast-2"),

    // Asia Pacific (Mumbai)
    AP_SOUTH_1("ap-south-1"),

    // South America (São Paulo)
    SA_EAST_1("sa-east-1");

    companion object {
        fun parse(name: String): AWSRegion {
            return AWSRegion.values().first { r -> name.equals(r.region, ignoreCase = true) }
        }
    }
}
