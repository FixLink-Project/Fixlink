package com.fixlink.chatbot.adapter.out.gemini;

import com.fixlink.chatbot.application.port.out.LlmChatPort.ChatOrchestrationResult;
import com.fixlink.chatbot.domain.service.SymptomClassifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Bộ trả lời dự phòng chạy cục bộ khi không gọi được Gemini (thiếu API key, hết quota, mất mạng).
 *
 * <p>Thứ tự ưu tiên: (1) câu trả lời soạn sẵn cho các ý định phổ biến nhất, đã đối chiếu với nghiệp
 * vụ trong mã nguồn; (2) trích đúng MỘT mục tri thức khớp nhất với câu hỏi; (3) đề nghị tạo phiếu hỗ trợ.
 * Không bao giờ in lại system prompt hay ghép bừa nhiều đoạn tài liệu.
 */
@Component
public class ExtractiveFallbackSynthesizer {

    private static final String MODEL_INTENT = "local-faq";
    private static final String MODEL_EXTRACT = "local-synthesizer";
    private static final String MODEL_DEFAULT = "local-fallback";

    private static final Map<String, String> LINK_POST = Map.of("type", "link", "label", "Đăng yêu cầu", "url", "/dang-yeu-cau");
    private static final Map<String, String> LINK_MY = Map.of("type", "link", "label", "Yêu cầu của tôi", "url", "/yeu-cau-cua-toi");
    private static final Map<String, String> LINK_LOGIN = Map.of("type", "link", "label", "Đăng nhập", "url", "/dang-nhap");
    private static final Map<String, String> LINK_TECH_REG = Map.of("type", "link", "label", "Đăng ký làm thợ", "url", "/dang-ky-tho");
    private static final Map<String, String> LINK_FORGOT = Map.of("type", "link", "label", "Quên mật khẩu", "url", "/quen-mat-khau");
    private static final Map<String, String> LINK_REGISTER = Map.of("type", "link", "label", "Đăng ký tài khoản", "url", "/dang-ky");
    private static final Map<String, String> LINK_TECH_PROFILE = Map.of("type", "link", "label", "Hồ sơ thợ", "url", "/tho/ho-so");

    /** Giá tham khảo khởi điểm theo dữ liệu gói dịch vụ (V2__seed_master_data.sql). */
    private static final Map<String, String> REFERENCE_PRICES = Map.of(
            "DIEN_LANH", "vệ sinh máy lạnh 150.000 đ, nạp ga máy lạnh 250.000 đ, sửa tủ lạnh 200.000 đ, sửa máy giặt 250.000 đ.",
            "DIEN_NUOC", "thông nghẹt/rò rỉ ống nước 180.000 đ, lắp thiết bị vệ sinh 200.000 đ, dò chập điện/nhảy CB 300.000 đ.",
            "DO_GIA_DUNG", "sửa bếp từ 200.000 đ, sửa lò vi sóng 180.000 đ (thiết bị khác chưa có gói giá riêng).",
            "KHOA_CUA", "mở khóa cấp tốc 150.000 đ, sửa cửa cuốn 350.000 đ.");

    private static final Pattern BROKEN_HINT = Pattern.compile(
            " (hong|hu|bi loi|khong chay|khong hoat dong|can sua|sua giup|sua dum) ");

    private record Intent(List<String> keywords, String reply, List<String> suggestions,
                          List<Map<String, String>> actions, boolean needsHuman) { }

