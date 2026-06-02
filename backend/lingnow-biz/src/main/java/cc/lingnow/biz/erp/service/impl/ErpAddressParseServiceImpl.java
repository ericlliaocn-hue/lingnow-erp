package cc.lingnow.biz.erp.service.impl;

import cc.lingnow.biz.erp.model.ErpAddressParseVO;
import cc.lingnow.biz.erp.service.ErpAddressParseService;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ErpAddressParseServiceImpl implements ErpAddressParseService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)");
    private static final Pattern LABEL_PATTERN = Pattern.compile("(收货人|收件人|联系人|姓名|电话|手机|手机号|地址|收货地址|详细地址)[:：]");
    private static final List<String> PROVINCE_SUFFIXES = List.of("省", "自治区", "市");
    private static final List<String> CITY_SUFFIXES = List.of("自治州", "地区", "盟", "市");
    private static final List<String> DISTRICT_SUFFIXES = List.of("自治县", "新区", "区", "县", "旗", "市");

    @Override
    public ErpAddressParseVO parse(String rawText) {
        String text = normalize(rawText);
        ErpAddressParseVO vo = new ErpAddressParseVO();
        vo.setPhone(extractPhone(text));

        String withoutPhone = StrUtil.isBlank(vo.getPhone()) ? text : text.replaceFirst(Pattern.quote(vo.getPhone()), " ");
        String cleaned = LABEL_PATTERN.matcher(withoutPhone).replaceAll(" ");
        cleaned = normalize(cleaned);

        Region region = extractRegion(cleaned);
        vo.setProvince(region.province());
        vo.setCity(region.city());
        vo.setDistrict(region.district());

        String detail = cleaned;
        if (StrUtil.isNotBlank(region.fullText())) {
            detail = detail.replaceFirst(Pattern.quote(region.fullText()), " ");
        }
        String contact = extractContact(detail);
        vo.setContactName(contact);
        if (StrUtil.isNotBlank(contact)) {
            detail = detail.replaceFirst(Pattern.quote(contact), " ");
        }
        detail = normalize(detail);
        vo.setDetailAddress(detail);
        vo.setNormalizedAddress(buildNormalized(vo));
        vo.setWarnings(buildWarnings(vo));
        vo.setConfidence(confidence(vo));
        return vo;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[,，;；\\n\\r\\t]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Region extractRegion(String text) {
        Token province = findFirstBySuffix(text, 0, PROVINCE_SUFFIXES, null);
        Token city = findFirstBySuffix(text, province == null ? 0 : province.end(), CITY_SUFFIXES, province);
        Token district = findFirstBySuffix(text, city == null ? (province == null ? 0 : province.end()) : city.end(), DISTRICT_SUFFIXES, city == null ? province : city);
        int start = firstStart(province, city, district);
        int end = lastEnd(province, city, district);
        String fullText = start >= 0 && end > start ? text.substring(start, end) : null;
        return new Region(value(province), value(city), value(district), fullText);
    }

    private Token findFirstBySuffix(String text, int from, List<String> suffixes, Token previous) {
        Token best = null;
        for (String suffix : suffixes) {
            int index = text.indexOf(suffix, Math.max(0, from));
            if (index < 0) {
                continue;
            }
            int start = resolveTokenStart(text, index, from, suffix, previous);
            String value = text.substring(start, index + suffix.length()).trim();
            if (value.length() < suffix.length() + 1) {
                continue;
            }
            Token token = new Token(start, index + suffix.length(), value);
            if (best == null || token.start() < best.start()) {
                best = token;
            }
        }
        return best;
    }

    private int resolveTokenStart(String text, int suffixIndex, int from, String suffix, Token previous) {
        int space = text.lastIndexOf(' ', suffixIndex);
        if (space >= from) {
            return space + 1;
        }
        if (previous != null && previous.end() <= suffixIndex) {
            return previous.end();
        }
        int maxNameLength = suffixMaxNameLength(suffix);
        return Math.max(from, suffixIndex - maxNameLength);
    }

    private int suffixMaxNameLength(String suffix) {
        return switch (suffix) {
            case "自治区" -> 5;
            case "自治州", "自治县" -> 4;
            case "地区", "新区" -> 4;
            case "省", "市", "区", "县", "旗", "盟" -> 3;
            default -> 4;
        };
    }

    private int firstStart(Token... tokens) {
        int start = -1;
        for (Token token : tokens) {
            if (token != null && (start < 0 || token.start() < start)) {
                start = token.start();
            }
        }
        return start;
    }

    private int lastEnd(Token... tokens) {
        int end = -1;
        for (Token token : tokens) {
            if (token != null && token.end() > end) {
                end = token.end();
            }
        }
        return end;
    }

    private String value(Token token) {
        return token == null ? null : token.value();
    }

    private String extractContact(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String[] parts = text.split(" ");
        for (String part : parts) {
            String candidate = part.replaceAll("[^\\u4e00-\\u9fa5A-Za-z·]", "");
            if (candidate.length() >= 2 && candidate.length() <= 8) {
                return candidate;
            }
        }
        return null;
    }

    private String buildNormalized(ErpAddressParseVO vo) {
        return normalize(String.join(" ",
                StrUtil.blankToDefault(vo.getProvince(), ""),
                StrUtil.blankToDefault(vo.getCity(), ""),
                StrUtil.blankToDefault(vo.getDistrict(), ""),
                StrUtil.blankToDefault(vo.getDetailAddress(), "")));
    }

    private List<String> buildWarnings(ErpAddressParseVO vo) {
        List<String> warnings = new java.util.ArrayList<>();
        if (StrUtil.isBlank(vo.getContactName())) {
            warnings.add("未识别到姓名");
        }
        if (StrUtil.isBlank(vo.getPhone())) {
            warnings.add("未识别到手机号");
        }
        if (StrUtil.isBlank(vo.getNormalizedAddress())) {
            warnings.add("未识别到地址");
        }
        if (StrUtil.isBlank(vo.getCity()) && StrUtil.isBlank(vo.getDistrict())) {
            warnings.add("省市区识别不完整");
        }
        return warnings;
    }

    private int confidence(ErpAddressParseVO vo) {
        int score = 0;
        if (StrUtil.isNotBlank(vo.getContactName())) score += 20;
        if (StrUtil.isNotBlank(vo.getPhone())) score += 30;
        if (StrUtil.isNotBlank(vo.getProvince())) score += 10;
        if (StrUtil.isNotBlank(vo.getCity())) score += 15;
        if (StrUtil.isNotBlank(vo.getDistrict())) score += 15;
        if (StrUtil.isNotBlank(vo.getDetailAddress())) score += 10;
        return Math.min(score, 100);
    }

    private record Token(int start, int end, String value) {
    }

    private record Region(String province, String city, String district, String fullText) {
    }
}
