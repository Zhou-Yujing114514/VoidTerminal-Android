package com.example.chatapp.model;

import java.util.ArrayList;
import java.util.List;

public class ChatRoom {
    public String id;
    public String name;
    public boolean isGroup;
    public List<Message> messages = new ArrayList<>();
    public String lastMsg;
    public long lastTime;

    public ChatRoom(String id, String name, boolean isGroup) {
        this.id = id;
        this.name = name;
        this.isGroup = isGroup;
    }

    public void addMessage(Message msg) {
        messages.add(msg);
        lastMsg = msg.content != null ? msg.content : "[图片]";
        lastTime = msg.time;
    }
}