    /** Thứ tự quan trọng: ý định an toàn khẩn cấp luôn được xét trước. */
    private static final List<Intent> INTENTS = List.of(
            new Intent(List.of("chay khet", "mui khet", "boc khoi", "boc chay", "ro gas", "ro ga", "mui gas", "xet lua",
                    "toe lua", "dien giat", "giat dien", "phat no", "ngap nuoc"),
                    """
                    ⚠️ **Đây có thể là tình huống nguy hiểm. Hãy làm ngay:**
                    1. Ngắt cầu dao/aptomat tổng của nhà.
                    2. Khóa van bình gas, mở cửa cho thoáng, **không** bật/tắt công tắc điện hay dùng lửa.
                    3. Đưa người già, trẻ nhỏ ra khỏi khu vực nguy hiểm.
                    4. Khẩn cấp gọi **114** (cứu hỏa – cứu nạn) hoặc **115** (cấp cứu).

                    Tuyệt đối không tự sửa. Khi đã an toàn, bạn có thể đăng yêu cầu để thợ FixLink có chuyên môn đến xử lý.""",
                    List.of("Đăng yêu cầu sửa chữa", "Chọn danh mục nào cho sự cố điện?"), List.of(LINK_POST), false),

            new Intent(List.of("tho chua den", "tho khong den", "tho den tre", "tho den muon", "tho tre hen", "cho tho lau",
                    "tho bung", "tho khong nghe may", "khong lien lac duoc tho"),
                    """
                    Mình hiểu việc phải chờ thợ rất khó chịu. Bạn xử lý theo các bước sau nhé:
                    1. Vào **Yêu cầu của tôi** (/yeu-cau-cua-toi), mở đơn để xem lịch hẹn và thông tin thợ, rồi liên hệ thợ xác nhận lại giờ.
                    2. Cần đổi giờ thì **dời lịch hẹn** ngay trong chi tiết đơn (dời được nhiều lần).
                    3. Thợ không đến và đơn **chưa ở trạng thái Đang sửa chữa** → bạn được **hủy đơn** (nhập lý do) rồi đăng lại.
                    4. Cần CSKH xem xét tiền cọc → mình tạo **phiếu hỗ trợ ưu tiên cao** cho bạn. Mức hoàn cọc cụ thể hiện chưa được công bố.""",
                    List.of("Tạo phiếu hỗ trợ cho tôi", "Cách hủy đơn", "Lịch hẹn của tôi"), List.of(LINK_MY), false),

            new Intent(List.of("doi them tien", "doi them", "tinh them tien", "tien mat", "chuyen khoan rieng", "tra ngoai",
                    "tra truc tiep cho tho", "phat sinh", "thay linh kien", "mua linh kien"),
                    """
                    **Bạn không trả tiền mặt hay chuyển khoản riêng cho thợ ngoài hệ thống.** Theo quy định FixLink:
                    1. Mọi chi phí phát sinh (kể cả tiền linh kiện) thợ phải **gửi đề xuất trên hệ thống**; đơn chuyển sang *Chờ duyệt chi phí phát sinh*.
                    2. Bạn xem giải trình trong chi tiết đơn: **Đồng ý** thì giá chốt và tiền cọc (30%) được tính lại theo giá mới; **không đồng ý** thì có thể hủy đơn.
                    3. Thợ không được sửa phần phát sinh khi bạn chưa đồng ý.

                    Nếu thợ ép trả ngoài hệ thống, mình có thể tạo **phiếu hỗ trợ khẩn cấp** để CSKH can thiệp.""",
                    List.of("Tạo phiếu hỗ trợ khẩn cấp", "Cách hủy đơn"), List.of(LINK_MY), false),

            new Intent(List.of("doi tho", "chon tho", "tho khac", "khong muon tho nay", "chi dinh tho", "xem danh gia tho"),
                    """
                    FixLink hoạt động theo nguyên tắc **ai nhận trước được trước**, nên **không có tính năng chọn, chỉ định hay đổi thợ**. Mọi thợ nhận việc đều đã được duyệt KYC và đúng chuyên môn, khu vực.

                    Nếu không muốn tiếp tục với thợ hiện tại:
                    1. Trao đổi trực tiếp với thợ trước.
                    2. Đơn **chưa ở trạng thái Đang sửa chữa** → bạn có thể hủy đơn (nhập lý do) và đăng yêu cầu mới.
                    3. Cần CSKH xem xét → mình tạo phiếu hỗ trợ cho bạn.""",
                    List.of("Cách hủy đơn", "Tạo phiếu hỗ trợ"), List.of(LINK_MY), false),

            new Intent(List.of("bao hanh", "lai hong", "sua roi van hong", "sua xong lai", "khong hai long", "lam hong them",
                    "lam hu them", "boi thuong", "den bu", "khieu nai", "thai do"),
                    """
                    Mình rất tiếc về trải nghiệm này. Bạn làm như sau:
                    1. Nếu đơn đang **Chờ nghiệm thu**: **chưa bấm Nghiệm thu** — tiền vẫn được giữ trung gian, hãy yêu cầu thợ kiểm tra lại.
                    2. Ghi lại bằng chứng (ảnh, video, thời điểm) và mã đơn.
                    3. Mình tạo **phiếu hỗ trợ** để CSKH thẩm định (ưu tiên cao với hư hỏng, khẩn cấp với tranh chấp tiền/gian lận).

                    Hệ thống kích hoạt bảo hành khi nghiệm thu, nhưng thời hạn bảo hành và chính sách bồi thường cụ thể hiện **chưa được công bố**.""",
                    List.of("Tạo phiếu hỗ trợ cho tôi", "Đơn của tôi đang ở trạng thái nào?"), List.of(LINK_MY), true),

            new Intent(List.of("sua dien thoai", "dien thoai bi", "dien thoai hong", "iphone", "laptop", "may tinh", "macbook", "xe may", "o to", "xay nha", "son nha",
                    "chong tham", "may in", "wifi"),
                    """
                    Hiện FixLink **chưa hỗ trợ** nhóm thiết bị/dịch vụ này. FixLink chỉ có 4 danh mục:
                    1. **Điện lạnh** – máy lạnh, tủ lạnh, máy giặt.
                    2. **Điện nước** – ống nước, thiết bị vệ sinh, chập điện, nhảy CB.
                    3. **Điện gia dụng** – bếp từ, lò vi sóng, máy hút mùi, nồi chiên, quạt.
                    4. **Khóa & Cửa cuốn**.""",
                    List.of("Bảng giá tham khảo dịch vụ", "FixLink phục vụ khu vực nào?"), List.of(), false),

            new Intent(List.of("hotline", "so dien thoai", "tong dai", "lien he", "goi cho", "email ho tro", "gio lam viec"),
                    """
                    Hiện số tổng đài hotline và khung giờ trực điện thoại chính thức của FixLink **đang được cập nhật**, nên mình chưa thể cung cấp số chính xác.

                    Trợ lý FixLink hỗ trợ **24/7** ngay tại đây. Nếu cần nhân viên CSKH, mình có thể tạo **phiếu hỗ trợ (mã TCK-...)** để nhân viên chủ động liên hệ lại bạn.""",
                    List.of("Tạo phiếu hỗ trợ", "Xem phiếu hỗ trợ của tôi"), List.of(), true),

            new Intent(List.of("dat coc", "tien coc", "coc bao nhieu", "coc", "thanh toan", "giai ngan", "trung gian", "escrow"),
                    """
                    **Chính sách cọc và thanh toán của FixLink:**
                    1. Khi thợ bấm *Nhận việc*, giá chốt = ngân sách bạn đã đặt; **tiền cọc = 30%** giá chốt (làm tròn lên) được giữ ở tài khoản trung gian.
                    2. **70% còn lại** được tất toán khi bạn kiểm tra thiết bị và bấm **Nghiệm thu**; lúc đó hệ thống mới giải ngân cho thợ.
                    3. Nếu bạn duyệt chi phí phát sinh, giá chốt và tiền cọc được tính lại theo giá mới.
                    4. Không giao tiền mặt ngoài hệ thống.

                    Ví dụ: đơn 500.000 đ → cọc 150.000 đ, còn lại 350.000 đ. Tỷ lệ phí nền tảng/hoàn cọc chi tiết hiện đang được cập nhật; cần giải đáp riêng, mình có thể tạo phiếu hỗ trợ.""",
                    List.of("Hủy đơn có mất phí không?", "Thợ báo thêm tiền phát sinh thì sao?"), List.of(LINK_MY), false),

            new Intent(List.of("huy don", "huy yeu cau", "huy viec", "muon huy", "cancel"),
                    """
                    **Quy định hủy yêu cầu sửa chữa:**
                    - Bạn **được hủy** khi đơn đang ở: Bản nháp, Đang tìm thợ (OPEN), Đã có thợ nhận (ASSIGNED), Đang khảo sát (INSPECTING), Chờ duyệt chi phí phát sinh.
                    - **Không thể hủy** khi đơn đã ở: Đang sửa chữa (IN_PROGRESS), Chờ nghiệm thu, Đã hoàn thành hoặc Đã hủy.
                    - Khi hủy cần nhập lý do. Đơn OPEN quá **3 ngày** không có thợ nhận sẽ tự động hủy.
                    - Tranh chấp về tiền cọc khi hủy được Admin thẩm định qua phiếu hỗ trợ.

                    Cách hủy: vào **Yêu cầu của tôi** → mở đơn → bấm *Hủy yêu cầu*.""",
                    List.of("Chính sách tiền cọc", "Tạo phiếu hỗ trợ"), List.of(LINK_MY), false),

            new Intent(List.of("quen mat khau", "dat lai mat khau", "reset mat khau", "lay lai mat khau", "doi mat khau"),
                    """
                    **Lấy lại mật khẩu:**
                    1. Vào trang **Quên mật khẩu** (/quen-mat-khau) và nhập email đã đăng ký.
                    2. Mở email, dùng liên kết/mã đặt lại (có hiệu lực **30 phút**; tối đa 3 lần yêu cầu mỗi giờ).
                    3. Đặt mật khẩu mới: tối thiểu **8 ký tự**, có chữ hoa, chữ thường, chữ số và ký tự đặc biệt (ví dụ `FixLink#2026`).

                    Trợ lý không bao giờ hỏi mật khẩu của bạn.""",
                    List.of("Tài khoản bị khóa 15 phút là sao?", "Quy định mật khẩu"), List.of(LINK_FORGOT), false),

            new Intent(List.of("bi khoa", "tam khoa", "khoa tai khoan", "nhap sai mat khau", "khong dang nhap duoc"),
                    """
                    **Các trường hợp không đăng nhập được:**
                    - Nhập sai mật khẩu **5 lần liên tiếp** → tài khoản tạm khóa **15 phút**, chờ hết thời gian rồi thử lại hoặc dùng *Quên mật khẩu*.
                    - Tài khoản thợ mới đăng ký đang **chờ Admin duyệt** → chưa đăng nhập được cho tới khi được duyệt.
                    - Tài khoản bị Admin **khóa** hoặc **ngừng hoạt động** → cần tạo phiếu hỗ trợ để CSKH kiểm tra.
                    - Lưu ý chọn đúng cổng: khách hàng ở /dang-nhap, thợ ở /dang-nhap-tho.""",
                    List.of("Quên mật khẩu phải làm sao?", "Tạo phiếu hỗ trợ"), List.of(LINK_FORGOT, LINK_LOGIN), false),

            new Intent(List.of("dang ky lam tho", "dang ky tho", "lam tho", "tro thanh tho", "tuyen tho", "kyc", "xac minh"),
                    """
                    **Đăng ký làm thợ FixLink:**
                    1. Vào /dang-ky-tho, điền họ tên, SĐT, email, mật khẩu, số CCCD, địa chỉ, chuyên môn, số năm kinh nghiệm.
                    2. Tải ảnh **mặt trước và mặt sau CCCD** rõ nét, không cắt góc.
                    3. Hồ sơ ở trạng thái **chờ duyệt (PENDING)**; Admin kiểm tra giấy tờ rồi duyệt (**APPROVED**) hoặc từ chối kèm lý do (**REJECTED**).
                    4. Chỉ thợ đã được duyệt mới xem bảng việc và bấm *Nhận việc*; thợ chỉ nhận được việc **đúng chuyên môn và khu vực** đã đăng ký.""",
                    List.of("Thợ nhận việc theo nguyên tắc nào?", "Lỗi JOB_ALREADY_TAKEN là gì?"), List.of(LINK_TECH_REG, LINK_TECH_PROFILE), false),

            new Intent(List.of("dang yeu cau", "tao yeu cau", "dat tho", "goi tho", "tim tho", "dat lich sua"),
                    """
                    **Cách đăng yêu cầu sửa chữa:**
                    1. Đăng nhập tài khoản khách hàng, vào **Đăng yêu cầu** (/dang-yeu-cau).
                    2. Chọn danh mục (Điện lạnh, Điện nước, Điện gia dụng, Khóa & Cửa cuốn), nhập tiêu đề (4–200 ký tự) và mô tả sự cố.
                    3. Tải ảnh/video hiện trường (tối đa **6 tệp**).
                    4. Chọn khu vực, nhập địa chỉ và **ngân sách cố định** (bắt buộc, lớn hơn 0).
                    5. Bấm *Đăng yêu cầu* (hoặc lưu nháp). Đơn mở cho thợ trong **3 ngày**; thợ phù hợp nào bấm *Nhận việc* trước sẽ nhận đơn.""",
                    List.of("Tiền cọc tính thế nào?", "Máy lạnh chảy nước chọn mục nào?"), List.of(LINK_POST), false),

            new Intent(List.of("khu vuc", "quan nao", "o dau", "dia ban", "phuc vu o", "ha noi", "da nang", "tinh thanh"),
                    """
                    FixLink hiện phục vụ tại **TP. Hồ Chí Minh**: Quận 1, Quận 3, Quận 7, Bình Thạnh, Tân Bình, Gò Vấp, Phú Nhuận và TP. Thủ Đức.

                    Các tỉnh/thành khác hiện chưa có thợ hoạt động trên hệ thống.""",
                    List.of("FixLink sửa những gì?", "Cách đăng yêu cầu"), List.of(LINK_POST), false),

            new Intent(List.of("bao nhieu tien", "gia bao nhieu", "chi phi", "bang gia", "gia ve sinh", "gia sua", "phi sua"),
                    """
                    **Giá tham khảo khởi điểm một số gói dịch vụ:**
                    - Vệ sinh máy lạnh treo tường: 150.000 đ · Nạp ga máy lạnh: 250.000 đ
                    - Sửa tủ lạnh không lạnh/kêu to: 200.000 đ · Sửa máy giặt: 250.000 đ
                    - Thông nghẹt, rò rỉ ống nước: 180.000 đ · Dò chập điện, nhảy CB: 300.000 đ
                    - Sửa bếp từ: 200.000 đ · Sửa lò vi sóng: 180.000 đ
                    - Mở khóa cấp tốc: 150.000 đ · Sửa cửa cuốn: 350.000 đ

                    Trên FixLink, **bạn tự đặt ngân sách cố định** khi đăng yêu cầu; nếu thợ khảo sát phát hiện lỗi phát sinh, thợ phải gửi chi phí mới để bạn duyệt trước khi sửa.""",
                    List.of("Tiền cọc tính thế nào?", "Cách đăng yêu cầu"), List.of(LINK_POST), false),

            new Intent(List.of("dau gia", "bao gia", "dau thau", "nhan viec", "ai nhan truoc", "job already taken"),
                    """
                    FixLink **không đấu giá hay so sánh báo giá**. Mô hình là **"ai nhận trước được trước"**:
                    - Khách đăng yêu cầu kèm **ngân sách cố định**.
                    - Thợ đã duyệt KYC, đúng chuyên môn và khu vực, bấm *Nhận việc* trước sẽ nhận đơn (đơn chuyển sang ASSIGNED).
                    - Thợ bấm chậm hơn sẽ nhận lỗi **JOB_ALREADY_TAKEN** – việc đã có người nhận, hãy chọn việc khác.""",
                    List.of("Đăng ký làm thợ cần gì?", "Cách đăng yêu cầu"), List.of(), false),

            new Intent(List.of("dang ky tai khoan", "tao tai khoan", "dang ky", "mat khau"),
                    """
                    **Đăng ký tài khoản khách hàng** tại /dang-ky: nhập tên đăng nhập (3–50 ký tự), mật khẩu, họ tên, số điện thoại Việt Nam và email (SĐT/email chưa được dùng).

                    Mật khẩu khách hàng tối thiểu **6 ký tự** (nên dùng 8+ ký tự gồm chữ hoa, chữ thường, số, ký tự đặc biệt). Tài khoản thợ đăng ký riêng tại /dang-ky-tho với mật khẩu tối thiểu 8 ký tự, có chữ in hoa và ký tự đặc biệt.""",
                    List.of("Cách đăng yêu cầu", "Đăng ký làm thợ cần gì?"), List.of(LINK_REGISTER), false),

            new Intent(List.of("fixlink la gi", "gioi thieu", "fixlink lam gi", "fixlink sua", "dich vu gi", "danh muc"),
                    """
                    **FixLink** là nền tảng kết nối khách hàng có thiết bị gia dụng hư hỏng với **thợ đã xác minh danh tính (KYC)** tại TP.HCM, gồm 4 nhóm dịch vụ:
                    1. **Điện lạnh** – máy lạnh, tủ lạnh, máy giặt.
                    2. **Điện nước** – rò rỉ/nghẹt ống nước, thiết bị vệ sinh, chập điện, nhảy CB.
                    3. **Điện gia dụng** – bếp từ, lò vi sóng, máy hút mùi, nồi chiên, quạt.
                    4. **Khóa & Cửa cuốn** – mở khóa cấp tốc, sửa cửa cuốn.

                    Khách đặt ngân sách cố định, thợ phù hợp nhận việc trước được trước, tiền cọc giữ trung gian và chỉ giải ngân khi khách nghiệm thu.""",
                    List.of("Cách đăng yêu cầu", "Tiền cọc tính thế nào?", "Đăng ký làm thợ"), List.of(LINK_POST), false)
    );

