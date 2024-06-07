package com.github.michaelboyles.s3extension;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;

public final class S3Wagon extends ListeningWagon {
    @Override
    public void get(String resourceName, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
    }

    @Override
    public boolean getIfNewer(String resourceName, File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        return false;
    }

    @Override
    public void put(File source, String destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
    }

    @Override
    public boolean supportsDirectoryCopy() {
        return false;
    }

    @Override
    public void connect(Repository source, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider) throws ConnectionException, AuthenticationException {
    }

    @Override
    public void disconnect() throws ConnectionException {
    }
}
