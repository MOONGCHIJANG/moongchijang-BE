package com.moongchijang.global.exception

enum class ErrorCode(val code: Int, val message: String, val httpStatus: Int) {
    // 100 Test
    TEST_ERROR(100_000, "테스트 에러입니다.", 200),

    // 400 Bad Request
    BAD_REQUEST(400_000, "잘못된 요청입니다.", 400),
    INVALID_FILE_FORMAT(400_001, "업로드된 파일 형식이 올바르지 않습니다.", 400),
    INVALID_INPUT(400_002, "입력값이 올바르지 않습니다.", 400),
    NULL_VALUE(400_003, "Null 값이 들어왔습니다.", 400),

    // 401 Unauthorized
    INVALID_CREDENTIALS(401_004, "인증 정보가 올바르지 않습니다.", 401),
    INVALID_LOGIN(401_008, "로그인이 필요합니다.", 401),

    // 403 Forbidden
    FORBIDDEN(403_000, "접속 권한이 없습니다.", 403),
    ACCESS_DENY(403_001, "접근이 거부되었습니다.", 403),
    UNAUTHORIZED_POST_ACCESS(403_002, "해당 게시글에 접근할 권한이 없습니다.", 403),

    // 404 Not Found
    NOT_FOUND_END_POINT(404_000, "요청한 대상이 존재하지 않습니다.", 404),
    POST_NOT_FOUND(404_003, "요청한 게시글을 찾을 수 없습니다.", 404),
    POST_TYPE_NOT_FOUND(404_004, "게시글 타입을 찾을 수 없습니다.", 404),
    COMMENT_NOT_FOUND(404_005, "요청한 댓글을 찾을 수 없습니다.", 404),

    // 409 Conflict

    // GroupBuyRequest
    GROUPBUY_REQUEST_NOT_FOUND(404_010, "공구 요청을 찾을 수 없습니다.", 404),
    GROUPBUY_REQUEST_FORBIDDEN(403_010, "본인의 공구 요청만 조회할 수 있습니다.", 403),
    GROUPBUY_REQUEST_INVALID_DATE(400_010, "희망 픽업 날짜는 오늘 이후여야 합니다.", 400),
    GROUPBUY_REQUEST_INVALID_STATUS_TRANSITION(400_011, "허용되지 않는 공구 요청 상태 전이입니다.", 400),
    GROUPBUY_REQUEST_REJECTION_REASON_REQUIRED(400_012, "거절 상태로 변경할 때는 거절 사유가 필요합니다.", 400),
    GROUPBUY_REQUEST_OPENED_GROUP_BUY_REQUIRED(400_013, "개설 완료 상태로 변경할 때는 개설된 공구 ID가 필요합니다.", 400),
    GROUPBUY_REQUEST_APPROVAL_STORE_REQUIRED(400_014, "공구 개설에 필요한 매장 정보가 누락되었습니다.", 400),
    GROUPBUY_REQUEST_APPROVAL_INVALID_PRICE(400_015, "공구 가격 조건이 올바르지 않습니다.", 400),
    GROUPBUY_REQUEST_APPROVAL_INVALID_QUANTITY(400_016, "공구 수량 조건이 올바르지 않습니다.", 400),
    GROUPBUY_REQUEST_APPROVAL_INVALID_PERIOD(400_017, "공구 모집 기간이 올바르지 않습니다.", 400),
    GROUPBUY_REQUEST_APPROVAL_INVALID_PICKUP(400_018, "공구 픽업 정보가 올바르지 않습니다.", 400),
    GROUPBUY_REQUEST_APPROVAL_STORE_REGION_MISMATCH(400_019, "매장 지역 정보가 일치하지 않습니다.", 400),

    // GroupBuyOpenRequest
    DUPLICATE_OPEN_REQUEST(409_010, "이미 알림 신청한 공구입니다.", 409),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(500_000, "서버 내부 오류입니다.", 500),
    
