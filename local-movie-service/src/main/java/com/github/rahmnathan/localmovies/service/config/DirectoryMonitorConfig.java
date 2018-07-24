package com.github.rahmnathan.localmovies.service.config;

import com.github.rahmnathan.directory.monitor.DirectoryMonitor;
import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.localmovies.service.filesystem.FileListProvider;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

@ManagedBean
public class DirectoryMonitorConfig {
    private final DirectoryMonitor directoryMonitor;
    private final FileListProvider fileListProvider;

    public DirectoryMonitorConfig(Collection<DirectoryMonitorObserver> observers, @Value("${media.path}") String[] mediaPaths,
                                  FileListProvider fileListProvider) {
        this.fileListProvider = fileListProvider;
        this.directoryMonitor = new DirectoryMonitor(observers);
        Arrays.stream(mediaPaths).forEach(directoryMonitor::registerDirectory);
    }

    @PostConstruct
    public void initializeFileList(){
        directoryMonitor.getPaths().stream().map(Path::toString).forEach(fileListProvider::listFiles);
    }
}
