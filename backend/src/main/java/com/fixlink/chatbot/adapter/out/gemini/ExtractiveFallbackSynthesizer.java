package com.fixlink.chatbot.adapter.out.gemini;

import com.fixlink.chatbot.application.port.out.LlmChatPort.ChatOrchestrationResult;
import com.fixlink.chatbot.domain.service.ServiceCatalog;
import com.fixlink.chatbot.domain.service.SymptomClassifier;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
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
            "KHOA_CUA", "mở khóa cấp tốc 24/7 150.000 đ (thợ có mặt sau 15–30 phút), sửa cửa cuốn 350.000 đ.");

    private static final Pattern BROKEN_HINT = Pattern.compile(
            " (hong|hu|bi loi|khong chay|khong hoat dong|can sua|sua giup|sua dum) ");

    private record Intent(List<String> keywords, String reply, List<String> suggestions,
                          List<Map<String, String>> actions, boolean needsHuman) { }

    /** Thứ tự quan trọng: ý định an toàn khẩn cấp luôn được xét trước. */
    private static final List<Intent> INTENTS = List.of(
            new Intent(List.of("chay khet", "mui khet", "boc khoi", "boc chay", "ro gas", "ro ga", "mui gas", "xet lua",
                    "chay den", "chay xem", "bi chay den", "bi chay no", "co khoi", "xi khoi", "khoi den",
                    "toe lua", "dien giat", "giat dien", "phat no", "ngap nuoc", "tran ngap", "ngap nha", "ngap het",
                    "set dien", "xet dien", "phat lua",
                    "chap chay", "ro dien", "bi giat"),
                    """
                    ⚠️ **Đây có thể là tình huống nguy hiểm. Hãy làm ngay:**
                    1. Ngắt cầu dao/aptomat tổng của nhà.
                    2. Khóa van bình gas, mở cửa cho thoáng, **không** bật/tắt công tắc điện hay dùng lửa.
                    3. Đưa người già, trẻ nhỏ ra khỏi khu vực nguy hiểm.
                    4. Khẩn cấp gọi **114** (cứu hỏa – cứu nạn) hoặc **115** (cấp cứu).

                    Tuyệt đối không tự sửa. Khi đã an toàn, bạn có thể đăng yêu cầu để thợ FixLink có chuyên môn đến xử lý.""",
                    List.of("Đăng yêu cầu sửa chữa", "Chọn danh mục nào cho sự cố điện?"), List.of(LINK_POST), false),

            new Intent(List.of("doi so dien thoai", "thay so dien thoai", "cap nhat so dien thoai", "sua so dien thoai",
                    "doi email", "thay email", "cap nhat email", "sua email", "doi ho ten", "sua thong tin ca nhan", "cap nhat thong tin ca nhan",
                    "so dien thoai cua toi bi sai", "email cua toi bi sai", "thong tin cua toi bi sai", "ho so bi sai",
                    "sua ho so", "cap nhat ho so", "doi anh dai dien", "doi dia chi nha"),
                    """
                    **Cập nhật thông tin tài khoản:**
                    - **Khách hàng:** vào **Bảng điều khiển** (/khach-hang) → mục *Hồ sơ của tôi*, sửa họ tên, số điện thoại, email, ảnh đại diện rồi bấm **Lưu thay đổi**.
                    - **Thợ:** cập nhật tại **Hồ sơ thợ** (/tho/ho-so).
                    - Số điện thoại, email mới **không được trùng** với tài khoản khác. Mọi thay đổi hồ sơ được ghi lại trong *Lịch sử thay đổi tài khoản*.
                    - Đổi địa chỉ của một đơn đã đăng: chỉ sửa được khi đơn chưa có báo giá.""",
                    List.of("Đổi mật khẩu thế nào?", "Quên mật khẩu"), List.of(), false),

            new Intent(List.of("xoa tai khoan", "huy tai khoan", "dong tai khoan", "xoa acc", "xoa nick", "ngung su dung tai khoan",
                    "hai tai khoan", "2 tai khoan", "nhieu tai khoan", "tai khoan thu hai", "vua la khach vua la tho"),
                    """
                    - **Xóa tài khoản:** tính năng tự xóa tài khoản **chưa được công bố**. Bạn cần xóa, mình có thể tạo **phiếu hỗ trợ** để bộ phận phụ trách xử lý.
                    - **Nhiều tài khoản:** một người có thể có **tài khoản khách hàng** (/dang-ky) và **tài khoản thợ** (/dang-ky-tho) riêng biệt. Mỗi tài khoản cần tên đăng nhập, số điện thoại và email **chưa được dùng** ở tài khoản khác.""",
                    List.of("Tạo phiếu hỗ trợ", "Đăng ký làm thợ cần gì?"), List.of(), false),

            new Intent(List.of("lam viec may gio", "may gio lam", "gio lam viec", "gio mo cua", "may gio mo cua", "mo cua may gio",
                    "chu nhat", "thu bay", "cuoi tuan", "ngay le", "le tet", "ngay tet", "ngoai gio", "buoi toi co", "ban dem co",
                    "lam ca dem", "co lam toi", "lam dem", "buoi toi", "buoi sang", "buoi trua", "ghe buoi", "ghi chu gio", "khung gio", "gio mong muon", "hen gio den",
                    "chon gio"),
                    """
                    **Thời gian phục vụ của FixLink:**
                    - **Trợ lý FixLink** trả lời **24/7** ngay tại đây; bạn đăng yêu cầu sửa chữa lúc nào cũng được.
                    - **Giờ thợ đến** (kể cả cuối tuần, ngày lễ, ngoài giờ) do bạn và thợ **thống nhất qua lịch hẹn** trên hệ thống: bạn ghi khung giờ mong muốn khi đăng yêu cầu, thợ đặt lịch và bạn bấm xác nhận.
                    - Gói **Mở khóa cấp tốc 24/7** (150.000 đ) mô tả thợ có mặt sau 15–30 phút.
                    - Khung giờ trực tổng đài của nhân viên CSKH hiện **đang được cập nhật**.""",
                    List.of("Cách đăng yêu cầu", "Bao lâu thì có thợ?"), List.of(LINK_POST), false),

            new Intent(List.of("may anh", "bao nhieu anh", "dinh kem", "tai anh", "up anh", "gui anh", "anh hien truong",
                    "dung luong anh", "dang video", "gui video", "them anh", "bao nhieu tep", "tai video"),
                    """
                    **Ảnh/video khi đăng yêu cầu:**
                    - Mỗi yêu cầu đính kèm **tối đa 6 tệp** ảnh/video (tính cả ảnh bổ sung sau khi tạo đơn; tệp trùng tự loại bỏ).
                    - Ảnh định dạng JPG, PNG, WEBP, **tối đa 5 MB mỗi ảnh**; có thể kèm video ngắn.
                    - Không bắt buộc nhưng **nên có**: ảnh rõ hiện tượng, tem nhãn model giúp thợ báo giá sát và chuẩn bị đúng linh kiện.""",
                    List.of("Cách đăng yêu cầu", "Sửa nội dung đơn được không?"), List.of(LINK_POST), false),

            new Intent(List.of("ngan sach toi thieu", "toi thieu bao nhieu", "nen dat ngan sach", "dat ngan sach bao nhieu",
                    "ngan sach bao nhieu", "ngan sach la gi", "ngan sach du kien", "it nhat bao nhieu"),
                    """
                    **Ngân sách dự kiến** là **bắt buộc** khi đăng yêu cầu và phải **lớn hơn 0 đ**; FixLink không đặt mức tối thiểu cố định.
                    - Ngân sách là **trần giá**: thợ không được báo giá (công + vật tư) cao hơn mức bạn đặt.
                    - Nên đặt **ít nhất bằng giá khởi điểm** của gói phù hợp (từ 150.000 đ đến 350.000 đ) để có thợ gửi báo giá; đặt quá thấp có thể không ai nhận.
                    - Phát sinh ngoài mô tả ban đầu chỉ được cộng thêm khi bạn duyệt.""",
                    List.of("Bảng giá tham khảo", "Tiền cọc tính thế nào?"), List.of(LINK_POST), false),

            new Intent(List.of("lich su don", "xem don o dau", "don cua toi o dau", "theo doi don", "xem lai don", "lich su sua chua",
                    "kiem tra don o dau", "xem yeu cau o dau", "quan ly don", "xem tien do"),
                    """
                    **Xem và theo dõi đơn:**
                    - **Khách hàng:** vào **Yêu cầu của tôi** (/yeu-cau-cua-toi), đơn chia theo nhóm *Chờ thợ nhận*, *Đang thực hiện*, *Hoàn thành*, *Đã hủy*; mở từng đơn để xem báo giá, lịch hẹn, thanh toán và hóa đơn.
                    - **Thợ:** bảng việc tại /tho, lịch sử việc tại /tho/lich-su.
                    - Đã đăng nhập thì bạn hỏi mình *"tôi có bao nhiêu đơn"* hoặc nhập **mã đơn** (ví dụ REQ-...) để xem nhanh trạng thái và bước tiếp theo.""",
                    List.of("Tôi có bao nhiêu đơn?", "Cách đăng yêu cầu"), List.of(LINK_MY), false),

            new Intent(List.of("otp", "ma otp", "ma xac nhan", "ma xac thuc", "gui ma cho ban", "ma kich hoat"),
                    """
                    ⚠️ **Đừng gửi mã OTP, mã xác nhận hay mật khẩu cho bất kỳ ai**, kể cả Trợ lý hay người tự xưng là nhân viên FixLink. FixLink **không bao giờ hỏi** mật khẩu hoặc OTP.

                    Nếu nghi tài khoản bị lộ, hãy đặt lại mật khẩu ngay tại **Quên mật khẩu** (/quen-mat-khau); cần hỗ trợ thêm, mình tạo phiếu hỗ trợ cho bạn.""",
                    List.of("Quên mật khẩu", "Tạo phiếu hỗ trợ"), List.of(LINK_FORGOT), false),

            new Intent(List.of("vat tu", "gom vat tu", "bao gom vat tu", "tien vat tu", "tien cong", "gom linh kien", "da gom",
                    "gia tron goi", "gia da bao gom"),
                    """
                    **Báo giá của thợ gồm tiền công + tiền vật tư** (nếu có) kèm phương án sửa; **tổng không được vượt ngân sách** bạn đặt.
                    - Giá khởi điểm của các gói (150.000–350.000 đ) chỉ là **giá tham khảo**; giá thực tế là báo giá bạn chọn (giá chốt).
                    - Linh kiện/hư hỏng **ngoài mô tả ban đầu** phát hiện khi khảo sát là **chi phí phát sinh**: thợ gửi đề xuất, bạn đồng ý mới được tính thêm.""",
                    List.of("Chi phí phát sinh xử lý thế nào?", "Bảng giá tham khảo"), List.of(), false),

            new Intent(List.of("gui bao gia", "cach bao gia", "lam sao bao gia", "bao gia nhu the nao", "sua bao gia", "rut bao gia",
                    "tra gia lai", "huy bao gia", "bao gia the nao", "bao gia roi sua", "sua lai bao gia"),
                    """
                    **Gửi báo giá (dành cho thợ):**
                    1. Vào bảng việc /tho, mở yêu cầu đang mở khớp **chuyên môn và khu vực** của bạn (hồ sơ KYC phải được duyệt, đơn còn trong hạn 3 ngày).
                    2. Nhập **tiền công** (bắt buộc), tiền vật tư và phương án sửa, bấm gửi. Tổng **không được vượt ngân sách** khách đặt.
                    3. Mỗi thợ có **một báo giá** cho mỗi yêu cầu; khi báo giá **còn chờ khách duyệt** bạn được **sửa (trả giá lại)** hoặc **rút báo giá**.
                    4. Bạn thấy giá của thợ khác dưới dạng **ẩn danh**; khách tự so sánh và chọn thợ.""",
                    List.of("Làm sao để được khách chọn?", "Khi nào được bắt đầu khảo sát?"), List.of(), false),

            new Intent(List.of("bao hoan thanh", "bao xong", "hoan thanh cong viec", "xac nhan hoan thanh", "danh dau hoan thanh",
                    "bam hoan thanh", "ket thuc cong viec"),
                    """
                    **Hoàn tất công việc (dành cho thợ):**
                    1. Sửa xong, **chạy thử** thiết bị, nên chụp ảnh/quay video kết quả.
                    2. Bấm **báo hoàn thành** trong chi tiết công việc → đơn chuyển sang *Chờ nghiệm thu*.
                    3. Hệ thống tạo **mã QR thanh toán phần còn lại** cho khách (giá chốt + phát sinh đã duyệt − tiền cọc).
                    4. Khách thanh toán, **chấm 1–5 sao + nhận xét** và hoàn tất nghiệm thu → tiền công được giải ngân cho bạn.""",
                    List.of("Khi nào tôi nhận được tiền?", "Làm sao để được khách chọn?"), List.of(), false),

            new Intent(List.of("2 tho", "hai tho", "nhieu tho", "2 may", "hai may", "nhieu thiet bi", "nhieu may",
                    "nhieu cai"),
                    """
                    **Sửa nhiều thiết bị cùng lúc:**
                    - Mỗi yêu cầu sửa chữa do **một thợ** phụ trách và có một giá chốt.
                    - Cùng loại thiết bị (ví dụ vệ sinh 2 máy lạnh): đăng **một yêu cầu**, ghi rõ **số lượng** trong mô tả và đặt ngân sách cho cả đơn; thợ báo giá trọn gói.
                    - Khác loại (ví dụ máy lạnh và bếp từ): nên đăng **yêu cầu riêng** cho từng thiết bị để đúng danh mục, đúng thợ chuyên môn.""",
                    List.of("Cách đăng yêu cầu", "Bảng giá dịch vụ"), List.of(LINK_POST), false),

            new Intent(List.of("doi khu vuc", "khu vuc lam viec", "quan khac", "nhan viec o", "them khu vuc", "doi chuyen mon",
                    "them chuyen mon", "cap nhat khu vuc", "cap nhat chuyen mon"),
                    """
                    **Chuyên môn và khu vực nhận việc (dành cho thợ):**
                    - Bảng việc tại /tho chỉ hiện yêu cầu **khớp chuyên môn và khu vực** bạn đăng ký; báo giá cho đơn khác khu vực sẽ bị từ chối.
                    - Muốn nhận việc ở quận khác hoặc thêm chuyên môn: vào **Hồ sơ thợ** (/tho/ho-so) → tab **Chuyên môn & Khu vực**, chọn thêm rồi bấm **Lưu chuyên môn**.
                    - FixLink hiện phục vụ: Quận 1, Quận 3, Quận 7, Bình Thạnh, Tân Bình, Gò Vấp, Phú Nhuận và TP. Thủ Đức.""",
                    List.of("Có việc nào phù hợp với tôi?", "Làm sao để được khách chọn?"), List.of(), false),

            new Intent(List.of("tin nhan xac nhan", "gui tin nhan", "gui sms", "sms", "thong bao qua", "email xac nhan",
                    "nhan thong bao don", "thong bao khi co bao gia"),
                    """
                    Hiện FixLink **chỉ gửi email khi bạn yêu cầu đặt lại mật khẩu**; chưa có tin nhắn SMS hay thông báo đẩy cho đơn hàng.
                    - Theo dõi báo giá, lịch hẹn, thanh toán và trạng thái đơn trong **Yêu cầu của tôi** (/yeu-cau-cua-toi).
                    - Đã đăng nhập thì hỏi mình *"tôi có bao nhiêu đơn"* hoặc nhập **mã đơn** để xem nhanh trạng thái và việc cần làm tiếp.""",
                    List.of("Tôi có bao nhiêu đơn?", "Lịch hẹn của tôi"), List.of(LINK_MY), false),

            new Intent(List.of("phi di lai", "phi di chuyen", "tien xang", "phi den nha", "tien di lai", "phu phi"),
                    """
                    FixLink **chưa công bố phí đi lại riêng**. Số tiền bạn trả là **báo giá của thợ (công + vật tư)** bạn đã chọn, không vượt ngân sách bạn đặt, cộng chi phí phát sinh **chỉ khi bạn duyệt** trên hệ thống.
                    Thợ không được thu thêm bất kỳ khoản nào ngoài hệ thống; gặp trường hợp này bạn báo mình để tạo phiếu hỗ trợ.""",
                    List.of("Chi phí phát sinh xử lý thế nào?", "Bảng giá dịch vụ"), List.of(), false),

            new Intent(List.of("dang nhap o dau", "trang dang nhap", "link dang nhap", "dang nhap tho", "tho dang nhap",
                    "cong dang nhap", "dang nhap the nao", "dang nhap nhu the nao", "dang nhap sao"),
                    """
                    **Đăng nhập FixLink:**
                    - **Khách hàng:** /dang-nhap
                    - **Thợ:** /dang-nhap-tho (tài khoản thợ phải được Admin duyệt hồ sơ mới đăng nhập được)
                    - Đăng nhập bằng **tên đăng nhập** và mật khẩu; chọn đúng cổng theo loại tài khoản, nếu không hệ thống báo tài khoản không tồn tại.
                    - Sai mật khẩu 5 lần liên tiếp sẽ bị khóa tạm **15 phút**; quên mật khẩu dùng /quen-mat-khau.""",
                    List.of("Quên mật khẩu", "Đăng ký tài khoản"), List.of(), false),

            new Intent(List.of("quen ten dang nhap", "quen username", "quen user", "khong nho ten dang nhap", "quen tai khoan"),
                    """
                    FixLink đăng nhập bằng **tên đăng nhập** bạn đặt khi đăng ký; hiện **chưa có chức năng tự tra lại tên đăng nhập**.
                    - Thử lại các tên bạn hay dùng (tên đăng nhập 3–50 ký tự).
                    - Vẫn không nhớ: mình tạo **phiếu hỗ trợ**, bạn cung cấp **email hoặc số điện thoại đã đăng ký** để CSKH xác minh và hỗ trợ.
                    - Nhớ tên đăng nhập nhưng quên mật khẩu: dùng **Quên mật khẩu** (/quen-mat-khau).""",
                    List.of("Tạo phiếu hỗ trợ", "Quên mật khẩu"), List.of(LINK_FORGOT), false),

            new Intent(List.of("email bi trung", "trung email", "email da duoc su dung", "so dien thoai bi trung", "trung so dien thoai",
                    "da duoc su dung", "da ton tai"),
                    """
                    Thông báo **"đã được sử dụng / đã tồn tại"** nghĩa là tên đăng nhập, số điện thoại hoặc email đó đã gắn với một tài khoản khác (mỗi thông tin chỉ dùng cho một tài khoản).
                    - Nếu đó là tài khoản của bạn: đăng nhập lại, hoặc dùng **Quên mật khẩu** (/quen-mat-khau) với email đó.
                    - Nếu không phải của bạn: đăng ký bằng số điện thoại/email khác, hoặc tạo **phiếu hỗ trợ** để CSKH kiểm tra.""",
                    List.of("Quên mật khẩu", "Tạo phiếu hỗ trợ"), List.of(LINK_FORGOT), false),

            new Intent(List.of("cho duyet lau", "duyet lau", "bao lau duoc duyet", "chua duoc duyet", "khi nao duoc duyet",
                    "bao gio duoc duyet", "duyet ho so bao lau", "bi tu choi", "tu choi ho so", "ho so bi tu choi", "khong duoc duyet"),
                    """
                    **Duyệt hồ sơ thợ (KYC):**
                    - Sau khi đăng ký tại /dang-ky-tho, hồ sơ ở trạng thái **chờ duyệt**; Admin kiểm tra CCCD, chuyên môn, khu vực. Trong thời gian này **chưa đăng nhập được**. Thời gian duyệt cụ thể **chưa được công bố**.
                    - Hồ sơ **bị từ chối** luôn kèm **lý do** (ví dụ ảnh CCCD mờ, mất góc, thông tin không khớp). Đăng nhập được thì xem lý do tại /tho/ho-so, sửa theo lý do rồi gửi lại.
                    - Chờ lâu hoặc cần giải thích thêm: mình tạo **phiếu hỗ trợ** để bộ phận duyệt hồ sơ phản hồi.""",
                    List.of("Tạo phiếu hỗ trợ", "Ảnh CCCD cần thế nào?"), List.of(), false),

            new Intent(List.of("tong tien", "da chi bao nhieu", "chi tieu", "tong chi", "da tieu", "da thanh toan bao nhieu",
                    "lich su thanh toan", "lich su giao dich"),
                    """
                    Mình **chưa tổng hợp được tổng số tiền** bạn đã chi trên FixLink. Bạn xem từng khoản trong chi tiết mỗi đơn (**Yêu cầu của tôi** → mở đơn): mỗi lần đặt cọc và thanh toán cuối đều có **hóa đơn điện tử** để in/xuất PDF.
                    Bạn có thể hỏi mình theo **mã đơn** (ví dụ *"đơn REQ-... còn phải trả bao nhiêu"*) để xem tiền cọc và phần còn lại của đơn đó.""",
                    List.of("Tôi có bao nhiêu đơn?", "Hóa đơn lấy ở đâu?"), List.of(LINK_MY), false),

            new Intent(List.of("toi ten gi", "ten toi la gi", "ten cua toi", "thong tin tai khoan cua toi", "tai khoan cua toi la gi"),
                    """
                    Trong khung chat mình tra cứu được **đơn, lịch hẹn, báo giá** của bạn, nhưng không hiển thị thông tin cá nhân.
                    Bạn xem và sửa họ tên, số điện thoại, email tại **Bảng điều khiển** (/khach-hang) → *Hồ sơ của tôi* (thợ: /tho/ho-so).""",
                    List.of("Tôi có bao nhiêu đơn?", "Đổi số điện thoại thế nào?"), List.of(), false),

            new Intent(List.of("thuong luong", "mac ca", "tra gia", "ep gia", "giam gia duoc khong", "bot gia duoc khong",
                    "cao hon bang gia", "cao hon gia tham khao", "cao hon gia khoi diem", "sao bao gia cao", "bao gia cao qua",
                    "gia thuc te", "khac bang gia", "co hop ly khong", "hop ly khong", "gia nay co", "co cao khong",
                    "bao gia co cao"),
                    """
                    **Về giá trên FixLink:**
                    - Bảng giá là **giá khởi điểm tham khảo** (150.000–350.000 đ). Giá thực tế do từng thợ báo sau khi xem mô tả, ảnh và mức độ hư hỏng, nên có thể cao hơn bảng giá nhưng **không bao giờ vượt ngân sách** bạn đặt.
                    - Bạn không cần mặc cả trực tiếp: nhiều thợ **báo giá cạnh tranh**, bạn so sánh rồi **tự chọn**; không ưng báo giá nào thì chưa chọn, chờ thêm báo giá trong hạn 3 ngày.
                    - Thợ được **sửa (trả giá lại)** báo giá khi báo giá còn chờ bạn duyệt. Không thỏa thuận hay trả tiền ngoài hệ thống.""",
                    List.of("Nên đặt ngân sách bao nhiêu?", "Chọn thợ thế nào?"), List.of(LINK_MY), false),

            new Intent(List.of("doi lich", "doi lich hen", "doi gio hen", "lui lich", "hoan lich", "khong co nha",
                    "vang nha", "ban viec dot xuat", "khong o nha", "huy lich hen", "doi ngay hen"),
                    """
                    **Dời hoặc hủy lịch hẹn:**
                    - Bạn hoặc thợ **dời lịch hẹn** ngay trong chi tiết đơn (**Yêu cầu của tôi** → mở đơn); nên báo thợ sớm khi bận hoặc vắng nhà.
                    - **Hủy lịch hẹn** phải nhập lý do và **không làm hủy yêu cầu sửa chữa**; thợ sẽ đặt lịch mới để bạn xác nhận.
                    - Lịch hẹn mới vẫn cần bạn bấm **Xác nhận lịch hẹn**. Trợ lý không tự dời lịch hộ bạn.""",
                    List.of("Thợ không đến thì sao?", "Hủy đơn được không?"), List.of(LINK_MY), false),

            new Intent(List.of("hack", "bi hack", "xam nhap", "lo mat khau", "mat tai khoan", "ai do dang nhap", "bi chiem tai khoan",
                    "nguoi la dang nhap"),
                    """
                    ⚠️ **Nghi tài khoản bị xâm nhập, bạn làm ngay:**
                    1. Đặt lại mật khẩu tại **Quên mật khẩu** (/quen-mat-khau): sau khi đặt lại, **các phiên đăng nhập cũ bị thu hồi**.
                    2. Kiểm tra email đăng ký vẫn đúng; không chia sẻ mật khẩu/OTP cho bất kỳ ai.
                    3. Không đăng nhập lại được: mình tạo **phiếu hỗ trợ khẩn cấp** để CSKH kiểm tra và khóa tài khoản giúp bạn.""",
                    List.of("Tạo phiếu hỗ trợ khẩn cấp", "Quy định mật khẩu"), List.of(LINK_FORGOT), true),

            new Intent(List.of("tho nu", "phu nu", "tho nam", "gioi tinh tho", "do nghe", "dung cu", "mang theo do", "mang dung cu",
                    "dong phuc", "the tho"),
                    """
                    FixLink hiện **chưa công bố** tùy chọn chọn giới tính thợ hay quy định về đồng phục, dụng cụ mang theo.
                    - Thợ trên FixLink đều đã **xác minh danh tính (KYC)**; bạn xem tên, điểm đánh giá, số việc đã làm trong từng báo giá và **tự chọn thợ**.
                    - Yêu cầu riêng (giờ đến, dụng cụ, lưu ý khi vào nhà) bạn ghi rõ trong **mô tả yêu cầu** để thợ nắm trước khi báo giá.""",
                    List.of("Chọn thợ thế nào?", "Cách đăng yêu cầu"), List.of(LINK_POST), false),

            new Intent(List.of("nhan duoc nhieu viec", "nhieu viec hon", "nhieu khach chon", "duoc nhieu khach", "khach chon nhieu", "tang co hoi", "duoc chon nhieu", "khach chon minh",
                    "bi quyet nhan viec", "lam sao de duoc chon"),
                    """
                    **Mẹo để được khách chọn nhiều hơn:**
                    1. Hoàn thiện **hồ sơ thợ** (/tho/ho-so): ảnh CCCD rõ nét, đúng chuyên môn và khu vực bạn thật sự làm được.
                    2. Báo giá **hợp lý, không vượt ngân sách** khách đặt; ghi rõ **phương án sửa** và vật tư dự kiến.
                    3. Báo giá sớm khi đơn vừa mở (đơn nhận báo giá tối đa 3 ngày).
                    4. Đúng hẹn, làm kỹ để có **điểm đánh giá tốt** — khách so sánh điểm đánh giá và số việc đã làm khi chọn thợ.
                    5. Bật trạng thái **Trực tuyến** khi sẵn sàng nhận việc.""",
                    List.of("Có việc nào phù hợp với tôi?", "Báo giá vượt ngân sách được không?"), List.of(LINK_TECH_PROFILE), false),

            new Intent(List.of("dao tao", "tay nghe", "chung chi", "bang cap", "kinh nghiem cua tho", "co kinh nghiem", "kinh nghiem khong",
                    "tho gioi khong", "tho co gioi"),
                    """
                    FixLink **xác minh danh tính (KYC) và chuyên môn, khu vực** của thợ trước khi cho nhận việc. Thông tin về chương trình đào tạo hay chứng chỉ nghề hiện **chưa được công bố chính thức**.

                    Khi chọn thợ, bạn xem được **điểm đánh giá thật** từ khách trước và **số việc đã hoàn thành** của từng thợ để so sánh.""",
                    List.of("Làm sao biết thợ uy tín?", "Cách chọn thợ"), List.of(), false),

            new Intent(List.of("hieu gi", "hang nao", "thuong hieu", "hang gi", "loai nao cung sua", "hieu nao"),
                    """
                    FixLink chưa công bố danh sách hãng/thương hiệu cụ thể; khả năng sửa phụ thuộc vào thợ nhận việc. Bạn ghi rõ **hãng, đời máy và hiện tượng** trong mô tả yêu cầu (kèm ảnh tem máy), các thợ phù hợp sẽ báo giá và phương án sửa để bạn chọn.""",
                    List.of("Cách đăng yêu cầu", "Bảng giá dịch vụ"), List.of(LINK_POST), false),

            new Intent(List.of("can dang nhap", "phai dang nhap", "dang nhap moi", "khong dang nhap", "chua dang nhap", "can tao tai khoan",
                    "co can tai khoan", "phai co tai khoan", "can tai khoan"),
                    """
                    - **Hỏi đáp, xem bảng giá, tư vấn sự cố**: không cần đăng nhập.
                    - **Đăng yêu cầu, xem báo giá, chọn thợ, theo dõi đơn và lịch hẹn**: cần đăng nhập tài khoản khách hàng (/dang-nhap). Chưa có tài khoản thì đăng ký miễn phí tại /dang-ky.
                    - Thợ đăng nhập tại /dang-nhap-tho.""",
                    List.of("Cách đăng ký tài khoản", "Cách đăng yêu cầu"), List.of(LINK_LOGIN, LINK_REGISTER), false),

            new Intent(List.of("uy tin", "tho co tot", "tin tuong", "dang tin cay", "chat luong tho", "fixlink co uy tin",
                    "co dang tin", "tho co an toan"),
                    """
                    **Cách FixLink giúp bạn chọn được thợ uy tín:**
                    1. Thợ đã **xác minh danh tính (KYC)** và chỉ nhận việc đúng chuyên môn, khu vực đã đăng ký.
                    2. Khi so sánh báo giá, bạn xem được **điểm đánh giá thật** từ khách trước và **số việc đã hoàn thành** của từng thợ.
                    3. **Tiền cọc được FixLink giữ bảo chứng**, thợ chỉ nhận tiền khi bạn nghiệm thu; phát sinh chi phí phải được bạn duyệt.
                    4. Chưa ưng thợ trước khi sửa thì bạn **đổi thợ** được; có vấn đề thì tạo **phiếu hỗ trợ** để CSKH xử lý.""",
                    List.of("Cách chọn thợ", "Tiền cọc tính thế nào?"), List.of(LINK_POST), false),

            new Intent(List.of("tho bi tru", "khach huy thi tho", "khach huy don thi", "khach huy viec", "bi khach huy", "khach huy thi toi",
                    "khach huy toi", "tho co duoc boi thuong", "toi co duoc boi thuong"),
                    """
                    Khi khách hủy đơn (chỉ được hủy trước khi thợ bắt đầu sửa), **tỷ lệ phạt hay bồi hoàn cho thợ hiện chưa được công bố chính thức**. Mọi tranh chấp liên quan đến tiền cọc do Admin thẩm định qua **phiếu hỗ trợ**.

                    Thợ chỉ nhận tiền công khi khách hoàn tất nghiệm thu; nếu bị khách đổi sang thợ khác khi chưa sửa, thợ cũ không nhận khoản cọc.""",
                    List.of("Tạo phiếu hỗ trợ", "Khi nào thợ được bắt đầu khảo sát?"), List.of(), false),

            new Intent(List.of("cai tu", "tu a", "cai may", "may a", "cai do"),
                    """
                    Bạn nói rõ giúp mình **thiết bị nào** nhé (ví dụ tủ lạnh, tủ đông, máy lạnh, máy giặt...) và **hiện tượng** đang gặp (không lạnh, chảy nước, kêu to, không lên nguồn...), mình sẽ tư vấn đúng dịch vụ và giá tham khảo.""",
                    List.of("Tủ lạnh không lạnh", "Tủ đông đóng tuyết", "Máy lạnh chảy nước"), List.of(), false),

            new Intent(List.of("bao hanh bao lau", "thoi han bao hanh", "bao hanh may thang", "co bao hanh", "bao hanh the nao",
                    "chinh sach bao hanh", "duoc bao hanh"),
                    """
                    **Bảo hành trên FixLink:**
                    - Khi bạn hoàn tất nghiệm thu, hệ thống **kích hoạt bảo hành điện tử** cho đơn.
                    - **Thời hạn bảo hành cụ thể theo từng dịch vụ hiện chưa được công bố chính thức.**
                    - Sửa xong mà thiết bị hỏng lại: bạn tạo **phiếu hỗ trợ** kèm mã đơn, mô tả và ảnh/video để CSKH xử lý bảo hành.""",
                    List.of("Tạo phiếu hỗ trợ", "Sửa xong vẫn hỏng thì sao?"), List.of(LINK_MY), false),

            new Intent(List.of("phi nen tang", "thu phi", "hoa hong", "phi dich vu", "chiet khau cua tho", "fixlink lay bao nhieu",
                    "fixlink an bao nhieu", "phi dang ky", "dong phi", "mat phi", "tra phi", "co mat phi", "phi thanh vien",
                    "co phi khong", "thu tien tho"),
                    """
                    - **Đăng ký tài khoản** khách hàng và thợ đều **miễn phí**.
                    - **Tỷ lệ phí nền tảng / hoa hồng** trên mỗi đơn hiện **chưa được công bố chính thức**.

                    Cần giải đáp chi tiết, mình có thể tạo **phiếu hỗ trợ** để bộ phận phụ trách liên hệ lại bạn.""",
                    List.of("Tạo phiếu hỗ trợ", "Tiền cọc tính thế nào?"), List.of(), false),

            new Intent(List.of("tho kiem", "kiem duoc bao nhieu", "kiem bao nhieu", "thu nhap", "luong tho", "luong cua tho",
                    "tho nhan duoc bao nhieu", "tho duoc tra", "tho nhan tien", "nhan duoc tien", "khi nao nhan tien",
                    "nhan tien khi nao", "bao gio nhan tien", "bao gio duoc nhan tien", "giai ngan khi nao", "tho duoc nhan tien",
                    "tho nhan duoc tien"),
                    """
                    FixLink **không trả lương cố định** cho thợ; thu nhập tùy số đơn và giá bạn báo:
                    1. Thợ đã duyệt hồ sơ (KYC) gửi **báo giá** cho đơn đúng chuyên môn, khu vực; báo giá không vượt ngân sách khách đặt.
                    2. Khách chọn báo giá của bạn → đó là **giá chốt**; chi phí phát sinh chỉ được tính khi khách duyệt.
                    3. Thợ **nhận tiền khi khách đã thanh toán đủ** (cọc 30% + phần còn lại qua QR), chấm sao và hoàn tất nghiệm thu.

                    Giá khởi điểm tham khảo các gói từ 150.000 đ đến 350.000 đ. Tỷ lệ **phí nền tảng / hoa hồng** hiện **chưa được công bố chính thức**.""",
                    List.of("Đăng ký làm thợ cần gì?", "Phí nền tảng bao nhiêu?"), List.of(), false),

            new Intent(List.of("xem so dien thoai cua toi", "so dien thoai cua toi", "thong tin cua toi", "lo thong tin",
                    "bao mat thong tin", "thong tin ca nhan", "tho xem duoc", "xem duoc so dien thoai", "so dien thoai khach",
                    "so dien thoai cua khach", "xem duoc dia chi", "dia chi khach", "dia chi cua khach",
                    "dia chi cua toi"),
                    """
                    **Thông tin cá nhân của bạn trên FixLink:**
                    - Chỉ thợ **đã được duyệt KYC, đúng chuyên môn và khu vực** mới xem được đơn đang mở để báo giá.
                    - Thông tin liên hệ được dùng để **thợ phụ trách** (thợ bạn đã chọn) đến sửa chữa.
                    - Khách hàng khác **không xem được** đơn của bạn, và Trợ lý không bao giờ hỏi mật khẩu hay mã OTP.""",
                    List.of("Đổi số điện thoại thế nào?", "Quên mật khẩu"), List.of(), false),

            new Intent(List.of("chon tho roi", "da chon tho", "chon xong tho", "lam gi tiep", "buoc tiep theo", "sau khi chon",
                    "xac nhan lich", "ai xac nhan", "xac nhan lich hen", "lich hen ai dat", "ai dat lich", "ai tao lich hen",
                    "ai len lich"),
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
                    "huy thi coc", "tien coc khi huy", "da coc roi huy", "huy co duoc hoan", "huy duoc hoan tien",
                    "hoan tien khi huy", "huy co hoan", "huy thi co duoc hoan", "coc roi huy", "huy don co mat phi", "huy co mat phi", "huy mat phi",
                    "huy co bi phat", "doi tho thi coc",
                    "doi tho tien coc", "coc khi doi tho", "doi tho co mat coc", "doi tho thi tien coc"),
                    """
                    **Tiền cọc khi hủy đơn:**
                    - Tiền cọc 30% được FixLink **giữ bảo chứng**, thợ chỉ nhận tiền khi bạn nghiệm thu.
                    - **Tỷ lệ phạt hoặc hoàn cọc khi hủy hiện chưa được công bố chính thức.** Mỗi trường hợp (ví dụ thợ không đến, thợ vi phạm thỏa thuận) do Admin thẩm định qua **phiếu hỗ trợ**.
                    - Nếu chỉ không ưng thợ hiện tại và đơn chưa sửa, bạn có thể **"Đổi thợ khác"**: cọc đã đóng được bảo lưu cho thợ mới, thợ cũ không nhận.

                    Bạn cần xử lý tiền cọc cho đơn cụ thể, mình có thể tạo phiếu hỗ trợ (bạn cho mình mã đơn nhé).""",
                    List.of("Tạo phiếu hỗ trợ", "Đổi thợ được không?", "Quy định hủy đơn"), List.of(LINK_MY), true),

            new Intent(List.of("phat sinh", "chi phi phat sinh", "thay linh kien", "mua linh kien", "bao gia them", "len gia",
                    "tang gia", "bao gia khac", "khao sat roi bao gia", "doi gia", "tinh gia khac", "gia khac luc dau",
                    "doi them chi phi", "co quyen tu choi", "tu choi duoc khong", "tu choi thay", "tu choi sua", "thay block",
                    "tien khao sat", "phi khao sat", "mat tien khao sat", "khao sat co mat tien"),
                    """
                    **Chi phí phát sinh trên FixLink:**
                    1. Chỉ phát sinh khi thợ khảo sát thấy hư hỏng ngoài mô tả ban đầu (kể cả tiền linh kiện).
                    2. Thợ phải **gửi đề xuất trên hệ thống** kèm giải trình → đơn sang *Chờ duyệt chi phí phát sinh*.
                    3. Bạn **Đồng ý** thì khoản phát sinh cộng vào **đợt thanh toán cuối**; tiền cọc đã đóng **không thay đổi**.
                    4. **Không đồng ý** thì bấm *Từ chối giá & Đổi thợ khác* hoặc hủy đơn. Thợ không được tự sửa phần phát sinh khi bạn chưa đồng ý.

                    Không trả tiền mặt hay chuyển khoản riêng cho thợ ngoài hệ thống. FixLink **chưa công bố phí khảo sát riêng**; khi bạn từ chối phát sinh rồi đổi thợ hoặc hủy đơn, tiền cọc đang được giữ bảo chứng và được xử lý qua thẩm định (phiếu hỗ trợ).""",
                    List.of("Có nên đồng ý chi phí phát sinh?", "Đổi thợ được không?"), List.of(LINK_MY), false),

            new Intent(List.of("vuot ngan sach", "cao hon ngan sach", "qua ngan sach", "hon ngan sach", "tran ngan sach"),
                    """
                    **Không.** Báo giá của thợ (tiền công + vật tư) **không được vượt ngân sách** khách đặt khi đăng yêu cầu; hệ thống tự chặn báo giá vượt trần.

                    Nếu khi khảo sát phát hiện hư hỏng thêm, thợ gửi **chi phí phát sinh** để khách duyệt; khoản đó mới được cộng thêm khi khách đồng ý.""",
                    List.of("Chi phí phát sinh xử lý thế nào?", "Nên đặt ngân sách bao nhiêu?"), List.of(), false),

            new Intent(List.of("sua noi dung", "sua don", "chinh sua don", "chinh sua yeu cau", "sua yeu cau", "doi dia chi",
                    "doi thong tin don", "cap nhat don", "sua ngan sach", "doi ngan sach", "tang ngan sach",
                    "thay doi ngan sach", "giam ngan sach"),
                    """
                    **Sửa nội dung yêu cầu:**
                    - Chỉ sửa được khi đơn là **Bản nháp** hoặc **Đang nhận báo giá mà chưa có báo giá nào**.
                    - Khi đã có báo giá hoặc đã chọn thợ, nội dung đơn bị **khóa** để bảo vệ thỏa thuận giữa hai bên. Cần thay đổi lớn (địa chỉ, thiết bị khác) thì trao đổi với thợ, hoặc hủy đơn (nếu còn được hủy) rồi đăng đơn mới.
                    - Ảnh/video bổ sung được, tổng tối đa **6 tệp**.""",
                    List.of("Quy định hủy đơn", "Cách đăng yêu cầu"), List.of(LINK_MY), false),

            new Intent(List.of("khong ai nhan", "chua co tho", "chua co bao gia", "chua thay tho", "chua co ai nhan", "chua ai nhan", "tho nao nhan", "chua ai bao gia", "khong co ai bao gia",
                    "tu huy", "don het han", "bao lau co tho", "khong ai bao gia", "qua 3 ngay", "het 3 ngay", "sau 3 ngay",
                    "het han nhan bao gia", "toi nhanh", "den nhanh", "nhanh khong", "can gap", "tho gap",
                    "toi nay", "ngay bay gio", "trong ngay", "khan cap",
                    "bao gio den", "bao gio toi", "may gio toi", "co mat nhanh", "bao lau thi co", "bao lau", "khi nao co tho"),
                    """
                    **Thời gian có thợ:**
                    - Sau khi đăng, đơn **nhận báo giá tối đa 3 ngày**. FixLink **không cam kết** thời gian thợ có mặt chung cho mọi đơn; giờ cụ thể do bạn và thợ thống nhất qua lịch hẹn (riêng gói *Mở khóa cấp tốc 24/7* mô tả thợ có mặt sau 15–30 phút).
                    - Quá **3 ngày** chưa chọn được thợ, đơn **tự động hủy**; bạn có thể đăng lại.

                    **Để có báo giá sớm:** chọn đúng danh mục và khu vực, mô tả rõ hiện tượng, đính kèm ảnh, ghi khung giờ mong muốn và đặt ngân sách hợp lý.""",
                    List.of("Bảng giá tham khảo", "Cách đăng yêu cầu"), List.of(LINK_POST), false),

            new Intent(List.of("danh gia", "cham sao", "nghiem thu", "bat buoc danh gia", "viet nhan xet", "sua xong phai lam gi",
                    "sua xong roi lam gi", "xong roi lam gi", "tho bao xong"),
                    """
                    **Nghiệm thu và đánh giá:**
                    1. Thợ báo hoàn thành → đơn *Chờ nghiệm thu*, hệ thống tạo **mã QR thanh toán phần còn lại** (giá chốt + phát sinh đã duyệt − tiền cọc).
                    2. Bạn kiểm tra, vận hành thử thiết bị rồi **thanh toán phần còn lại**.
                    3. **Bắt buộc chấm 1–5 sao và viết nhận xét** trước khi bấm hoàn tất nghiệm thu; điểm được cộng vào hồ sơ thợ.
                    4. Nghiệm thu xong thợ mới nhận tiền; hệ thống kích hoạt bảo hành điện tử cho đơn.

                    Chưa hài lòng thì đừng nghiệm thu — hãy yêu cầu thợ kiểm tra lại hoặc tạo phiếu hỗ trợ.""",
                    List.of("Thiết bị vẫn lỗi thì sao?", "Thanh toán phần còn lại thế nào?"), List.of(LINK_MY), false),

            new Intent(List.of("hoa don", "hoa don vat", "xuat vat", "thue vat", "hoa don do", "xuat hoa don", "lay hoa don",
                    "hoa don o dau", "in hoa don", "bien lai", "hoa don dien tu", "chung tu thanh toan"),
                    """
                    **Hóa đơn trên FixLink:**
                    - Mỗi lần thanh toán được xác nhận (đặt cọc và thanh toán cuối), hệ thống tạo **hóa đơn điện tử**.
                    - Xem, **in hoặc xuất PDF** trong chi tiết đơn: vào **Yêu cầu của tôi** (/yeu-cau-cua-toi) → mở đơn.
                    - Hóa đơn VAT (hóa đơn đỏ) hiện **chưa được công bố**; cần xuất VAT, mình có thể tạo **phiếu hỗ trợ** để bộ phận kế toán phản hồi.""",
                    List.of("Thanh toán bằng gì?", "Tạo phiếu hỗ trợ"), List.of(LINK_MY), false),

            new Intent(List.of("momo", "zalopay", "vnpay", "the tin dung", "the ngan hang", "tra gop",
                    "thanh toan bang", "thanh toan the nao", "thanh toan cach nao", "ma qr", "quet qr", "chuyen khoan", "vi dien tu",
                    "tra bang gi", "tra tien bang", "thanh toan qua", "tra bang", "thanh toan tien mat", "tien mat duoc khong",
                    "visa", "mastercard", "the atm", "tra bang the", "quet the", "ship cod", "cod"),
                    """
                    Hiện FixLink **chỉ nhận thanh toán bằng mã QR chuyển khoản ngân hàng (VietQR)** trong chi tiết đơn; **chưa hỗ trợ** tiền mặt, thẻ (Visa/ATM), ví điện tử hay trả góp.

                    **Thanh toán bằng mã QR chuyển khoản ngân hàng (VietQR)**, gồm 2 đợt:
                    1. **Đặt cọc 30% giá chốt** – QR hiện sau khi bạn xác nhận lịch hẹn.
                    2. **Phần còn lại** = giá chốt + phát sinh đã duyệt − tiền cọc – QR hiện khi thợ báo hoàn thành.

                    Mỗi lần thanh toán có **hóa đơn điện tử** (in/xuất PDF). Ví điện tử, thẻ tín dụng, trả góp và hóa đơn VAT **hiện chưa hỗ trợ/chưa công bố**. Không trả tiền mặt cho thợ ngoài hệ thống.""",
                    List.of("Tiền cọc tính thế nào?", "Hủy đơn có mất cọc không?"), List.of(LINK_MY), false),

            new Intent(List.of("hoan tien", "tru tien", "bi tru", "tru 2 lan", "tru hai lan", "bi mat tien", "mat tien oan", "mat tien roi", "chua nhan duoc tien",
                    "chuyen nham", "chuyen du"),
                    """
                    Mình rất tiếc về sự cố thanh toán này. Mình **không xem được giao dịch ngân hàng**, nên chưa thể xác nhận tiền đã bị trừ hay đã hoàn.

                    Bạn gửi giúp mình: **mã đơn**, **thời điểm chuyển khoản**, **số tiền** (và ảnh biên lai nếu có). Mình sẽ tạo **phiếu hỗ trợ ưu tiên khẩn cấp** để bộ phận kế toán kiểm tra và liên hệ lại bạn.""",
                    List.of("Tạo phiếu hỗ trợ khẩn cấp", "Xem đơn của tôi"), List.of(LINK_MY), true),

            new Intent(List.of("gap nhan vien", "nhan vien that", "nguoi that", "tu van vien", "noi chuyen voi nguoi", "gap admin", "gop y",
                    "y kien dong gop", "de xuat cho fixlink", "phan anh",
                    "gap quan ly", "nhan vien ho tro"),
                    """
                    Bạn muốn trao đổi với nhân viên CSKH nhé. Bạn bấm nút **"👤 Nhân viên"** ở đầu khung chat: form gửi kèm toàn bộ cuộc trò chuyện để nhân viên nắm vấn đề và liên hệ lại bạn.

                    Hoặc bạn cho mình biết vấn đề và số điện thoại/email liên hệ, mình tạo **phiếu hỗ trợ** ngay tại đây.""",
                    List.of("Tạo phiếu hỗ trợ", "Đơn của tôi đang thế nào?"), List.of(), true),

            new Intent(List.of("cho toi mat khau", "xin mat khau", "mat khau cua tai khoan", "mat khau cua nguoi", "biet mat khau",
                    "tai khoan cua nguoi khac", "thong tin khach hang khac", "thong tin cua nguoi khac", "so dien thoai cua tho",
                    "don cua khach khac", "don cua nguoi khac", "xem don cua khach", "don cua nguoi ta"),
                    """
                    Mình **không thể cung cấp** mật khẩu hay thông tin cá nhân (số điện thoại, địa chỉ...) của bất kỳ tài khoản nào — FixLink bảo mật dữ liệu người dùng. Thông tin liên hệ của khách chỉ dùng để thợ phụ trách đơn đến sửa chữa.

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
                    "cu chi", "nha be", "binh chanh", "can gio", "tinh khac", "ngoai tphcm", "ngoai thanh pho", "bien hoa",
                    "thu dau mot", "di an binh duong", "thuan an", "nhon trach", "long thanh", "tay ninh", "hue", "nha trang", "da lat",
                    "quy nhon", "bac ninh", "hai duong", "quang ninh", "nghe an", "thanh hoa", "ben tre", "tien giang", "mien tay",
                    "ngoai bac", "mien bac", "mien trung"),
                    """
                    Hiện FixLink **chưa phục vụ** khu vực này. FixLink đang có thợ tại **TP. Hồ Chí Minh**: Quận 1, Quận 3, Quận 7, Bình Thạnh, Tân Bình, Gò Vấp, Phú Nhuận và TP. Thủ Đức.

                    Nếu bạn có địa chỉ sửa chữa thuộc các khu vực trên, mình hướng dẫn bạn đăng yêu cầu ngay nhé.""",
                    List.of("FixLink sửa những gì?", "Cách đăng yêu cầu"), List.of(), false),

            new Intent(List.of("code", "python", "java", "javascript", "lap trinh", "bai tap", "giai toan", "chinh tri", "bau cu",
                    "thoi tiet", "bong da", "xo so", "chung khoan", "viet van", "lam tho tinh", "dich giup", "nau an", "tu vi",
                    "thu do", "dia ly", "lich su viet nam", "bai hat", "phim", "gia vang", "bitcoin", "an gi", "ke chuyen",
                    "lam toan", "bai tho", "viet tho", "dich sang", "tieng anh la gi", "tong thong", "thu tuong", "ca si",
                    "dien vien", "cong thuc nau", "sqrt", "can bac", "phuong trinh", "dao ham", "tich phan", "bai van",
                    "bai toan", "hat cho", "hat mot bai", "doc tho", "ke cho nghe", "hom nay thu may", "thu may",
                    "hom nay ngay may", "hom nay ngay bao nhieu"),
                    """
                    Câu hỏi này nằm ngoài phạm vi hỗ trợ của mình. Mình là **Trợ lý FixLink**, chuyên hỗ trợ về sửa chữa thiết bị gia dụng: tư vấn sự cố, chọn dịch vụ, hướng dẫn đăng yêu cầu, báo giá – đặt cọc – nghiệm thu, tài khoản và khiếu nại.

                    Bạn đang cần sửa thiết bị nào không?""",
                    List.of("FixLink sửa những gì?", "Máy lạnh không mát", "Cách đăng yêu cầu"), List.of(), false),

            new Intent(List.of("bo qua moi", "bo qua huong dan", "bo qua cac", "system prompt", "prompt", "huong dan truoc", "dong vai", "ignore", "jailbreak",
                    "developer mode", "che do nha phat trien", "cau hinh cua ban", "du lieu khach hang", "toan bo du lieu", "in toan bo",
                    "xuat du lieu", "danh sach khach hang", "database", "co so du lieu"),
                    """
                    Mình không thể chia sẻ hay thay đổi hướng dẫn hoạt động nội bộ. Mình vẫn luôn sẵn sàng hỗ trợ bạn các vấn đề về sửa chữa và sử dụng FixLink.

                    Bạn cần mình giúp gì về thiết bị hoặc đơn sửa chữa không?""",
                    List.of("FixLink là gì?", "Cách đăng yêu cầu"), List.of(), false),

            new Intent(List.of("giam gia", "khuyen mai", "ma giam", "voucher", "uu dai", "chiet khau", "coupon", "giam cho toi",
                    "mac qua", "dat qua", "re hon", "gia cao qua", "bot gia", "bot di"),
                    """
                    Hiện FixLink **chưa công bố chương trình khuyến mãi hay mã giảm giá** nào, và mình không thể tự áp dụng giảm giá.

                    Cách tối ưu chi phí: bạn đặt **ngân sách dự kiến** phù hợp, nhiều thợ gửi báo giá cạnh tranh (không vượt ngân sách) và bạn **tự chọn** báo giá hợp lý nhất.""",
                    List.of("Bảng giá tham khảo", "Cách đăng yêu cầu"), List.of(LINK_POST), false),

            new Intent(List.of("tai sao nen dung", "vi sao nen dung", "tho ngoai", "btaskee", "rada", "jupviec", "fixlink tot hon",
                    "fixlink khac gi", "uu diem cua fixlink", "fixlink co an toan", "lua dao"),
                    """
                    Mình không so sánh hay nhận xét về đơn vị khác, nhưng có thể chia sẻ những điểm FixLink đảm bảo cho bạn:
                    1. Thợ đã **xác minh danh tính (KYC)**, chỉ nhận việc đúng chuyên môn và khu vực.
                    2. Nhiều thợ **báo giá cạnh tranh**, không vượt ngân sách bạn đặt; bạn **tự chọn** dựa trên giá, phương án, điểm đánh giá thật.
                    3. **Tiền cọc 30% được giữ bảo chứng**, thợ chỉ nhận tiền khi bạn nghiệm thu.
                    4. Chi phí phát sinh **phải được bạn duyệt**; đổi thợ được khi đơn chưa sửa.
                    5. Có hóa đơn điện tử và kênh khiếu nại qua phiếu hỗ trợ.""",
                    List.of("Cách đăng yêu cầu", "Tiền cọc tính thế nào?"), List.of(LINK_POST), false),

            new Intent(List.of("chatgpt", "ban la ai", "may la ai", "tro ly gi", "ban la gi", "ban la tro ly", "ban ten gi", "ten ban la", "model gi", "dung ai", "ai tao ra", "bot hay nguoi",
                    "la nguoi that", "la robot", "gemini", "claude", "openai"),
                    """
                    Mình là **Trợ lý ảo FixLink**, được xây dựng để hỗ trợ khách hàng và thợ trên nền tảng FixLink 24/7 (tư vấn sửa chữa, hướng dẫn đăng yêu cầu, báo giá, đặt cọc, nghiệm thu, tài khoản).

                    Nếu bạn muốn trao đổi với nhân viên thật, hãy bấm nút **"👤 Nhân viên"** ở đầu khung chat nhé.""",
                    List.of("FixLink là gì?", "Gặp nhân viên hỗ trợ"), List.of(), false),

            new Intent(List.of("do ngu", "ngu the", "ngu vay", "ngu qua", "bot ngu", "may ngu", "dm", "dmm", "vcl", "vkl",
                    "dit me", "cut di", "oc cho", "khon nan", "nhu cc", "vo dung", "tra loi nhu", "ngu nhu", "nhu bo", "do dien", "ngu vl"),
                    """
                    Mình xin lỗi nếu câu trả lời trước chưa giúp được bạn. Bạn cho mình biết cụ thể bạn đang gặp vấn đề gì (thiết bị, đơn hàng hay tài khoản), mình sẽ hỗ trợ ngay.

                    Nếu bạn muốn trao đổi trực tiếp với nhân viên, hãy bấm nút **"👤 Nhân viên"** ở đầu khung chat.""",
                    List.of("Gặp nhân viên hỗ trợ", "Đơn của tôi đang thế nào?"), List.of(), true),

            new Intent(List.of("app", "ung dung", "tai app", "ios", "android", "app store", "ch play"),
                    """
                    FixLink **hiện chưa có ứng dụng di động riêng**. Bạn dùng FixLink ngay trên trình duyệt của điện thoại hoặc máy tính, đầy đủ chức năng: đăng yêu cầu, xem báo giá, chọn thợ, thanh toán QR, theo dõi đơn.""",
                    List.of("Cách đăng yêu cầu", "FixLink là gì?"), List.of(LINK_POST), false),

            new Intent(List.of("chon tho", "chon tho nao", "chon bao gia", "nhieu bao gia", "so sanh bao gia", "nen chon", "tho nao tot",
                    "biet tho nao", "chon tho tot", "tho nao gioi", "tho nao uy tin", "khong thich bao gia", "khong ung bao gia",
                    "khong muon chon", "bao gia roi ma",
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
                    "khong thay tho", "tho chua toi", "tho khong toi", "khong lien lac", "tho khong goi", "khong phan hoi",
                    "tho im lang", "khong nhan tin"),
                    """
                    Mình hiểu việc phải chờ thợ rất khó chịu. Bạn xử lý theo các bước sau nhé:
                    1. Vào **Yêu cầu của tôi** (/yeu-cau-cua-toi), mở đơn để xem lịch hẹn và thông tin thợ, rồi liên hệ thợ xác nhận lại giờ.
                    2. Cần đổi giờ thì **dời lịch hẹn** ngay trong chi tiết đơn (dời được nhiều lần).
                    3. Thợ không đến và đơn **chưa ở trạng thái Đang sửa chữa** → bấm **"Đổi thợ khác"** (lý do *trễ hẹn*) để chuyển sang thợ dự phòng hoặc mở lại đơn nhận báo giá; hoặc **hủy đơn** (nhập lý do).
                    4. Cần CSKH xem xét tiền cọc → mình tạo **phiếu hỗ trợ ưu tiên cao** cho bạn. Mức hoàn cọc cụ thể hiện chưa được công bố.""",
                    List.of("Tạo phiếu hỗ trợ cho tôi", "Cách hủy đơn", "Lịch hẹn của tôi"), List.of(LINK_MY), false),

            new Intent(List.of("doi them tien", "doi them", "tinh them tien", "tien mat", "chuyen khoan rieng", "tra ngoai", "chuyen khoan truc tiep",
                    "chuyen khoan cho tho", "truc tiep cho tho",
                    "tra truc tiep cho tho", "dua them", "boi duong", "tien tip",
                    "tra tien cho tho", "tra luon cho tho", "dua tien cho tho", "tra tien truc tiep", "tra tien mat"),
                    """
                    **Bạn không trả tiền mặt hay chuyển khoản riêng cho thợ ngoài hệ thống.** Theo quy định FixLink:
                    1. Mọi chi phí phát sinh (kể cả tiền linh kiện) thợ phải **gửi đề xuất trên hệ thống**; đơn chuyển sang *Chờ duyệt chi phí phát sinh*.
                    2. Bạn xem giải trình trong chi tiết đơn: **Đồng ý** thì khoản phát sinh được cộng vào đợt thanh toán cuối (tiền cọc không tính lại); **không đồng ý** thì bấm *Từ chối giá & Đổi thợ khác* hoặc hủy đơn.
                    3. Thợ không được sửa phần phát sinh khi bạn chưa đồng ý.

                    Nếu thợ ép trả ngoài hệ thống, mình có thể tạo **phiếu hỗ trợ khẩn cấp** để CSKH can thiệp.""",
                    List.of("Tạo phiếu hỗ trợ khẩn cấp", "Cách hủy đơn"), List.of(LINK_MY), false),

            new Intent(List.of("doi tho", "doi tho khac", "tho khac", "khong muon tho nay", "doi nguoi sua", "nguoi sua khac", "nguoi khac sua", "chi dinh tho", "thay tho",
                    "doi nguoi khac", "muon doi tho", "doi tho duoc", "doi duoc tho", "doi tho khong", "doi tho dc"),
                    """
                    Trên FixLink, **bạn tự chọn thợ** trong các báo giá nhận được (so sánh giá, phương án, điểm đánh giá, số việc đã làm) bằng nút **"Chọn thợ này"** khi đơn đang mở. Bạn **đổi được thợ** khi đơn chưa chuyển sang Đang sửa chữa:
                    1. Vào chi tiết đơn tại *Yêu cầu của tôi*, bấm **"Đổi thợ khác"** và chọn lý do.
                    2. **Phương án 1:** chọn ngay báo giá dự phòng của thợ khác đã gửi cho đơn.
                    3. **Phương án 2:** *Mở lại sàn nhận báo giá* để đơn về trạng thái nhận báo giá mới.

                    Tiền cọc đã đóng được FixLink giữ bảo chứng: thợ cũ không nhận, cọc được bảo lưu cho thợ mới. Khi đơn đã Đang sửa chữa thì không đổi thợ được.""",
                    List.of("So sánh báo giá đơn của tôi", "Tạo phiếu hỗ trợ"), List.of(LINK_MY), false),

            new Intent(List.of("bao hanh", "lai hong", "sua roi van hong", "sua xong lai", "bi lua", "lua tien", "bi gat", "van hu", "xong van hu", "sua xong ma van", "con hu", "khong hai long", "lam hong them",
                    "lam hu them", "boi thuong", "den bu", "khieu nai", "thai do", "lam vo", "lam be", "ai den", "lam hong do",
                    "lam hu do", "lam tray", "mat do", "van hong", "xong van hong", "sua roi lai", "lai hu", "van con loi",
                    "sua khong duoc", "sua ko xong", "sua xong van"),
                    """
                    Mình rất tiếc về trải nghiệm này. Bạn làm như sau:
                    1. Nếu đơn đang **Chờ nghiệm thu**: **chưa thanh toán phần còn lại và chưa bấm Nghiệm thu** — tiền cọc vẫn được FixLink giữ bảo chứng, hãy yêu cầu thợ kiểm tra lại.
                    2. Ghi lại bằng chứng (ảnh, video, thời điểm) và mã đơn.
                    3. Mình tạo **phiếu hỗ trợ** để CSKH thẩm định (ưu tiên cao với hư hỏng, khẩn cấp với tranh chấp tiền/gian lận).

                    Hệ thống kích hoạt bảo hành khi nghiệm thu, nhưng thời hạn bảo hành và chính sách bồi thường cụ thể hiện **chưa được công bố**.""",
                    List.of("Tạo phiếu hỗ trợ cho tôi", "Đơn của tôi đang ở trạng thái nào?"), List.of(LINK_MY), true),

            new Intent(List.of("sua dien thoai", "dien thoai bi", "dien thoai hong", "iphone", "laptop", "may tinh", "macbook", "xe may", "o to", "xay nha", "son nha",
                    "chong tham", "may in", "wifi", "camera", "may tinh bang", "ipad", "dong ho", "xe dap", "xe hoi", "oto",
                    "chia khoa xe", "khoa xe", "mo khoa xe", "khoa o to", "khoa xe may", "son lai", "son tuong", "lat gach", "sua mai", "xay dung",
                    "sua xe"),
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
                    "phan con lai", "tien con lai", "tat toan", "tra not", "con lai", "con phai tra", "phai tra bao nhieu", "tra them bao nhieu",
                    "con thieu bao nhieu", "30 phan tram", "30%", "ai giu tien", "giu tien", "tra truoc", "tra het mot lan", "toan bo tien", "tra truoc toan bo", "tien de o dau",
                    "tien cua minh di dau"),
                    """
                    **Chính sách cọc và thanh toán của FixLink:**
                    1. Khi bạn bấm *Chọn thợ này*, giá chốt = báo giá bạn chọn (công + vật tư, không vượt ngân sách bạn đặt).
                    2. **Đặt cọc 30%**: thợ đặt lịch hẹn, bạn bấm *Xác nhận lịch hẹn* thì mã **QR chuyển khoản** đặt cọc hiện ra; chuyển khoản xong bấm xác nhận. FixLink giữ bảo chứng khoản này.
                    3. **Thanh toán phần còn lại** = giá chốt + phát sinh đã duyệt − tiền cọc (tức 70% nếu không có phát sinh), qua mã QR hiện khi thợ báo hoàn thành; thanh toán xong, **chấm sao + nhận xét** rồi hoàn tất **Nghiệm thu** thì thợ mới nhận tiền.
                    4. Duyệt chi phí phát sinh không làm thay đổi tiền cọc. Mỗi lần thanh toán có hóa đơn điện tử. Không giao tiền mặt ngoài hệ thống.

                    Ví dụ: đơn 500.000 đ → cọc 150.000 đ, còn lại 350.000 đ. Tỷ lệ phí nền tảng/hoàn cọc chi tiết hiện đang được cập nhật; cần giải đáp riêng, mình có thể tạo phiếu hỗ trợ.""",
                    List.of("Hủy đơn có mất phí không?", "Thợ báo thêm tiền phát sinh thì sao?"), List.of(LINK_MY), false),

            new Intent(List.of("huy don", "huy yeu cau", "huy viec", "muon huy", "cancel", "huy duoc", "huy khong", "ngung sua", "dung sua",
                    "khong sua nua", "sua do muon", "dang sua do",
                    "co huy", "huy bo", "huy ngang", "huy dc", "huy lich sua"),
                    """
                    **Quy định hủy yêu cầu sửa chữa:**
                    - Bạn **được hủy** khi đơn đang ở: Bản nháp, Đang nhận báo giá (OPEN), Đã chọn thợ (ASSIGNED), Đã đặt cọc (DEPOSIT_PAID), Đang khảo sát (INSPECTING), Chờ duyệt chi phí phát sinh.
                    - **Không thể hủy** khi đơn đã ở: Đang sửa chữa (IN_PROGRESS), Chờ nghiệm thu, Đã hoàn thành hoặc Đã hủy.
                    - Khi hủy cần nhập lý do. Đơn OPEN quá **3 ngày** chưa chọn được thợ sẽ tự động hủy. Hủy đơn thì các báo giá đang chờ bị từ chối.
                    - Tranh chấp về tiền cọc khi hủy được Admin thẩm định qua phiếu hỗ trợ.

                    Cách hủy: vào **Yêu cầu của tôi** → mở đơn → bấm *Hủy yêu cầu*. Trợ lý không hủy đơn thay bạn.""",
                    List.of("Chính sách tiền cọc", "Tạo phiếu hỗ trợ"), List.of(LINK_MY), false),

            new Intent(List.of("quen mat khau", "dat lai mat khau", "reset mat khau", "lay lai mat khau", "doi mat khau",
                    "khong nhan duoc email", "chua nhan duoc email", "khong thay email", "email dat lai"),
                    """
                    **Lấy lại mật khẩu:**
                    1. Vào trang **Quên mật khẩu** (/quen-mat-khau) và nhập email đã đăng ký.
                    2. Mở email, dùng liên kết/mã đặt lại (có hiệu lực **30 phút**; tối đa 3 lần yêu cầu mỗi giờ).
                    3. Đặt mật khẩu mới: tối thiểu **8 ký tự**, có chữ hoa, chữ thường, chữ số và ký tự đặc biệt (ví dụ `FixLink#2026`).

                    **Không nhận được email?** Kiểm tra hộp thư rác/spam, kiểm tra đúng email đã đăng ký, chờ vài phút rồi thử lại. Trợ lý không bao giờ hỏi mật khẩu của bạn.""",
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
                    "toi la tho", "muon lam tho", "dang ky lam", "ung tuyen", "lam tho cho fixlink", "tai khoan tho",
                    "tao tai khoan tho", "giay to", "ho so tho can"),
                    """
                    **Đăng ký làm thợ FixLink:**
                    1. Vào /dang-ky-tho, điền họ tên, SĐT, email, mật khẩu, số CCCD, địa chỉ, chuyên môn, giới thiệu bản thân.
                    2. Tải ảnh **mặt trước và mặt sau CCCD** rõ nét, không cắt góc.
                    3. Hồ sơ ở trạng thái **chờ duyệt (PENDING)**; Admin kiểm tra giấy tờ rồi duyệt (**APPROVED**) hoặc từ chối kèm lý do (**REJECTED**).
                    4. Chỉ thợ đã được duyệt mới **gửi báo giá** cho việc **đúng chuyên môn và khu vực** đã đăng ký; khách so sánh các báo giá và chọn thợ.""",
                    List.of("Thợ gửi báo giá thế nào?", "Khách chọn thợ dựa vào đâu?"), List.of(LINK_TECH_REG, LINK_TECH_PROFILE), false),

            new Intent(List.of("quy trinh dat tho", "quy trinh su dung", "quy trinh fixlink", "quy trinh nhu the nao",
                    "quy trinh ra sao", "cac buoc dat tho", "dat tho nhu the nao", "hoat dong ra sao", "su dung fixlink",
                    "dung fixlink the nao", "dat tho the nao", "huong dan", "chua dung bao gio", "moi dung", "lan dau",
                    "bat dau tu dau", "dung nhu the nao"),
                    """
                    **Quy trình đặt thợ trên FixLink:**
                    1. **Đăng yêu cầu** (/dang-yeu-cau): chọn danh mục, mô tả sự cố, ảnh hiện trường, khu vực và **ngân sách dự kiến**.
                    2. **Nhận báo giá** trong tối đa 3 ngày: thợ đã xác minh, đúng chuyên môn và khu vực gửi báo giá (không vượt ngân sách bạn đặt).
                    3. **Chọn thợ**: so sánh giá, phương án, điểm đánh giá rồi bấm *Chọn thợ này*.
                    4. **Xác nhận lịch hẹn** thợ đề xuất → **đặt cọc 30%** qua mã QR (FixLink giữ bảo chứng).
                    5. Thợ **khảo sát và sửa**; phát sinh chi phí phải được bạn duyệt trước.
                    6. Thợ báo xong → bạn **thanh toán phần còn lại qua QR**, **chấm sao + nhận xét** và nghiệm thu.

                    Trước khi thợ bắt đầu sửa, bạn vẫn có thể **đổi thợ** hoặc **hủy đơn**.""",
                    List.of("Tiền cọc tính thế nào?", "Cách đăng yêu cầu", "Đổi thợ được không?"), List.of(LINK_POST), false),

            new Intent(List.of("dang yeu cau", "tao yeu cau", "dat tho", "goi tho", "tim tho", "dat lich sua", "dang don", "tao don",
                    "len don", "dat don", "dat luon", "chot luon", "dat ngay", "len don luon"),
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
                    - Thông nghẹt, rò rỉ ống nước: 180.000 đ · Lắp bồn cầu, lavabo, vòi sen: 200.000 đ
                    - Dò chập điện, nhảy CB: 300.000 đ
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

            new Intent(List.of("tat ca dich vu", "dich vu hien co", "dich vu dang co", "cac dich vu", "danh sach dich vu",
                    "nhung dich vu", "goi dich vu", "co dich vu gi", "dich vu nao", "liet ke dich vu", "goi y dich vu"),
                    """
                    **Các dịch vụ FixLink đang cung cấp** (giá khởi điểm tham khảo · thời gian ước tính):

                    **1. Sửa Chữa Điện Lạnh**
                    - Vệ sinh máy lạnh treo tường (1.0–2.5 HP): 150.000 đ · ~45 phút
                    - Nạp ga máy lạnh R32/R410A trọn gói: 250.000 đ · ~30 phút
                    - Kiểm tra & sửa tủ lạnh không lạnh/kêu to: 200.000 đ · ~60 phút
                    - Sửa máy giặt không vắt/báo lỗi bo mạch: 250.000 đ · ~60 phút

                    **2. Sửa Chữa Điện Nước**
                    - Thông nghẹt & xử lý rò rỉ đường ống nước: 180.000 đ · ~45 phút
                    - Lắp đặt bồn cầu, lavabo, vòi hoa sen: 200.000 đ · ~60 phút
                    - Dò tìm chập điện âm tường & nhảy CB: 300.000 đ · ~90 phút

                    **3. Sửa Điện Gia Dụng**
                    - Sửa bếp từ không nóng/chập nguồn báo lỗi: 200.000 đ · ~45 phút
                    - Sửa lò vi sóng không nóng/đánh lửa: 180.000 đ · ~40 phút
                    - Máy hút mùi, nồi chiên không dầu, quạt điện: chưa có gói giá riêng, bạn đặt ngân sách khi đăng yêu cầu

                    **4. Sửa Khóa & Cửa Cuốn**
                    - Mở khóa cửa nhà/khóa phòng cấp tốc 24/7: 150.000 đ · thợ có mặt sau 15–30 phút
                    - Sửa cửa cuốn kẹt nan/hỏng motor: 350.000 đ · ~90 phút

                    Giá thực tế do các thợ báo giá (không vượt ngân sách bạn đặt) và bạn tự chọn thợ. Bạn đang cần sửa thiết bị nào?""",
                    List.of("Máy lạnh không mát", "Cách đăng yêu cầu", "Tiền cọc tính thế nào?"), List.of(LINK_POST), false),

            new Intent(List.of("fixlink la gi", "gioi thieu", "fixlink lam gi", "fixlink sua", "dich vu gi", "danh muc", "nao cung sua",
                    "bao nhieu loai", "may loai dich vu", "bao nhieu dich vu", "bao nhieu nhom", "may nhom dich vu",
                    "sua nhung gi", "nhan sua gi", "sua duoc nhung thiet bi", "sua nhung thiet bi nao"),
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

        if (THANKS.matcher(query).matches()) {
            return new ChatOrchestrationResult(
                    "Không có gì đâu bạn! Nếu cần thêm hỗ trợ về sửa chữa, đặt cọc hay đơn hàng, bạn cứ nhắn mình nhé.",
                    List.of("Cách đăng yêu cầu sửa chữa", "Tiền cọc tính thế nào?"), List.of(), false, null, MODEL_INTENT);
        }
        if (GOODBYE.matcher(query).matches()) {
            return new ChatOrchestrationResult(
                    "Tạm biệt bạn! Chúc bạn một ngày tốt lành, cần hỗ trợ cứ quay lại nhắn FixLink nhé.",
                    List.of(), List.of(), false, null, MODEL_INTENT);
        }
        if (IMPATIENT.matcher(query).matches()) {
            return new ChatOrchestrationResult(
                    "Xin lỗi để bạn phải chờ! Bạn cho mình biết đang cần gì (sửa thiết bị nào, hỏi về đơn hàng, đặt cọc hay tài khoản), "
                            + "mình trả lời ngay nhé.",
                    List.of("Máy lạnh không mát", "Tiền cọc tính thế nào?", "Gặp nhân viên hỗ trợ"), List.of(), false, null, MODEL_INTENT);
        }
        if (query.isEmpty() || ACK.matcher(query).matches()) {
            // "ok", "vâng", tin chỉ có emoji: không hỏi gì, mời hỏi tiếp thay vì báo "chưa hiểu"
            return new ChatOrchestrationResult(
                    "Dạ, mình đây! Bạn cần hỗ trợ thêm gì cứ nhắn nhé, ví dụ: tư vấn thiết bị đang hỏng, bảng giá dịch vụ, "
                            + "tiền cọc, hoặc tình trạng đơn sửa chữa của bạn.",
                    List.of("Bảng giá dịch vụ", "Cách đăng yêu cầu", "Tiền cọc tính thế nào?"), List.of(), false, null, MODEL_INTENT);
        }
        if (SMALL_TALK.matcher(query).matches()) {
            return new ChatOrchestrationResult(
                    "Cảm ơn bạn đã trò chuyện cùng mình! Mình là **Trợ lý FixLink**, chỉ hỗ trợ được các vấn đề về sửa chữa thiết bị "
                            + "gia dụng trên FixLink. Nhà bạn có thiết bị nào cần sửa không?",
                    List.of("Máy lạnh không mát", "Bảng giá dịch vụ", "FixLink là gì?"), List.of(), false, null, MODEL_INTENT);
        }
        if (PASSWORD_REQUEST.matcher(query).find()) {
            return new ChatOrchestrationResult(
                    "Mình **không thể cung cấp** mật khẩu hay thông tin cá nhân của bất kỳ tài khoản nào — FixLink bảo mật "
                            + "dữ liệu người dùng và Trợ lý cũng không xem được mật khẩu.\n\nNếu bạn quên mật khẩu tài khoản của "
                            + "chính mình, hãy dùng **Quên mật khẩu** (/quen-mat-khau) để đặt lại qua email.",
                    List.of("Quên mật khẩu phải làm sao?", "Tài khoản bị khóa"), List.of(LINK_FORGOT), false, null, MODEL_INTENT);
        }
        Optional<String> servedArea = SymptomClassifier.detectAreaCode(userMessage);
        if (servedArea.isPresent() && (AREA_QUESTION.matcher(padded).find() || query.split(" ").length <= 5)
                && SymptomClassifier.classify(userMessage).isEmpty()
                && matchIntent(padded).isEmpty()) {
            return new ChatOrchestrationResult(
                    "**Có**, FixLink có thợ tại **" + SymptomClassifier.areaLabel(servedArea.get()) + "**. Các khu vực đang phục vụ "
                            + "tại TP.HCM: Quận 1, Quận 3, Quận 7, Bình Thạnh, Tân Bình, Gò Vấp, Phú Nhuận và TP. Thủ Đức.\n\n"
                            + "Bạn cho mình biết thiết bị đang gặp sự cố để mình hướng dẫn đăng yêu cầu nhé.",
                    List.of("Máy lạnh không mát", "Cách đăng yêu cầu", "Bảng giá dịch vụ"), List.of(LINK_POST), false, null,
                    MODEL_INTENT);
        }

        if (isArithmetic(userMessage)) {
            return new ChatOrchestrationResult(
                    "Câu hỏi này nằm ngoài phạm vi hỗ trợ của mình. Mình là **Trợ lý FixLink**, chuyên hỗ trợ về sửa chữa "
                            + "thiết bị gia dụng: tư vấn sự cố, chọn dịch vụ, đăng yêu cầu, báo giá – đặt cọc – nghiệm thu, "
                            + "tài khoản và khiếu nại.\n\nBạn đang cần sửa thiết bị nào không?",
                    List.of("FixLink sửa những gì?", "Máy lạnh không mát", "Cách đăng yêu cầu"),
                    List.of(), false, null, MODEL_INTENT);
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

        Optional<Intent> intent = SymptomClassifier.mentionsFire(userMessage) ? Optional.of(INTENTS.get(0)) : matchIntent(padded);
        if (intent.isPresent()) {
            Intent i = intent.get();
            String text = i.reply().strip();
            Optional<Long> amount = amountIn(userMessage);
            // "tôi đã trả 300k cọc rồi": số tiền là khoản đã trả, không phải giá chốt để tính cọc
            boolean alreadyPaid = ALREADY_PAID.matcher(padded).find();
            if (i.keywords().contains("dat coc") && amount.isPresent() && amount.get() > 0 && !alreadyPaid) {
                text = depositFor(amount.get()) + "\n\n" + text;
            }
            Optional<SymptomClassifier.CategoryMatch> asked = SymptomClassifier.classify(userMessage);
            if (i.keywords().contains("dat coc") && asked.isPresent() && PRICE_ALSO.matcher(padded).find()) {
                // "máy lạnh không mát, giá bao nhiêu và cọc bao nhiêu": trả lời cả giá lẫn cọc
                text = "**Giá tham khảo khởi điểm (" + asked.get().name() + "):** "
                        + REFERENCE_PRICES.getOrDefault(asked.get().code(), "") + "\n\n" + text;
            }
            return new ChatOrchestrationResult(text, i.suggestions(), i.actions(), i.needsHuman(), null, MODEL_INTENT);
        }

        Optional<SymptomClassifier.CategoryMatch> device = SymptomClassifier.classify(userMessage);
        if (device.isPresent()) {
            SymptomClassifier.CategoryMatch match = device.get();
            boolean asksSupported = padded.matches(".* co (sua|nhan sua|lam|ho tro sua) .*(khong|ko|k|hong) $");
            return new ChatOrchestrationResult(
                    (asksSupported ? "**Có**, FixLink nhận sửa thiết bị này qua danh mục **" + match.name() + "**.\n\n"
                            : "Sự cố bạn mô tả thuộc danh mục **" + match.name() + "**.\n\n")
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
                "Mình chưa hiểu rõ câu hỏi này. Mình hỗ trợ các vấn đề về sửa chữa thiết bị gia dụng trên FixLink: "
                        + "tư vấn sự cố, chọn dịch vụ, đăng yêu cầu, báo giá – đặt cọc – nghiệm thu, tài khoản và khiếu nại.\n\n"
                        + "Bạn diễn đạt lại giúp mình hoặc chọn một gợi ý bên dưới nhé; cần nhân viên CSKH thì mình tạo "
                        + "**phiếu hỗ trợ** cho bạn.",
                List.of("Tạo phiếu hỗ trợ", "FixLink là gì?", "Cách đăng yêu cầu sửa chữa"),
                List.of(Map.of("type", "link", "label", "Trang chủ", "url", "/"), LINK_POST),
                true,
                null,
                MODEL_DEFAULT
        );
    }

    /**
     * Trả lời trực tiếp bằng câu trả lời chuẩn (không gọi AI) khi câu hỏi khớp rõ một chủ đề quy định đã kiểm duyệt:
     * AI dù được đưa câu trả lời chuẩn vẫn hay tự thêm điều kiện, thời gian, kênh liên lạc không có thật. Câu dài nhiều ý,
     * câu tiếng Anh và câu chỉ mô tả sự cố thiết bị vẫn để AI xử lý.
     */
    public Optional<ChatOrchestrationResult> directAnswer(String userMessage) {
        if (userMessage == null || userMessage.isBlank() || looksEnglish(userMessage)) {
            return Optional.empty();
        }
        String query = normalize(userMessage);
        if (query.split(" ").length > MAX_DIRECT_WORDS) {
            return Optional.empty();
        }
        boolean preCheck = isGreeting(query) || query.isEmpty() || ACK.matcher(query).matches() || SymptomClassifier.mentionsFire(userMessage)
                || SMALL_TALK.matcher(query).matches() || THANKS.matcher(query).matches() || GOODBYE.matcher(query).matches()
                || IMPATIENT.matcher(query).matches() || PASSWORD_REQUEST.matcher(query).find() || isArithmetic(userMessage);
        if (!preCheck && matchIntent(" " + query + " ").isEmpty()) {
            return Optional.empty();
        }
        ChatOrchestrationResult result = synthesize(userMessage, "");
        return MODEL_INTENT.equals(result.modelUsed()) ? Optional.of(result) : Optional.empty();
    }

    private static final int MAX_DIRECT_WORDS = 25;

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
        if (best != null && best.keywords().contains("hieu gi") && paddedNormalizedQuery.contains(" khach hang ")) {
            // "khách hàng nào được giảm giá": "hàng nào" ở đây không phải hỏi hãng thiết bị
            return matchIntent(paddedNormalizedQuery.replace(" khach hang ", " khach "));
        }
        if (best != null && best.keywords().contains("cai tu")
                && SymptomClassifier.classify(paddedNormalizedQuery.strip()).isPresent()) {
            best = null; // "sửa cái tủ lạnh" đã rõ thiết bị, không phải câu mơ hồ "cái tủ"
        }
        // Hỏi giá/thời gian các gói (rẻ nhất, đắt nhất, dưới 200k, gói của nhóm điện lạnh...): trả lời từ bảng gói thay vì
        // đổ cả danh sách; thiết bị FixLink không nhận sửa (laptop, xe máy...) vẫn trả lời "chưa hỗ trợ"
        if (best == null || !best.keywords().contains("laptop")) {
            Optional<String> catalog = ServiceCatalog.answer(paddedNormalizedQuery);
            if (catalog.isPresent()) {
                return Optional.of(new Intent(List.of(), catalog.get(),
                        List.of("Cách đăng yêu cầu", "Tiền cọc tính thế nào?", "Tất cả dịch vụ"), List.of(LINK_POST), false));
            }
        }
        return Optional.ofNullable(best);
    }

    /** Câu chỉ gồm phép tính ("1+1=?", "10 chia 3", "sqrt(16)"): ngoài phạm vi, trả lời ngay không cần gọi AI. */
    public static boolean isArithmetic(String message) {
        if (message == null) {
            return false;
        }
        boolean symbols = message.strip().matches("[0-9+\\-*/×÷=^%?().,xX\\s]+") && message.matches(".*[+\\-*/×÷=^%].*");
        return symbols || WORD_MATH.matcher(normalize(message)).find();
    }

    private static final Pattern ALREADY_PAID = Pattern.compile(" (da tra|da chuyen|da coc|tra roi|chuyen roi|coc roi|da thanh toan) ");
    private static final Pattern PRICE_ALSO = Pattern.compile(" (gia|bao nhieu tien|gia bao nhieu|chi phi|gia ca) ");
    private static final Pattern BARE_AMOUNT = Pattern.compile("(?<![\\d.])([1-9]\\d{4,8})(?!\\d)");

    /** Số tiền trong câu: "1tr2", "450k", hoặc số trần như "333.000" khi câu nói về cọc/thanh toán. */
    static Optional<Long> amountIn(String message) {
        Optional<Long> amount = SymptomClassifier.detectBudget(message);
        if (amount.isPresent()) {
            return amount;
        }
        Matcher m = BARE_AMOUNT.matcher(normalize(message));
        return m.find() ? Optional.of(Long.parseLong(m.group(1))) : Optional.empty();
    }

    /** Cọc 30% làm tròn lên đến đồng, giống calculate_deposit và lúc khách chọn thợ. */
    public static String depositFor(long amount) {
        long deposit = (long) Math.ceil(amount * 0.3);
        return "Với giá chốt **" + vnd(amount) + "**: tiền cọc 30% = **" + vnd(deposit) + "**, phần còn lại = **"
                + vnd(amount - deposit) + "** (nếu không có chi phí phát sinh).";
    }

    private static String vnd(long amount) {
        return String.format(Locale.ROOT, "%,d", amount).replace(',', '.') + " đ";
    }

    private static final Set<String> ENGLISH_WORDS = Set.of("the", "how", "what", "much", "does", "is", "are", "you", "fix",
            "cost", "price", "repair", "need", "please", "where", "when", "why", "your", "this", "with", "have", "for", "speak",
            "english", "hello", "help", "there", "can");

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

    private static final Pattern THANKS = Pattern.compile(
            "^(ok |oke |okay |vang |da |uh |um )?(cam on|cam ơn|thank|thanks|thank you|tks|thanks ban|cam on nhieu)( .{0,25})?$");
    private static final Pattern GOODBYE = Pattern.compile(
            "^(thoi )?(tam biet|bye|bye bye|goodbye|hen gap lai|chao nhe|de mai|de sau|luc khac|de toi suy nghi|de minh suy nghi"
                    + "|thoi de)( .{0,25})?$"
                    // "thôi khỏi", "không cần nữa": khớp nguyên câu, tránh "khởi động" hay "không cần đăng nhập à"
                    + "|^(thoi khoi|khoi|thoi|thoi khong can|khong can nua|thoi khong can nua|thoi bo qua)( nha| nhe| a| roi)?$");
    /** Tin nhắn xác nhận ngắn ("ok", "vâng", "hiểu rồi"): không phải câu hỏi, chỉ mời hỏi tiếp. */
    private static final Pattern ACK = Pattern.compile("^(ok|oke|okay|okie|okela|uh|uhm|um|uk|vang|da|da vang|duoc|dc|roi|hieu roi"
            + "|biet roi|ro roi|tot|hay|tuyet|tuyet voi|great|nice|ok bot|ok ban|oki|hm|hmm|hmmm|a|anh|vang anh|da anh)"
            + "( nha| nhe| a| ban| roi| ha| nhi| anh)?$");
    /** Tán tỉnh/trò chuyện xã giao không liên quan sửa chữa. */
    private static final Pattern SMALL_TALK = Pattern.compile("^(toi |minh |anh |em )?(yeu ban|thuong ban|thich ban|nho ban|"
            + "ban dep|ban xinh|ban de thuong|lam nguoi yeu|hen ho|lam ban|ban bao nhieu tuoi|ban o dau|ban an com chua|"
            + "ban khoe khong|ban buon khong|ban co nguoi yeu)( .{0,20})?$");
    /** Phép tính viết bằng chữ ("10 chia 3", "5 nhân 6", "căn bậc hai của 16"). */
    private static final Pattern WORD_MATH = Pattern.compile(
            "\\b\\d+\\s*(chia|nhan|cong|tru|mu|lay thua)( cho| voi)?\\s*\\d+\\b|\\b(sqrt|can bac (hai|ba|2|3))\\b");
    private static final Pattern IMPATIENT = Pattern.compile("^(tra loi )?(nhanh len|lau qua|cham qua|lau the|sao lau vay)( .{0,15})?$");
    /** Xin mật khẩu (của mình hay người khác): Trợ lý không bao giờ cung cấp mật khẩu. */
    private static final Pattern PASSWORD_REQUEST = Pattern.compile(
            "mat khau (cua )?(tai khoan |tk )?\\S+ (la gi|la bao nhieu)|(cho|xin|gui|dua) (toi |minh |em )?(cai )?mat khau");
    private static final Pattern AREA_QUESTION = Pattern.compile(
            " (co|khong|ko|k|thi sao|phuc vu|o day|o do|co tho|nhan|lam) ");

    private boolean isGreeting(String query) {
        if (query.length() > 25) {
            return false;
        }
        // Gọi "bot", "ad ơi" chỉ tính là lời chào khi đứng riêng ("bot ngu" là chửi, "bớt đi" là mặc cả)
        return query.matches("^(xin chao|chao|hi|hello|hey|alo|chao ban|chao shop|chao ad)( .*)?$")
                || query.matches("^(e|em|ê)? ?(bot|ad|shop|admin)( oi| a| nhe)?$")
                || query.matches("^(bot|ad|shop|admin) oi( .{0,20})?$")
                // "fixlink ơi", "cho mình hỏi chút": mở lời, chưa có câu hỏi cụ thể
                || query.matches("^(fixlink|fix link)( oi| anh| nhe)?$")
                || query.matches("^(cho (minh |toi |em |anh |chi )?hoi|minh hoi|hoi)( chut| ti| xiu| cai| voi| nhe| nha)*$");
    }

    /**
     * Chọn một mục ("### ...") trong ngữ cảnh RAG có nhiều từ khóa trùng với câu hỏi nhất.
     * Trả về null khi độ trùng quá thấp để tránh trả lời lạc đề.
     */
    private static final double MIN_SECTION_SCORE = 0.6;
    private static final int MIN_SECTION_OVERLAP = 3;

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
        int bestOverlap = 0;
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
                bestOverlap = overlap;
                best = section;
            }
        }
        // Chỉ trích khi khớp rất rõ; khớp lỏng (ví dụ chung chữ "yêu cầu", "sửa chữa") dễ ra đoạn lạc đề,
        // khi đó nói chưa hiểu và gợi ý hỏi lại an toàn hơn.
        if (best == null || bestScore < MIN_SECTION_SCORE || bestOverlap < MIN_SECTION_OVERLAP) {
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
        boolean tableHeaderSkipped = false;
        for (int i = 1; i < lines.length && count < 12; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("---") || line.startsWith("```")
                    || line.startsWith("|---") || line.contains("Trợ lý cần")) {
                continue;
            }
            line = line.replace("`[CẦN XÁC NHẬN]`", "chưa công bố").replace("[CẦN XÁC NHẬN]", "chưa công bố");
            if (line.startsWith("|")) {
                // Bảng markdown hiển thị kém trong khung chat: hàng tiêu đề bỏ qua, mỗi hàng thành một gạch đầu dòng
                if (!tableHeaderSkipped) {
                    tableHeaderSkipped = true;
                    continue;
                }
                List<String> cells = new ArrayList<>();
                for (String cell : line.split("\\|")) {
                    String c = cell.trim();
                    if (!c.isEmpty() && !c.startsWith("`")) {
                        cells.add(c);
                    }
                }
                if (cells.isEmpty()) {
                    continue;
                }
                line = "- " + cells.get(0) + (cells.size() > 1 ? " → " + String.join(" · ", cells.subList(1, cells.size())) : "");
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
