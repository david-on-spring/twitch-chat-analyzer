package com.dlepe.twitchchatanalyzer.repository;

import com.dlepe.twitchchatanalyzer.model.VideoDetails;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoDetailsRepository extends CrudRepository<VideoDetails, String> {

}
