package com.dlepe.twitchchatanalyzer.model.mapper;

import com.dlepe.twitchchatanalyzer.model.VideoDetails;
import com.dlepe.twitchchatanalyzer.service.LogParseUtils;
import io.swagger.model.TwitchHelixVideoResponse;
import io.swagger.model.TwitchHelixVideoResponseData;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class TwitchVideoMapper {

    public VideoDetails toVideoDetails(TwitchHelixVideoResponseData source) {
        final Duration videoDuration = LogParseUtils.getVideoDurationFromString(
            source.getDuration());

        return VideoDetails.builder()
            .id(source.getId())
            .channelName(source.getUserLogin())
            .videoTitle(source.getTitle())
            .videoUrl(source.getUrl())
            .videoStartTime(source.getCreatedAt().toLocalDateTime())
            .videoEndTime(source.getCreatedAt()
                .plusSeconds(videoDuration.toSeconds()).toLocalDateTime())
            .indexed(false)
            .build();
    }

    public abstract List<VideoDetails> toVideoDetails(
        Collection<TwitchHelixVideoResponse> twitchVods);
}
