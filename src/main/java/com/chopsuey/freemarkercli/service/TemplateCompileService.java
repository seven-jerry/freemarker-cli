package com.chopsuey.freemarkercli.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.NumberUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TemplateCompileService {

    @Value("${workspace.folder}")
    private String workspaceFolder;

    @Autowired
    private freemarker.template.Configuration freemarkerConfiguration;

    @PostConstruct
    public void compile() {
        Map<String, Map<String, Object>> values = buildValues();
        try (Stream<Path> files = Files.walk(Path.of(workspaceFolder)).filter(e -> Files.isRegularFile(e) && e.getFileName().toString().contains(".template.json"))) {
            StringTemplateLoader templateLoader = new PathStringTemplateLoader();
            freemarkerConfiguration.setTemplateLoader(templateLoader);
            files.forEach(p -> {


                String templateKey = "";
                try (Stream<String> content = Files.lines(p)) {
                    String key = p.toAbsolutePath().toString();
                    templateKey = key;
                    templateLoader.putTemplate(key, content.collect(Collectors.joining("\n")));
                    Template template = freemarkerConfiguration.getTemplate(key);

                    for (Map.Entry<String, Map<String, Object>> stringMapEntry : values.entrySet()) {
                        String envKey = "";
                        try {
                            envKey = stringMapEntry.getKey();
                            String templateText = processTemplateIntoString(template, stringMapEntry.getValue());
                            writeOutputFile(stringMapEntry.getKey(), p, templateText);
                        } catch (TemplateException ex) {
                            System.err.println("<template env=\"" + envKey + "\" name=\"" + templateKey + "\">");
                            System.err.println(ex.getMessage());
                            System.err.println("</template>");
                        }
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeOutputFile(String env, Path p, String templateText) {
        Path workspace = Path.of(workspaceFolder);
        Path relativize1 = workspace.relativize(p);

        String replace = relativize1.getFileName().toString().replace(".template", "");
        if (relativize1.getParent() != null) {
            relativize1 = Path.of(relativize1.getParent().toString(), replace);
        } else {
            relativize1 = Path.of(replace);
        }
        Path relativize = Paths.get(workspace.toAbsolutePath().toString(), "out", env).resolve(relativize1);

        try {
            Files.createDirectories(relativize.getParent());
            Files.write(relativize, templateText.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static String processTemplateIntoString(Template template, Object model) throws IOException, TemplateException {
        StringWriter result = new StringWriter();
        template.process(model, result);
        return result.toString();
    }

    private Map<String, Map<String, Object>> buildValues() {
        Path valuesPath = Path.of(workspaceFolder, "environment");
        Map<String, Map<String, Object>> values = new HashMap<>();
        if (!Files.exists(valuesPath) || !Files.isDirectory(valuesPath)) {
            System.err.println("environment folder not found");
            return values;
        }

        try (Stream<Path> files = Files.list(valuesPath)) {
            Map<String, Object> sharedEnvs = new HashMap<>();
            files
                    .filter(f -> Files.isRegularFile(f) && f.getFileName().toString().endsWith(".json"))
                    .forEach(f -> {
                        if (f.getFileName().toString().equals("all.json")) {
                            Map<String, Object> all = readAllVariables(f);
                            sharedEnvs.putAll(all);
                            return;
                        }
                        Map<String, Object> envProperties = readAllVariables(f);
                        values.put(f.getFileName().toString().replace(".json", ""), envProperties);
                    });
            String projectName = findProjectName(workspaceFolder);
            String appName = findApplicationName(workspaceFolder);
            sharedEnvs.put("cs_project", projectName);
            sharedEnvs.put("cs_application", appName);
            for (Map.Entry<String, Map<String, Object>> stringMapEntry : values.entrySet()) {
                stringMapEntry.getValue().putAll(sharedEnvs);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        System.out.println("using variables:" + values);
        return values;
    }

    private String findProjectName(String workspaceFolder) {
        Path workspace = Path.of(workspaceFolder);
        String fileName = workspace.getFileName().toString();
        if (fileName.endsWith("-origin")) {
            return fileName.substring(0, fileName.indexOf("-"));
        }
        return "sampleProject";
    }

    private String findApplicationName(String workspaceFolder) {
        Path workspace = Path.of(workspaceFolder);
        String fileName = workspace.getFileName().toString();
        if (fileName.endsWith("-origin")) {
            int firstDash = fileName.indexOf("-");
            int lastDash = fileName.lastIndexOf("-");
            return fileName.substring(firstDash + 1, lastDash);
        }
        return "sampleApplication";
    }

    private Map<String, Object> readAllVariables(Path path) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<>() {
            };

            return mapper.readValue(path.toFile(), typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object parseNumber(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException nf) {
            return string;
        }
    }


}
