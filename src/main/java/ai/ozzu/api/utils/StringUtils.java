package ai.ozzu.api.utils;

public final class StringUtils {

    private StringUtils(){}

    public static final  String DEFAULT_EVENT_IMAGE_URL = "https://ozzu-media-stage.s3.ap-south-1.amazonaws.com/domains/dced2aae-fce0-45ce-9b7f-8888aef3fd96/events/event.png";
    public static final  String DEFAULT_LOUNGE_IMAGE_URL = "https://ozzu-media-stage.s3.ap-south-1.amazonaws.com/domains/dced2aae-fce0-45ce-9b7f-8888aef3fd96/lounges/lounge.png";
    public static final  String DEFAULT_EVENT_LOUNGE_IMAGE_URL = "https://ozzu-media-stage.s3.ap-south-1.amazonaws.com/domains/dced2aae-fce0-45ce-9b7f-8888aef3fd96/eventLounges/event_lounge.png";
    public static final  String DEFAULT_TEAM_IMAGE_URL = "https://ozzu-media-stage.s3.ap-south-1.amazonaws.com/domains/dced2aae-fce0-45ce-9b7f-8888aef3fd96/teams/team.png";
    public static final  String DEFAULT_PLAYER_IMAGE_URL = "https://ozzu-media-stage.s3.ap-south-1.amazonaws.com/domains/dced2aae-fce0-45ce-9b7f-8888aef3fd96/players/player.png";
    public static final  String DEFAULT_VIDEO_URL = "";
    public static final  String DEFAULT_THUMBNAIL_URL = "";

    public static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank()
                ? defaultValue
                : value;
    }
}
