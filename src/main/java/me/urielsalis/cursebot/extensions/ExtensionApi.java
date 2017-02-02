package me.urielsalis.cursebot.extensions;

import me.urielsalis.cursebot.Main;
import me.urielsalis.cursebot.api.CurseApi;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by urielsalis on 1/28/2017
 */
public class ExtensionApi {
    ConcurrentHashMap<String, ArrayList<Listener>> bus = new ConcurrentHashMap<>();
    CurseApi curseAPI = Main.api;

    public void addListener(String eventName, Listener listener) {
        if(!bus.containsKey(eventName))
            bus.put(eventName, new ArrayList<Listener>());
        bus.get(eventName).add(listener);
    }

    public void fire(String eventName, Event event) {
        if(bus.containsKey(eventName)) {
            for(Listener listener: bus.get(eventName)) {
                List<Method> methods = getMethodsAnnotatedWith(listener.getClass(), Handle.class);
                for(Method method: methods) {
                    try {
                        method.invoke(listener, event);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
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

    public static List<Method> getMethodsAnnotatedWith(final Class<?> type, final Class<? extends Annotation> annotation) {
        final List<Method> methods = new ArrayList<Method>();
        Class<?> klass = type;
        while (klass != Object.class) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            final List<Method> allMethods = new ArrayList<Method>(Arrays.asList(klass.getDeclaredMethods()));
            for (final Method method : allMethods) {
                if (method.isAnnotationPresent(annotation)) {
                    Annotation annotInstance = method.getAnnotation(annotation);
                    // TODO process annotInstance
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                        methods.add(method);
                    }
                }
                // move to the upper class in the hierarchy in search for more methods
                klass = klass.getSuperclass();
            }
            return methods;
        }
        return methods;
    }

}
