package nr.localmovies.web;

import nr.localmovies.control.MovieInfoControl;
import nr.localmovies.exception.EmptyDirectoryException;
import nr.localmovies.exception.UnauthorizedFolderException;
import nr.localmovies.movieinfoapi.MovieInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class RestListener {
    private final MovieInfoControl movieInfoControl;
    private final MultipartFileSender fileSender;
    private static final Logger logger = Logger.getLogger(RestListener.class.getName());

    @Autowired
    public RestListener(MovieInfoControl movieInfoControl, MultipartFileSender fileSender){
        this.movieInfoControl = movieInfoControl;
        this.fileSender = fileSender;
    }

    /**
     * @param currentPath - Path to directory you wish to list
     * @return - List of files in specified directory
     */
    @RequestMapping(value = "/titlerequest", produces="application/json")
    public List<MovieInfo> titleRequest(@RequestParam(value = "path") String currentPath,
            HttpServletRequest request, HttpServletResponse response) throws ExecutionException {
        logger.log(Level.INFO, "Received request for - " + currentPath + " from " + request.getRemoteAddr());
        List<MovieInfo> movieInfoList = new ArrayList<>();
        try {
            for (File videoFile : movieInfoControl.listFiles(currentPath)) {
                movieInfoList.add(movieInfoControl.movieInfoCache.get(videoFile.getAbsolutePath()));
            }
        } catch (UnauthorizedFolderException | EmptyDirectoryException e){
            movieInfoList.add(MovieInfo.Builder.newInstance()
                    .setTitle("Path must contain 'LocalMedia' directory and not be empty").build());
        }
        response.addHeader("Access-Control-Allow-Origin", "*");
        return movieInfoList;
    }

    /**
     * This endpoint clears the cache of all movie info
     */
    @RequestMapping("/refresh")
    public void refresh(HttpServletResponse response){
        response.addHeader("Access-Control-Allow-Origin", "*");
        movieInfoControl.movieInfoCache.invalidateAll();
    }

    /**
     * @param path - Path to video file to stream
     * throws Exception
     */
    @RequestMapping("/video.mp4")
    public void streamVideo(HttpServletResponse response, HttpServletRequest request,
                            @RequestParam("path") String path) throws IOException {
        logger.info("Streaming - " + path + " to " + request.getRemoteAddr());
        if(path.contains("LocalMedia")) {
            response.addHeader("Access-Control-Allow-Origin", "*");
            fileSender.serveResource(Paths.get(path), request, response);
        }
    }

    /**
     * @param path - Path to video file
     * @return - Poster image for specified video file
     * throws Exception
     */
    @RequestMapping("/poster")
    public byte[] servePoster(@RequestParam("path") String path, HttpServletResponse response) throws ExecutionException {
        if(!path.contains("LocalMedia"))
            return null;

        MovieInfo info = movieInfoControl.movieInfoCache.get(path);
        response.addHeader("Access-Control-Allow-Origin", "*");
        String image = info.getImage();
        if(image == null)
            return null;

        return Base64.getDecoder().decode(image);
    }
}