package com.fixlink.chatbot.domain.service;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Các gói dịch vụ FixLink (dữ liệu mới nhất từ database qua LiveCatalog) và câu trả lời cho câu hỏi về giá, thời gian các gói:
 * rẻ nhất, đắt nhất, nhanh nhất, lâu nhất, dưới/trên một mức giá, các gói của một nhóm, giá một gói cụ thể.
 * Nhận câu đã chuẩn hóa (không dấu, chữ thường) có khoảng trắng hai đầu.
 */
public final class ServiceCatalog {

    public record Item(String code, String group, String name, long price, int minutes, String note, List<String> keywords) { }

    /** Gói dịch vụ hiện tại (đọc từ database qua {@link LiveCatalog}). */
    private static List<Item> items() {
        return LiveCatalog.items();
    }

    private static String groupName(String code) {
        return LiveCatalog.categoryName(code);
    }

    /** Câu hỏi về đơn, cọc, báo giá của thợ, khuyến mãi... không phải câu hỏi về bảng giá gói dịch vụ. */
    private static final Pattern OTHER_TOPIC = Pattern.compile(" (coc|dat coc|giam gia|khuyen mai|voucher|ma giam|uu dai|"
            + "bao gia|hoan tien|phat sinh|bao hanh|phi nen tang|hoa hong|don cua toi|don hang|ma don|req|lua dao|"
            + "thanh toan|nghiem thu|luong|thu nhap|kiem|lam tho|dang ky|tham gia|toi la tho|nhan viec|nhan duoc viec|tuyen) ");
    private static final Pattern ASKS_SERVICE = Pattern.compile(" (dich vu|goi|cai nao|nao|gi|o dau|loai|sua) ");
    private static final Pattern NAMES_SERVICE = Pattern.compile(" (dich vu|goi) ");

    private static final Pattern CHEAP = Pattern.compile(
            " (gia re|re nhat|re nhe|re tien|gia thap|thap nhat|gia mem|binh dan|it tien|tiet kiem nhat|re re thoi|dich vu re re|re thoi|re mot chut) ");
    private static final Pattern EXPENSIVE = Pattern.compile(
            " (gia dat|dat nhat|dat tien|dat do|gia mac|mac nhat|mac tien|gia cao|cao nhat|nhieu tien nhat) ");
    private static final Pattern FAST = Pattern.compile(" (nhanh nhat|lam nhanh|it thoi gian|nhanh gon|ngan nhat|le nhat|nhanh le) ");
    private static final Pattern SLOW = Pattern.compile(" (lau nhat|nhieu thoi gian nhat|ton thoi gian nhat|mat nhieu thoi gian) ");
    private static final Pattern ADVICE = Pattern.compile(" (tot nhat|phu hop nhat|nen chon|nen dung|nen dat|pho bien|"
            + "nhieu nguoi|hot nhat|ban chay|ua chuong|noi bat) ");
    /** "giá" nhưng không phải "gia đình", "gia dụng"; "bao nhiêu" đứng cuối câu ("sửa máy lạnh bao nhiêu"). */
    private static final Pattern PRICE_QUESTION = Pattern.compile(
            " (gia(?! dinh| dung)|bao nhieu tien|het bao nhieu|ton bao nhieu|mat bao nhieu|chi phi|tien cong|tien sua|nhieu tien"
                    + "|ton khoang|ton nhieu|dat khong|mac khong|het nhieu|bao tien) "
                    + "|bao nhieu (vay |a |ha |nhi )?$");
    private static final Pattern LIST_QUESTION = Pattern.compile(" (nao|gi|nhung|gom|co cac|liet ke|cac goi|cac dich vu) ");

