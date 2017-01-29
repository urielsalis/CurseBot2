package me.urielsalis.cursebot.extensions;

import org.apache.commons.collections4.bag.SynchronizedSortedBag;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by urielsalis on 1/28/2017
 */
public class ExtensionHandler {
    public static ExtensionApi api = new ExtensionApi();
    public void init() {
        loadJars();
        Configuration configuration = new ConfigurationBuilder().addUrls(ClasspathHelper.forJavaClassPath());

        Reflections reflections = new Reflections(configuration);
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Extension.class);
        for(Class clazz : annotated) {
            while (clazz != Object.class) {
                // need to iterated thought hierarchy in order to retrieve methods from above the current instance
                // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
                final List<Method> allMethods = new ArrayList<Method>(Arrays.asList(clazz.getDeclaredMethods()));
                for (final Method method : allMethods) {
                    if (method.isAnnotationPresent(ExtensionInit.class)) {
                        try {
                            System.out.println("Invoking " + method.getName());

                            method.invoke(null, api); //invoker is null as its static
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            System.out.println("Error while trying to run method");
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                }
                // move to the upper class in the hierarchy in search for more methods
                clazz = clazz.getSuperclass();
            }
        }
    }

    @Retention(value = RetentionPolicy.RUNTIME)
    @Target(value = ElementType.METHOD)
    public @interface ExtensionInit {
        String value();
    }

    private static void loadJars() {
        File directory = new File("src\\main\\java\\me\\urielsalis\\cursebot\\extensions");
        File[] files = directory.listFiles((dir, name) -> new File(dir, name).isDirectory());

        for (int i = 0; i < files.length; i++)
        {
            files[i] = new File(files[i].toPath() + "\\" + files[i].listFiles()[0].getName());
        }

        /*int x = 0;
        for (int i = 0; i < annotatedDir.length; i++)
        {
            String addPath = "";
            if(annotatedDir[i].isDirectory())
                addPath = directory.toPath() + "\\" + test[i].getName();
        }*/

        if (files != null) {
            for (File file : files) {
                try {
                    System.out.println("Loading .jar: " + file.getName());
                    ClassPathHacker.addFile(file);
                } catch (IOException e) {
                    System.out.println("This should never happen, this is bad");
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        } else {
            System.out.println("No extensions to load");
        }
    }
}
