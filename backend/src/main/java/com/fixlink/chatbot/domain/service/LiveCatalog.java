package com.fixlink.chatbot.domain.service;

import com.fixlink.chatbot.domain.service.ServiceCatalog.Item;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bản chụp dữ liệu dịch vụ đang dùng để trả lời: danh mục, gói dịch vụ (giá, thời gian) và khu vực phục vụ.
 * Được làm mới định kỳ từ database (xem LiveCatalogRefresher) để chatbot luôn nói đúng dữ liệu mới nhất; khi chưa đọc
 * được database thì dùng bộ mặc định khớp dữ liệu khởi tạo V2__seed_master_data.sql.
 *
 * <p>Câu trả lời soạn sẵn dùng các thẻ {@code {{...}}} (xem {@link #resolve(String)}) thay vì ghi cứng giá, số nhóm
 * hay danh sách khu vực.
 */
public final class LiveCatalog {

    public record Category(String code, String name, String description) { }

    public record Area(String code, String name) { }

    public record Snapshot(List<Category> categories, List<Item> items, List<Area> areas, boolean fromDatabase) { }

    /** Từ khóa nhận diện các gói có sẵn (theo mã gói trong database); gói mới thì tự tách từ tên gói. */
    private static final Map<String, List<String>> KEYWORDS_BY_CODE = Map.ofEntries(
            Map.entry("VE_SINH_MAY_LANH", List.of("ve sinh may lanh", "ve sinh dieu hoa", "bao duong may lanh")),
            Map.entry("NAP_GA_R32", List.of("nap ga", "bom ga", "het ga", "nap gas", "bom gas", "het gas")),
            Map.entry("SUA_TU_LANH", List.of("tu lanh")),
            Map.entry("SUA_MAY_GIAT", List.of("may giat")),
            Map.entry("SUA_ONG_NUOC", List.of("ong nuoc", "thong nghet", "thong tac", "bon rua", "thong cong", "cong nghet",
                    "nghet cong")),
            Map.entry("LAP_THIET_BI_VE_SINH", List.of("bon cau", "lavabo", "voi sen", "voi hoa sen", "thiet bi ve sinh")),
            Map.entry("SUA_CHAP_DIEN", List.of("chap dien", "nhay cb", "aptomat", "cau dao")),
            Map.entry("SUA_BEP_TU", List.of("bep tu")),
            Map.entry("SUA_LO_VI_SONG", List.of("lo vi song")),
            Map.entry("MO_KHOA_CAP_TOC", List.of("mo khoa", "khoa cua", "khoa phong", "mat chia")),
            Map.entry("SUA_CUA_CUON", List.of("cua cuon")));

    private static final Snapshot DEFAULT = new Snapshot(
            List.of(new Category("DIEN_LANH", "Sửa Chữa Điện Lạnh", "Điều hòa, tủ lạnh, máy giặt, tủ đông"),
                    new Category("DIEN_NUOC", "Sửa Chữa Điện Nước",
                            "Chập điện, rò rỉ đường ống nước, lắp thiết bị vệ sinh, máy bơm nước"),
                    new Category("DO_GIA_DUNG", "Sửa Điện Gia Dụng", "Bếp từ, lò vi sóng, máy hút mùi, nồi chiên không dầu, quạt điện"),
                    new Category("KHOA_CUA", "Sửa Khóa & Cửa Cuốn", "Mở khóa cấp tốc 24/7, chìa khóa thông minh, cửa cuốn")),
            List.of(
                    item("VE_SINH_MAY_LANH", "DIEN_LANH", "Vệ sinh máy lạnh treo tường (1.0–2.5 HP)", 150_000, 45, null),
                    item("NAP_GA_R32", "DIEN_LANH", "Nạp ga máy lạnh R32/R410A trọn gói", 250_000, 30, null),
                    item("SUA_TU_LANH", "DIEN_LANH", "Kiểm tra & sửa tủ lạnh không lạnh/kêu to", 200_000, 60, null),
                    item("SUA_MAY_GIAT", "DIEN_LANH", "Sửa máy giặt không vắt/báo lỗi bo mạch", 250_000, 60, null),
                    item("SUA_ONG_NUOC", "DIEN_NUOC", "Thông nghẹt & xử lý rò rỉ đường ống nước", 180_000, 45, null),
                    item("LAP_THIET_BI_VE_SINH", "DIEN_NUOC", "Lắp đặt bồn cầu, lavabo, vòi hoa sen", 200_000, 60, null),
                    item("SUA_CHAP_DIEN", "DIEN_NUOC", "Dò tìm chập điện âm tường & nhảy CB", 300_000, 90, null),
                    item("SUA_BEP_TU", "DO_GIA_DUNG", "Sửa bếp từ không nóng/chập nguồn báo lỗi", 200_000, 45, null),
                    item("SUA_LO_VI_SONG", "DO_GIA_DUNG", "Sửa lò vi sóng không nóng/đánh lửa", 180_000, 40, null),
                    item("MO_KHOA_CAP_TOC", "KHOA_CUA", "Mở khóa cửa nhà/khóa phòng cấp tốc 24/7", 150_000, 30,
                            "thợ có mặt sau 15–30 phút"),
                    item("SUA_CUA_CUON", "KHOA_CUA", "Sửa cửa cuốn kẹt nan/hỏng motor", 350_000, 90, null)),
            List.of(new Area("Q1", "Quận 1"), new Area("Q3", "Quận 3"), new Area("Q7", "Quận 7"),
                    new Area("BINH_THANH", "Quận Bình Thạnh"), new Area("TAN_BINH", "Quận Tân Bình"),
                    new Area("GO_VAP", "Quận Gò Vấp"), new Area("PHU_NHUAN", "Quận Phú Nhuận"), new Area("THU_DUC", "TP. Thủ Đức")),
            false);

    private static volatile Snapshot current = DEFAULT;

    private LiveCatalog() {
    }

    public static Snapshot get() {
        return current;
    }

    public static List<Item> items() {
        return current.items();
    }

    /** Thay bản chụp (dữ liệu rỗng hoặc lỗi thì giữ bản cũ để không trả lời "không có dịch vụ nào"). */
    public static void update(Snapshot snapshot) {
        if (snapshot != null && !snapshot.categories().isEmpty()) {
            current = snapshot;
        }
    }

    /** Khôi phục bộ mặc định (dùng trong test). */
    public static void reset() {
        current = DEFAULT;
    }

    /** Gói dịch vụ từ dữ liệu database: chuẩn hóa tên ("A / B" → "A/B"), lấy ghi chú "có mặt sau 15-30 phút" từ mô tả. */
    public static Item fromDatabase(String code, String categoryCode, String name, String description, long price,
                                    Integer minutes) {
        String cleanName = name == null ? "" : name.replace(" / ", "/").replaceAll("\\s+", " ").trim();
        String note = null;
        Matcher m = ARRIVAL.matcher(description == null ? "" : description);
        if (m.find()) {
            note = "thợ có mặt sau " + m.group(1) + "–" + m.group(2) + " phút";
        }
        return item(code, categoryCode, cleanName, price, minutes == null ? 0 : minutes, note);
    }

    private static final Pattern ARRIVAL = Pattern.compile("có mặt sau (\\d+)\\s*[-–]\\s*(\\d+) phút", Pattern.CASE_INSENSITIVE);

    private static Item item(String code, String group, String name, long price, int minutes, String note) {
        List<String> keywords = KEYWORDS_BY_CODE.getOrDefault(code, derivedKeywords(name));
        return new Item(code, group, name, price, minutes, note, keywords);
    }

    /** Gói mới chưa có từ khóa riêng: tách tên gói thành các cụm 2–4 chữ ("Sửa laptop / PC" → "sua laptop", "pc"). */
    static List<String> derivedKeywords(String name) {
        List<String> out = new ArrayList<>();
        for (String rawPart : name.split("[/&,;()]")) {
            String p = SymptomClassifier.normalize(rawPart).trim();
            int words = p.isEmpty() ? 0 : p.split(" ").length;
            if (words >= 2 && words <= 4) {
                out.add(p);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ truy vấn

    public static String categoryName(String code) {
        return current.categories().stream().filter(c -> c.code().equals(code)).map(Category::name).findFirst().orElse(code);
    }

    public static Optional<Item> item(String code) {
        return current.items().stream().filter(i -> i.code().equals(code)).findFirst();
    }

    public static String money(long amount) {
        return NumberFormat.getIntegerInstance(Locale.GERMANY).format(amount) + " đ";
    }

    public static long minPrice() {
        return current.items().stream().mapToLong(Item::price).min().orElse(0);
    }

    public static long maxPrice() {
        return current.items().stream().mapToLong(Item::price).max().orElse(0);
    }

    /** "Quận 1, Quận 3, Quận 7, Bình Thạnh, ... và TP. Thủ Đức". */
    public static String areaList() {
        List<String> names = current.areas().stream().map(a -> a.name().replaceFirst("^Quận (?=\\D)", "")).toList();
        if (names.size() <= 1) {
            return String.join("", names);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " và " + names.get(names.size() - 1);
    }

    /** Danh sách nhóm có đánh số và mô tả. */
    public static String categoryList() {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Category c : current.categories()) {
            sb.append(i++).append(". **").append(c.name()).append("**");
            if (c.description() != null && !c.description().isBlank()) {
                sb.append(" – ").append(lowerFirst(c.description().trim()));
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /** Tên các nhóm trên một dòng: "Sửa Chữa Điện Lạnh, Sửa Chữa Điện Nước, ...". */
    public static String categoryNames() {
        return String.join(", ", current.categories().stream().map(Category::name).toList());
    }

    /** Giá tham khảo các gói của một nhóm trên một dòng. */
    public static String referencePrices(String categoryCode) {
        List<String> parts = current.items().stream().filter(i -> i.group().equals(categoryCode))
                .map(i -> lowerFirst(i.name()) + " " + money(i.price()) + (i.note() != null ? " (" + i.note() + ")" : ""))
                .toList();
        return parts.isEmpty() ? "nhóm này chưa có gói giá riêng, bạn đặt ngân sách khi đăng yêu cầu."
                : String.join(", ", parts) + ". Thiết bị khác trong nhóm chưa có gói giá riêng, bạn đặt ngân sách khi đăng yêu cầu.";
    }

    /** Bảng đầy đủ theo nhóm: dùng cho câu "tất cả dịch vụ". */
    public static String serviceList() {
        StringBuilder sb = new StringBuilder();
        int n = 1;
        for (Category c : current.categories()) {
            sb.append("**").append(n++).append(". ").append(c.name()).append("**\n");
            List<Item> items = current.items().stream().filter(i -> i.group().equals(c.code())).toList();
            if (items.isEmpty()) {
                sb.append("- ").append(c.description() != null ? c.description() : "Thiết bị trong nhóm")
                        .append(": chưa có gói giá riêng, bạn đặt ngân sách khi đăng yêu cầu\n");
            }
            for (Item i : items) {
                sb.append("- ").append(i.name()).append(": ").append(money(i.price())).append(" · ")
                        .append(i.note() != null ? i.note() : "~" + i.minutes() + " phút").append('\n');
            }
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /** Bảng giá gọn, mỗi gói một dòng. */
    public static String priceList() {
        StringBuilder sb = new StringBuilder();
        for (Item i : current.items()) {
            sb.append("- ").append(i.name()).append(": ").append(money(i.price())).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /** Khối dữ liệu đưa vào prompt của AI: dữ liệu thật mới nhất, ưu tiên hơn tài liệu tĩnh. */
    public static String promptBlock() {
        StringBuilder sb = new StringBuilder("DỮ LIỆU DỊCH VỤ HIỆN TẠI TỪ HỆ THỐNG (mới nhất, ƯU TIÊN hơn kho tri thức nếu khác nhau):\n");
        sb.append("- ").append(current.categories().size()).append(" danh mục: ").append(categoryNames()).append('\n');
        for (Category c : current.categories()) {
            List<String> parts = current.items().stream().filter(i -> i.group().equals(c.code()))
                    .map(i -> i.name() + " " + money(i.price())).toList();
            sb.append("  • ").append(c.name()).append(c.description() != null ? " (" + c.description() + ")" : "").append(": ")
                    .append(parts.isEmpty() ? "chưa có gói giá riêng" : String.join("; ", parts)).append('\n');
        }
        sb.append("- Khu vực đang phục vụ: ").append(areaList()).append(". Khu vực khác: chưa phục vụ.\n");
        sb.append("- Thiết bị không thuộc các danh mục trên: FixLink chưa hỗ trợ.");
        return sb.toString();
    }

    private static final Pattern PRICE_TOKEN = Pattern.compile("\\{\\{PRICE:([A-Z0-9_]+)}}");

    /** Thay các thẻ {{...}} trong câu trả lời soạn sẵn bằng dữ liệu hiện tại. */
    public static String resolve(String text) {
        if (text == null || !text.contains("{{")) {
            return text;
        }
        String out = text
                .replace("{{CATEGORY_COUNT}}", String.valueOf(current.categories().size()))
                .replace("{{CATEGORY_LIST}}", categoryList())
                .replace("{{CATEGORY_NAMES}}", categoryNames())
                .replace("{{AREA_LIST}}", areaList())
                .replace("{{MIN_PRICE}}", money(minPrice()))
                .replace("{{MAX_PRICE}}", money(maxPrice()))
                .replace("{{SERVICE_LIST}}", serviceList())
                .replace("{{PRICE_LIST}}", priceList());
        Matcher m = PRICE_TOKEN.matcher(out);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String price = item(m.group(1)).map(i -> money(i.price())).orElse("giá do thợ báo");
            m.appendReplacement(sb, Matcher.quoteReplacement(price));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String lowerFirst(String s) {
        if (s.isEmpty() || s.length() > 1 && Character.isUpperCase(s.charAt(1))) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    /** Mã các danh mục đang hoạt động. */
    public static List<String> categoryCodes() {
        return current.categories().stream().map(Category::code).toList();
    }

    /** Có dữ liệu database chưa (để bộ nhận diện thiết bị biết có lọc danh mục đã tắt hay không). */
    public static boolean fromDatabase() {
        return current.fromDatabase();
    }

    /** Danh mục kèm mô tả, dùng để nhận diện thiết bị thuộc danh mục mới thêm. */
    public static Map<String, List<String>> categoryDeviceWords() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        for (Category c : current.categories()) {
            List<String> words = new ArrayList<>();
            String text = (c.description() == null ? "" : c.description()) + "," + c.name();
            // Tách theo dấu câu TRƯỚC khi chuẩn hóa (chuẩn hóa bỏ luôn dấu phẩy)
            for (String rawPart : Arrays.asList(text.split("[,/&;()]"))) {
                String p = SymptomClassifier.normalize(rawPart).trim()
                        .replaceFirst("^(sua chua|sua|chuyen sua chua|chuyen|khac phuc|lap dat|lap|bao tri|bao duong) ", "");
                if (p.length() >= 2 && p.split(" ").length <= 4) {
                    words.add(p);
                }
            }
            out.put(c.code(), words);
        }
        return out;
    }
}
