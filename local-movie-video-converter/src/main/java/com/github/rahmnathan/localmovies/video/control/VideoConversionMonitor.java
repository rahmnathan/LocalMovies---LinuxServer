package com.github.rahmnathan.localmovies.video.control;

import com.github.rahmnathan.directory.monitor.DirectoryMonitorObserver;
import com.github.rahmnathan.video.cast.handbrake.control.VideoController;
import com.github.rahmnathan.video.cast.handbrake.data.SimpleConversionJob;
import net.bramp.ffmpeg.FFprobe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.ManagedBean;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ManagedBean
public class VideoConversionMonitor implements DirectoryMonitorObserver {
    private final Logger logger = LoggerFactory.getLogger(VideoConversionMonitor.class.getName());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile Set<String> activeConversions = ConcurrentHashMap.newKeySet();
    private FFprobe ffprobe;

    public VideoConversionMonitor(@Value("${ffprobe.location:/usr/bin/ffprobe}") String ffprobeLocation){
        try {
            this.ffprobe = new FFprobe(ffprobeLocation);
        } catch (IOException e){
            logger.error("Failed to instantiate VideoConversionMonitor", e);
        }
    }

    @Override
    public void directoryModified(WatchEvent event, Path absolutePath) {
        if (ffprobe == null)
            return;

        if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isRegularFile(absolutePath) &&
                !activeConversions.contains(absolutePath.toString())) {

            // I need to find a way to wait until a file is fully written before converting it
            try {
                Thread.sleep(7000);
            } catch (InterruptedException e){
                logger.error("Failed sleep", e);
            }

            String newFilePath = absolutePath.toString().substring(0, absolutePath.toString().lastIndexOf('.')) + ".mp4";

            SimpleConversionJob conversionJob = new SimpleConversionJob(ffprobe, absolutePath.toFile(), new File(newFilePath));

            executorService.submit(new VideoController(conversionJob, activeConversions));
        }
    }

    public Set<String> getActiveConversions() {
        return activeConversions;
    }
}
