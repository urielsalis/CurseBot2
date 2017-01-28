package me.urielsalis.cursebot.extensions;

import net.engio.mbassy.bus.MBassador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by urielsalis on 1/28/2017
 */
public class ExtensionApi {
    ConcurrentHashMap<String, MBassador> bus = new ConcurrentHashMap<>();

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

    public interface Listener {
        String name();
    }

    public class Event {
        String eventName;
        String value;
    }
}
