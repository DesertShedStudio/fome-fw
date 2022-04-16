package com.rusefi.ldmp;

import com.rusefi.ConfigDefinition;
import com.rusefi.ReaderState;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UsagesReader {
    private final static String header = "// generated by gen_live_documentation.sh / UsagesReader.java\n";

    private final StringBuilder enumContent = new StringBuilder(header +
            "#pragma once\n" +
            "\n" +
            "typedef enum {\n");

    private final StringBuilder fragmentsContent = new StringBuilder(
            header +
                    "#include \"pch.h\"\n" +
                    "#include \"tunerstudio.h\"\n");

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("One parameter expected: name of live data yaml input file");
            System.exit(-1);
        }
        String yamlFileName = args[0];
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(new FileReader(yamlFileName));

        EntryHandler handler = new EntryHandler() {
            @Override
            public void onEntry(String name, List elements) throws IOException {
                String javaName = (String) elements.get(0);
                String folder = (String) elements.get(1);

                boolean withCDefines = false;
                String prepend = "";
                for (int i = 2; i < elements.size(); i++) {
                    String keyValue = (String) elements.get(i);
                    String[] pair = keyValue.trim().split("=");
                    String key = pair[0];
                    String value = pair[1];
                    if (key.equals(ConfigDefinition.KEY_WITH_C_DEFINES)) {
                        withCDefines = Boolean.parseBoolean(value);
                    } else if (key.equals(ConfigDefinition.KEY_PREPEND)) {
                        prepend = value;
                    }
                }

//            String macroName = elements.size() > 2 ? ((String)elements.get(2)).trim() : "";


                ReaderState state = new ReaderState();
                state.setDefinitionInputFile(folder + File.separator + name + ".txt");
                state.withC_Defines = withCDefines;

                state.addPrepend(prepend);
                state.addCHeaderDestination(folder + File.separator + name + "_generated.h");
                state.addJavaDestination("../java_console/models/src/main/java/com/rusefi/config/generated/" + javaName);
                state.doJob();
            }
        };

        UsagesReader usagesReader = new UsagesReader();
        usagesReader.handleYaml(data, handler);
        usagesReader.writeFiles();
    }

    interface EntryHandler {
        void onEntry(String name, List elements) throws IOException;
    }

    private void handleYaml(Map<String, Object> data, EntryHandler handler) throws IOException {

        LinkedHashMap<?, ?> liveDocs = (LinkedHashMap) data.get("Usages");

        fragmentsContent.append("static FragmentEntry fragments[" + liveDocs.size() + "];\n\n");

        fragmentsContent.append("int getFragmentsCount() {\n" +
                "\treturn " + liveDocs.size() + ";\n" +
                "}\n" +
                "\n" +
                "FragmentEntry *getFragments() {\n" +
                "\treturn fragments;\n" +
                "}\n\n" +
                "void initFragments() {\n");

        int index = 0;

        for (Map.Entry entry : liveDocs.entrySet()) {
            String name = (String) entry.getKey();
            System.out.println(" " + name);
            System.out.println("  " + entry.getValue());
            List elements = (List) entry.getValue();

            handler.onEntry(name, elements);

            String enumName = "LDS_" + name;
            String type = name + "_s"; // convention
            enumContent.append(enumName + ",\n");

            fragmentsContent
                    .append("\tfragments[")
                    .append(index++)
                    .append("].init(")
                    .append("(const uint8_t *)getStructAddr(")
                    .append(enumName)
                    .append("), sizeof(")
                    .append(type)
                    .append("));\n");
        }
        enumContent.append("} live_data_e;\n");
        fragmentsContent.append("};\n");
    }

    private void writeFiles() throws IOException {
        try (FileWriter fw = new FileWriter("console/binary/generated/live_data_ids.h")) {
            fw.write(enumContent.toString());
        }

        try (FileWriter fw = new FileWriter("console/binary/generated/live_data_fragments.cpp")) {
            fw.write(fragmentsContent.toString());
        }
    }
}
