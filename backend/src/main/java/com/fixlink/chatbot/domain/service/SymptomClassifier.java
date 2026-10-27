package com.fixlink.chatbot.domain.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phân loại tin nhắn tự do của khách (có thể viết tắt, không dấu) thành dữ kiện nghiệp vụ FixLink:
 * danh mục dịch vụ, dấu hiệu nguy hiểm, khu vực phục vụ và ngân sách. Thuần Java, không phụ thuộc
 * Spring — dùng chung cho tool suggest_category, bộ kịch bản tình huống và phần điền sẵn form.
 */
public final class SymptomClassifier {

    private SymptomClassifier() {
    }

    public record CategoryMatch(String code, String name, String reason, int score) {
        public String confidence() {
            return score >= 3 ? "cao" : score == 2 ? "trung bình" : "thấp";
        }
    }

    private record CategoryRule(String code, String name, String reason, List<String> keywords) { }

    private static final List<CategoryRule> CATEGORY_RULES = List.of(
            new CategoryRule("DIEN_LANH", "Sửa Chữa Điện Lạnh",
                    "Thiết bị làm lạnh, điều hòa, tủ lạnh, tủ đông, máy giặt thuộc chuyên ngành Điện lạnh.",
                    List.of("tu lanh", "may lanh", "dieu hoa", "may dieu hoa", "tu dong", "tu mat", "may giat", "may giac",
                            "nap ga", "bom ga", "het ga", "xi ga", "ga lanh", "dong tuyet", "dong da", "khong lanh",
                            "khong mat", "dan lanh", "dan nong", "block", "may say", "ve sinh may lanh", "inverter",
                            "air conditioner", "fridge", "refrigerator", "washing machine", "aircon")),
            new CategoryRule("DIEN_NUOC", "Sửa Chữa Điện Nước",
                    "Sự cố hệ thống điện sinh hoạt và cấp thoát nước trong nhà.",
                    List.of("ong nuoc", "vo ong", "ro ri nuoc", "ri nuoc", "nghet", "tac nghet", "bi tac", "tac bon cau",
                            "bon cau", "lavabo", "voi nuoc", "voi sen", "voi rua", "sen tam", "bon rua", "may bom",
                            "bon nuoc", "nuoc yeu", "mat nuoc", "tham nuoc", "tham dot",
                            "chap dien", "nhay cb", "nhay aptomat", "aptomat", "cau dao", "cau chi", "mat dien", "o cam",
                            "cong tac", "ro dien", "dien giat", "day dien", "bong den", "den khong sang", "pipe", "leak",
                            "toilet", "circuit breaker", "short circuit", "power outage")),
            new CategoryRule("DO_GIA_DUNG", "Sửa Điện Gia Dụng",
                    "Thiết bị điện gia dụng trong gia đình.",
                    List.of("bep tu", "bep hong ngoai", "bep dien", "lo vi song", "lo nuong", "noi chien", "noi com",
                            "may hut mui", "quat", "am sieu toc", "binh nong lanh", "may loc nuoc", "may xay",
                            // không dùng "ban la" (bàn là): trùng với "bạn là" sau khi bỏ dấu
                            "ban ui", "ban la hoi", "may nuoc nong", "may nong lanh", "binh nuoc nong", "may hut bui", "tivi", "ti vi", "loa", "induction", "microwave",
                            "air fryer")),
            new CategoryRule("KHOA_CUA", "Sửa Khóa & Cửa Cuốn",
                    "Sự cố khóa, chìa khóa và cửa cuốn.",
                    List.of("khoa cua", "o khoa", "mat chia", "chia khoa", "ket khoa", "khoa phong", "khoa van tay",
                            "khoa thong minh", "cua cuon", "ket nan", "motor cua", "remote cua", "lock", "key",
                            "rolling door", "khong mo duoc cua"))
    );

    private static final List<String> DANGER_KEYWORDS = List.of(
            "bi chay", "chay khet", "mui khet", "khet", "boc chay", "boc khoi", "ra khoi", "xet lua", "toe lua",
            "phat no", "bi no", "no tung", "ro gas", "ro ga", "xi gas", "mui gas", "dien giat", "giat dien", "bi giat",
            "set dien", "xet dien", "phat lua", "chap chay", "ro dien",
            "ngap nuoc", "nuoc ngap", "fire", "smoke", "burning", "gas leak", "sparks", "electric shock");

