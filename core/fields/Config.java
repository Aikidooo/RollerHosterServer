package core.fields;

import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Config {
    private static final String resource = "resources/config.json";

    public static JSONObject parseConfig() throws IOException{
        String JSONString = new String(Files.readAllBytes(Paths.get(resource)));
        return new JSONObject(JSONString);
    }
}
