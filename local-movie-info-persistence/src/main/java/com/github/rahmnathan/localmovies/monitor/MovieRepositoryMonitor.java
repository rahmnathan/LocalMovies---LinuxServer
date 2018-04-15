package com.github.rahmnathan.localmovies.monitor;

import com.github.rahmnathan.localmovies.omdb.provider.boundary.OmdbMovieProvider;
import com.github.rahmnathan.localmovies.persistence.MovieRepository;
import com.github.rahmnathan.movie.api.MovieProvider;
import com.github.rahmnathan.movie.data.Movie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.ManagedBean;

@ManagedBean
public class MovieRepositoryMonitor {
    private final Logger logger = LoggerFactory.getLogger(MovieRepositoryMonitor.class.getName());
    private final MovieRepository movieRepository;
    private final MovieProvider movieProvider;

    public MovieRepositoryMonitor(MovieRepository movieRepository, OmdbMovieProvider movieProvider) {
        this.movieRepository = movieRepository;
        this.movieProvider = movieProvider;
    }

    @Scheduled(fixedRate = 86400000)
    public void checkForEmptyValues(){
        logger.info("Checking for null MovieInfo fields in database");

        movieRepository.findAll().forEach(mediaFile -> {
            Movie existingMovie = mediaFile.getMovie();
            if(existingMovie.hasMissingValues()){
                logger.info("Detected missing fields: {}", existingMovie.toString());

                Movie newMovie = movieProvider.loadMovieInfo(existingMovie.getTitle());

                Movie mergedMovie = Movie.Builder.newInstance()
                        .setGenre(newMovie.getGenre() != null && !newMovie.getGenre().equals("null") ? newMovie.getGenre() : existingMovie.getGenre())
                        .setImage(newMovie.getImage() != null && !newMovie.getImage().equals("null") ? newMovie.getImage() : existingMovie.getImage())
                        .setIMDBRating(newMovie.getIMDBRating() != null && !newMovie.getIMDBRating().equals("null") ? newMovie.getIMDBRating() : existingMovie.getIMDBRating())
                        .setMetaRating(newMovie.getMetaRating() != null && !newMovie.getMetaRating().equals("null")? newMovie.getMetaRating() : existingMovie.getMetaRating())
                        .setReleaseYear(newMovie.getReleaseYear() != null && !newMovie.getReleaseYear().equals("null") ? newMovie.getReleaseYear() : existingMovie.getReleaseYear())
                        .setTitle(newMovie.getTitle() != null && !newMovie.getTitle().equals("null") ? newMovie.getTitle() : existingMovie.getTitle())
                        .setActors(newMovie.getActors() != null && !newMovie.getActors().equals("null") ? newMovie.getActors() : existingMovie.getActors())
                        .setPlot(newMovie.getPlot() != null && !newMovie.getPlot().equals("null") ? newMovie.getPlot() : existingMovie.getPlot())
                        .build();

                mediaFile.setMovie(mergedMovie);
                movieRepository.save(mediaFile);
            }
        });
    }
}