    private static final Pattern AMOUNT = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(k|nghin|ngan|tram|trieu|tr|cu|d|dong|vnd)?(?![a-z0-9])");
    private static final Pattern BELOW = Pattern.compile(" (duoi|it hon|khong qua|toi da|re hon|nho hon|thap hon|chua toi|khong vuot qua) ");
    private static final Pattern ABOVE = Pattern.compile(" (tren|cao hon|lon hon|dat hon|tro len|hon) ");
    /** "có 150k sửa được gì", "ngân sách 300k làm được những gì": các gói có giá khởi điểm không quá số tiền đó. */
    private static final Pattern AFFORD = Pattern.compile(
            " ((sua|lam|dung) duoc (nhung )?((dich vu|goi|cai|viec) )?(gi|nao)|duoc (nhung )?gi|du (sua|tien|lam)) ");
    private static final Pattern ITEM_FILLER = Pattern.compile(
            " ((\\d{1,2}|hai|ba|bon|nam)( cai| chiec| bo)?|cai|chiec)(?= (may|tu|bep|lo|cua|voi|bon|o) )");
    /** Số lượng thiết bị: "2 cái", "3 máy", "hai bộ". */
    private static final Pattern QUANTITY = Pattern.compile(" (\\d{1,2}|hai|ba|bon|nam) (cai|may|bo|chiec|phong|cua) ");

    private static final String PRICE_NOTE = "Đây là giá khởi điểm tham khảo; giá thực tế do thợ báo giá và **không vượt ngân sách bạn đặt**.";
    private static final String TIME_NOTE = "Thời gian là ước tính cho một lần xử lý, thực tế tùy tình trạng thiết bị.";

    private ServiceCatalog() {
    }

    public static Optional<String> answer(String padded) {
        if (padded == null || padded.isBlank() || OTHER_TOPIC.matcher(padded).find()) {
            return Optional.empty();
        }
        List<Item> scope = scopeFor(padded);
        boolean narrowed = scope.size() < items().size();
        String where = narrowed ? scopeLabel(scope) : "trên FixLink";
        boolean asksService = ASKS_SERVICE.matcher(padded).find() || narrowed;

        Optional<String> range = priceRange(padded, scope, where);
        if (range.isPresent()) {
            return range;
        }
        boolean cheap = CHEAP.matcher(padded).find();
        if (asksService && (cheap || EXPENSIVE.matcher(padded).find())) {
            return Optional.of(scope.size() == 1 ? listing(scope, padded) : byPrice(scope, cheap, where));
        }
        boolean fast = FAST.matcher(padded).find();
        // "thợ đến nhanh nhất bao lâu" hỏi thời gian thợ có mặt, không phải thời gian xử lý của gói
        if (asksService && !padded.contains(" tho ") && (fast || SLOW.matcher(padded).find())) {
            return Optional.of(scope.size() == 1 ? listing(scope, padded) : byTime(scope, fast, where));
        }
        if (NAMES_SERVICE.matcher(padded).find() && ADVICE.matcher(padded).find() && !narrowed) {
            return Optional.of(advice());
        }
        // "máy lạnh bị chảy nước, gợi ý dịch vụ": khách đã nêu thiết bị/sự cố thì gợi ý đúng gói theo bảng triệu chứng
        if (narrowed && SUGGEST.matcher(padded).find() && SymptomClassifier.classify(padded.strip()).isPresent()) {
            // Triệu chứng ngoài bảng ("đèn nháy", "báo lỗi"): không liệt kê cả nhóm, để AI chẩn đoán
            return suggestion(padded, scope);
        }
        if (narrowed && (PRICE_QUESTION.matcher(padded).find() || (scope.size() == 1 && padded.contains(" bao lau "))
                || (NAMES_SERVICE.matcher(padded).find() && LIST_QUESTION.matcher(padded).find()))) {
            return Optional.of(listing(scope, padded));
        }
        return Optional.empty();
    }

    /** Xin gợi ý/tư vấn gói cho sự cố vừa kể. */
    private static final Pattern SUGGEST = Pattern.compile(" (goi y|tu van|nen dung|nen chon|nen dat|can dich vu|dich vu nao|"
            + "dich vu gi|goi nao|dich vu phu hop|goi phu hop|sua the nao|nen sua|chon dich vu|chon goi) ");
    /** Máy lạnh không mát/hết ga → nạp ga; chảy nước, có mùi, bẩn → vệ sinh (theo bảng triệu chứng FixLink). */
    private static final Pattern AC_GAS = Pattern.compile(" (khong mat|khong lanh|gio nong|thoi nong|het ga|thieu ga|xi ga|nap ga) ");
    private static final Pattern AC_CLEAN = Pattern.compile(" (chay nuoc|ri nuoc|ro nuoc|nho nuoc|mui hoi|co mui|bam bui|ve sinh) ");

