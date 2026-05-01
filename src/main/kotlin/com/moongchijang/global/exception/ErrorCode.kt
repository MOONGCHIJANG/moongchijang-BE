package com.moongchijang.global.exception

enum class ErrorCode(val code: Int, val message: String, val httpStatus: Int) {
    // 100 Test
    TEST_ERROR(100_000, "테스트 에러입니다.", 200),

    // 400 Bad Request
    BAD_REQUEST(400_000, "잘못된 요청입니다.", 400),
    INVALID_FILE_FORMAT(400_001, "업로드된 파일 형식이 올바르지 않습니다.", 400),
    INVALID_INPUT(400_002, "입력값이 올바르지 않습니다.", 400),
    NULL_VALUE(400_003, "Null 값이 들어왔습니다.", 400),
    INVALID_NICKNAME_FORMAT(400_004, "2~10자, 한글/영문/숫자만 입력 가능해요.", 400),
    INVALID_PHONE_NUMBER_FORMAT(400_005, "올바른 전화번호를 입력해주세요.", 400),

    // 401 Unauthorized
    TOKEN_EXPIRED(401_000, "토큰이 만료되었습니다.", 401),
    TOKEN_INVALID(401_001, "유효하지 않은 토큰입니다.", 401),
    TOKEN_NOT_FOUND(401_002, "토큰이 존재하지 않습니다.", 401),
    TOKEN_UNSUPPORTED(401_003, "지원하지 않는 토큰 형식입니다.", 401),
    INVALID_CREDENTIALS(401_004, "인증 정보가 올바르지 않습니다.", 401),
    INVALID_REFRESH_TOKEN(401_005, "재발급 토큰이 유효하지 않습니다.", 401),
    INVALID_ACCESS_TOKEN(401_006, "접근 토큰이 유효하지 않습니다.", 401),
    INVALID_TOKEN(401_007, "토큰이 생성되지 않았습니다.", 401),
    INVALID_LOGIN(401_008, "로그인이 필요합니다.", 401),
    REFRESH_TOKEN_MISMATCH(401_009, "저장된 리프레시 토큰과 일치하지 않습니다.", 401),
    EXPIRED_REFRESH_TOKEN(401_010, "리프레시 토큰이 만료되었습니다.", 401),
    REFRESH_TOKEN_NOT_FOUND(401_011, "저장된 리프레시 토큰이 존재하지 않습니다.", 401),
    KAKAO_TOKEN_REQUEST_INVALID(401_012, "카카오 토큰 요청 파라미터가 올바르지 않습니다.", 401),
    KAKAO_TOKEN_EXCHANGE_FAILED(401_013, "카카오 토큰 교환에 실패했습니다.", 401),
    KAKAO_TOKEN_RESPONSE_INVALID(401_014, "카카오 토큰 응답이 올바르지 않습니다.", 401),
    KAKAO_ACCESS_TOKEN_MISSING(401_015, "카카오 액세스 토큰이 응답에 없습니다.", 401),
    KAKAO_USER_INFO_FETCH_FAILED(401_016, "카카오 사용자 정보 조회에 실패했습니다.", 401),
    KAKAO_USER_INFO_INVALID(401_017, "카카오 사용자 정보가 유효하지 않습니다.", 401),

    // 403 Forbidden
    FORBIDDEN(403_000, "접속 권한이 없습니다.", 403),
    ACCESS_DENY(403_001, "접근이 거부되었습니다.", 403),
    UNAUTHORIZED_POST_ACCESS(403_002, "해당 게시글에 접근할 권한이 없습니다.", 403),

    // 404 Not Found
    NOT_FOUND_END_POINT(404_000, "요청한 대상이 존재하지 않습니다.", 404),
    USER_NOT_FOUND(404_001, "사용자를 찾을 수 없습니다.", 404),
    USER_NOT_FOUND_IN_COOKIE(404_002, "쿠키에서 사용자 정보를 찾을 수 없습니다.", 404),
    POST_NOT_FOUND(404_003, "요청한 게시글을 찾을 수 없습니다.", 404),
    POST_TYPE_NOT_FOUND(404_004, "게시글 타입을 찾을 수 없습니다.", 404),
    COMMENT_NOT_FOUND(404_005, "요청한 댓글을 찾을 수 없습니다.", 404),

    // 409 Conflict
    DUPLICATE_EMAIL(409_001, "이미 사용 중인 이메일입니다.", 409),
    REJOIN_NOT_AVAILABLE_YET(409_002, "탈퇴 후 30일 이후에 다시 가입할 수 있습니다.", 409),
    DUPLICATE_NICKNAME(409_003, "이미 사용 중인 닉네임이에요.", 409),

    // GroupBuyRequest
    GROUPBUY_REQUEST_NOT_FOUND(404_010, "공구 요청을 찾을 수 없습니다.", 404),
    GROUPBUY_REQUEST_FORBIDDEN(403_010, "본인의 공구 요청만 조회할 수 있습니다.", 403),
    GROUPBUY_REQUEST_INVALID_DATE(400_010, "희망 픽업 날짜는 오늘 이후여야 합니다.", 400),

    // Store
    STORE_SEARCH_FAILED(502_001, "매장 검색 중 오류가 발생했습니다.", 502),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(500_000, "서버 내부 오류입니다.", 500),
    SMS_SEND_FAILED(500_001, "문자 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", 500);
}
