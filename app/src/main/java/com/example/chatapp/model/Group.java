package com.example.chatapp.model;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
public class Group {
    public String id;
    public String name;
    public String owner;
    public String avatar;
    public int memberCount;
    public List<String> members = new ArrayList<>();
    public List<String> memberNames = new ArrayList<>();
    public static Group fromJson(JSONObject obj) {
        Group g = new Group();
        g.id = obj.optString("id", "");
        g.name = obj.optString("name", "");
        g.owner = obj.optString("owner", "");
        g.avatar = obj.optString("avatar", "");
        JSONArray members = obj.optJSONArray("members");
        if (members != null) {
            for (int i = 0; i < members.length(); i++) {
                g.members.add(members.optString(i, ""));
            }
            g.memberCount = g.members.size();
        } else {
            g.memberCount = obj.optInt("memberCount", 0);
        }
        JSONArray names = obj.optJSONArray("memberNames");
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                g.memberNames.add(names.optString(i, ""));
            }
        }
        return g;
    }
}
