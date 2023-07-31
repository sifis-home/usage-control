package it.cnr.iit.ucsdht;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatusWatcher {

    private final List<Path> directories;
    private final ExecutorService executorService;
    private final WatchService watchService;

    public StatusWatcher(List<Path> directories) throws IOException {
        this.directories = directories;
        this.watchService = FileSystems.getDefault().newWatchService();
        this.executorService = Executors.newFixedThreadPool(1);
    }

    public void startMonitoring() throws IOException {

        for(Path directory : directories) {
            directory.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
        }

        executorService.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    WatchKey key = watchService.take(); // Wait for events

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        Path eventPath = (Path) event.context();

                        // Ignore hidden files (files starting with '.')
                        if (!eventPath.getFileName().toString().startsWith(".")) {
                            // Handle different event types
                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                System.out.println("File created: " + eventPath.getFileName());
                            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                System.out.println("File modified: " + eventPath.getFileName());
                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                System.out.println("File deleted: " + eventPath.getFileName());
                            }
                            UCSDht.uploadStatus();
                        }
                    }

                    key.reset();
                }
            } catch (InterruptedException e) {
                System.out.println("Folder monitoring thread interrupted.");
            }
        });
    }

    public void stopMonitoring() {
        if (executorService != null) {
            executorService.shutdownNow();
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