package uk.gov.companieshouse.officerssearch.subdelta.common.exception;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageFlagsTest {

    private MessageFlags messageFlags;

    @BeforeEach
    void setUp() {
        messageFlags = new MessageFlags();
    }

    @AfterEach
    void tearDown() {
        // The flag lives in a static ThreadLocal shared by every instance, so a value left
        // set on the test thread would otherwise leak into the next test.
        messageFlags.destroy();
    }

    @Test
    @DisplayName("isRetryable defaults to false when nothing has been set on this thread")
    void isRetryableDefaultsToFalse() {
        assertFalse(messageFlags.isRetryable());
    }

    @Test
    @DisplayName("setRetryable(true) makes isRetryable return true")
    void setRetryableTrueMakesIsRetryableTrue() {
        messageFlags.setRetryable(true);

        assertTrue(messageFlags.isRetryable());
    }

    @Test
    @DisplayName("setRetryable(false) makes isRetryable return false")
    void setRetryableFalseMakesIsRetryableFalse() {
        messageFlags.setRetryable(true);
        messageFlags.setRetryable(false);

        assertFalse(messageFlags.isRetryable());
    }

    @Test
    @DisplayName("destroy() clears the flag back to the unset default")
    void destroyClearsFlagBackToDefault() {
        messageFlags.setRetryable(true);

        messageFlags.destroy();

        assertFalse(messageFlags.isRetryable());
    }

    @Test
    @DisplayName("The flag is backed by a single static ThreadLocal, so a second instance on the same thread shares its value")
    void flagIsSharedAcrossInstancesOnTheSameThread() {
        MessageFlags otherInstance = new MessageFlags();

        messageFlags.setRetryable(true);

        assertTrue(otherInstance.isRetryable());
    }

    @Test
    @DisplayName("The flag is thread-local: setting it on one thread does not affect another")
    void flagDoesNotLeakAcrossThreads() throws InterruptedException {
        messageFlags.setRetryable(true);

        AtomicBoolean retryableOnOtherThread = new AtomicBoolean(true);
        Thread otherThread = new Thread(() -> retryableOnOtherThread.set(messageFlags.isRetryable()));
        otherThread.start();
        otherThread.join();

        assertFalse(retryableOnOtherThread.get());
        // The flag this test thread set is untouched by the other thread's read.
        assertTrue(messageFlags.isRetryable());
    }
}