    /** Cụm từ (đã bỏ dấu) → mã khu vực phục vụ trong dữ liệu master. */
    private static final Map<String, String> AREA_ALIASES = new LinkedHashMap<>();

    static {
        AREA_ALIASES.put("quan 1", "Q1");
        AREA_ALIASES.put("q1", "Q1");
        AREA_ALIASES.put("quan 3", "Q3");
        AREA_ALIASES.put("q3", "Q3");
        AREA_ALIASES.put("quan 7", "Q7");
        AREA_ALIASES.put("q7", "Q7");
        AREA_ALIASES.put("binh thanh", "BINH_THANH");
        AREA_ALIASES.put("thu duc", "THU_DUC");
        AREA_ALIASES.put("tan binh", "TAN_BINH");
        AREA_ALIASES.put("go vap", "GO_VAP");
        AREA_ALIASES.put("phu nhuan", "PHU_NHUAN");
    }

    /** Viết tắt / teen code phổ biến (đã bỏ dấu) → dạng đầy đủ không dấu. */
    private static final Map<String, String> ABBREVIATIONS = Map.ofEntries(
            Map.entry("ko", "khong"), Map.entry("k", "khong"), Map.entry("kh", "khong"), Map.entry("hok", "khong"),
            Map.entry("hk", "khong"), Map.entry("hem", "khong"), Map.entry("j", "gi"), Map.entry("z", "vay"),
            Map.entry("dc", "duoc"), Map.entry("dk", "duoc"), Map.entry("bn", "bao nhieu"), Map.entry("bnh", "bao nhieu"),
            Map.entry("ml", "may lanh"), Map.entry("dh", "dieu hoa"), Map.entry("tl", "tu lanh"), Map.entry("mg", "may giat"),
            Map.entry("sdt", "so dien thoai"), Map.entry("ib", "nhan tin"), Map.entry("ad", "admin"), Map.entry("tk", "tai khoan"),
            Map.entry("mk", "mat khau"), Map.entry("ntn", "nhu the nao"), Map.entry("r", "roi"), Map.entry("vs", "voi"),
            Map.entry("bik", "biet"), Map.entry("bt", "biet"), Map.entry("lm", "lam"), Map.entry("e", "em"), Map.entry("a", "anh"));

    /** Nghĩa tiếng Việt có dấu của các viết tắt dễ hiểu nhầm, để giải thích cho LLM. */
    private static final Map<String, String> ABBREVIATION_MEANINGS = Map.ofEntries(
            Map.entry("ml", "máy lạnh"), Map.entry("dh", "điều hòa"), Map.entry("tl", "tủ lạnh"), Map.entry("mg", "máy giặt"),
            Map.entry("ko", "không"), Map.entry("k", "không"), Map.entry("bn", "bao nhiêu"), Map.entry("sdt", "số điện thoại"),
            Map.entry("tk", "tài khoản"), Map.entry("mk", "mật khẩu"), Map.entry("ntn", "như thế nào"), Map.entry("j", "gì"));

    private static final Pattern BUDGET = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(k|nghin|ngan(?!\\s*sach)|d|dong|vnd)?(?![a-z0-9])");
    /** Số đứng sau "quận"/"q" là tên quận (quận 7), không phải số tiền. */
    private static final Pattern DISTRICT_PREFIX = Pattern.compile("(?:quan|q)\\s*$");
    private static final Pattern TRIEU = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(?:trieu|tr|cu)(?![a-z])\\s*(\\d)?(?!\\d)");