    /**
     * Chỉ gợi ý gói cố định khi triệu chứng có trong bảng triệu chứng FixLink (máy lạnh chảy nước/có mùi → vệ sinh,
     * không mát/hết ga → nạp ga; thiết bị có đúng một gói). Triệu chứng ngoài bảng (đèn nháy, báo lỗi, tự tắt...) để AI chẩn đoán.
     */
    private static Optional<String> suggestion(String padded, List<Item> scope) {
        List<Item> picks;
        String reason = null;
        boolean airConditioner = padded.contains(" may lanh ") || padded.contains(" dieu hoa ");
        Optional<Item> cleanItem = LiveCatalog.item("VE_SINH_MAY_LANH");
        Optional<Item> gasItem = LiveCatalog.item("NAP_GA_R32");
        if (airConditioner && cleanItem.isPresent() && gasItem.isPresent()) {
            Item clean = cleanItem.get();
            Item gas = gasItem.get();
            boolean gasSymptom = AC_GAS.matcher(padded).find();
            boolean cleanSymptom = AC_CLEAN.matcher(padded).find();
            if (cleanSymptom && !gasSymptom) {
                picks = List.of(clean, gas);
                reason = "Máy lạnh **chảy nước hoặc có mùi** thường do dàn lạnh bẩn, tắc đường thoát nước, nên gói phù hợp nhất là "
                        + "**Vệ sinh máy lạnh**. Nếu thợ kiểm tra thấy thiếu ga thì mới cần **Nạp ga**.";
            } else if (gasSymptom && !cleanSymptom) {
                picks = List.of(gas, clean);
                reason = "Máy lạnh **không mát, thổi gió nóng** thường do thiếu ga hoặc dàn bẩn: thợ kiểm tra rồi **nạp ga** hoặc "
                        + "**vệ sinh** tùy nguyên nhân.";
            } else if (gasSymptom) {
                picks = List.of(clean, gas);
            } else {
                return Optional.empty();
            }
        } else if (scope.size() == 1) {
            picks = scope;
        } else {
            return Optional.empty();
        }
        StringBuilder sb = new StringBuilder("**Gói dịch vụ gợi ý cho sự cố của bạn** (nhóm ")
                .append(groupName(picks.get(0).group())).append("):\n");
        picks.forEach(i -> sb.append(line(i)).append('\n'));
        sb.append('\n');
        if (reason != null) {
            sb.append(reason).append("\n\n");
        }
        sb.append(PRICE_NOTE).append(" Nếu khi khảo sát phát hiện hư hỏng khác, thợ gửi chi phí phát sinh để bạn duyệt trước khi sửa. ")
                .append("Bạn cho mình biết **quận** và **khung giờ mong muốn** để mình hướng dẫn đăng yêu cầu nhé.");
        return Optional.of(sb.toString());
    }

    /** Các gói câu hỏi nhắc tới: gói cụ thể ("vệ sinh máy lạnh"), nhóm ("điện lạnh") hoặc thiết bị; không nhắc thì cả 11 gói. */
    private static List<Item> scopeFor(String padded) {
        // "vệ sinh 3 máy lạnh", "sửa cái tủ lạnh": bỏ số lượng và từ chỉ loại trước khi so tên gói
        String text = ITEM_FILLER.matcher(padded).replaceAll(" ").replaceAll(" +", " ");
        List<Item> items = items().stream()
                .filter(i -> i.keywords().stream().anyMatch(k -> text.contains(" " + k + " ") || allWords(text, k)))
                .toList();
        if (!items.isEmpty()) {
            return items;
        }
        String group = null;
        if (padded.contains(" dien lanh ")) {
            group = "DIEN_LANH";
        } else if (padded.contains(" dien nuoc ")) {
            group = "DIEN_NUOC";
        } else if (padded.contains(" gia dung ")) {
            group = "DO_GIA_DUNG";
        } else if (padded.contains(" khoa ") || padded.contains(" cua cuon ")) {
            group = "KHOA_CUA";
        } else {
            group = SymptomClassifier.classify(padded.strip()).map(SymptomClassifier.CategoryMatch::code).orElse(null);
        }
        if (group == null) {
            return items();
        }
        String code = group;
        return items().stream().filter(i -> i.group().equals(code)).toList();
    }

