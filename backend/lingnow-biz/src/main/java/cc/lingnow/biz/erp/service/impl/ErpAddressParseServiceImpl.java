package cc.lingnow.biz.erp.service.impl;

import cc.lingnow.biz.erp.model.ErpAddressParseVO;
import cc.lingnow.biz.erp.model.ErpAddressRegionVO;
import cc.lingnow.biz.erp.service.ErpAddressParseService;
import cc.lingnow.biz.erp.service.ErpAddressRegionService;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ErpAddressParseServiceImpl implements ErpAddressParseService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(1[3-9]\\d{9}(?:\\s*[-－—转#]\\s*\\d{1,6})?)(?!\\d)");
    private static final Pattern LABEL_PATTERN = Pattern.compile("(?<![\\u4e00-\\u9fa5A-Za-z0-9])(收货地址|工作室地址|所在地区|详细地址|收货电话|收件电话|联系电话|手机号码|手机号|收货人|收件人|联系人|姓名|手机|电话|地址)\\s*[:：]");
    private static final Pattern HONORIFIC_CONTACT_PATTERN = Pattern.compile("(?:^|[^\\u4e00-\\u9fa5])([\\u4e00-\\u9fa5]{1,3}(?:先生|女士|小姐|老板|总))$");
    private static final List<String> PROVINCE_SUFFIXES = List.of("省", "自治区", "市");
    private static final List<String> CITY_SUFFIXES = List.of("自治州", "地区", "盟", "市");
    private static final List<String> DISTRICT_SUFFIXES = List.of("自治县", "新区", "区", "县", "旗", "市");
    private static final Set<String> GENERIC_REGION_NAMES = Set.of("市辖区", "县");

    private final ErpAddressRegionService addressRegionService;

    @Override
    public ErpAddressParseVO parse(String rawText) {
        String text = normalize(rawText);
        ErpAddressParseVO vo = new ErpAddressParseVO();
        LabelValues labels = extractLabels(text);
        PhoneMatch textPhoneMatch = extractPhone(text);
        PhoneMatch labelPhoneMatch = extractPhone(labels.phoneText());
        PhoneMatch selectedPhoneMatch = firstNonBlankPhoneMatch(labelPhoneMatch, textPhoneMatch);
        vo.setPhone(selectedPhoneMatch == null ? null : normalizePhone(selectedPhoneMatch.value()));

        String withoutPhone = selectedPhoneMatch == null ? text : removePhoneValue(text, selectedPhoneMatch.value());
        String cleaned = LABEL_PATTERN.matcher(withoutPhone).replaceAll(" ");
        cleaned = normalize(cleaned);

        String contact = extractContactFromLabel(labels.contactText());
        if (StrUtil.isBlank(contact)) {
            contact = extractContactNearPhone(text, textPhoneMatch);
        }

        String addressText = StrUtil.isNotBlank(labels.addressText()) ? labels.addressText() : cleaned;
        addressText = selectedPhoneMatch == null ? addressText : removePhoneValue(addressText, selectedPhoneMatch.value());
        List<String> contactCandidates = extractContactCandidates(text, labels, selectedPhoneMatch, addressText, contact);
        if (StrUtil.isBlank(contact) && !contactCandidates.isEmpty()) {
            contact = contactCandidates.get(0);
        }
        if (StrUtil.isBlank(contact) && selectedPhoneMatch == null) {
            contact = extractContact(addressText);
        }
        if (StrUtil.isNotBlank(contact)) {
            addressText = removeFirst(addressText, contact);
        }
        addressText = LABEL_PATTERN.matcher(addressText).replaceAll(" ");
        addressText = normalize(addressText);

        Region fallbackRegion = extractRegion(addressText);
        ErpAddressRegionVO matchedRegion = addressRegionService.matchAddress(addressText);
        applyRegion(vo, matchedRegion, fallbackRegion);

        String detail = addressText;
        vo.setContactName(contact);
        vo.setContactCandidates(contactCandidates);
        detail = matchedRegion == null ? normalize(detail) : removeRegionPrefix(detail, matchedRegion.getPathNames());
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

    private PhoneMatch extractPhone(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        Matcher matcher = PHONE_PATTERN.matcher(text);
        return matcher.find() ? new PhoneMatch(matcher.start(1), matcher.end(1), matcher.group(1)) : null;
    }

    private String normalizePhone(String value) {
        return value == null ? null : value.replaceAll("\\s+", "")
                .replace('－', '-')
                .replace('—', '-')
                .replace("转", "-")
                .replace("#", "-");
    }

    private String removePhone(String text) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        return normalize(PHONE_PATTERN.matcher(text).replaceFirst(" "));
    }

    private String removePhoneValue(String text, String phoneValue) {
        if (StrUtil.isBlank(text) || StrUtil.isBlank(phoneValue)) {
            return text;
        }
        return normalize(text.replaceFirst(Pattern.quote(phoneValue), " "));
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
            Matcher honorificMatcher = HONORIFIC_CONTACT_PATTERN.matcher(part);
            if (honorificMatcher.find()) {
                return honorificMatcher.group(1);
            }
            String candidate = cleanContactCandidate(part);
            if (isContactCandidate(candidate, 2)) {
                return candidate;
            }
        }
        return null;
    }

    private String extractContactFromLabel(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String withoutPhone = removePhone(text);
        String token = firstContactToken(withoutPhone, 1);
        if (StrUtil.isNotBlank(token)) {
            return token;
        }
        String cleaned = cleanContactCandidate(withoutPhone);
        return cleaned.length() >= 1 && cleaned.length() <= 8 ? cleaned : extractContact(text);
    }

    private String extractContactNearPhone(String text, PhoneMatch phone) {
        if (StrUtil.isBlank(text) || phone == null || phone.start() <= 0) {
            return null;
        }
        String beforePhone = normalize(LABEL_PATTERN.matcher(text.substring(0, phone.start())).replaceAll(" "));
        Matcher matcher = HONORIFIC_CONTACT_PATTERN.matcher(beforePhone);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return lastImmediateContactToken(beforePhone, 1);
    }

    private List<String> extractContactCandidates(String text, LabelValues labels, PhoneMatch phone, String addressText, String selectedContact) {
        Set<String> candidates = new LinkedHashSet<>();
        addContactCandidate(candidates, extractContactFromLabel(labels.contactText()), true);
        addContactCandidate(candidates, extractContactBeforeRegion(addressText), true);
        addContactCandidate(candidates, selectedContact, true);
        addContactCandidate(candidates, extractHonorificNearPhone(text, phone), true);
        addContactCandidate(candidates, extractLooseContactNearPhone(text, phone), false);
        return new ArrayList<>(candidates);
    }

    private String extractContactBeforeRegion(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        Region region = extractRegion(text);
        if (region.fullText() == null) {
            return null;
        }
        int index = text.indexOf(region.fullText());
        if (index <= 0) {
            return null;
        }
        String beforeRegion = normalize(text.substring(0, index));
        String candidate = firstContactToken(beforeRegion, 1);
        return isImmediateContactCandidate(candidate, 1) ? candidate : null;
    }

    private String extractHonorificNearPhone(String text, PhoneMatch phone) {
        if (StrUtil.isBlank(text) || phone == null || phone.start() <= 0) {
            return null;
        }
        String beforePhone = normalize(LABEL_PATTERN.matcher(text.substring(0, phone.start())).replaceAll(" "));
        Matcher matcher = HONORIFIC_CONTACT_PATTERN.matcher(beforePhone);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractLooseContactNearPhone(String text, PhoneMatch phone) {
        if (StrUtil.isBlank(text) || phone == null || phone.start() <= 0) {
            return null;
        }
        String beforePhone = normalize(LABEL_PATTERN.matcher(text.substring(0, phone.start())).replaceAll(" "));
        return lastContactToken(beforePhone, 1);
    }

    private void addContactCandidate(Set<String> candidates, String value, boolean strict) {
        String candidate = cleanContactCandidate(value);
        if (StrUtil.isBlank(candidate)) {
            return;
        }
        boolean valid = strict ? isImmediateContactCandidate(candidate, 1) || isContactCandidate(candidate, 2) : isContactCandidate(candidate, 1);
        if (valid) {
            candidates.add(candidate);
        }
    }

    private String firstContactToken(String text, int minLength) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String[] parts = text.split(" ");
        for (String part : parts) {
            String candidate = cleanContactCandidate(part);
            if (isContactCandidate(candidate, minLength)) {
                return candidate;
            }
        }
        return null;
    }

    private String lastContactToken(String text, int minLength) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String[] parts = text.split(" ");
        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = cleanContactCandidate(parts[i]);
            if (isContactCandidate(candidate, minLength)) {
                return candidate;
            }
        }
        return null;
    }

    private String lastImmediateContactToken(String text, int minLength) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String[] parts = text.split(" ");
        if (parts.length == 0) {
            return null;
        }
        String candidate = cleanContactCandidate(parts[parts.length - 1]);
        return isImmediateContactCandidate(candidate, minLength) ? candidate : null;
    }

    private String cleanContactCandidate(String value) {
        return value == null ? "" : value.replaceAll("[^\\u4e00-\\u9fa5A-Za-z·]", "");
    }

    private boolean isContactCandidate(String value, int minLength) {
        if (value == null) {
            return false;
        }
        return value.length() >= minLength && value.length() <= 8;
    }

    private boolean isImmediateContactCandidate(String value, int minLength) {
        if (StrUtil.isBlank(value) || value.length() < minLength) {
            return false;
        }
        if (value.length() <= 4) {
            return true;
        }
        return HONORIFIC_CONTACT_PATTERN.matcher(value).find();
    }

    private LabelValues extractLabels(String text) {
        if (StrUtil.isBlank(text)) {
            return new LabelValues(null, null, null);
        }
        Matcher matcher = LABEL_PATTERN.matcher(text);
        List<LabelToken> tokens = new java.util.ArrayList<>();
        while (matcher.find()) {
            tokens.add(new LabelToken(matcher.group(1), matcher.start(), matcher.end()));
        }
        String contact = null;
        String phone = null;
        String region = null;
        String detailAddress = null;
        String address = null;
        for (int i = 0; i < tokens.size(); i++) {
            LabelToken current = tokens.get(i);
            int nextStart = i + 1 < tokens.size() ? tokens.get(i + 1).start() : text.length();
            String value = normalize(text.substring(current.end(), nextStart));
            if (StrUtil.isBlank(value)) {
                continue;
            }
            if (isContactLabel(current.name()) && StrUtil.isBlank(contact)) {
                contact = value;
            } else if (isPhoneLabel(current.name()) && StrUtil.isBlank(phone)) {
                phone = value;
            } else if (isRegionLabel(current.name()) && StrUtil.isBlank(region)) {
                region = value;
            } else if (isDetailAddressLabel(current.name()) && StrUtil.isBlank(detailAddress)) {
                detailAddress = value;
            } else if (isAddressLabel(current.name()) && StrUtil.isBlank(address)) {
                address = value;
            }
        }
        if (StrUtil.isNotBlank(region) || StrUtil.isNotBlank(detailAddress)) {
            address = normalize(String.join(" ",
                    StrUtil.blankToDefault(region, ""),
                    StrUtil.blankToDefault(detailAddress, "")));
        }
        return new LabelValues(contact, phone, address);
    }

    private boolean isContactLabel(String label) {
        return "收货人".equals(label) || "收件人".equals(label) || "联系人".equals(label) || "姓名".equals(label);
    }

    private boolean isPhoneLabel(String label) {
        return "电话".equals(label)
                || "手机".equals(label)
                || "手机号".equals(label)
                || "手机号码".equals(label)
                || "收货电话".equals(label)
                || "收件电话".equals(label)
                || "联系电话".equals(label);
    }

    private boolean isRegionLabel(String label) {
        return "所在地区".equals(label);
    }

    private boolean isDetailAddressLabel(String label) {
        return "详细地址".equals(label);
    }

    private boolean isAddressLabel(String label) {
        return "地址".equals(label) || "收货地址".equals(label) || "工作室地址".equals(label);
    }

    private String removeFirst(String text, String value) {
        if (StrUtil.isBlank(text) || StrUtil.isBlank(value)) {
            return text;
        }
        return normalize(text.replaceFirst(Pattern.quote(value) + "\\s*(?:\\[[^\\]]+\\])?", " "));
    }

    private PhoneMatch firstNonBlankPhoneMatch(PhoneMatch first, PhoneMatch second) {
        return first == null ? second : first;
    }

    private void applyRegion(ErpAddressParseVO vo, ErpAddressRegionVO matchedRegion, Region fallbackRegion) {
        if (matchedRegion == null || matchedRegion.getPathNames() == null || matchedRegion.getPathNames().isEmpty()) {
            vo.setProvince(fallbackRegion.province());
            vo.setCity(fallbackRegion.city());
            vo.setDistrict(fallbackRegion.district());
            return;
        }
        List<String> names = matchedRegion.getPathNames();
        vo.setRegionPath(matchedRegion.getPath());
        vo.setRegionPathNames(names);
        vo.setProvince(valueAt(names, 0));
        vo.setCity(valueAt(names, 1));
        vo.setDistrict(valueAt(names, 2));
        vo.setStreet(valueAt(names, 3));
        vo.setVillage(valueAt(names, 4));
    }

    private String valueAt(List<String> values, int index) {
        return values == null || values.size() <= index ? null : values.get(index);
    }

    private String removeRegionPrefix(String text, List<String> pathNames) {
        String result = normalize(text);
        if (StrUtil.isBlank(result) || pathNames == null || pathNames.isEmpty()) {
            return result;
        }
        for (String name : pathNames) {
            if (GENERIC_REGION_NAMES.contains(name)) {
                continue;
            }
            result = removeLeadingRegionName(result, name);
        }
        return normalize(result);
    }

    private String removeLeadingRegionName(String text, String regionName) {
        String result = normalize(text);
        for (String alias : regionAliases(regionName)) {
            String next = removeLeadingTextIgnoreSpaces(result, alias);
            if (!next.equals(result)) {
                return next;
            }
        }
        return result;
    }

    private String removeLeadingTextIgnoreSpaces(String text, String prefix) {
        String normalizedPrefix = normalizeComparable(prefix);
        if (StrUtil.isBlank(text) || StrUtil.isBlank(normalizedPrefix)) {
            return text;
        }
        StringBuilder compact = new StringBuilder();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            compact.append(ch);
            indexes.add(i);
        }
        if (!compact.toString().startsWith(normalizedPrefix)) {
            return text;
        }
        int compactEnd = normalizedPrefix.length() - 1;
        if (compactEnd < 0 || compactEnd >= indexes.size()) {
            return text;
        }
        int originalEnd = indexes.get(compactEnd) + 1;
        return normalize(text.substring(originalEnd));
    }

    private List<String> regionAliases(String name) {
        String normalized = normalizeComparable(name);
        if (StrUtil.isBlank(normalized)) {
            return List.of();
        }
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(normalized);
        addRegionAlias(aliases, normalized, "街道办事处");
        addRegionAlias(aliases, normalized, "街道");
        addRegionAlias(aliases, normalized, "镇");
        addRegionAlias(aliases, normalized, "乡");
        addRegionAlias(aliases, normalized, "苏木");
        addRegionAlias(aliases, normalized, "社区居民委员会");
        addRegionAlias(aliases, normalized, "社区居委会");
        addRegionAlias(aliases, normalized, "居民委员会");
        addRegionAlias(aliases, normalized, "村民委员会");
        addRegionAlias(aliases, normalized, "村委会");
        addRegionAlias(aliases, normalized, "委员会");
        return new ArrayList<>(aliases);
    }

    private void addRegionAlias(Set<String> aliases, String value, String suffix) {
        if (value.endsWith(suffix) && value.length() > suffix.length()) {
            aliases.add(value.substring(0, value.length() - suffix.length()));
        }
    }

    private String normalizeComparable(String value) {
        return StrUtil.blankToDefault(value, "").replaceAll("\\s+", "");
    }

    private String buildNormalized(ErpAddressParseVO vo) {
        return normalize(String.join(" ",
                StrUtil.blankToDefault(vo.getProvince(), ""),
                StrUtil.blankToDefault(vo.getCity(), ""),
                StrUtil.blankToDefault(vo.getDistrict(), ""),
                StrUtil.blankToDefault(vo.getStreet(), ""),
                StrUtil.blankToDefault(vo.getVillage(), ""),
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
        if (vo.getContactCandidates() != null && vo.getContactCandidates().size() > 1) {
            warnings.add("收货人存在多个候选，请确认");
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

    private record PhoneMatch(int start, int end, String value) {
    }

    private record LabelToken(String name, int start, int end) {
    }

    private record LabelValues(String contactText, String phoneText, String addressText) {
    }
}
