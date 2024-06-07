package com.github.michaelboyles.s3extension;

import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.repository.RepositoryPermissions;
import org.apache.maven.wagon.resource.Resource;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;

import static com.github.michaelboyles.s3extension.S3Auth.getAuthChain;

public final class S3Wagon extends ListeningWagon {
    private S3Client s3;

    @Override
    public boolean supportsDirectoryCopy() {
        return true;
    }

    @Override
    public void connect(Repository source, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider) throws ConnectionException, AuthenticationException {
        setRepository(source);
        var credentials = getAuthChain(authenticationInfo);
        this.s3 = S3Client.builder().credentialsProvider(credentials).build();
    }

    @Override
    public void get(String resourceName, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = new Resource(resourceName);
        fireGetTransferInitiated(resource, destination);
        fireGetTransferStarted(resource, destination);

        _get(resource, destination);
        fireGetTransferCompleted(resource, destination);
    }

    private void _get(Resource resource, File destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(getBucketName())
                .key(getKey(resource.getName()))
                .build();
            InputStream stream = s3.getObject(request);
            writeStreamToFile(stream, resource, destination);
        }
        catch (NoSuchKeyException e) {
            throw new ResourceDoesNotExistException("Resource " + resource + " does not exist in the repository", e);
        }
        catch (Exception e) {
            throw new TransferFailedException("Transfer failed", e);
        }
        // TODO how about AuthorizationException?
    }

    private String getBucketName() {
        return getRepository().getHost();
    }

    private String getKey(String location) {
        String baseDir = getBaseDir();
        String path = Paths.get(baseDir + location).normalize().toString();
        return path.replace("\\", "/"); // for Windows OS
    }

    private String getBaseDir() {
        StringBuilder sb = new StringBuilder(getRepository().getBasedir());
        sb.deleteCharAt(0);
        if (sb.length() == 0) return "";
        if (sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        return sb.toString();
    }

    private void writeStreamToFile(InputStream objectInputStream, Resource resource, File destination) throws IOException {
        File temp = File.createTempFile(destination.getName() , ".tmp", destination.getParentFile());
        try (InputStream in = objectInputStream; OutputStream out = new FileOutputStream(temp)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
                fireGetTransferProgress(resource, destination, buffer, length);
            }
        }
        // finally, move the temp file. Means that if it fails halfway, we aren't left with half a file
        Files.move(temp.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public boolean getIfNewer(String resourceName, File destination, long timestamp) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        HeadObjectResponse response = s3.headObject(
            HeadObjectRequest.builder()
                .bucket(getBucketName())
                .key(getKey(resourceName))
                .build()
        );
        if (response.lastModified().isAfter(Instant.ofEpochMilli(timestamp))) {
            _get(new Resource(resourceName), destination);
            return true;
        }
        return false;
    }

    @Override
    public void put(File source, String destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        Resource resource = new Resource(destination);
        firePutTransferInitiated(resource, source);
        firePutTransferStarted(resource, source);
        _put(source, resource);
        firePutTransferCompleted(resource, source);
    }

    private void _put(File source, Resource destination) throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        if (!source.exists()) {
            throw new ResourceDoesNotExistException("Source does not exist");
        }
        var putObjectRequest = PutObjectRequest.builder()
            .bucket(getBucketName())
            .key(getKey(destination.getName()))
            .acl(getAccessControlList().orElse(null))
            .build();
        try (InputStream inputStream = newUploadStream(source, destination)) {
            RequestBody body = RequestBody.fromInputStream(inputStream, source.length());
            s3.putObject(putObjectRequest, body);
        }
        catch (Exception e) {
            throw new TransferFailedException("Failed to transfer " + source, e);
        }
    }

    private Optional<ObjectCannedACL> getAccessControlList() {
        return Optional.ofNullable(getRepository())
            .map(Repository::getPermissions)
            .map(RepositoryPermissions::getFileMode)
            .map(String::trim)
            .map(ObjectCannedACL::fromValue);
    }

    @Override
    public boolean resourceExists(String resourceName) {
        try {
            s3.headObject(
                HeadObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(getKey(resourceName))
                    .build()
            );
            return true;
        }
        catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public void disconnect() {
        fireSessionDisconnecting();
        fireSessionLoggedOff();
        S3Client s3 = this.s3;
        if (s3 != null) {
            s3.close();
            this.s3 = null;
        }
        fireSessionDisconnected();
    }

    private InputStream newUploadStream(File source, Resource resource) throws ResourceDoesNotExistException, TransferFailedException {
        try {
            return new PutInputStream(source, resource);
        }
        catch (FileNotFoundException e) {
            throw new ResourceDoesNotExistException("Does not exist", e);
        }
        catch (IOException e) {
            throw new TransferFailedException("Transfer failed", e);
        }
    }

    private class PutInputStream extends FileInputStream {
        private final File source;
        private final Resource resource;

        PutInputStream(File source, Resource resource) throws IOException {
            super(source);
            this.source = source;
            this.resource = resource;
        }

        @Override
        public int read() throws IOException {
            int theByte = super.read();
            firePutTransferProgress(resource, source, new byte[] { (byte) theByte },  1);
            return theByte;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int bytesRead = super.read(buffer, offset, length);
            firePutTransferProgress(resource, source, buffer,  bytesRead);
            return bytesRead;
        }
    }
}
