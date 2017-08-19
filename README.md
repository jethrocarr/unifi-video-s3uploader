# UniFi Video S3 Uploader

This companion Java application runs on any server running the Ubiquiti UniFi NVR software. It regularly polls the
MongoDB database used by the NVR software and when a video event occurs, exports the video and uploads it directly
into an S3 bucket.

Warning: This connector is *still in development and is terrible code that needs a refactor and much more unit testing*.


# Requirements

* Unifi Video controller 3.6.0+
* JDK 7 or later
* An Amazon S3 bucket.


# Usage

You must provide the API key for one of the users inside the Unifi Video software (see the user management page to
configure these).

    export UNIFI_API_KEY=abc123

And you must supply AWS IAM credentials, region and the S3 bucket to use:

    export S3_BUCKET=example
    export AWS_ACCESS_KEY_ID=ap-southeast-2
    export AWS_ACCESS_KEY_ID=??
    export AWS_SECRET_ACCESS_KEY=??
    
    java -jar -Xmx128M JARFILE

As this is a relatively simple connector, the memory allocation is low (128MB). It runs on JDK 7 and above.


# Build & Execution

Development is done with IntelliJ, however any Gradle & Java compatible IDE should work.

Standard gradle build commands can be used to build and run the application:

    gradle bootRun

A standalone self-contained JAR executable can be built and run with:

    gradle bootRepackage
    LATEST=`find build -name '*.jar' | tail -n1`

    export UNIFI_API_KEY=foobar
    export AWS_ACCESS_KEY_ID=ap-southeast-2
    export AWS_ACCESS_KEY_ID=??
    export AWS_SECRET_ACCESS_KEY=??
  
    java -jar -Xmx128M $LATEST


# Testing

We aim for 80%+ code coverage with unit and integration tests. The tests can be executed with:

    gradle test

If the tests fail, you can obtain additional information with the --info parameter. This can show errors such as missing configuration causing faults with the test suite.

    gradle test --info

Note that the tests require access to a function AWS account as a number of the tests take place against AWS service endpoints.

It is possible to bypass tests by adding -x test to your normal gradle commands, for example:

    gradle bootRun -x test

This of course is not recommended, but it can be useful if you need to separate the build task and the testing task (eg as part of a CI/CD workflow).


# Contributions

All contributions are welcome via Pull Requests including documentation fixes,
unit testing and better functionality. Note that new Java code should have
associated unit tests in order to be accepted.

# License

    Unifi Video S3 Uploader is licensed under the Apache License, Version 2.0 (the "License").
    See the LICENSE.txt or http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