    /** Tên gói dài ("vệ sinh máy lạnh") khớp cả khi khách đảo thứ tự: "máy lạnh vệ sinh hết nhiêu". */
    private static boolean allWords(String text, String keyword) {
        String[] words = keyword.split(" ");
        return words.length >= 4 && List.of(words).stream().allMatch(w -> text.contains(" " + w + " "));
    }

    private static String scopeLabel(List<Item> scope) {
        String group = scope.get(0).group();
        boolean wholeGroup = scope.equals(items().stream().filter(i -> i.group().equals(group)).toList());
        return wholeGroup ?"nhóm **" + groupName(group) + "**" : "các gói bạn hỏi";
    }

    private static String byPrice(List<Item> scope, boolean cheapest, String where) {
        Comparator<Item> order = Comparator.comparingLong(Item::price);
        List<Item> sorted = scope.stream().sorted(cheapest ? order : order.reversed()).toList();
        StringBuilder sb = new StringBuilder();
        if (scope.size() == items().size()) {
            sb.append(cheapest ? "**Các gói có giá khởi điểm thấp nhất** " : "**Các gói có giá khởi điểm cao nhất** ")
                    .append(where).append(" (giá tham khảo · thời gian ước tính):\n\n");
            Map<Long, List<Item>> tiers = tiers(sorted, Item::price);
            int shown = 0;
            for (Map.Entry<Long, List<Item>> tier : tiers.entrySet()) {
                if (shown++ == 3) {
                    break;
                }
                sb.append("**").append(money(tier.getKey())).append("**\n");
                tier.getValue().forEach(i -> sb.append("- ").append(i.name()).append(" · ").append(duration(i)).append('\n'));
                sb.append('\n');
            }
            List<Item> rest = sorted.stream().skip(tiers.values().stream().limit(3).mapToLong(List::size).sum()).toList();
            if (!rest.isEmpty()) {
                sb.append("Các gói còn lại có giá từ ").append(money(rest.stream().mapToLong(Item::price).min().orElse(0)))
                        .append(" đến ").append(money(rest.stream().mapToLong(Item::price).max().orElse(0))).append(".\n\n");
            }
        } else {
            Item top = sorted.get(0);
            sb.append("Trong ").append(where).append(", gói có giá khởi điểm ").append(cheapest ? "thấp nhất" : "cao nhất")
                    .append(" là **").append(top.name()).append("**: ").append(money(top.price())).append(" · ")
                    .append(duration(top)).append(".\n\n");
            if (sorted.size() > 1) {
                sb.append("Các gói ").append(cheapest ? "(giá tăng dần)" : "(giá giảm dần)").append(":\n");
                sorted.forEach(i -> sb.append(line(i)).append('\n'));
                sb.append('\n');
            }
        }
        sb.append(PRICE_NOTE).append(cheapest
                ? " Để tiết kiệm, bạn đặt ngân sách phù hợp rồi so sánh các báo giá và tự chọn thợ."
                : " Giá cao hơn thường do công việc phức tạp, cần thiết bị đo chuyên dụng hoặc thời gian xử lý dài hơn.");
        sb.append(" Bạn đang cần sửa thiết bị nào?");
        return sb.toString();
    }