    public ChatOrchestrationResult synthesize(String userMessage, String ragContext) {
        String query = normalize(userMessage != null ? userMessage : "");
        String padded = " " + query + " ";

        if (isGreeting(query)) {
            return new ChatOrchestrationResult(
                    "Xin chào bạn! Mình là **FixLink Assistant**. Mình có thể giúp bạn chọn đúng dịch vụ sửa chữa, "
                            + "hướng dẫn đăng yêu cầu, giải thích chính sách cọc – nghiệm thu, quy trình làm thợ, "
                            + "hoặc tạo phiếu hỗ trợ gặp nhân viên CSKH.",
                    List.of("FixLink là gì?", "Cách đăng yêu cầu sửa chữa", "Tiền cọc tính thế nào?"),
                    List.of(LINK_POST),
                    false,
                    null,
                    MODEL_INTENT
            );
        }

        Optional<Intent> intent = matchIntent(padded);
        if (intent.isPresent()) {
            Intent i = intent.get();
            return new ChatOrchestrationResult(i.reply().strip(), i.suggestions(), i.actions(),
                    i.needsHuman(), null, MODEL_INTENT);
        }

        Optional<SymptomClassifier.CategoryMatch> device = SymptomClassifier.classify(userMessage);
        if (device.isPresent()) {
            SymptomClassifier.CategoryMatch match = device.get();
            return new ChatOrchestrationResult(
                    "Sự cố bạn mô tả thuộc danh mục **" + match.name() + "**.\n\n"
                            + "**Giá tham khảo khởi điểm:** " + REFERENCE_PRICES.getOrDefault(match.code(), "") + "\n\n"
                            + "Trên FixLink bạn tự đặt **ngân sách cố định** khi đăng yêu cầu; nếu thợ khảo sát phát hiện lỗi phát sinh, "
                            + "thợ phải gửi chi phí mới để bạn duyệt trước khi sửa. Bạn cho mình biết thêm **quận** và **khung giờ mong muốn** "
                            + "để mình hướng dẫn đăng yêu cầu nhé.",
                    List.of("Tiền cọc tính thế nào?", "Bao lâu thì có thợ nhận?", "Cách đăng yêu cầu"),
                    List.of(LINK_POST), false, null, MODEL_INTENT);
        }
        if (BROKEN_HINT.matcher(" " + query + " ").find()) {
            return new ChatOrchestrationResult(
                    "Mình sẵn sàng hỗ trợ bạn! Bạn cho mình biết thêm:\n"
                            + "1. **Thiết bị nào** đang hỏng? (máy lạnh, tủ lạnh, máy giặt, vòi nước, ổ cắm, bếp từ, cửa cuốn...)\n"
                            + "2. **Hiện tượng cụ thể** ra sao? (không mát, chảy nước, kêu to, không lên nguồn...)\n\n"
                            + "FixLink hỗ trợ 4 nhóm: Điện lạnh, Điện nước, Điện gia dụng, Khóa & Cửa cuốn.",
                    List.of("Máy lạnh không mát", "Vòi nước bị rò rỉ", "Bếp từ không nóng", "Cửa cuốn bị kẹt"),
                    List.of(), false, null, MODEL_INTENT);
        }

        String bestSection = pickBestSection(query, ragContext);
        if (bestSection != null) {
            return new ChatOrchestrationResult(
                    bestSection,
                    List.of("Cách đăng yêu cầu sửa chữa", "Tiền cọc tính thế nào?", "Gặp nhân viên hỗ trợ"),
                    List.of(LINK_POST, LINK_MY),
                    false,
                    null,
                    MODEL_EXTRACT
            );
        }

        return new ChatOrchestrationResult(
                "Xin lỗi bạn, hệ thống AI đang tạm quá tải nên mình chưa trả lời chính xác được câu hỏi này. "
                        + "Bạn thử hỏi lại sau ít phút, hoặc để mình tạo **phiếu hỗ trợ** để nhân viên CSKH liên hệ giải đáp nhé.",
                List.of("Tạo phiếu hỗ trợ", "FixLink là gì?", "Cách đăng yêu cầu sửa chữa"),
                List.of(Map.of("type", "link", "label", "Trang chủ", "url", "/"), LINK_POST),
                true,
                null,
                MODEL_DEFAULT
        );
    }

