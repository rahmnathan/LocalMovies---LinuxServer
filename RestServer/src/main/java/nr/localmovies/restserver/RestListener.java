package nr.localmovies.restserver;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import nr.localmovies.movieinfoapi.IMovieInfoProvider;
import nr.localmovies.movieinfoapi.MovieInfo;
import nr.localmovies.movieinfoapi.MovieInfoEntity;
import nr.localmovies.movieinfoapi.MovieInfoRepository;
import nr.localmovies.omdbmovieinfoprovider.OMDBIMovieInfoProvider;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import nr.linuxmedieserver.directoryexplorer.DirectoryExplorer;
import nr.linuxmedieserver.keypressexecutor.KeyPressExecutor;
import nr.linuxmedieserver.keypressexecutor.KeyPressExecutor.Controls;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
public class RestListener {

    private static final DirectoryExplorer directoryExplorer = new DirectoryExplorer();
    private static final KeyPressExecutor KEY_PRESS_EXECUTOR = new KeyPressExecutor();
    private static final IMovieInfoProvider I_MOVIE_INFO_PROVIDER = new OMDBIMovieInfoProvider();
    private static File video;
    private static volatile boolean seek;

    @Autowired
    private MovieInfoRepository repository;

    private final LoadingCache<String, List<MovieInfo>> MOVIE_INFO_LOADER =
            CacheBuilder.newBuilder()
                    .maximumSize(250)
                    .build(
                            new CacheLoader<String, List<MovieInfo>>() {
                                @Override
                                public List<MovieInfo> load(String currentPath) {
                                    return loadMovieInfo(currentPath);
                                }
                            });

    /**
     *
     * @param currentPath - Path to directory you wish to list
     * @return - List of files in specified directory
     */
    @RequestMapping(value = "/titlerequest", produces="application/json")
    public List<MovieInfo> titlerequest(@RequestParam(value = "path") String currentPath) {

        try {
            return MOVIE_INFO_LOADER.get(currentPath);
        }catch(ExecutionException e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param currentPath - Path to video you wish to play
     */
    @RequestMapping("/playmovie")
    public void playMovie(@RequestParam(value = "path") String currentPath){

        video = new File(currentPath);
    }

    /**
     *
     * @param control - The desired control to execute (VOLUME_UP, VOLUME_DOWN, SEEK_FORWARD, SEEK_BACK, PLAY_PAUSE, STOP)
     * @param name - The name of your device that you started playback with
     */
    @RequestMapping("/control")
    public void start(@RequestParam("control") String control,
                      @RequestParam("name") String name) {

        Controls keyPress = Controls.valueOf(control);

        KEY_PRESS_EXECUTOR.executeCommand(keyPress, name);
    }

    /**
     * This endpoint clears the cache of all movie info and retrieves updated info
     */
    @RequestMapping("/refresh")
    public void refresh(){
        MOVIE_INFO_LOADER.invalidateAll();
        repository.deleteAll();
    }

    @RequestMapping("/video.mp4")
    public void streamVideo(HttpServletResponse response) throws Exception {
        InputStream is = new DataInputStream(new FileInputStream(video));
        long totalLength = video.length();
        int bufferSize = 4000;
        response.setContentType("video/mp4");
        response.setBufferSize(bufferSize);
        response.setContentLengthLong(totalLength);
        OutputStream os = response.getOutputStream();

        byte[] buffer = new byte[bufferSize];

        while(is.read(buffer, 0, bufferSize) != -1){
            os.write(buffer);
        }
        os.close();
    }


    private List<MovieInfo> loadMovieInfo(String path){
        ObjectMapper mapper = new ObjectMapper();
        String[] currentPathArray = path.toLowerCase().split("localmedia")[1].split("/");
        if (repository.exists(path)) {
            try {
                return mapper.readValue(repository.findOne(path).getData(), new TypeReference<List<MovieInfo>>() {
                });
            } catch (IOException e){
                e.printStackTrace();
            }
        } else if(currentPathArray.length == 2) {
            try {
                List<MovieInfo> movieInfoList = I_MOVIE_INFO_PROVIDER.getMovieInfo(directoryExplorer.getTitleList(path), path);
                repository.save(new MovieInfoEntity(path, mapper.writeValueAsString(movieInfoList)));

                return movieInfoList;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            int depth = 0;
            String title = currentPathArray[2];
            if(currentPathArray.length == 3)
                depth = 1;
            else if (currentPathArray.length == 4)
                depth = 2;

            String imagePath = "";
            for(int i = 0; i < path.split("/").length - depth; i++){
                imagePath += path.split("/")[i] + "/";
            }
            String image = "";
            String MetaRating = "";
            String IMDBRating = "";
            String year = "";
            try {
                for (MovieInfo info : (List<MovieInfo>) mapper.readValue(repository.findOne(imagePath).getData(), new TypeReference<List<MovieInfo>>() {})) {
                    if (info.getTitle().toLowerCase().equals(title.toLowerCase())) {
                        image = info.getImage();
                        MetaRating = info.getMetaRating();
                        IMDBRating = info.getIMDBRating();
                        year = info.getReleaseYear();
                    }
                }
            } catch (IOException e){
                e.printStackTrace();
            }
            List<String> titleList = directoryExplorer.getTitleList(path);
            List<MovieInfo> movieInfoList = new ArrayList<>();
            for(String title1 : titleList){
                MovieInfo info = new MovieInfo();
                info.setTitle(title1);
                info.setImage(image);
                info.setIMDBRating(IMDBRating);
                info.setMetaRating(MetaRating);
                info.setReleaseYear(year);
                movieInfoList.add(info);
            }
            return movieInfoList;
        }
        return null;
    }
}