    private static String byTime(List<Item> scope, boolean fastest, String where) {
        Comparator<Item> order = Comparator.comparingInt(Item::minutes);
        List<Item> sorted = scope.stream().sorted(fastest ? order : order.reversed()).toList();
        StringBuilder sb = new StringBuilder();
        sb.append(fastest ? "**Các gói có thời gian xử lý ngắn nhất** " : "**Các gói có thời gian xử lý lâu nhất** ")
                .append(where).append(" (thời gian ước tính · giá khởi điểm):\n\n");
        int shown = 0;
        for (Map.Entry<Long, List<Item>> tier : tiers(sorted, i -> (long) i.minutes()).entrySet()) {
            if (shown++ == 3) {
                break;
            }
            sb.append("**~").append(tier.getKey()).append(" phút**\n");
            tier.getValue().forEach(i -> sb.append("- ").append(i.name()).append(": ").append(money(i.price()))
                    .append(i.note() != null ? " · " + i.note() : "").append('\n'));
            sb.append('\n');
        }
        sb.append(TIME_NOTE).append(" Bạn đang cần sửa thiết bị nào?");
        return sb.toString();
    }

    private static Optional<String> priceRange(String padded, List<Item> scope, String where) {
        boolean afford = AFFORD.matcher(padded).find();
        boolean below = BELOW.matcher(padded).find() || afford;
        boolean above = ABOVE.matcher(padded).find();
        Matcher m = AMOUNT.matcher(padded);
        List<Long> amounts = new ArrayList<>();
        while (m.find()) {
            String unit = m.group(2);
            String before = padded.substring(0, m.start());
            if (before.matches(".*(?:quan|q)\\s*$")) {
                continue;
            }
            double number = Double.parseDouble(m.group(1).replace(',', '.'));
            long value;
            if (unit == null || unit.equals("d") || unit.equals("dong") || unit.equals("vnd")) {
                value = Math.round(number);
            } else if (unit.equals("tram")) {
                value = Math.round(number * 100_000); // "2 trăm" (nghìn) = 200.000 đ
            } else if (unit.equals("trieu") || unit.equals("tr") || unit.equals("cu")) {
                value = Math.round(number * 1_000_000);
            } else {
                value = Math.round(number * 1_000);
            }
            if (value >= 10_000) {
                amounts.add(value);
            }
        }
        boolean between = amounts.size() >= 2 && padded.matches(".* (tu|khoang) \\d.*");
        if (amounts.isEmpty() || (!below && !above && !between) || !ASKS_SERVICE.matcher(padded).find()) {
            return Optional.empty();
        }
        long low;
        long high;
        String label;
        if (between) {
            low = Math.min(amounts.get(0), amounts.get(1));
            high = Math.max(amounts.get(0), amounts.get(1));
            label = "từ " + money(low) + " đến " + money(high);
        } else if (afford) {
            low = 0;
            high = amounts.get(0);
            label = "không quá " + money(amounts.get(0));
        } else if (below) {
            low = 0;
            high = amounts.get(0) - 1;
            label = "dưới " + money(amounts.get(0));
        } else {
            low = amounts.get(0) + 1;
            high = Long.MAX_VALUE;
            label = "trên " + money(amounts.get(0));
        }
        List<Item> hits = scope.stream().filter(i -> i.price() >= low && i.price() <= high)
                .sorted(Comparator.comparingLong(Item::price)).toList();
        StringBuilder sb = new StringBuilder();
        if (hits.isEmpty()) {
            Item nearest = scope.stream().min(Comparator.comparingLong(i -> Math.min(Math.abs(i.price() - low),
                    Math.abs(i.price() - Math.min(high, 10_000_000L))))).orElse(items().get(0));
            sb.append("Hiện ").append(where).append(" **chưa có gói nào có giá khởi điểm ").append(label).append("**. ")
                    .append("Gói gần nhất là ").append(nearest.name()).append(": ").append(money(nearest.price())).append(".\n\n");
        } else {
            sb.append("**Các gói có giá khởi điểm ").append(label).append("** ").append(where).append(":\n");
            hits.forEach(i -> sb.append(line(i)).append('\n'));
            sb.append('\n');
        }
        sb.append(PRICE_NOTE).append(" Khi đăng yêu cầu, bạn có thể đặt ngân sách đúng mức mong muốn. Bạn đang cần sửa thiết bị nào?");
        return Optional.of(sb.toString());
    }

