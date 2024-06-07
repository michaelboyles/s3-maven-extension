[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/michaelboyles/s3-maven-extension/build.yml?branch=develop)](https://github.com/michaelboyles/s3-maven-extension/actions)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/michaelboyles/s3-maven-extension?sort=semver)](https://github.com/michaelboyles/s3-maven-extension/releases)
[![GitHub](https://img.shields.io/github/license/michaelboyles/s3-maven-extension)](https://github.com/michaelboyles/s3-maven-extension/blob/develop/LICENSE)

This [Maven extension](https://maven.apache.org/guides/mini/guide-using-extensions.html) provides support for
Amazon [S3](https://aws.amazon.com/s3/). It provides a ["wagon"](https://maven.apache.org/wagon/) (a transport
abstraction) which lets you use the S3 scheme `s3://` in place of HTTP(S).

---

This repo was written from scratch, but took cues from [seahen/maven-s3-wagon](https://github.com/seahen/maven-s3-wagon),
which in turn was forked from [jcaddel/maven-s3-wagon](https://github.com/jcaddel/maven-s3-wagon/). They are no longer
maintained, and the Amazon SDK they are based on is being phased out.

Major differences:

 - Removed support for plain HTTP
 - Wagon::getFileList and Wagon::putDirectory are not supported. Judging by
   [AbstractWagon](https://github.com/apache/maven-wagon/blob/master/wagon-provider-api/src/main/java/org/apache/maven/wagon/AbstractWagon.java)
   they're optional, and I don't know what they're used for.

If you need these things then let me know.