    public static Optional<CategoryMatch> classify(String rawText) {
        String text = " " + normalize(rawText) + " ";
        CategoryRule best = null;
        int bestScore = 0;
        for (CategoryRule rule : CATEGORY_RULES) {
            int score = 0;
            for (String kw : rule.keywords()) {
                if (text.contains(" " + kw + " ")) {
                    score += kw.length() > 6 ? 2 : 1;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = rule;
            }
        }
        return best == null ? Optional.empty()
                : Optional.of(new CategoryMatch(best.code(), best.name(), best.reason(), bestScore));
    }

    public static List<String> categoryNames() {
        return CATEGORY_RULES.stream().map(CategoryRule::name).toList();
    }

    public static boolean isDanger(String rawText) {
        String text = " " + normalize(rawText) + " ";
        return DANGER_KEYWORDS.stream().anyMatch(kw -> text.contains(" " + kw + " "));
    }

    private static final Map<String, String> AREA_LABELS = Map.of(
            "Q1", "Quận 1", "Q3", "Quận 3", "Q7", "Quận 7", "BINH_THANH", "Quận Bình Thạnh", "THU_DUC", "TP. Thủ Đức",
            "TAN_BINH", "Quận Tân Bình", "GO_VAP", "Quận Gò Vấp", "PHU_NHUAN", "Quận Phú Nhuận");

    public static String areaLabel(String areaCode) {
        return AREA_LABELS.getOrDefault(areaCode, areaCode);
    }

    public static Optional<String> detectAreaCode(String rawText) {
        String text = " " + normalize(rawText) + " ";
        for (Map.Entry<String, String> e : AREA_ALIASES.entrySet()) {
            if (text.contains(" " + e.getKey() + " ")) {
                return Optional.of(e.getValue());
            }
        }
        return Optional.empty();
    }

    /** Nhận "300k", "300 nghìn", "1tr2", "1,5 triệu", "500000đ"; chỉ chấp nhận khi có ngữ cảnh tiền rõ ràng. */
    public static Optional<Long> detectBudget(String rawText) {
        String text = normalize(rawText);
        Matcher trieu = TRIEU.matcher(text);
        if (trieu.find()) {
            double millions = Double.parseDouble(trieu.group(1).replace(',', '.'));
            long value = Math.round(millions * 1_000_000L);
            if (trieu.group(2) != null) {
                value += Long.parseLong(trieu.group(2)) * 100_000L; // "1tr2" = 1.200.000
            }
            return Optional.of(value);
        }
        Matcher m = BUDGET.matcher(text);
        while (m.find()) {
            if (DISTRICT_PREFIX.matcher(text.substring(0, m.start())).find()) {
                continue;
            }
            String unit = m.group(2);
            double number = Double.parseDouble(m.group(1).replace(',', '.'));
            if (unit == null) {
                String digits = m.group(1).replaceAll("\\D", "");
                boolean looksLikePhone = digits.startsWith("0") || digits.length() > 9;
                if (digits.length() >= 5 && !looksLikePhone
                        && text.matches(".*\\b(ngan sach|gia|tien|chi phi|tam|khoang|budget)\\b.*")) {
                    return Optional.of(Long.parseLong(digits));
                }
                continue;
            }
            switch (unit) {
                case "k", "nghin", "ngan" -> {
                    return Optional.of(Math.round(number * 1_000));
                }
                default -> {
                    String digits = m.group(1).replaceAll("\\D", "");
                    if (digits.length() >= 5) {
                        return Optional.of(Long.parseLong(digits));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Liệt kê các viết tắt dễ hiểu nhầm có trong tin nhắn, ví dụ ["tl = tủ lạnh"]. */
    public static List<String> explainAbbreviations(String rawText) {
        String plain = basicNormalize(rawText);
        if (plain.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(plain.split(" "))
                .distinct()
                .filter(ABBREVIATION_MEANINGS::containsKey)
                .map(w -> w + " = " + ABBREVIATION_MEANINGS.get(w))
                .toList();
    }

    /** Chữ thường, bỏ dấu, bỏ ký tự đặc biệt và mở rộng viết tắt. */
    public static String normalize(String text) {
        String plain = basicNormalize(text);
        if (plain.isEmpty()) {
            return plain;
        }
        StringBuilder sb = new StringBuilder();
        for (String word : plain.split(" ")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(ABBREVIATIONS.getOrDefault(word, word));
        }
        return sb.toString();
    }

    private static String basicNormalize(String text) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT).replace('đ', 'd');
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("(\\d)[.,](\\d{3})", "$1$2")
                .replaceAll("[^a-z0-9 ,.]", " ")
                .replaceAll("(?<!\\d)[.,]|[.,](?!\\d)", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
