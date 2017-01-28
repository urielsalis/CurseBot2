package me.urielsalis.cursebot.extensions;

import me.urielsalis.cursebot.Main;
import me.urielsalis.cursebot.api.CurseApi;
import net.engio.mbassy.bus.MBassador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by urielsalis on 1/28/2017
 */
public class ExtensionApi {
    ConcurrentHashMap<String, MBassador> bus = new ConcurrentHashMap<>();
    CurseApi curseAPI = Main.api;

    public void addListener(String eventName, Listener listener) {
        if(!bus.containsKey(eventName))
            bus.put(eventName, new MBassador());
        bus.get(eventName).subscribe(listener);
    }

    public void fire(String eventName, Event event) {
        if(bus.containsKey(eventName)) {
            bus.get(eventName).post(event);
        }
    }

    public CurseApi getCurseAPI() {
        return curseAPI;
    }

    public static interface Listener {
        String name();
    }

    public static class Event {
        String eventName;
        String value;
    }
}
