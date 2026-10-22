package burp;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.hotkey.HotKey;
import burp.api.montoya.ui.hotkey.HotKeyHandler;
import burp.api.montoya.core.Registration;
import burp.api.montoya.ui.settings.SettingsPanel;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LazyFlow implements BurpExtension {

    // HTML viewer bundled from src/main/resources
    private static final String HTML_RESOURCE   = "/lazyflow.html";
    private static final String FLOW_DATA_MARKER = "`__FLOW_DATA__`";

    // Hotkey settings, persisted in Burp's user-level preferences
    private static final String HOTKEY_PREF_KEY = "lazyflow.hotkey";
    private static final String DEFAULT_HOTKEY  = "Ctrl+L";
    private static final String HOTKEY_NAME     = "Send to LazyFlow";

    private MontoyaApi api;
    private String htmlTemplate;

    private volatile String currentHotKey;      // null when no hotkey is active
    private Registration hotKeyRegistration;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;
        api.extension().setName("LazyFlow");

        try {
            htmlTemplate = loadResource(HTML_RESOURCE);
        } catch (IOException e) {
            api.logging().logToError("LazyFlow: failed to load " + HTML_RESOURCE + ": " + e);
            return;
        }

        api.userInterface().registerContextMenuItemsProvider(new FlowContextMenu());

        api.userInterface().registerSettingsPanel(new HotKeySettingsPanel());

        applyHotKey(loadHotKeyPref());

        api.logging().logToOutput("LazyFlow loaded | hotkey: " + hotKeyLabel() + " | no server required");
    }

    // ── hotkey ────────────────────────────────────────────────────────────────

    private String loadHotKeyPref() {
        try {
            String stored = api.persistence().preferences().getString(HOTKEY_PREF_KEY);
            return stored == null ? DEFAULT_HOTKEY : stored;
        } catch (Exception e) {
            api.logging().logToError("LazyFlow: failed to read hotkey preference: " + e);
            return DEFAULT_HOTKEY;
        }
    }

    private void saveHotKeyPref(String value) {
        try {
            api.persistence().preferences().setString(HOTKEY_PREF_KEY, value == null ? "" : value);
        } catch (Exception e) {
            api.logging().logToError("LazyFlow: failed to save hotkey preference: " + e);
        }
    }

    /**
     * Replaces the active hotkey. A null/blank value just disables the hotkey.
     * Returns null on success, or an error message if the hotkey could not be registered.
     */
    private synchronized String applyHotKey(String rawValue) {
        if (hotKeyRegistration != null) {
            try { hotKeyRegistration.deregister(); }
            catch (Exception e) { api.logging().logToError("LazyFlow: failed to deregister hotkey: " + e); }
            hotKeyRegistration = null;
        }
        currentHotKey = null;

        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isEmpty()) {
            api.logging().logToOutput("LazyFlow: hotkey disabled (empty value).");
            return null;
        }

        HotKeyHandler handler = event -> {
            List<HttpRequestResponse> selected = event.selectedRequestResponses();
            if (selected == null || selected.isEmpty()) {
                api.logging().logToOutput("LazyFlow: hotkey fired but nothing selected.");
                return;
            }
            List<HttpRequestResponse> ordered = reversed(selected);
            api.logging().logToOutput("LazyFlow: hotkey — " + ordered.size() + " item(s).");
            new Thread(() -> handleExport(ordered)).start();
        };

        try {
            Registration reg = api.userInterface().registerHotKeyHandler(HotKey.hotKey(HOTKEY_NAME, value), handler);
            if (reg == null || !reg.isRegistered()) {
                String msg = "Burp rejected hotkey \"" + value + "\"";
                api.logging().logToError("LazyFlow: " + msg);
                return msg;
            }
            hotKeyRegistration = reg;
            currentHotKey = value;
            api.logging().logToOutput("LazyFlow: hotkey set to " + value);
            return null;
        } catch (Exception e) {
            String msg = "Invalid hotkey \"" + value + "\": " + e.getMessage();
            api.logging().logToError("LazyFlow: " + msg);
            return msg;
        }
    }

    private String hotKeyLabel() {
        String hk = currentHotKey;
        return hk == null ? "none" : hk;
    }

    // ── settings panel ────────────────────────────────────────────────────────

    private class HotKeySettingsPanel implements SettingsPanel {
        private final JPanel panel = new JPanel(new GridBagLayout());
        private final JTextField hotKeyField = new JTextField(20);
        private final JLabel statusLabel = new JLabel(" ");

        HotKeySettingsPanel() {
            hotKeyField.setText(loadHotKeyPref());

            JButton applyButton = new JButton("Apply");
            applyButton.addActionListener(e -> apply(hotKeyField.getText()));
            hotKeyField.addActionListener(e -> apply(hotKeyField.getText()));

            JButton resetButton = new JButton("Reset to default");
            resetButton.addActionListener(e -> {
                hotKeyField.setText(DEFAULT_HOTKEY);
                apply(DEFAULT_HOTKEY);
            });

            JLabel title = new JLabel("LazyFlow");
            title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 2f));

            JLabel help = new JLabel("<html>Hotkey for \"" + HOTKEY_NAME + "\" (e.g. Ctrl+L, Ctrl+Shift+F). "
                + "Leave empty to disable the hotkey.</html>");

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 4, 4, 4);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0; c.gridy = 0; c.gridwidth = 4;
            panel.add(title, c);
            c.gridy = 1;
            panel.add(help, c);

            c.gridy = 2; c.gridwidth = 1;
            panel.add(new JLabel("Hotkey:"), c);
            c.gridx = 1;
            panel.add(hotKeyField, c);
            c.gridx = 2;
            panel.add(applyButton, c);
            c.gridx = 3;
            panel.add(resetButton, c);

            c.gridx = 0; c.gridy = 3; c.gridwidth = 4;
            panel.add(statusLabel, c);

            // push content to the top-left
            c.gridy = 4; c.weightx = 1; c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            panel.add(Box.createGlue(), c);
        }

        private void apply(String value) {
            String error = applyHotKey(value);
            if (error == null) {
                saveHotKeyPref(value == null ? "" : value.trim());
                statusLabel.setForeground(UIManager.getColor("Label.foreground"));
                statusLabel.setText("Active hotkey: " + hotKeyLabel());
            } else {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText(error + " — hotkey is now disabled.");
            }
        }

        @Override
        public JComponent uiComponent() {
            return panel;
        }

        public Set<String> keywords() {
            return Set.of("LazyFlow", "hotkey", "shortcut", "flow");
        }
    }

    // ── context menu ──────────────────────────────────────────────────────────

    private class FlowContextMenu implements ContextMenuItemsProvider {
        @Override
        public List<Component> provideMenuItems(ContextMenuEvent event) {
            List<HttpRequestResponse> selected = event.selectedRequestResponses();
            if (selected == null || selected.isEmpty()) return List.of();
            List<HttpRequestResponse> ordered = reversed(selected);
            String hk = currentHotKey;
            JMenuItem item = new JMenuItem(hk == null ? "Send to LazyFlow" : "Send to LazyFlow [" + hk + "]");
            item.addActionListener(e -> new Thread(() -> handleExport(ordered)).start());
            return List.of(item);
        }
    }

    // ── core: build HTML with injected flow data and open it ──────────────────

    private void handleExport(List<HttpRequestResponse> items) {
        try {
            String flowTxt = buildFlowTxt(items);
            api.logging().logToOutput(
                "LazyFlow: built flow.txt — " + items.size() + " pairs, " + flowTxt.length() + " bytes");

            String html = htmlTemplate.replace(FLOW_DATA_MARKER, "`" + escapeForJsTemplate(flowTxt) + "`");

            File tmp = Files.createTempFile("lazyflow_", ".html").toFile();
            tmp.deleteOnExit();
            try (FileWriter fw = new FileWriter(tmp, StandardCharsets.UTF_8)) {
                fw.write(html);
            }

            api.logging().logToOutput("LazyFlow: opening " + tmp.getAbsolutePath());
            openBrowser(tmp.toURI());

            final File toDelete = tmp;
            new Thread(() -> {
                try { Thread.sleep(5_000); } catch (InterruptedException ignored) {}
                if (toDelete.delete()) api.logging().logToOutput("LazyFlow: temp file deleted.");
                else                  api.logging().logToOutput("LazyFlow: temp file already gone.");
            }, "LazyFlow-cleanup").start();

        } catch (Exception ex) {
            api.logging().logToError("LazyFlow handleExport error: " + ex);
        }
    }

    // ── flow.txt builder ──────────────────────────────────────────────────────

    private String buildFlowTxt(List<HttpRequestResponse> items) {
        StringBuilder sb = new StringBuilder();
        for (HttpRequestResponse rr : items) {
            sb.append("===REQUEST===\n");
            if (rr.request() != null)
                sb.append(toLF(rr.request().toString()));
            sb.append("\n===RESPONSE===\n");
            if (rr.response() != null)
                sb.append(toLF(rr.response().toString()));
            else
                sb.append("HTTP/1.1 000 No Response\n");
            sb.append("\n");
        }
        return sb.toString();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String loadResource(String path) throws IOException {
        try (InputStream in = LazyFlow.class.getResourceAsStream(path)) {
            if (in == null) throw new FileNotFoundException("resource not found on classpath: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Escapes text so it can sit safely inside a JS template literal within a &lt;script&gt; block. */
    private static String escapeForJsTemplate(String s) {
        return s
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
            .replace("</script>", "<\\/script>");
    }

    private static List<HttpRequestResponse> reversed(List<HttpRequestResponse> items) {
        List<HttpRequestResponse> copy = new ArrayList<>(items);
        Collections.reverse(copy);
        return copy;
    }

    private static String toLF(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }

    // ── open browser ──────────────────────────────────────────────────────────

    private void openBrowser(URI uri) {
        try {
            if (Desktop.isDesktopSupported() &&
                    Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                return;
            }
            String url = uri.toString();
            for (String cmd : new String[]{"xdg-open", "firefox", "chromium-browser", "google-chrome"}) {
                try { Runtime.getRuntime().exec(new String[]{cmd, url}); return; }
                catch (IOException ignored) {}
            }
            api.logging().logToOutput("LazyFlow: open manually: " + url);
        } catch (Exception e) {
            api.logging().logToError("LazyFlow openBrowser: " + e);
        }
    }
}
