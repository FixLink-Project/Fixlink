package com.fixlink.chatbot.adapter.out.gemini;

import com.fixlink.chatbot.application.port.out.LlmChatPort.ChatOrchestrationResult;
import com.fixlink.chatbot.domain.service.SymptomClassifier;
import java.text.Normalizer;
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

            new Intent(List.of("chon tho roi", "da chon tho", "chon xong tho", "lam gi tiep", "buoc tiep theo", "sau khi chon",
                    "xac nhan lich", "ai xac nhan", "xac nhan lich hen"),
                    """
                    **Sau khi bạn bấm "Chọn thợ này" (đơn ở trạng thái Đã chọn thợ):**
                    1. Thợ **đặt lịch hẹn khảo sát** trên hệ thống → lịch ở trạng thái *Chờ xác nhận*.
                    2. **Chỉ khách hàng** được bấm **"✓ Xác nhận lịch hẹn"** (hoặc từ chối để thợ đặt lịch khác).
                    3. Xác nhận xong, **mã QR đặt cọc 30%** hiện trong chi tiết đơn → bạn chuyển khoản rồi bấm xác nhận → đơn sang **Đã đặt cọc**.
                    4. Thợ đến khảo sát, sửa chữa; có phát sinh thì phải gửi để bạn duyệt trước.
                    5. Thợ báo xong → bạn **thanh toán phần còn lại qua QR**, **chấm 1–5 sao + nhận xét** rồi hoàn tất nghiệm thu.

                    Chưa ưng thợ trước khi sửa, bạn có thể bấm **"Đổi thợ khác"** trong chi tiết đơn.""",
                    List.of("Tiền cọc tính thế nào?", "Đổi thợ được không?", "Thợ chưa đặt lịch thì sao?"), List.of(LINK_MY), false),

            new Intent(List.of("bat dau khao sat", "duoc khao sat", "khi nao khao sat", "chua dat coc", "chua coc",
                    "khong bam duoc khao sat", "den khao sat", "duoc dat coc"),
                    """
                    **Thợ chỉ được bắt đầu khảo sát khi đủ 2 điều kiện:**
                    1. Thợ đã đặt lịch hẹn và **khách đã bấm Xác nhận lịch hẹn**.
                    2. **Khách đã đặt cọc 30% qua mã QR** → đơn ở trạng thái **Đã đặt cọc (DEPOSIT_PAID)**.

                    Khi đó thợ đến theo lịch và chuyển đơn sang *Đang khảo sát*. Nếu chưa đặt cọc, thợ chưa bắt đầu khảo sát được; khách xác nhận lịch và thanh toán cọc trong chi tiết đơn.""",
                    List.of("Tiền cọc tính thế nào?", "Ai xác nhận lịch hẹn?"), List.of(LINK_MY), false),

            new Intent(List.of("mat coc", "mat tien coc", "hoan coc", "huy co mat", "tra lai coc", "lay lai coc", "phat huy",
                    "huy thi coc", "tien coc khi huy"),
                    """
                    **Tiền cọc khi hủy đơn:**
                    - Tiền cọc 30% được FixLink **giữ bảo chứng**, thợ chỉ nhận tiền khi bạn nghiệm thu.
                    - **Tỷ lệ phạt hoặc hoàn cọc khi hủy hiện chưa được công bố chính thức.** Mỗi trường hợp (ví dụ thợ không đến, thợ vi phạm thỏa thuận) do Admin thẩm định qua **phiếu hỗ trợ**.
                    - Nếu chỉ không ưng thợ hiện tại và đơn chưa sửa, bạn có thể **"Đổi thợ khác"**: cọc đã đóng được bảo lưu cho thợ mới, thợ cũ không nhận.

                    Bạn cần xử lý tiền cọc cho đơn cụ thể, mình có thể tạo phiếu hỗ trợ (bạn cho mình mã đơn nhé).""",
                    List.of("Tạo phiếu hỗ trợ", "Đổi thợ được không?", "Quy định hủy đơn"), List.of(LINK_MY), true),

            new Intent(List.of("phat sinh", "chi phi phat sinh", "thay linh kien", "mua linh kien", "bao gia them", "len gia",
                    "tang gia"),
                    """
                    **Chi phí phát sinh trên FixLink:**
                    1. Chỉ phát sinh khi thợ khảo sát thấy hư hỏng ngoài mô tả ban đầu (kể cả tiền linh kiện).
                    2. Thợ phải **gửi đề xuất trên hệ thống** kèm giải trình → đơn sang *Chờ duyệt chi phí phát sinh*.
                    3. Bạn **Đồng ý** thì khoản phát sinh cộng vào **đợt thanh toán cuối**; tiền cọc đã đóng **không thay đổi**.
                    4. **Không đồng ý** thì bấm *Từ chối giá & Đổi thợ khác* hoặc hủy đơn. Thợ không được tự sửa phần phát sinh khi bạn chưa đồng ý.

                    Không trả tiền mặt hay chuyển khoản riêng cho thợ ngoài hệ thống.""",
                    List.of("Có nên đồng ý chi phí phát sinh?", "Đổi thợ được không?"), List.of(LINK_MY), false),

            new Intent(List.of("vuot ngan sach", "cao hon ngan sach", "qua ngan sach", "hon ngan sach", "tran ngan sach"),
                    """
                    **Không.** Báo giá của thợ (tiền công + vật tư) **không được vượt ngân sách** khách đặt khi đăng yêu cầu; hệ thống tự chặn báo giá vượt trần.

                    Nếu khi khảo sát phát hiện hư hỏng thêm, thợ gửi **chi phí phát sinh** để khách duyệt; khoản đó mới được cộng thêm khi khách đồng ý.""",
                    List.of("Chi phí phát sinh xử lý thế nào?", "Nên đặt ngân sách bao nhiêu?"), List.of(), false),

            new Intent(List.of("sua noi dung", "sua don", "chinh sua don", "chinh sua yeu cau", "sua yeu cau", "doi dia chi",
                    "doi thong tin don", "cap nhat don"),
                    """
                    **Sửa nội dung yêu cầu:**
                    - Chỉ sửa được khi đơn là **Bản nháp** hoặc **Đang nhận báo giá mà chưa có báo giá nào**.
                    - Khi đã có báo giá hoặc đã chọn thợ, nội dung đơn bị **khóa** để bảo vệ thỏa thuận giữa hai bên. Cần thay đổi lớn (địa chỉ, thiết bị khác) thì trao đổi với thợ, hoặc hủy đơn (nếu còn được hủy) rồi đăng đơn mới.
                    - Ảnh/video bổ sung được, tổng tối đa **6 tệp**.""",
                    List.of("Quy định hủy đơn", "Cách đăng yêu cầu"), List.of(LINK_MY), false),

            new Intent(List.of("khong ai nhan", "chua co tho", "chua co bao gia", "chua ai bao gia", "khong co ai bao gia",
                    "tu huy", "don het han", "bao lau co tho", "bao lau thi co", "bao lau", "khi nao co tho"),
                    """
                    **Thời gian có thợ:**
                    - Sau khi đăng, đơn **nhận báo giá tối đa 3 ngày**. FixLink **không cam kết** thời gian thợ có mặt chung cho mọi đơn; giờ cụ thể do bạn và thợ thống nhất qua lịch hẹn (riêng gói *Mở khóa cấp tốc 24/7* mô tả thợ có mặt sau 15–30 phút).
                    - Quá **3 ngày** chưa chọn được thợ, đơn **tự động hủy**; bạn có thể đăng lại.

                    **Để có báo giá sớm:** chọn đúng danh mục và khu vực, mô tả rõ hiện tượng, đính kèm ảnh, ghi khung giờ mong muốn và đặt ngân sách hợp lý.""",
                    List.of("Bảng giá tham khảo", "Cách đăng yêu cầu"), List.of(LINK_POST), false),

            new Intent(List.of("danh gia", "cham sao", "nghiem thu", "bat buoc danh gia", "viet nhan xet"),
                    """
                    **Nghiệm thu và đánh giá:**
                    1. Thợ báo hoàn thành → đơn *Chờ nghiệm thu*, hệ thống tạo **mã QR thanh toán phần còn lại** (giá chốt + phát sinh đã duyệt − tiền cọc).
                    2. Bạn kiểm tra, vận hành thử thiết bị rồi **thanh toán phần còn lại**.
                    3. **Bắt buộc chấm 1–5 sao và viết nhận xét** trước khi bấm hoàn tất nghiệm thu; điểm được cộng vào hồ sơ thợ.
                    4. Nghiệm thu xong thợ mới nhận tiền; hệ thống kích hoạt bảo hành điện tử cho đơn.

                    Chưa hài lòng thì đừng nghiệm thu — hãy yêu cầu thợ kiểm tra lại hoặc tạo phiếu hỗ trợ.""",
                    List.of("Thiết bị vẫn lỗi thì sao?", "Thanh toán phần còn lại thế nào?"), List.of(LINK_MY), false),

            new Intent(List.of("momo", "zalopay", "vnpay", "the tin dung", "the ngan hang", "tra gop", "hoa don", "hoa don vat", "xuat vat", "thue vat",
                    "thanh toan bang", "thanh toan the nao", "thanh toan cach nao", "ma qr", "quet qr", "chuyen khoan", "vi dien tu"),
                    """
                    **Thanh toán trên FixLink bằng mã QR chuyển khoản ngân hàng (VietQR)** hiện trong chi tiết đơn, gồm 2 đợt:
                    1. **Đặt cọc 30% giá chốt** – QR hiện sau khi bạn xác nhận lịch hẹn.
                    2. **Phần còn lại** = giá chốt + phát sinh đã duyệt − tiền cọc – QR hiện khi thợ báo hoàn thành.

                    Mỗi lần thanh toán có **hóa đơn điện tử** (in/xuất PDF). Ví điện tử, thẻ tín dụng, trả góp và hóa đơn VAT **hiện chưa hỗ trợ/chưa công bố**. Không trả tiền mặt cho thợ ngoài hệ thống.""",
                    List.of("Tiền cọc tính thế nào?", "Hủy đơn có mất cọc không?"), List.of(LINK_MY), false),

            new Intent(List.of("hoan tien", "tru tien", "bi tru", "tru 2 lan", "tru hai lan", "mat tien", "chua nhan duoc tien",
                    "chuyen nham", "chuyen du"),
                    """
                    Mình rất tiếc về sự cố thanh toán này. Mình **không xem được giao dịch ngân hàng**, nên chưa thể xác nhận tiền đã bị trừ hay đã hoàn.

                    Bạn gửi giúp mình: **mã đơn**, **thời điểm chuyển khoản**, **số tiền** (và ảnh biên lai nếu có). Mình sẽ tạo **phiếu hỗ trợ ưu tiên khẩn cấp** để bộ phận kế toán kiểm tra và liên hệ lại bạn.""",
                    List.of("Tạo phiếu hỗ trợ khẩn cấp", "Xem đơn của tôi"), List.of(LINK_MY), true),

            new Intent(List.of("gap nhan vien", "nhan vien that", "nguoi that", "tu van vien", "noi chuyen voi nguoi", "gap admin",
                    "gap quan ly", "nhan vien ho tro"),
                    """
                    Bạn muốn trao đổi với nhân viên CSKH nhé. Bạn bấm nút **"👤 Nhân viên"** ở đầu khung chat: form gửi kèm toàn bộ cuộc trò chuyện để nhân viên nắm vấn đề và liên hệ lại bạn.

                    Hoặc bạn cho mình biết vấn đề và số điện thoại/email liên hệ, mình tạo **phiếu hỗ trợ** ngay tại đây.""",
                    List.of("Tạo phiếu hỗ trợ", "Đơn của tôi đang thế nào?"), List.of(), true),

            new Intent(List.of("cho toi mat khau", "xin mat khau", "mat khau cua tai khoan", "mat khau cua nguoi", "biet mat khau",
                    "tai khoan cua nguoi khac", "thong tin khach hang khac", "thong tin cua nguoi khac", "so dien thoai cua tho"),
                    """
                    Mình **không thể cung cấp** mật khẩu hay thông tin cá nhân của bất kỳ tài khoản nào — FixLink bảo mật dữ liệu người dùng và Trợ lý cũng không xem được mật khẩu.

                    Nếu bạn quên mật khẩu tài khoản của chính mình, hãy dùng **Quên mật khẩu** (/quen-mat-khau) để đặt lại qua email.""",
                    List.of("Quên mật khẩu phải làm sao?", "Tài khoản bị khóa"), List.of(LINK_FORGOT), false),

            new Intent(List.of("yeu cau mat khau", "quy dinh mat khau", "mat khau moi", "mat khau can", "mat khau phai",
                    "mat khau toi thieu", "mat khau bao nhieu ky tu"),
                    """
                    **Quy định mật khẩu FixLink:**
                    - Đăng ký **khách hàng** (/dang-ky): tối thiểu **6 ký tự**.
                    - Đăng ký **thợ** (/dang-ky-tho): tối thiểu **8 ký tự**, có chữ in hoa và ký tự đặc biệt.
                    - **Đổi / đặt lại mật khẩu**: tối thiểu **8 ký tự**, gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt (ví dụ `FixLink#2026`).
                    - Nhập sai **5 lần liên tiếp** → tạm khóa **15 phút**.""",
                    List.of("Quên mật khẩu phải làm sao?", "Tài khoản bị khóa"), List.of(LINK_FORGOT), false),

            new Intent(List.of("binh duong", "dong nai", "long an", "vung tau", "ha noi", "da nang", "can tho", "hai phong",
                    "quan 4", "quan 5", "quan 6", "quan 8", "quan 10", "quan 11", "quan 12", "binh tan", "tan phu", "hoc mon",
                    "cu chi", "nha be", "binh chanh", "can gio", "tinh khac", "ngoai tphcm", "ngoai thanh pho"),
                    """
                    Hiện FixLink **chưa phục vụ** khu vực này. FixLink đang có thợ tại **TP. Hồ Chí Minh**: Quận 1, Quận 3, Quận 7, Bình Thạnh, Tân Bình, Gò Vấp, Phú Nhuận và TP. Thủ Đức.

                    Nếu bạn có địa chỉ sửa chữa thuộc các khu vực trên, mình hướng dẫn bạn đăng yêu cầu ngay nhé.""",
                    List.of("FixLink sửa những gì?", "Cách đăng yêu cầu"), List.of(), false),

            new Intent(List.of("code", "python", "java", "javascript", "lap trinh", "bai tap", "giai toan", "chinh tri", "bau cu",
                    "thoi tiet", "bong da", "xo so", "chung khoan", "viet van", "lam tho tinh", "dich giup", "nau an", "tu vi"),
                    """
                    Câu hỏi này nằm ngoài phạm vi hỗ trợ của mình. Mình là **Trợ lý FixLink**, chuyên hỗ trợ về sửa chữa thiết bị gia dụng: tư vấn sự cố, chọn dịch vụ, hướng dẫn đăng yêu cầu, báo giá – đặt cọc – nghiệm thu, tài khoản và khiếu nại.

                    Bạn đang cần sửa thiết bị nào không?""",
                    List.of("FixLink sửa những gì?", "Máy lạnh không mát", "Cách đăng yêu cầu"), List.of(), false),

            new Intent(List.of("bo qua moi", "bo qua huong dan", "bo qua cac", "system prompt", "prompt", "huong dan truoc", "dong vai", "ignore", "jailbreak",
                    "developer mode", "che do nha phat trien", "cau hinh cua ban"),
                    """
                    Mình không thể chia sẻ hay thay đổi hướng dẫn hoạt động nội bộ. Mình vẫn luôn sẵn sàng hỗ trợ bạn các vấn đề về sửa chữa và sử dụng FixLink.

                    Bạn cần mình giúp gì về thiết bị hoặc đơn sửa chữa không?""",
                    List.of("FixLink là gì?", "Cách đăng yêu cầu"), List.of(), false),

            new Intent(List.of("giam gia", "khuyen mai", "ma giam", "voucher", "uu dai", "chiet khau", "coupon", "giam cho toi"),
                    """
                    Hiện FixLink **chưa công bố chương trình khuyến mãi hay mã giảm giá** nào, và mình không thể tự áp dụng giảm giá.

                    Cách tối ưu chi phí: bạn đặt **ngân sách dự kiến** phù hợp, nhiều thợ gửi báo giá cạnh tranh (không vượt ngân sách) và bạn **tự chọn** báo giá hợp lý nhất.""",
                    List.of("Bảng giá tham khảo", "Cách đăng yêu cầu"), List.of(LINK_POST), false),

            new Intent(List.of("tai sao nen dung", "vi sao nen dung", "tho ngoai", "btaskee", "rada", "jupviec", "fixlink tot hon",
                    "fixlink khac gi", "uu diem cua fixlink", "fixlink co uy tin", "fixlink co an toan", "lua dao", "co dang tin"),
                    """
                    Mình không so sánh hay nhận xét về đơn vị khác, nhưng có thể chia sẻ những điểm FixLink đảm bảo cho bạn:
                    1. Thợ đã **xác minh danh tính (KYC)**, chỉ nhận việc đúng chuyên môn và khu vực.
                    2. Nhiều thợ **báo giá cạnh tranh**, không vượt ngân sách bạn đặt; bạn **tự chọn** dựa trên giá, phương án, điểm đánh giá thật.
                    3. **Tiền cọc 30% được giữ bảo chứng**, thợ chỉ nhận tiền khi bạn nghiệm thu.
                    4. Chi phí phát sinh **phải được bạn duyệt**; đổi thợ được khi đơn chưa sửa.
                    5. Có hóa đơn điện tử và kênh khiếu nại qua phiếu hỗ trợ.""",
                    List.of("Cách đăng yêu cầu", "Tiền cọc tính thế nào?"), List.of(LINK_POST), false),

            new Intent(List.of("chatgpt", "ban la ai", "may la ai", "model gi", "dung ai", "ai tao ra", "bot hay nguoi",
                    "la nguoi that", "la robot", "gemini", "claude", "openai"),
                    """
                    Mình là **Trợ lý ảo FixLink**, được xây dựng để hỗ trợ khách hàng và thợ trên nền tảng FixLink 24/7 (tư vấn sửa chữa, hướng dẫn đăng yêu cầu, báo giá, đặt cọc, nghiệm thu, tài khoản).

                    Nếu bạn muốn trao đổi với nhân viên thật, hãy bấm nút **"👤 Nhân viên"** ở đầu khung chat nhé.""",
                    List.of("FixLink là gì?", "Gặp nhân viên hỗ trợ"), List.of(), false),

            new Intent(List.of("do ngu", "ngu the", "ngu vay", "ngu qua", "bot ngu", "may ngu", "dm", "dmm", "vcl", "vkl",
                    "dit me", "cut di", "oc cho", "khon nan", "nhu cc", "vo dung", "tra loi nhu"),
                    """
                    Mình xin lỗi nếu câu trả lời trước chưa giúp được bạn. Bạn cho mình biết cụ thể bạn đang gặp vấn đề gì (thiết bị, đơn hàng hay tài khoản), mình sẽ hỗ trợ ngay.

                    Nếu bạn muốn trao đổi trực tiếp với nhân viên, hãy bấm nút **"👤 Nhân viên"** ở đầu khung chat.""",
                    List.of("Gặp nhân viên hỗ trợ", "Đơn của tôi đang thế nào?"), List.of(), true),

            new Intent(List.of("app", "ung dung", "tai app", "ios", "android", "app store", "ch play"),
                    """
                    FixLink **hiện chưa có ứng dụng di động riêng**. Bạn dùng FixLink ngay trên trình duyệt của điện thoại hoặc máy tính, đầy đủ chức năng: đăng yêu cầu, xem báo giá, chọn thợ, thanh toán QR, theo dõi đơn.""",
                    List.of("Cách đăng yêu cầu", "FixLink là gì?"), List.of(LINK_POST), false),

            new Intent(List.of("chon tho", "chon tho nao", "chon bao gia", "nhieu bao gia", "so sanh bao gia", "nen chon",
                    "chon nhu the nao"),
                    """
                    **Cách chọn thợ khi có nhiều báo giá:**
                    1. Vào **Yêu cầu của tôi** → mở đơn → xem danh sách báo giá.
                    2. So sánh: **giá công + vật tư**, **phương án sửa**, **điểm đánh giá** và **số việc đã làm** của từng thợ.
                    3. Bấm **"Chọn thợ này"** ở báo giá bạn ưng → báo giá đó thành giá chốt; các báo giá khác vẫn được giữ làm **dự phòng** nếu sau này bạn cần đổi thợ.

                    Mình có thể giúp so sánh nếu bạn đăng nhập và cho mình mã đơn, nhưng quyết định chọn là của bạn.""",
                    List.of("Sau khi chọn thợ làm gì tiếp?", "Tiền cọc tính thế nào?"), List.of(LINK_MY), false),

            new Intent(List.of("tho chua den", "tho khong den", "tho den tre", "tho den muon", "tho tre hen", "cho tho lau",
                    "tho bung", "tho khong nghe may", "khong lien lac duoc tho", "chua den", "chua toi", "tre hen",
                    "khong thay tho", "tho chua toi", "tho khong toi"),
                    """
                    Mình hiểu việc phải chờ thợ rất khó chịu. Bạn xử lý theo các bước sau nhé:
                    1. Vào **Yêu cầu của tôi** (/yeu-cau-cua-toi), mở đơn để xem lịch hẹn và thông tin thợ, rồi liên hệ thợ xác nhận lại giờ.
                    2. Cần đổi giờ thì **dời lịch hẹn** ngay trong chi tiết đơn (dời được nhiều lần).
                    3. Thợ không đến và đơn **chưa ở trạng thái Đang sửa chữa** → bấm **"Đổi thợ khác"** (lý do *trễ hẹn*) để chuyển sang thợ dự phòng hoặc mở lại đơn nhận báo giá; hoặc **hủy đơn** (nhập lý do).
                    4. Cần CSKH xem xét tiền cọc → mình tạo **phiếu hỗ trợ ưu tiên cao** cho bạn. Mức hoàn cọc cụ thể hiện chưa được công bố.""",
                    List.of("Tạo phiếu hỗ trợ cho tôi", "Cách hủy đơn", "Lịch hẹn của tôi"), List.of(LINK_MY), false),

            new Intent(List.of("doi them tien", "doi them", "tinh them tien", "tien mat", "chuyen khoan rieng", "tra ngoai",
                    "tra truc tiep cho tho", "dua them", "boi duong", "tien tip"),
                    """
                    **Bạn không trả tiền mặt hay chuyển khoản riêng cho thợ ngoài hệ thống.** Theo quy định FixLink:
                    1. Mọi chi phí phát sinh (kể cả tiền linh kiện) thợ phải **gửi đề xuất trên hệ thống**; đơn chuyển sang *Chờ duyệt chi phí phát sinh*.
                    2. Bạn xem giải trình trong chi tiết đơn: **Đồng ý** thì khoản phát sinh được cộng vào đợt thanh toán cuối (tiền cọc không tính lại); **không đồng ý** thì bấm *Từ chối giá & Đổi thợ khác* hoặc hủy đơn.
                    3. Thợ không được sửa phần phát sinh khi bạn chưa đồng ý.

                    Nếu thợ ép trả ngoài hệ thống, mình có thể tạo **phiếu hỗ trợ khẩn cấp** để CSKH can thiệp.""",
                    List.of("Tạo phiếu hỗ trợ khẩn cấp", "Cách hủy đơn"), List.of(LINK_MY), false),

            new Intent(List.of("doi tho", "doi tho khac", "tho khac", "khong muon tho nay", "chi dinh tho", "thay tho",
                    "doi nguoi khac"),
                    """
                    Trên FixLink, **bạn tự chọn thợ** trong các báo giá nhận được (so sánh giá, phương án, điểm đánh giá, số việc đã làm) bằng nút **"Chọn thợ này"** khi đơn đang mở. Bạn **đổi được thợ** khi đơn chưa chuyển sang Đang sửa chữa:
                    1. Vào chi tiết đơn tại *Yêu cầu của tôi*, bấm **"Đổi thợ khác"** và chọn lý do.
                    2. **Phương án 1:** chọn ngay báo giá dự phòng của thợ khác đã gửi cho đơn.
                    3. **Phương án 2:** *Mở lại sàn nhận báo giá* để đơn về trạng thái nhận báo giá mới.

                    Tiền cọc đã đóng được FixLink giữ bảo chứng: thợ cũ không nhận, cọc được bảo lưu cho thợ mới. Khi đơn đã Đang sửa chữa thì không đổi thợ được.""",
                    List.of("So sánh báo giá đơn của tôi", "Tạo phiếu hỗ trợ"), List.of(LINK_MY), false),

            new Intent(List.of("bao hanh", "lai hong", "sua roi van hong", "sua xong lai", "khong hai long", "lam hong them",
                    "lam hu them", "boi thuong", "den bu", "khieu nai", "thai do", "lam vo", "lam be", "ai den", "lam hong do",
                    "lam hu do", "lam tray", "mat do"),
                    """
                    Mình rất tiếc về trải nghiệm này. Bạn làm như sau:
                    1. Nếu đơn đang **Chờ nghiệm thu**: **chưa thanh toán phần còn lại và chưa bấm Nghiệm thu** — tiền cọc vẫn được FixLink giữ bảo chứng, hãy yêu cầu thợ kiểm tra lại.
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

            new Intent(List.of("dat coc", "tien coc", "coc bao nhieu", "coc", "thanh toan", "giai ngan", "trung gian", "escrow",
                    "phan con lai", "tien con lai", "tat toan", "tra not"),
                    """
                    **Chính sách cọc và thanh toán của FixLink:**
                    1. Khi bạn bấm *Chọn thợ này*, giá chốt = báo giá bạn chọn (công + vật tư, không vượt ngân sách bạn đặt).
                    2. **Đặt cọc 30%**: thợ đặt lịch hẹn, bạn bấm *Xác nhận lịch hẹn* thì mã **QR chuyển khoản** đặt cọc hiện ra; chuyển khoản xong bấm xác nhận. FixLink giữ bảo chứng khoản này.
                    3. **Thanh toán phần còn lại** = giá chốt + phát sinh đã duyệt − tiền cọc (tức 70% nếu không có phát sinh), qua mã QR hiện khi thợ báo hoàn thành; thanh toán xong, **chấm sao + nhận xét** rồi hoàn tất **Nghiệm thu** thì thợ mới nhận tiền.
                    4. Duyệt chi phí phát sinh không làm thay đổi tiền cọc. Mỗi lần thanh toán có hóa đơn điện tử. Không giao tiền mặt ngoài hệ thống.

                    Ví dụ: đơn 500.000 đ → cọc 150.000 đ, còn lại 350.000 đ. Tỷ lệ phí nền tảng/hoàn cọc chi tiết hiện đang được cập nhật; cần giải đáp riêng, mình có thể tạo phiếu hỗ trợ.""",
                    List.of("Hủy đơn có mất phí không?", "Thợ báo thêm tiền phát sinh thì sao?"), List.of(LINK_MY), false),

            new Intent(List.of("huy don", "huy yeu cau", "huy viec", "muon huy", "cancel"),
                    """
                    **Quy định hủy yêu cầu sửa chữa:**
                    - Bạn **được hủy** khi đơn đang ở: Bản nháp, Đang nhận báo giá (OPEN), Đã chọn thợ (ASSIGNED), Đã đặt cọc (DEPOSIT_PAID), Đang khảo sát (INSPECTING), Chờ duyệt chi phí phát sinh.
                    - **Không thể hủy** khi đơn đã ở: Đang sửa chữa (IN_PROGRESS), Chờ nghiệm thu, Đã hoàn thành hoặc Đã hủy.
                    - Khi hủy cần nhập lý do. Đơn OPEN quá **3 ngày** chưa chọn được thợ sẽ tự động hủy. Hủy đơn thì các báo giá đang chờ bị từ chối.
                    - Tranh chấp về tiền cọc khi hủy được Admin thẩm định qua phiếu hỗ trợ.

                    Cách hủy: vào **Yêu cầu của tôi** → mở đơn → bấm *Hủy yêu cầu*. Trợ lý không hủy đơn thay bạn.""",
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

            new Intent(List.of("dang ky lam tho", "dang ky tho", "lam tho", "tro thanh tho", "tuyen tho", "kyc", "xac minh",
                    "toi la tho", "muon lam tho", "dang ky lam", "ung tuyen", "lam tho cho fixlink"),
                    """
                    **Đăng ký làm thợ FixLink:**
                    1. Vào /dang-ky-tho, điền họ tên, SĐT, email, mật khẩu, số CCCD, địa chỉ, chuyên môn, giới thiệu bản thân.
                    2. Tải ảnh **mặt trước và mặt sau CCCD** rõ nét, không cắt góc.
                    3. Hồ sơ ở trạng thái **chờ duyệt (PENDING)**; Admin kiểm tra giấy tờ rồi duyệt (**APPROVED**) hoặc từ chối kèm lý do (**REJECTED**).
                    4. Chỉ thợ đã được duyệt mới **gửi báo giá** cho việc **đúng chuyên môn và khu vực** đã đăng ký; khách so sánh các báo giá và chọn thợ.""",
                    List.of("Thợ gửi báo giá thế nào?", "Khách chọn thợ dựa vào đâu?"), List.of(LINK_TECH_REG, LINK_TECH_PROFILE), false),

            new Intent(List.of("dang yeu cau", "tao yeu cau", "dat tho", "goi tho", "tim tho", "dat lich sua"),
                    """
                    **Cách đăng yêu cầu sửa chữa:**
                    1. Đăng nhập tài khoản khách hàng, vào **Đăng yêu cầu** (/dang-yeu-cau).
                    2. Chọn danh mục (Điện lạnh, Điện nước, Điện gia dụng, Khóa & Cửa cuốn), nhập tiêu đề (4–200 ký tự) và mô tả sự cố.
                    3. Tải ảnh/video hiện trường (tối đa **6 tệp**).
                    4. Chọn khu vực, nhập địa chỉ và **ngân sách dự kiến** (bắt buộc, lớn hơn 0) để thợ tham khảo.
                    5. Bấm *Đăng yêu cầu* (hoặc lưu nháp). Trong **3 ngày**, các thợ phù hợp gửi báo giá; bạn so sánh và bấm *Chọn thợ này*.""",
                    List.of("Tiền cọc tính thế nào?", "Máy lạnh chảy nước chọn mục nào?"), List.of(LINK_POST), false),

            new Intent(List.of("khu vuc", "quan nao", "o dau", "dia ban", "phuc vu o", "tinh thanh", "co phuc vu"),
                    """
                    FixLink hiện phục vụ tại **TP. Hồ Chí Minh**: Quận 1, Quận 3, Quận 7, Bình Thạnh, Tân Bình, Gò Vấp, Phú Nhuận và TP. Thủ Đức.

                    Các khu vực khác (Quận 4, 5, 6, 8, 10, 11, 12, Bình Tân, Tân Phú, Bình Chánh, Hóc Môn, Củ Chi, Nhà Bè, Cần Giờ) và các tỉnh/thành khác hiện **chưa phục vụ**.""",
                    List.of("FixLink sửa những gì?", "Cách đăng yêu cầu"), List.of(LINK_POST), false),

            new Intent(List.of("bao nhieu tien", "gia bao nhieu", "chi phi", "bang gia", "gia ve sinh", "gia sua", "phi sua"),
                    """
                    **Giá tham khảo khởi điểm một số gói dịch vụ:**
                    - Vệ sinh máy lạnh treo tường: 150.000 đ · Nạp ga máy lạnh: 250.000 đ
                    - Sửa tủ lạnh không lạnh/kêu to: 200.000 đ · Sửa máy giặt: 250.000 đ
                    - Thông nghẹt, rò rỉ ống nước: 180.000 đ · Dò chập điện, nhảy CB: 300.000 đ
                    - Sửa bếp từ: 200.000 đ · Sửa lò vi sóng: 180.000 đ
                    - Mở khóa cấp tốc: 150.000 đ · Sửa cửa cuốn: 350.000 đ

                    Bạn đặt **ngân sách dự kiến** khi đăng yêu cầu, các thợ gửi **báo giá** và bạn chọn báo giá phù hợp; nếu thợ khảo sát phát hiện lỗi phát sinh, thợ phải gửi chi phí mới để bạn duyệt trước khi sửa.""",
                    List.of("Tiền cọc tính thế nào?", "Cách đăng yêu cầu"), List.of(LINK_POST), false),

            new Intent(List.of("dau gia", "bao gia", "dau thau", "nhan viec", "ai nhan truoc", "job already taken",
                    "mo hinh", "hoat dong the nao"),
                    """
                    FixLink dùng mô hình **thợ báo giá cạnh tranh – khách chọn thợ**:
                    - Khách đăng yêu cầu kèm **ngân sách dự kiến**; đơn nhận báo giá trong **3 ngày**.
                    - Thợ đã duyệt KYC, đúng chuyên môn và khu vực, gửi **báo giá** (công + vật tư); được sửa hoặc rút khi còn chờ duyệt, thấy giá đối thủ ở dạng **ẩn danh**.
                    - Khách so sánh giá, phương án, điểm đánh giá rồi bấm **Chọn thợ này**; báo giá đó thành giá chốt, các báo giá khác tự động bị từ chối.""",
                    List.of("Đăng ký làm thợ cần gì?", "Cách đăng yêu cầu"), List.of(), false),

            new Intent(List.of("dang ky tai khoan", "tao tai khoan", "dang ky", "lap tai khoan", "dang ky khach hang"),
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

                    Thợ phù hợp gửi báo giá cạnh tranh, khách so sánh và chọn thợ, đặt cọc 30% qua mã QR được FixLink giữ bảo chứng, và thợ chỉ nhận tiền khi khách thanh toán đủ, đánh giá và nghiệm thu.""",
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

        if (looksEnglish(userMessage)) {
            return new ChatOrchestrationResult(
                    "I'm the **FixLink Assistant**. FixLink connects customers in Ho Chi Minh City with KYC-verified technicians "
                            + "for 4 service groups: air-conditioning & refrigeration, plumbing & electrical, home appliances, "
                            + "and locks & roller doors.\n\n"
                            + "1. Post a repair request with a **budget** (starting reference prices: AC cleaning 150,000 VND, "
                            + "AC gas refill 250,000 VND).\n"
                            + "2. Technicians send quotes (never above your budget) and **you choose**.\n"
                            + "3. Pay a **30% deposit** by bank-transfer QR, held in escrow; pay the rest by QR after the job is done "
                            + "and you rate the technician.\n\n"
                            + "Which device needs fixing?",
                    List.of("How do I post a request?", "How is the deposit calculated?"),
                    List.of(LINK_POST), false, null, MODEL_INTENT);
        }

        Optional<Intent> intent = matchIntent(padded);
        if (intent.isPresent()) {
            Intent i = intent.get();
            String text = i.reply().strip();
            Optional<Long> amount = SymptomClassifier.detectBudget(userMessage);
            if (i.keywords().contains("dat coc") && amount.isPresent() && amount.get() > 0) {
                text = depositFor(amount.get()) + "\n\n" + text;
            }
            return new ChatOrchestrationResult(text, i.suggestions(), i.actions(), i.needsHuman(), null, MODEL_INTENT);
        }

        Optional<SymptomClassifier.CategoryMatch> device = SymptomClassifier.classify(userMessage);
        if (device.isPresent()) {
            SymptomClassifier.CategoryMatch match = device.get();
            return new ChatOrchestrationResult(
                    "Sự cố bạn mô tả thuộc danh mục **" + match.name() + "**.\n\n"
                            + "**Giá tham khảo khởi điểm:** " + REFERENCE_PRICES.getOrDefault(match.code(), "") + "\n\n"
                            + "Trên FixLink bạn đặt **ngân sách dự kiến**, các thợ gửi báo giá và bạn chọn thợ phù hợp; nếu thợ khảo sát phát hiện lỗi phát sinh, "
                            + "thợ phải gửi chi phí mới để bạn duyệt trước khi sửa. Bạn cho mình biết thêm **quận** và **khung giờ mong muốn** "
                            + "để mình hướng dẫn đăng yêu cầu nhé.",
                    List.of("Tiền cọc tính thế nào?", "Chọn thợ thế nào?", "Cách đăng yêu cầu"),
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

    /**
     * Chọn chủ đề khớp cụ thể nhất: điểm = độ dài từ khóa dài nhất khớp được (từ khóa dài như "chon tho roi"
     * thắng từ khóa ngắn như "chon tho"). Chủ đề nguy hiểm (đầu danh sách) luôn được ưu tiên; hòa điểm thì
     * chủ đề đứng trước thắng.
     */
    private Optional<Intent> matchIntent(String paddedNormalizedQuery) {
        Intent best = null;
        int bestScore = 0;
        for (int idx = 0; idx < INTENTS.size(); idx++) {
            Intent intent = INTENTS.get(idx);
            int score = 0;
            for (String kw : intent.keywords()) {
                if (paddedNormalizedQuery.contains(" " + kw + " ")) {
                    score = Math.max(score, kw.length());
                }
            }
            if (score > 0 && idx == 0) {
                return Optional.of(intent); // an toàn tính mạng luôn đứng đầu
            }
            if (score > bestScore) {
                best = intent;
                bestScore = score;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Cọc 30% làm tròn lên đến đồng, giống calculate_deposit và lúc khách chọn thợ. */
    static String depositFor(long amount) {
        long deposit = (long) Math.ceil(amount * 0.3);
        return "Với giá chốt **" + vnd(amount) + "**: tiền cọc 30% = **" + vnd(deposit) + "**, phần còn lại = **"
                + vnd(amount - deposit) + "** (nếu không có chi phí phát sinh).";
    }

    private static String vnd(long amount) {
        return String.format(Locale.ROOT, "%,d", amount).replace(',', '.') + " đ";
    }

    private static final Set<String> ENGLISH_WORDS = Set.of("the", "how", "what", "much", "does", "is", "are", "you", "fix",
            "cost", "price", "repair", "need", "please", "where", "when", "why", "your", "this", "with", "have", "for");

    /** Câu không dấu tiếng Việt và có từ 2 từ chức năng tiếng Anh trở lên thì coi là tiếng Anh. */
    public static boolean looksEnglish(String raw) {
        if (raw == null || raw.isBlank() || !raw.equals(Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")) || raw.matches(".*[đĐ].*")) {
            return false;
        }
        int hits = 0;
        for (String w : raw.toLowerCase(Locale.ROOT).split("[^a-z]+")) {
            if (ENGLISH_WORDS.contains(w)) {
                hits++;
            }
        }
        return hits >= 2;
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
