[![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/michaelboyles/s3-maven-extension/build.yml?branch=develop)](https://github.com/michaelboyles/s3-maven-extension/actions)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/michaelboyles/s3-maven-extension?sort=semver)](https://github.com/michaelboyles/s3-maven-extension/releases)
[![GitHub](https://img.shields.io/github/license/michaelboyles/s3-maven-extension)](https://github.com/michaelboyles/s3-maven-extension/blob/develop/LICENSE)

This [Maven extension](https://maven.apache.org/guides/mini/guide-using-extensions.html) provides support for
Amazon [S3](https://aws.amazon.com/s3/), instead of an artifact repository (e.g. Artifactory, Sonatype Nexus). It provides
a ["wagon"](https://maven.apache.org/wagon/) (a transport abstraction) which lets you use the URL scheme `s3://` where
you'd usually use `https://`.

## Usage

Add the extension to the `<build>` section of your POM, e.g.

```xml
<extensions>
   <extension>
      <groupId>com.github.michaelboyles</groupId>
      <artifactId>s3-maven-extension</artifactId>
      <version>0.5.0</version>
   </extension>
</extensions>
```

Specify the region of your S3 bucket using one of the follow methods, listed with highest precedence first. This uses
[the default AWS SDK logic](https://sdk.amazonaws.com/java/api/2.25.67/software/amazon/awssdk/regions/providers/DefaultAwsRegionProviderChain.html):

1. `aws.region` system property, i.e. `-Daws.region=my-region`
2. `AWS_REGION` environment variable
3. `{user.home}/.aws/credentials` and `{user.home}/.aws/config` files
4. Container metadata (e.g. EC2)

Specify your credentials using on the following methods, listed with highest precedence first.

1. `aws.accessKeyId`, `aws.secretAccessKey` and (optionally) `aws.sessionToken` [system properties](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/auth/credentials/SystemPropertyCredentialsProvider.html),
   e.g. `-Daws.accessKeyId=my-key`
2. `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` and (optionally) `AWS_SESSION_TOKEN` [environment variables](https://sdk.amazonaws.com/java/api/2.25.67/software/amazon/awssdk/auth/credentials/EnvironmentVariableCredentialsProvider.html) 
3. [Web identity token file](https://sdk.amazonaws.com/java/api/2.25.67/software/amazon/awssdk/auth/credentials/WebIdentityTokenFileCredentialsProvider.html)
4. The server 'username' and 'password' fields from Maven [settings.xml](https://maven.apache.org/settings.html#servers), where username is AWS access key ID, and password is
   AWS secret access key
5. [Profile file](https://sdk.amazonaws.com/java/api/2.25.67/software/amazon/awssdk/auth/credentials/ProfileCredentialsProvider.html)
6. [Container metadata](https://sdk.amazonaws.com/java/api/2.25.67/software/amazon/awssdk/auth/credentials/ContainerCredentialsProvider.html) (e.g. EC2)
7. [EC2 Instance Metadata Service](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/auth/credentials/InstanceProfileCredentialsProvider.html)

You're done! You can now use `s3://` scheme in the following format (base directory is optional):

```text
s3://{bucket}/{base-directory}
```

This works in both `<repositories>` and `<pluginRepositories>`, for pulling artifacts, as well as
`<distributionManagement>`, for pushing artifacts.

```xml
<project>
   <!-- Sample section of a POM -->
   <distributionManagement>
      <repository>
         <id>red-team-releases</id>
         <url>s3://red-team-maven/releases</url>
      </repository>
      <repository>
         <id>red-team-snapshots</id>
         <url>s3://red-team-maven/snapshots</url>
      </repository>
   </distributionManagement>
   <repositories>
      <repository>
         <id>blue-team-releases</id>
         <url>s3://blue-team-maven-releases</url>
      </repository>
   </repositories>
</project>
```
---

This repo was written from scratch, but took cues from [seahen/maven-s3-wagon](https://github.com/seahen/maven-s3-wagon),
which in turn was forked from [jcaddel/maven-s3-wagon](https://github.com/jcaddel/maven-s3-wagon/). They are no longer
maintained, and the Amazon SDK they are based on is being phased out.

Major differences:

 - Removed support for plain HTTP
 - Wagon::getFileList and Wagon::putDirectory are not supported. Judging by
   [AbstractWagon](https://github.com/apache/maven-wagon/blob/master/wagon-provider-api/src/main/java/org/apache/maven/wagon/AbstractWagon.java)
   they're optional, and I don't know what they're used for.
 - Won't create the bucket if it doesn't exist

If you need these things then raise an issue explaining your use-case.