    // Auth, User (100~149)
    INVALID_NICKNAME_FORMAT(400_101, "2~10자, 한글/영문/숫자만 입력 가능해요.", 400),
    INVALID_PHONE_NUMBER_FORMAT(400_102, "올바른 전화번호를 입력해주세요.", 400),
    INVALID_EMAIL_FORMAT(400_103, "올바른 이메일 형식이 아니에요.", 400),
    EMAIL_VERIFICATION_CODE_MISMATCH(400_104, "인증코드가 일치하지 않아요.", 400),
    EMAIL_VERIFICATION_CODE_EXPIRED(400_105, "인증코드가 만료됐어요. 재발송해주세요.", 400),
    EMAIL_VERIFICATION_RESEND_COOLDOWN(400_106, "인증코드 재발송은 잠시 후 다시 시도해주세요.", 400),
    INVALID_PASSWORD_FORMAT(400_107, "8자 이상, 영문+숫자를 포함해주세요.", 400),
    INVALID_PASSWORD_SAME_AS_EMAIL_ID(400_108, "이메일 아이디와 동일한 비밀번호는 사용할 수 없습니다.", 400),
    EMAIL_PASSWORD_CHANGE_NOT_ALLOWED(403_109, "이메일 로그인 사용자만 비밀번호를 변경할 수 있습니다.", 403),
    PASSWORD_CHANGE_CURRENT_PASSWORD_MISMATCH(401_109, "현재 비밀번호가 일치하지 않습니다.", 401),
    INVALID_BUSINESS_REGISTRATION_NUMBER_FORMAT(400_110, "올바른 사업자등록번호를 입력해주세요.", 400),
    SELLER_BUSINESS_INFO_REQUIRED(400_111, "사장님 사업자 정보를 먼저 입력해주세요.", 400),
    OWNER_WITHDRAWAL_REASON_DETAIL_REQUIRED(400_112, "기타 탈퇴 사유를 입력해주세요.", 400),
    OWNER_WITHDRAWAL_BLOCKED_OPEN_GROUPBUY(409_105, "개설된 공구가 있어 탈퇴할 수 없어요.", 409),
    OWNER_WITHDRAWAL_BLOCKED_PENDING_CUSTOMER_PICKUP(409_106, "달성 완료 공구의 픽업이 모두 완료된 후 탈퇴할 수 있어요.", 409),
    OWNER_REFUND_REVIEW_ALREADY_PROCESSED(409_107, "이미 처리된 환불 요청입니다.", 409),
    EMAIL_VERIFICATION_DAILY_LIMIT_EXCEEDED(429_101, "내일 다시 시도해주세요.", 429),
    INVALID_SIGNUP_TOKEN(401_107, "회원가입 인증정보가 유효하지 않습니다. 이메일 인증을 다시 진행해주세요.", 401),
    KAKAO_TOKEN_REQUEST_INVALID(401_101, "카카오 토큰 요청 파라미터가 올바르지 않습니다.", 401),
    KAKAO_TOKEN_EXCHANGE_FAILED(401_102, "카카오 토큰 교환에 실패했습니다.", 401),
    KAKAO_TOKEN_RESPONSE_INVALID(401_103, "카카오 토큰 응답이 올바르지 않습니다.", 401),
    KAKAO_ACCESS_TOKEN_MISSING(401_104, "카카오 액세스 토큰이 응답에 없습니다.", 401),
    KAKAO_USER_INFO_FETCH_FAILED(401_105, "카카오 사용자 정보 조회에 실패했습니다.", 401),
    KAKAO_USER_INFO_INVALID(401_106, "카카오 사용자 정보가 유효하지 않습니다.", 401),
    USER_NOT_FOUND(404_101, "사용자를 찾을 수 없습니다.", 404),
    USER_NOT_FOUND_IN_COOKIE(404_102, "쿠키에서 사용자 정보를 찾을 수 없습니다.", 404),
    DUPLICATE_EMAIL(409_101, "이미 사용 중인 이메일입니다.", 409),
    REJOIN_NOT_AVAILABLE_YET(409_102, "탈퇴 후 30일 이후에 다시 가입할 수 있습니다.", 409),
    DUPLICATE_NICKNAME(409_103, "이미 사용 중인 닉네임이에요.", 409),
    WITHDRAWAL_BLOCKED_PENDING_PICKUP(409_104, "수령 예정인 공구가 있어요. 카카오 채널로 문의해주세요.", 409),
    WITHDRAWAL_REASON_DETAIL_REQUIRED(400_109, "기타 탈퇴 사유를 입력해주세요.", 400),

