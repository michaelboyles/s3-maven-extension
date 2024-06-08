package io.github.michaelboyles.s3extension;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * A better implementation of {@link org.apache.maven.wagon.AbstractWagon}. It does ONLY the bare minimum of managing
 * some basic fields, various defaults for method overloads (which should be default methods on the interface...), and
 * making some methods "optional" by throwing {@link UnsupportedOperationException}.
 */
abstract class AbstractWagon implements Wagon {
    private Repository repository;
    private int timeout;
    private int readTimeout;
    private boolean isInteractive;

    @Override
    public Repository getRepository() {
        return repository;
    }

    protected void setRepository(Repository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public int getReadTimeout() {
        return readTimeout;
    }

    @Override
    public boolean isInteractive() {
        return isInteractive;
    }

    @Override
    public void setInteractive(boolean isInteractive) {
        this.isInteractive = isInteractive;
    }

    @Override
    @Deprecated
    public void openConnection() {
        throw new RuntimeException("Never call openConnection");
    }

    @Override
    public void connect(Repository repository) throws ConnectionException, AuthenticationException {
        connect(repository, null, (ProxyInfoProvider) null);
    }

    @Override
    public void connect(Repository repository, ProxyInfo proxyInfo) throws ConnectionException, AuthenticationException {
        connect(repository, null, proxyInfo);
    }

    @Override
    public void connect(Repository repository, ProxyInfoProvider proxyInfoProvider) throws ConnectionException, AuthenticationException {
        connect(repository, null, proxyInfoProvider);
    }

    @Override
    public void connect(Repository repository, AuthenticationInfo authenticationInfo) throws ConnectionException, AuthenticationException {
        connect(repository, authenticationInfo, (ProxyInfoProvider) null);
    }

    @Override
    public void connect(Repository repository, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo)
        throws ConnectionException, AuthenticationException
    {
        final ProxyInfo proxy = proxyInfo;
        connect(repository, authenticationInfo, protocol -> {
            if (protocol == null || proxy == null || protocol.equalsIgnoreCase(proxy.getType())) {
                return proxy;
            }
            return null;
        });
    }

    @Override
    public List<String> getFileList(String destinationDirectory) {
        throw new UnsupportedOperationException("getFileList is not supported");
    }

    @Override
    public void putDirectory(File sourceDirectory, String destinationDirectory) {
        throw new UnsupportedOperationException("putDirectory is not supported");
    }
}
