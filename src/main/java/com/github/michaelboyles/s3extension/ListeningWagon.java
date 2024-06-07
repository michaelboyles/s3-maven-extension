package com.github.michaelboyles.s3extension;

import org.apache.maven.wagon.events.SessionEvent;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.resource.Resource;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * An abstract Wagon which additionally provides all the boilerplate for listeners.
 */
abstract class ListeningWagon extends AbstractWagon {
    private final List<TransferListener> transferListeners = new CopyOnWriteArrayList<>();
    private final List<SessionListener> sessionListeners = new CopyOnWriteArrayList<>();

    @Override
    public void addSessionListener(SessionListener listener) {
        sessionListeners.add(Objects.requireNonNull(listener));
    }

    @Override
    public void removeSessionListener(SessionListener listener) {
        sessionListeners.remove(listener);
    }

    @Override
    public boolean hasSessionListener(SessionListener listener) {
        return sessionListeners.contains(listener);
    }

    protected void fireSessionOpening() {
        notifySessionListeners(SessionEvent.SESSION_OPENING, SessionListener::sessionOpening);
    }

    protected void fireSessionOpened() {
        notifySessionListeners(SessionEvent.SESSION_OPENED, SessionListener::sessionOpened);
    }

    protected void fireSessionDisconnecting() {
        notifySessionListeners(SessionEvent.SESSION_DISCONNECTING, SessionListener::sessionDisconnecting);
    }

    protected void fireSessionDisconnected() {
        notifySessionListeners(SessionEvent.SESSION_DISCONNECTED, SessionListener::sessionDisconnected);
    }

    protected void fireSessionLoggedIn() {
        notifySessionListeners(SessionEvent.SESSION_LOGGED_IN, SessionListener::sessionLoggedIn);
    }

    protected void fireSessionLoggedOff() {
        notifySessionListeners(SessionEvent.SESSION_LOGGED_OFF, SessionListener::sessionLoggedOff);
    }

    private void notifySessionListeners(int eventType, BiConsumer<SessionListener, SessionEvent> handler) {
        SessionEvent event = new SessionEvent(this, eventType);
        for (SessionListener listener : sessionListeners) {
            handler.accept(listener, event);
        }
    }

    @Override
    public void addTransferListener(TransferListener listener) {
        transferListeners.add(Objects.requireNonNull(listener));
    }

    @Override
    public void removeTransferListener(TransferListener listener) {
        transferListeners.remove(listener);
    }

    @Override
    public boolean hasTransferListener(TransferListener listener) {
        return transferListeners.contains(listener);
    }

    protected void fireGetTransferInitiated(Resource resource, File localFile) {
        TransferEvent event = newTransferEvent(
            TransferEvent.TRANSFER_INITIATED, TransferEvent.REQUEST_GET, resource, localFile
        );
        notifyTransferListeners(event, TransferListener::transferInitiated);
    }

    protected void firePutTransferInitiated(Resource resource, File localFile) {
        TransferEvent event = newTransferEvent(
            TransferEvent.TRANSFER_INITIATED, TransferEvent.REQUEST_PUT, resource, localFile
        );
        notifyTransferListeners(event, TransferListener::transferInitiated);
    }

    protected void fireGetTransferStarted(Resource resource, File localFile) {
        TransferEvent event = newTransferEvent(
            TransferEvent.TRANSFER_STARTED, TransferEvent.REQUEST_GET, resource, localFile
        );
        notifyTransferListeners(event, TransferListener::transferStarted);
    }

    protected void firePutTransferStarted(Resource resource, File localFile) {
        TransferEvent event = newTransferEvent(
            TransferEvent.TRANSFER_STARTED, TransferEvent.REQUEST_PUT, resource, localFile
        );
        notifyTransferListeners(event, TransferListener::transferStarted);
    }

    protected void fireGetTransferProgress(Resource resource, File destination, byte[] buffer, int length) {
        TransferEvent event = newTransferEvent(
            TransferEvent.TRANSFER_PROGRESS, TransferEvent.REQUEST_GET, resource, destination
        );
        for (TransferListener listener : transferListeners) {
            listener.transferProgress(event, buffer, length);
        }
    }

    protected void firePutTransferProgress(Resource resource, File destination, byte[] buffer, int length) {
        TransferEvent event = newTransferEvent(
            TransferEvent.TRANSFER_PROGRESS, TransferEvent.REQUEST_PUT, resource, destination
        );
        for (TransferListener listener : transferListeners) {
            listener.transferProgress(event, buffer, length);
        }
    }

    protected void fireGetTransferCompleted(Resource resource, File localFile) {
        TransferEvent event = newTransferEvent(
            TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_GET, resource, localFile
        );
        notifyTransferListeners(event, TransferListener::transferCompleted);
    }

    protected void firePutTransferCompleted(Resource resource, File localFile) {
        TransferEvent event = newTransferEvent(
            TransferEvent.TRANSFER_COMPLETED, TransferEvent.REQUEST_PUT, resource, localFile
        );
        notifyTransferListeners(event, TransferListener::transferCompleted);
    }

    private TransferEvent newTransferEvent(int eventType, int requestType, Resource resource, File localFile) {
        TransferEvent event = new TransferEvent(this, resource, eventType, requestType);
        event.setTimestamp(System.currentTimeMillis());
        event.setLocalFile(localFile);
        return event;
    }

    private void notifyTransferListeners(TransferEvent event, BiConsumer<TransferListener, TransferEvent> handler) {
        for (TransferListener listener : transferListeners) {
            handler.accept(listener, event);
        }
    }
}
