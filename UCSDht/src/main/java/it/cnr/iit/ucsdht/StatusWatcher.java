package it.cnr.iit.ucsdht;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;

public class StatusWatcher {

    private static final long T1_DURATION = 5000; // 5 seconds for the hard timeout
    private static final long T2_DURATION = 1000; // 1 second for the reschedulable soft timeout

    private final List<Path> directories;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduler;
    private final WatchService watchService;
    private ScheduledFuture<?> t1Task;
    private ScheduledFuture<?> t2Task;
    private long firstEventTime = -1;

    public StatusWatcher(List<Path> directories) throws IOException {
        this.directories = directories;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.executorService = Executors.newFixedThreadPool(1);
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void startMonitoring() throws IOException {

        for (Path directory : directories) {
            directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
        }

        executorService.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        Path eventPath = (Path) event.context();

                        // Ignore hidden files (files starting with '.') and "-journal" files
                        if (!eventPath.getFileName().toString().startsWith(".") &&
                                !eventPath.getFileName().toString().endsWith("-journal")) {
                            // Handle different event types
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                System.out.println("File created: " + eventPath.getFileName());
                            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                System.out.println("File modified: " + eventPath.getFileName());
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                System.out.println("File deleted: " + eventPath.getFileName());
                            }

                            // Start or reschedule the hybrid timeout logic
                            handleFileEvent(eventPath.getFileName());
                        }
                    }

                    if (!key.reset()) {
                        System.err.println("The directory is inaccessible.");
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Folder monitoring thread interrupted.");
            }
        });
    }

    private void handleFileEvent(Path fileName) {
        System.out.println("Detected file event for: " + fileName);

        long currentTime = System.currentTimeMillis();
        if (firstEventTime == -1) {
            // First event, start both T1 and T2
            firstEventTime = currentTime;
            startT1();
        }

        rescheduleT2(currentTime);
    }

    private void startT1() {
        t1Task = scheduler.schedule(() -> {
            uploadStatus();
            resetTimeouts();
        }, T1_DURATION, TimeUnit.MILLISECONDS);
    }

    private void rescheduleT2(long currentTime) {
        // Cancel any existing T2 task to avoid it firing unexpectedly
        if (t2Task != null && !t2Task.isCancelled()) {
            t2Task.cancel(false);
        }

        long timeUntilT1Ends = T1_DURATION - (currentTime - firstEventTime);
        long t2Interval = Math.min(T2_DURATION, timeUntilT1Ends);

        // If `t2Interval` is less than `timeUntilT1Ends`, schedule `T2`
        if (t2Interval < timeUntilT1Ends) {
            t2Task = scheduler.schedule(() -> {
                uploadStatus();
                resetTimeouts();
            }, t2Interval, TimeUnit.MILLISECONDS);
        }
    }

    private void uploadStatus() {
        UCSDht.uploadStatus();
    }

    private void resetTimeouts() {
        if (t1Task != null) {
            t1Task.cancel(false);
        }
        if (t2Task != null) {
            t2Task.cancel(false);
        }
        firstEventTime = -1;
    }

    public void stopMonitoring() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
