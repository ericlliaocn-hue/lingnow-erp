package cc.lingnow.admin.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvExportUtil {

    private CsvExportUtil() {
    }

    public static void write(HttpServletResponse response, String filename, List<String> headers, List<? extends List<?>> rows) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        StringBuilder csv = new StringBuilder("\uFEFF");
        appendLine(csv, headers);
        for (List<?> row : rows) {
            appendLine(csv, row);
        }
        response.getWriter().write(csv.toString());
    }

    private static void appendLine(StringBuilder csv, List<?> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(values.get(i)));
        }
        csv.append('\n');
    }

    private static String escape(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
