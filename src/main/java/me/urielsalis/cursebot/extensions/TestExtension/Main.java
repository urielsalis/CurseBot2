package me.urielsalis.cursebot.extensions.TestExtension;

import me.urielsalis.cursebot.extensions.Extension;
import me.urielsalis.cursebot.extensions.ExtensionApi;
import me.urielsalis.cursebot.extensions.ExtensionHandler;

/**
 * Created by urielsalis on 1/28/2017
 */
@Extension(name = "TestExtension", version = "1.0.0", id = "TestExtension/1.0.0")
public class Main {
    @ExtensionHandler.ExtensionInit("TestExtension/1.0.0")
    public void init(ExtensionApi api) {
        //do init here
        //api can be used to register events or listen to them
        
    }

    //other methods can be here
}
