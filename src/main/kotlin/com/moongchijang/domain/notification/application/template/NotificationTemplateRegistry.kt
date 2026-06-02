package com.moongchijang.domain.notification.application.template

import com.moongchijang.domain.notification.domain.entity.NotificationDeeplinkType
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.stereotype.Component

@Component
class NotificationTemplateRegistry {

    private val triggerTypeToTemplateType: Map<NotificationTriggerType, NotificationTemplateType> = mapOf(
        NotificationTriggerType.PICKUP_SAME_DAY_MORNING to NotificationTemplateType.PICKUP_SAME_DAY_REMINDER,
        NotificationTriggerType.PICKUP_DAY_BEFORE_MORNING to NotificationTemplateType.PICKUP_DAY_BEFORE_REMINDER,
        NotificationTriggerType.PICKUP_NOT_COMPLETED_AFTER_CUTOFF to NotificationTemplateType.PICKUP_INCOMPLETE_AFTER_CUTOFF,
        NotificationTriggerType.WISH_DEADLINE_MINUS_3_DAYS to NotificationTemplateType.WISH_DEADLINE_MINUS_3_DAYS,
        NotificationTriggerType.WISH_DEADLINE_MINUS_1_DAY to NotificationTemplateType.WISH_DEADLINE_MINUS_1_DAY,
        NotificationTriggerType.WISH_TARGET_ACHIEVED_IMMEDIATE to NotificationTemplateType.WISH_TARGET_ACHIEVED,
        NotificationTriggerType.APPLY_PAYMENT_SUCCESS_IMMEDIATE to NotificationTemplateType.APPLY_PAYMENT_SUCCESS,
        NotificationTriggerType.APPLY_GROUPBUY_ACHIEVED_IMMEDIATE to NotificationTemplateType.APPLY_GROUPBUY_ACHIEVED,
        NotificationTriggerType.APPLY_GROUPBUY_FAILED_IMMEDIATE to NotificationTemplateType.APPLY_GROUPBUY_FAILED,
        NotificationTriggerType.REQUEST_OPENED_IMMEDIATE to NotificationTemplateType.REQUEST_OPENED,
        NotificationTriggerType.REQUEST_REJECTED_IMMEDIATE to NotificationTemplateType.REQUEST_REJECTED,
        NotificationTriggerType.REQUEST_NEW_PARTICIPANT_IMMEDIATE to NotificationTemplateType.REQUEST_NEW_PARTICIPANT,
        NotificationTriggerType.REQUEST_TARGET_ACHIEVED_IMMEDIATE to NotificationTemplateType.REQUEST_TARGET_ACHIEVED,
        NotificationTriggerType.REQUEST_DEADLINE_MINUS_3_DAYS to NotificationTemplateType.REQUEST_DEADLINE_MINUS_3_DAYS,
        NotificationTriggerType.OWNER_PICKUP_SAME_DAY_MORNING to NotificationTemplateType.OWNER_PICKUP_SAME_DAY_REMINDER,
        NotificationTriggerType.OWNER_PICKUP_DAY_BEFORE_MORNING to NotificationTemplateType.OWNER_PICKUP_DAY_BEFORE_REMINDER,
        NotificationTriggerType.OWNER_GROUPBUY_ACHIEVED_IMMEDIATE to NotificationTemplateType.OWNER_GROUPBUY_ACHIEVED,
        NotificationTriggerType.OWNER_GROUPBUY_FAILED_IMMEDIATE to NotificationTemplateType.OWNER_GROUPBUY_FAILED,
        NotificationTriggerType.OWNER_CLOSE_REQUEST_APPROVED_IMMEDIATE to NotificationTemplateType.OWNER_CLOSE_REQUEST_APPROVED,
        NotificationTriggerType.OWNER_CLOSE_REQUEST_REJECTED_IMMEDIATE to NotificationTemplateType.OWNER_CLOSE_REQUEST_REJECTED,
        NotificationTriggerType.OWNER_OPEN_REQUEST_APPROVED_IMMEDIATE to NotificationTemplateType.OWNER_OPEN_REQUEST_APPROVED,
        NotificationTriggerType.OWNER_OPEN_REQUEST_REJECTED_IMMEDIATE to NotificationTemplateType.OWNER_OPEN_REQUEST_REJECTED,
        NotificationTriggerType.OWNER_ORDER_CONFIRM_REQUIRED_IMMEDIATE to NotificationTemplateType.OWNER_ORDER_CONFIRM_REQUIRED,
        NotificationTriggerType.OWNER_ORDER_CANCELLED_IMMEDIATE to NotificationTemplateType.OWNER_ORDER_CANCELLED,
    )

