package org.passerbya.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 插件清单解析测试。用临时目录里的 manifest.json 验证 Jackson 映射，
 * 不依赖工作目录下真实的 plugins/。
 */
class PluginManifestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesFullManifest() throws IOException {
        String json = """
                {
                  "id": "demo.echo",
                  "version": "1.0.0",
                  "name": "回声",
                  "description": "把输入原样打回来",
                  "type": "shell",
                  "executable": "echo",
                  "isExecutable": true,
                  "parameters": [
                    {
                      "name": "message",
                      "label": "消息",
                      "type": "text",
                      "required": true
                    },
                    {
                      "name": "loud",
                      "label": "大声",
                      "type": "flag",
                      "required": false,
                      "flagValue": "--loud"
                    }
                  ]
                }
                """;

        PluginManifest manifest = mapper.readValue(json, PluginManifest.class);

        assertEquals("demo.echo", manifest.getId());
        assertEquals("回声", manifest.getName());
        assertEquals("echo", manifest.getExecutable());
        assertTrue(manifest.isExecutable(), "isExecutable 应映射到 executableFlag");

        List<PluginParameter> params = manifest.getParameters();
        assertNotNull(params);
        assertEquals(2, params.size());

        PluginParameter message = params.get(0);
        assertEquals("message", message.getName());
        assertEquals("消息", message.getLabel());
        assertTrue(message.isRequired());

        PluginParameter loud = params.get(1);
        assertEquals("flag", loud.getType());
        assertEquals("--loud", loud.getFlagValue());
    }

    @Test
    void parsesOptionsForSelectParameter(@TempDir Path tempDir) throws IOException {
        Path manifestFile = tempDir.resolve("manifest.json");
        Files.writeString(manifestFile, """
                {
                  "id": "demo.select",
                  "name": "选择器",
                  "executable": "tool",
                  "parameters": [
                    {
                      "name": "mode",
                      "label": "模式",
                      "type": "select",
                      "required": true,
                      "options": ["fast", "slow"]
                    }
                  ]
                }
                """);

        PluginManifest manifest = mapper.readValue(manifestFile.toFile(), PluginManifest.class);

        assertEquals(List.of("fast", "slow"), manifest.getParameters().get(0).getOptions());
    }

    @Test
    void missingOptionalFieldsStayNull() throws IOException {
        PluginManifest manifest = mapper.readValue(
                "{\"id\":\"bare\",\"name\":\"裸的\"}", PluginManifest.class);

        assertEquals("bare", manifest.getId());
        assertNull(manifest.getExecutable());
        assertNull(manifest.getParameters());
    }
}
