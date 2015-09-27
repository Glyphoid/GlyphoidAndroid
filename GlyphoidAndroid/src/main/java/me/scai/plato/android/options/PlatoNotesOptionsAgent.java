package me.scai.plato.android.options;


import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class PlatoNotesOptionsAgent {
    /* Constants */
    private static final String OPTIONS_FILE_NAME = "options.json";
    private static final String OPTIONS_FILE_ENCODING = "utf-8";
    private static final Gson gson = new Gson();

    /* Member variables */
    private String optionsDir;

    private PlatoNotesOptions options;

    /*
     * @param    dirName: directory where the options file will be stored
     */
    public PlatoNotesOptionsAgent(String optionsDir) {
        this.optionsDir = optionsDir;
    }

    /***
     * Get the current stored options, create to default value if stored options file does not exist
     */

    public PlatoNotesOptions createOrGetOptionsFromFile() throws IOException {
        File f = getOptionsFile();
        if ( f.exists() && !f.isDirectory() ) {
            String optionsJsonStr = FileUtils.readFileToString(f, OPTIONS_FILE_ENCODING);

            this.options = gson.fromJson(optionsJsonStr, PlatoNotesOptions.class);
        } else {
            this.options = createDefaultPlatoNotesOptionsFile(f);
        }

        return this.options;
    }

    public PlatoNotesOptions restoreDefaultOptionsToFile() throws IOException {
        File f = getOptionsFile();
        this.options = createDefaultPlatoNotesOptionsFile(f);

        return this.options;
    }

    public PlatoNotesOptions getOptions() {
        return options;
    }

    public void writeOptionsToFile(PlatoNotesOptions options) throws IOException {
        File f = getOptionsFile();

        FileUtils.write(f, gson.toJson(options), OPTIONS_FILE_ENCODING);
    }

    public void writeOptionsToFile() throws IOException {
        writeOptionsToFile(this.options);
    }

    private File getOptionsFile() {
        String optionsFilePath = optionsDir + File.separator + OPTIONS_FILE_NAME;

        return new File(optionsFilePath);
    }

    private PlatoNotesOptions createDefaultPlatoNotesOptionsFile(File f) throws IOException {
        PlatoNotesOptions defaultOptions = new PlatoNotesOptions();

        String defaultOptionsJsonStr = gson.toJson(defaultOptions);

        FileUtils.write(f, defaultOptionsJsonStr, OPTIONS_FILE_ENCODING);

        return defaultOptions;
    }
}
