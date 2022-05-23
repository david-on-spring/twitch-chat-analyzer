package com.dlepe.twitchchatanalyzer.repository;

import com.dlepe.twitchchatanalyzer.model.VideoChatTimestamp;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoChatTimestampRepository extends CrudRepository<VideoChatTimestamp, String> {

}