    // Token (150~199)
    TOKEN_EXPIRED(401_151, "토큰이 만료되었습니다.", 401),
    TOKEN_INVALID(401_152, "유효하지 않은 토큰입니다.", 401),
    TOKEN_NOT_FOUND(401_153, "토큰이 존재하지 않습니다.", 401),
    TOKEN_UNSUPPORTED(401_154, "지원하지 않는 토큰 형식입니다.", 401),
    INVALID_REFRESH_TOKEN(401_155, "재발급 토큰이 유효하지 않습니다.", 401),
    INVALID_ACCESS_TOKEN(401_156, "접근 토큰이 유효하지 않습니다.", 401),
    INVALID_TOKEN(401_157, "토큰이 생성되지 않았습니다.", 401),
    REFRESH_TOKEN_MISMATCH(401_158, "저장된 리프레시 토큰과 일치하지 않습니다.", 401),
    EXPIRED_REFRESH_TOKEN(401_159, "리프레시 토큰이 만료되었습니다.", 401),
    REFRESH_TOKEN_NOT_FOUND(401_160, "저장된 리프레시 토큰이 존재하지 않습니다.", 401),

    // Verification (200~249)
    PHONE_VERIFICATION_CODE_NOT_FOUND(400_201, "인증번호가 만료되었거나 존재하지 않습니다.", 400),
    PHONE_VERIFICATION_CODE_MISMATCH(400_202, "인증번호가 올바르지 않습니다.", 400),
    PHONE_VERIFICATION_REQUIRED(400_203, "전화번호 인증이 필요합니다.", 400),
    SMS_SEND_FAILED(500_201, "문자 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", 500),
    EMAIL_SEND_FAILED(500_202, "이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", 500),

    // Store(250~299)
    STORE_SEARCH_FAILED(502_250, "매장 검색 중 오류가 발생했습니다.", 502),
    STORE_NOT_FOUND(404_250, "매장을 찾을 수 없습니다.", 404),
    INVALID_REGION_TYPE_LABEL(400_251, "유효하지 않은 지역 값입니다.", 400),
    INVALID_DISTRICT_TYPE_LABEL(400_252, "유효하지 않은 세부지역 값입니다.", 400),

    // GroupBuy(300~349)
    GROUPBUY_FEED_TOO_MANY_DISTRICTS(400_300, "지역 필터는 최대 10개까지 선택할 수 있습니다.", 400),
    GROUPBUY_FEED_INVALID_DISTRICT_COMBINATION(400_301, "같은 지역의 전체/하위 세부지역은 동시에 선택할 수 없습니다.", 400),
    GROUPBUY_NOT_FOUND(404_302, "공구를 찾을 수 없습니다.", 404),
    GROUPBUY_NOT_RECRUITING(400_303, "모집중인 공구만 참여할 수 있습니다.", 400),
    GROUPBUY_DEADLINE_PASSED(400_304, "마감된 공구예요.", 400),
    GROUPBUY_SOLD_OUT(409_305, "참여 가능한 수량이 부족합니다.", 409),
    GROUPBUY_ALREADY_PARTICIPATED(409_306, "이미 참여한 공구예요.", 409),
    GROUPBUY_LOCK_ACQUISITION_FAILED(409_300, "요청이 많아 잠시 처리 지연 중입니다. 잠시 후 다시 시도해주세요.", 409),
    GROUPBUY_LOCK_INTERRUPTED(500_300, "요청 처리 중 일시적인 오류가 발생했습니다.", 500),