    private static String listing(List<Item> scope, String padded) {
        StringBuilder sb = new StringBuilder();
        if (scope.size() == 1) {
            Item i = scope.get(0);
            sb.append("**").append(i.name()).append("** (nhóm ").append(groupName(i.group())).append("): giá khởi điểm **")
                    .append(money(i.price())).append("** · ").append(duration(i)).append(".\n\n");
            int qty = quantity(padded);
            if (qty > 1) {
                sb.append("Với **").append(qty).append("** thiết bị: khoảng **").append(money(i.price() * qty))
                        .append("** theo giá khởi điểm (tham khảo; thợ báo giá thực tế cho cả đơn).\n\n");
            }
        } else {
            String label = scopeLabel(scope);
            sb.append(label.startsWith("nhóm") ? "**Các gói của " + label.replace("**", "") + "**" : "**Các gói phù hợp với câu hỏi của bạn**")
                    .append(" (giá khởi điểm · thời gian ước tính):\n");
            scope.stream().sorted(Comparator.comparingLong(Item::price)).forEach(i -> sb.append(line(i)).append('\n'));
            if (label.startsWith("nhóm")) {
                sb.append("- Thiết bị khác trong nhóm này: chưa có gói giá riêng, bạn đặt ngân sách khi đăng yêu cầu\n");
            }
            sb.append('\n');
        }
        sb.append(PRICE_NOTE).append(" Bạn cho mình biết thêm tình trạng thiết bị để mình hướng dẫn đăng yêu cầu nhé.");
        return sb.toString();
    }

    private static String advice() {
        return LiveCatalog.resolve("""
                FixLink **không xếp hạng dịch vụ nào "tốt nhất"** và hiện chưa công bố thống kê dịch vụ được dùng nhiều nhất: \
                gói phù hợp là gói đúng với thiết bị và sự cố của bạn. Ví dụ:
                - Máy lạnh chảy nước, có mùi, lâu chưa vệ sinh → Vệ sinh máy lạnh ({{PRICE:VE_SINH_MAY_LANH}})
                - Máy lạnh chạy nhưng không mát → Nạp ga máy lạnh ({{PRICE:NAP_GA_R32}}), thợ sẽ kiểm tra trước
                - Ống nước rò rỉ, bồn rửa nghẹt → Thông nghẹt & xử lý rò rỉ ({{PRICE:SUA_ONG_NUOC}})
                - Nhảy CB, chập điện → Dò tìm chập điện & nhảy CB ({{PRICE:SUA_CHAP_DIEN}})
                - Bị khóa ngoài cửa → Mở khóa cấp tốc 24/7 ({{PRICE:MO_KHOA_CAP_TOC}})

                Bạn mô tả thiết bị và hiện tượng, mình gợi ý đúng gói và hướng dẫn đăng yêu cầu nhé. Thợ đã xác minh danh tính (KYC) \
                sẽ gửi báo giá, bạn so sánh đánh giá, giá và tự chọn thợ.""");
    }

    private static int quantity(String padded) {
        Matcher m = QUANTITY.matcher(padded);
        if (!m.find()) {
            return 1;
        }
        int n = switch (m.group(1)) {
            case "hai" -> 2;
            case "ba" -> 3;
            case "bon" -> 4;
            case "nam" -> 5;
            default -> Integer.parseInt(m.group(1));
        };
        return n >= 1 && n <= 20 ? n : 1;
    }

    private static <K> Map<K, List<Item>> tiers(List<Item> sorted, Function<Item, K> key) {
        Map<K, List<Item>> tiers = new LinkedHashMap<>();
        sorted.forEach(i -> tiers.computeIfAbsent(key.apply(i), k -> new ArrayList<>()).add(i));
        return tiers;
    }

    private static String line(Item i) {
        return "- " + i.name() + ": " + money(i.price()) + " · " + duration(i);
    }

    private static String duration(Item i) {
        return i.note() != null ? i.note() : "~" + i.minutes() + " phút";
    }

    public static String money(long amount) {
        return NumberFormat.getIntegerInstance(Locale.GERMANY).format(amount) + " đ";
    }
}
