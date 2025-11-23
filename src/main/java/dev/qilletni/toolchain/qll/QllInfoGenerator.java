package dev.qilletni.toolchain.qll;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.qilletni.api.lib.qll.ComparableVersion;
import dev.qilletni.api.lib.qll.QllInfo;
import dev.qilletni.api.lib.qll.Version;
import dev.qilletni.pkgutil.adapters.ComparableVersionTypeAdapter;
import dev.qilletni.pkgutil.adapters.VersionTypeAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class QllInfoGenerator {
    
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ComparableVersion.class, new ComparableVersionTypeAdapter())
            .registerTypeAdapter(Version.class, new VersionTypeAdapter())
            .create();
    
    public void writeQllInfo(QllInfo qllInfo, Path destinationDir) throws IOException {
        var json = gson.toJson(qllInfo);
        
        Files.writeString(destinationDir.resolve("qll.info"), json);
    }
    
    public QllInfo readQllInfo(InputStream qllInfoPath) {
        return gson.fromJson(new InputStreamReader(qllInfoPath), QllInfo.class);
    }
    
}
