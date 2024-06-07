package com.github.michaelboyles.s3extension;

import org.apache.maven.wagon.authentication.AuthenticationInfo;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.ContainerCredentialsProvider;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.auth.credentials.WebIdentityTokenFileCredentialsProvider;

import java.util.Objects;

final class S3Auth {
    private S3Auth() {
        throw new UnsupportedOperationException();
    }

    public static AwsCredentialsProviderChain getAuthChain(AuthenticationInfo auth) {
        // This is basically the default order from software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider,
        // with the exceptions that system properties "-D" come before environment variables, and that values from the
        // Maven config is inserted 4th
        var builder = AwsCredentialsProviderChain.builder()
            .addCredentialsProvider(SystemPropertyCredentialsProvider.create())
            .addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .addCredentialsProvider(WebIdentityTokenFileCredentialsProvider.create());
        if (auth != null) {
            String accessKey = Objects.requireNonNull(auth.getUserName());
            String secretKey = Objects.requireNonNull(auth.getPassword());
            builder.addCredentialsProvider(() -> AwsBasicCredentials.create(accessKey, secretKey));
        }
        return builder
            .addCredentialsProvider(ProfileCredentialsProvider.create())
            .addCredentialsProvider(ContainerCredentialsProvider.builder().build())
            .addCredentialsProvider(InstanceProfileCredentialsProvider.builder().build())
            .build();
    }
}