    /**
     * Câu trả lời chuẩn (đã đối chiếu nghiệp vụ) cho chủ đề của câu hỏi, nếu có. Orchestrator đưa nội
     * dung này vào prompt để LLM — kể cả model nhỏ — không nói sai các quy định quan trọng.
     */
    public Optional<String> verifiedAnswerFor(String userMessage) {
        String padded = " " + normalize(userMessage != null ? userMessage : "") + " ";
        return matchIntent(padded).map(i -> i.reply().strip());
    }

    private Optional<Intent> matchIntent(String paddedNormalizedQuery) {
        for (Intent intent : INTENTS) {
            for (String kw : intent.keywords()) {
                if (paddedNormalizedQuery.contains(" " + kw + " ")) {
                    return Optional.of(intent);
                }
            }
        }
        return Optional.empty();
    }

    private boolean isGreeting(String query) {
        if (query.length() > 25) {
            return false;
        }
        return query.matches("^(xin chao|chao|hi|hello|hey|alo|chao ban|chao shop|chao ad)( .*)?$");
    }

    /**
     * Chọn một mục ("### ...") trong ngữ cảnh RAG có nhiều từ khóa trùng với câu hỏi nhất.
     * Trả về null khi độ trùng quá thấp để tránh trả lời lạc đề.
     */
    private String pickBestSection(String query, String ragContext) {
        if (ragContext == null || ragContext.isBlank()) {
            return null;
        }
        Set<String> queryTokens = tokens(query);
        if (queryTokens.isEmpty()) {
            return null;
        }

        String best = null;
        double bestScore = 0;
        for (String section : ragContext.split("(?m)^### ")) {
            if (section.isBlank()) {
                continue;
            }
            Set<String> sectionTokens = tokens(normalize(section));
            int overlap = 0;
            for (String t : queryTokens) {
                if (sectionTokens.contains(t)) {
                    overlap++;
                }
            }
            double score = (double) overlap / queryTokens.size();
            if (score > bestScore) {
                bestScore = score;
                best = section;
            }
        }
        if (best == null || bestScore < 0.34) {
            return null;
        }
        return formatSection(best);
    }