    // Payment(350~399)
    PAYMENT_INVALID_QUANTITY(400_350, "참여 수량이 올바르지 않습니다.", 400),
    PAYMENT_GROUPBUY_NOT_AVAILABLE(400_351, "참여할 수 없는 공구입니다.", 400),
    PAYMENT_QUANTITY_EXCEEDED(409_350, "참여 가능 수량을 초과했습니다.", 409),
    PAYMENT_AGREEMENT_REQUIRED(400_352, "필수 동의가 필요합니다.", 400),
    PAYMENT_ORDER_NOT_FOUND(404_350, "결제 주문을 찾을 수 없습니다.", 404),
    PAYMENT_ORDER_FORBIDDEN(403_350, "본인의 결제 주문만 처리할 수 있습니다.", 403),
    PAYMENT_ORDER_ALREADY_PROCESSED(409_351, "이미 처리된 결제 주문입니다.", 409),
    PAYMENT_AMOUNT_MISMATCH(400_353, "결제 금액이 일치하지 않습니다.", 400),
    PAYMENT_APPROVAL_FAILED(400_355, "결제 승인에 실패했습니다.", 400),
    PAYMENT_DUPLICATE_PARTICIPATION(409_352, "이미 참여한 공구입니다.", 409),
    PAYMENT_WEBHOOK_INVALID(400_354, "결제 웹훅 요청이 올바르지 않습니다.", 400),
    PAYMENT_REFUND_NOT_ALLOWED_AFTER_ACHIEVED(409_355, "달성 완료된 공구는 환불할 수 없습니다.", 409),
    PAYMENT_CANCEL_FAILED(502_355, "결제 취소에 실패했습니다.", 502),
    PARTICIPATION_NOT_FOUND(404_356, "참여 정보를 찾을 수 없습니다.", 404),
    PARTICIPATION_CANCEL_NOT_ALLOWED(409_357, "취소할 수 없는 참여 상태입니다.", 409),
    PARTICIPATION_CANCEL_REASON_DETAIL_REQUIRED(400_358, "기타 취소 사유를 입력해주세요.", 400),
    PAYMENT_PER_USER_LIMIT_EXCEEDED(409_359, "1인 구매 제한 수량을 초과했습니다.", 409),

    // Pickup(400~449)
    PICKUP_PARTICIPATION_FORBIDDEN(403_400, "본인의 참여 정보만 조회할 수 있습니다.", 403),
    PICKUP_LOCKED(400_400, "픽업일 00시 이후 이용할 수 있습니다.", 400),
    PICKUP_QR_NOT_FOUND(404_401, "픽업 QR을 찾을 수 없습니다.", 404),
    PICKUP_ALREADY_USED(409_400, "이미 사용된 픽업 QR입니다.", 409),
    PICKUP_NOT_READY(409_401, "픽업 대기 상태인 QR만 완료 처리할 수 있습니다.", 409),

    OWNER_GROUPBUY_REQUEST_INVALID_DEADLINE(400_450, "희망 공구 기간은 최소 7일 이상이어야 합니다.", 400),
    OWNER_GROUPBUY_REQUEST_INVALID_QUANTITY(400_451, "공구 수량 조건이 올바르지 않습니다.", 400),
    OWNER_GROUPBUY_REQUEST_INVALID_PICKUP_TIME(400_452, "픽업 종료 시간은 시작 시간 이후여야 합니다.", 400),
    OWNER_GROUPBUY_REQUEST_INVALID_PICKUP_DATE(400_453, "픽업일은 공구 마감일 이후여야 합니다.", 400),
    OWNER_GROUPBUY_REQUEST_NOT_FOUND(404_450, "사장님 공구 개설 요청을 찾을 수 없습니다.", 404),

    // Notification(360~369)
    NOTIFICATION_NOT_FOUND(404_360, "알림을 찾을 수 없습니다.", 404),
    NOTIFICATION_FORBIDDEN(403_360, "본인 알림만 처리할 수 있습니다.", 403),
    NOTIFICATION_TEMPLATE_NOT_FOUND(404_361, "알림 템플릿을 찾을 수 없습니다.", 404),
    NOTIFICATION_TEMPLATE_VARIABLE_MISSING(400_361, "알림 템플릿 변수 값이 누락되었습니다.", 400),
}
