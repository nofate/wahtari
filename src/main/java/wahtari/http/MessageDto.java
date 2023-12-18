package wahtari.http;


import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.JsonAttribute;

// {"customerID":1,"tagID":2,"userID":"aaaaaaaa-bbbb-cccc-1111-222222222222","remoteIP":"123.234.56.78","timestamp":1500000000}
@CompiledJson(onUnknown = CompiledJson.Behavior.IGNORE)
public class MessageDto {
    @JsonAttribute(name = "customerID")
    private Integer customerId;

    @JsonAttribute(name = "tagID")
    private Integer tagId;

    @JsonAttribute(name = "userID")
    private String userId;

    @JsonAttribute(name = "remoteIP")
    private String remoteIp;

    @JsonAttribute(name = "timestamp")
    private Long timestamp;

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getTagId() {
        return tagId;
    }

    public void setTagId(Integer tagId) {
        this.tagId = tagId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean validate() {
        return customerId != null && tagId != null && userId != null && remoteIp != null && timestamp != null;
    }
}