    private val templates: Map<NotificationTemplateType, NotificationTemplate> = mapOf(
        NotificationTemplateType.PICKUP_SAME_DAY_REMINDER to NotificationTemplate(
            type = NotificationTemplateType.PICKUP_SAME_DAY_REMINDER,
            titleTemplate = "오늘 픽업일이에요! 시간 확인하세요.",
            bodyTemplate = "{상품명} 픽업시간은 오늘 {픽업시간범위}, {매장명}(이)에요.",
            deeplinkType = NotificationDeeplinkType.PICKUP_GUIDE,
        ),
        NotificationTemplateType.PICKUP_DAY_BEFORE_REMINDER to NotificationTemplate(
            type = NotificationTemplateType.PICKUP_DAY_BEFORE_REMINDER,
            titleTemplate = "내일 픽업일이에요. 잊지마세요!",
            bodyTemplate = "{상품명} 픽업이\n내일 {픽업시간범위}로 예정됐어요.",
            deeplinkType = NotificationDeeplinkType.PICKUP_GUIDE,
        ),
        NotificationTemplateType.PICKUP_INCOMPLETE_AFTER_CUTOFF to NotificationTemplate(
            type = NotificationTemplateType.PICKUP_INCOMPLETE_AFTER_CUTOFF,
            titleTemplate = "픽업완료 확인이 필요해요.",
            bodyTemplate = "{상품명} 픽업이 완료됐나요?\n사장님께 QR 확인 후 완료 처리해 주세요.",
            deeplinkType = NotificationDeeplinkType.PICKUP_GUIDE,
        ),
        NotificationTemplateType.WISH_DEADLINE_MINUS_3_DAYS to NotificationTemplate(
            type = NotificationTemplateType.WISH_DEADLINE_MINUS_3_DAYS,
            titleTemplate = "찜한 공구 마감 3일 전이에요.",
            bodyTemplate = "{상품명} 신청 마감까지 3일 남았어요.",
            deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL,
        ),
        NotificationTemplateType.WISH_DEADLINE_MINUS_1_DAY to NotificationTemplate(
            type = NotificationTemplateType.WISH_DEADLINE_MINUS_1_DAY,
            titleTemplate = "찜한 공구 마감이 내일이에요.",
            bodyTemplate = "{상품명} 신청 마감이 내일 {마감시각}까지예요.\n아직 신청 안 하셨나요?",
            deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL,
        ),
        NotificationTemplateType.WISH_TARGET_ACHIEVED to NotificationTemplate(
            type = NotificationTemplateType.WISH_TARGET_ACHIEVED,
            titleTemplate = "찜한 공구 목표 인원 달성!",
            bodyTemplate = "{상품명}(이)가 목표 인원 {목표참여개수}명을 달성했어요.\n지금 참여하면 무조건 픽업 가능!",
            deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL,
        ),
        NotificationTemplateType.APPLY_PAYMENT_SUCCESS to NotificationTemplate(
            type = NotificationTemplateType.APPLY_PAYMENT_SUCCESS,
            titleTemplate = "공구 참여 완료! 결제됐어요.",
            bodyTemplate = "{상품명} 참여가 완료됐어요.\n달성되면 바로 알려드릴게요!",
            deeplinkType = NotificationDeeplinkType.MY_APPLYING,
        ),
        NotificationTemplateType.APPLY_GROUPBUY_ACHIEVED to NotificationTemplate(
            type = NotificationTemplateType.APPLY_GROUPBUY_ACHIEVED,
            titleTemplate = "공구 성공! 픽업 일정 확인하세요.",
            bodyTemplate = "{상품명} 공구가 달성됐어요.\n픽업: {픽업일자} {픽업시간범위}\n매장: {매장명}\n주소: {매장주소}",
            deeplinkType = NotificationDeeplinkType.MY_APPLYING,
        ),
        NotificationTemplateType.APPLY_GROUPBUY_FAILED to NotificationTemplate(
            type = NotificationTemplateType.APPLY_GROUPBUY_FAILED,
            titleTemplate = "아쉽게도 공구가 미달성됐어요.",
            bodyTemplate = "{상품명} 공구가 목표 수량에 미달해 마감됐어요.\n결제 금액은 {환불예상시각} 환불될 예정이에요.",
            deeplinkType = NotificationDeeplinkType.MY_APPLYING,
        ),
        NotificationTemplateType.REQUEST_OPENED to NotificationTemplate(
            type = NotificationTemplateType.REQUEST_OPENED,
            titleTemplate = "[요청공구] 축하해요! 공구 개설 성공!",
            bodyTemplate = "요청하신 {상품명} 공구가 개설됐어요.\n지금 바로 참여해 달성률을 높여주세요!",
            deeplinkType = NotificationDeeplinkType.REQUEST_STATUS,
        ),
        NotificationTemplateType.REQUEST_REJECTED to NotificationTemplate(
            type = NotificationTemplateType.REQUEST_REJECTED,
            titleTemplate = "[요청공구] 공구 개설 실패..",
            bodyTemplate = "요청하신 {상품명} 공구가 개설 실패했어요.\n다른 공구 상품을 둘러볼까요?",
            deeplinkType = NotificationDeeplinkType.REQUEST_STATUS,
        ),
        NotificationTemplateType.REQUEST_NEW_PARTICIPANT to NotificationTemplate(
            type = NotificationTemplateType.REQUEST_NEW_PARTICIPANT,
            titleTemplate = "[요청공구] 새 참여자가 신청했어요.",
            bodyTemplate = "{상품명}에 새 신청이 들어왔어요.\n현재 {현재참여개수}/{목표참여개수}개 참여 중이에요.",
            deeplinkType = NotificationDeeplinkType.REQUEST_STATUS,
        ),
        NotificationTemplateType.REQUEST_TARGET_ACHIEVED to NotificationTemplate(
            type = NotificationTemplateType.REQUEST_TARGET_ACHIEVED,
            titleTemplate = "[요청공구] 축하해요! 공구 달성 성공!",
            bodyTemplate = "주최하신 {상품명}(이)가 목표 {목표참여개수}개을 달성했어요.\n픽업 일정을 확정해 주세요.",
            deeplinkType = NotificationDeeplinkType.REQUEST_STATUS,
        ),
        NotificationTemplateType.REQUEST_DEADLINE_MINUS_3_DAYS to NotificationTemplate(
            type = NotificationTemplateType.REQUEST_DEADLINE_MINUS_3_DAYS,
            titleTemplate = "[요청공구] 공구 마감이 3일 남았어요.",
            bodyTemplate = "주최하신 {상품명} 신청 마감까지 3일 남았어요.\n현재 {현재참여개수}/{목표참여개수}개 달성중",
            deeplinkType = NotificationDeeplinkType.REQUEST_STATUS,
        ),
        NotificationTemplateType.OWNER_PICKUP_SAME_DAY_REMINDER to NotificationTemplate(
            type = NotificationTemplateType.OWNER_PICKUP_SAME_DAY_REMINDER,
            titleTemplate = "오늘 픽업일이에요! 시간 확인하세요.",
            bodyTemplate = "{상품명} 픽업시간은 오늘 {픽업시간범위}(이)에요.",
            deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL,
        ),
        NotificationTemplateType.OWNER_PICKUP_DAY_BEFORE_REMINDER to NotificationTemplate(
            type = NotificationTemplateType.OWNER_PICKUP_DAY_BEFORE_REMINDER,
            titleTemplate = "내일 픽업 손님이 있어요! 미리 준비하세요.",
            bodyTemplate = "{상품명} 픽업시간은 내일 {픽업시간범위}(이)에요.",
            deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL,
        ),
        NotificationTemplateType.OWNER_GROUPBUY_ACHIEVED to NotificationTemplate(
            type = NotificationTemplateType.OWNER_GROUPBUY_ACHIEVED,
            titleTemplate = "공구가 확정됐어요!🎉",
            bodyTemplate = "{상품명} 공구가 목표 수량을 달성해 확정됐어요. 픽업 일정을 확인해주세요.",
            deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL,
        ),
        NotificationTemplateType.OWNER_GROUPBUY_FAILED to NotificationTemplate(
            type = NotificationTemplateType.OWNER_GROUPBUY_FAILED,
            titleTemplate = "공구가 미달성으로 취소됐어요.",
            bodyTemplate = "{상품명} 공구가 목표 수량에 미달해 취소됐어요.",
            deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL,
        ),
        NotificationTemplateType.OWNER_CLOSE_REQUEST_APPROVED to NotificationTemplate(
            type = NotificationTemplateType.OWNER_CLOSE_REQUEST_APPROVED,
            titleTemplate = "마감 요청이 승인됐어요.",
            bodyTemplate = "{상품명} 공구 마감 요청이 승인됐어요.",
            deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL,
        ),
        NotificationTemplateType.OWNER_CLOSE_REQUEST_REJECTED to NotificationTemplate(
            type = NotificationTemplateType.OWNER_CLOSE_REQUEST_REJECTED,
            titleTemplate = "마감 요청이 반려됐어요.",
            bodyTemplate = "{상품명} 공구 마감 요청이 반려됐어요. 자세한 내용을 확인해주세요.",
            deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL,
        ),
        NotificationTemplateType.OWNER_OPEN_REQUEST_APPROVED to NotificationTemplate(
            type = NotificationTemplateType.OWNER_OPEN_REQUEST_APPROVED,
            titleTemplate = "공구 개설이 승인됐어요!",
            bodyTemplate = "{상품명} 공구가 승인되어 소비자 사이트에 게시됐어요.",
            deeplinkType = NotificationDeeplinkType.REQUEST_STATUS,
        ),
        NotificationTemplateType.OWNER_OPEN_REQUEST_REJECTED to NotificationTemplate(
            type = NotificationTemplateType.OWNER_OPEN_REQUEST_REJECTED,
            titleTemplate = "공구 개설이 반려됐어요.",
            bodyTemplate = "{상품명} 공구 개설이 반려됐어요. 반려 사유를 확인해주세요.\n반려 사유: {반려사유}",
            deeplinkType = NotificationDeeplinkType.REQUEST_STATUS,
        ),
        NotificationTemplateType.OWNER_ORDER_CONFIRM_REQUIRED to NotificationTemplate(
            type = NotificationTemplateType.OWNER_ORDER_CONFIRM_REQUIRED,
            titleTemplate = "공구가 달성됐어요! 발주를 확정해주세요.",
            bodyTemplate = "{상품명} 공구가 목표를 달성했어요. 48시간 내로 발주를 확정해주세요.",
            deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL,
        ),
        NotificationTemplateType.OWNER_ORDER_CANCELLED to NotificationTemplate(
            type = NotificationTemplateType.OWNER_ORDER_CANCELLED,
            titleTemplate = "발주가 취소됐어요.",
            bodyTemplate = "{상품명} 발주 미확정으로 인해 발주가 취소됐어요.\n패널티가 1회 누적됐어요. (3회 누적 시 공구 개설 제한)",
            deeplinkType = NotificationDeeplinkType.GROUPBUY_DETAIL,
        ),
    )

    fun getTemplateByTriggerType(triggerType: NotificationTriggerType): NotificationTemplate {
        val templateType = triggerTypeToTemplateType[triggerType]
            ?: throw CustomException(
                ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND,
                "알림 템플릿 트리거 매핑을 찾을 수 없습니다. triggerType=$triggerType"
            )
        return templates[templateType]
            ?: throw CustomException(
                ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND,
                "알림 템플릿이 등록되지 않았습니다. templateType=$templateType"
            )
    }

    fun getTemplateByType(templateType: NotificationTemplateType): NotificationTemplate {
        return templates[templateType]
            ?: throw CustomException(
                ErrorCode.NOTIFICATION_TEMPLATE_NOT_FOUND,
                "알림 템플릿이 등록되지 않았습니다. templateType=$templateType"
            )
    }
}
