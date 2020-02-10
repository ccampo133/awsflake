FROM oracle/graalvm-ce:19.3.0-java8 as graalvm
COPY . /home/app/awsflake
WORKDIR /home/app/awsflake
RUN gu install native-image
RUN native-image --no-server --static -cp build/libs/awsflake-*-all.jar

FROM frolvlad/alpine-glibc
EXPOSE 8080
COPY --from=graalvm /home/app/awsflake/awsflake /app/awsflake
ENTRYPOINT ["/app/awsflake", "-Djava.library.path=/app"]