    private String formatSection(String section) {
        String[] lines = section.split("\n");
        StringBuilder sb = new StringBuilder();
        String heading = lines[0].replaceAll("\\(Nguồn:[^)]*\\)", "").trim();
        sb.append("**").append(heading).append("**\n");
        int count = 0;
        for (int i = 1; i < lines.length && count < 10; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("---") || line.startsWith("```")
                    || line.startsWith("|---") || line.contains("Trợ lý cần")) {
                continue;
            }
            sb.append(line).append("\n");
            count++;
        }
        return sb.toString().trim();
    }

    private static Set<String> tokens(String normalized) {
        Set<String> stop = Set.of("la", "gi", "thi", "co", "khong", "cua", "toi", "minh", "ban", "the", "nao", "lam",
                "sao", "va", "o", "de", "duoc", "cho", "nhu", "voi", "mot", "cac", "nhung", "neu", "khi", "bi", "ve");
        Set<String> result = new HashSet<>();
        List<String> words = new ArrayList<>(List.of(normalized.split(" ")));
        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i);
            if (w.length() > 1 && !stop.contains(w)) {
                result.add(w);
            }
            if (i + 1 < words.size()) {
                result.add(w + " " + words.get(i + 1)); // bigram giúp phân biệt "mat khau", "dat coc"...
            }
        }
        return result;
    }

    /** Chuẩn hóa không dấu và mở rộng viết tắt ("tl" → "tu lanh", "ko" → "khong"). */
    static String normalize(String text) {
        return SymptomClassifier.normalize(text);
    }
}
