package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.CreateGroupBuyOpenRequestRequest
import com.moongchijang.domain.groupbuy.application.dto.StoreRecommendationRequest
import com.moongchijang.domain.groupbuy.application.dto.StoreRecommendationResponse
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOpenRequest
import com.moongchijang.domain.groupbuy.domain.entity.NotificationStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyOpenRequestRepository
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.notification.infrastructure.aligo.AligoAlimtalkClient
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.store.domain.repository.StoreRepository
import com.moongchijang.domain.store.infrastructure.naver.NaverLocalSearchClient
import com.moongchijang.domain.store.infrastructure.naver.dto.NaverLocalSearchItem
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToLong

@Service
@Transactional
class GroupBuyOpenRequestService(
    private val openRequestRepository: GroupBuyOpenRequestRepository,
    private val naverLocalSearchClient: NaverLocalSearchClient,
    private val storeRepository: StoreRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val userRepository: UserRepository,
    private val aligoAlimtalkClient: AligoAlimtalkClient,
    private val notificationEventPublisher: NotificationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val NAVER_DISPLAY_COUNT = 20
        private const val RECOMMENDATION_LIMIT = 10
        private val CATEGORY_KEYWORDS = setOf(
            "베이커리", "카페", "디저트", "제과", "제빵", "빵", "케이크",
            "도넛", "떡", "쿠키", "마카롱", "와플", "푸딩", "샌드위치"
        )
        private val NORMALIZED_CATEGORY_KEYWORDS = CATEGORY_KEYWORDS.map { normalize(it) }.toSet()
        private val NORMALIZED_FOOD_KEYWORD = normalize("음식")

        private fun normalize(value: String): String =
            value.lowercase()
                .replace(Regex("\\s+"), "")
    }

    fun create(userId: Long, request: CreateGroupBuyOpenRequestRequest) {
        if (openRequestRepository.existsByUser_IdAndRegionAndProductName(userId, request.region, request.productName)) {
            throw CustomException(ErrorCode.DUPLICATE_OPEN_REQUEST)
        }
        try {
            openRequestRepository.saveAndFlush(
                GroupBuyOpenRequest(
                    user = findUser(userId),
                    region = request.region,
                    productName = request.productName
                )
            )
        } catch (e: DataIntegrityViolationException) {
            throw CustomException(ErrorCode.DUPLICATE_OPEN_REQUEST)
        }
    }

    fun notifyOpened(groupBuy: GroupBuy): GroupBuyOpenNotificationResult {
        val result = notifyOpened(
            regions = notificationRegionCodes(groupBuy.store),
            productName = groupBuy.productName,
        )
        if (result.targetUserIds.isNotEmpty()) {
            val requestId = groupBuy.groupBuyRequest?.id ?: return result
            notificationEventPublisher.publishRequestOpened(
                requestId = requestId,
                requesterUserIds = result.targetUserIds,
                occurredAt = java.time.LocalDateTime.now()
            )
        }
        return result
    }

    fun notifyOpened(region: String, productName: String): GroupBuyOpenNotificationResult {
        return notifyOpened(regions = listOf(region), productName = productName)
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun notifyOpened(regions: Collection<String>, productName: String): GroupBuyOpenNotificationResult {
        val normalizedRegions = regions.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (normalizedRegions.isEmpty()) {
            return GroupBuyOpenNotificationResult(targetCount = 0, sentCount = 0, failedCount = 0)
        }

        val pendingRequests = openRequestRepository.findAllByRegionInAndProductNameAndNotificationStatus(
            regions = normalizedRegions,
            productName = productName,
            notificationStatus = NotificationStatus.PENDING,
        )

        val requestsByUserId = pendingRequests.groupBy { requireUserId(it) }

        val targetUserIds = requestsByUserId.keys.toList()
        var sentCount = 0
        var failedCount = 0

        requestsByUserId.forEach { (userId, userRequests) ->
            val receiverPhone = userRequests.first().user.phoneNumber
            val message = buildOpenNotificationMessage(userRequests.first().region, productName)

            val sent = if (receiverPhone.isNullOrBlank()) {
                false
            } else {
                aligoAlimtalkClient.send(receiverPhone, message)
            }

            if (sent) {
                userRequests.forEach { it.markSent() }
                sentCount += 1
            } else {
                userRequests.forEach { it.markFailed() }
                failedCount += 1
            }
        }

        openRequestRepository.saveAll(pendingRequests)

        return GroupBuyOpenNotificationResult(
            targetCount = requestsByUserId.size,
            sentCount = sentCount,
            failedCount = failedCount,
            targetUserIds = targetUserIds
        )
    }

    private fun buildOpenNotificationMessage(region: String, productName: String): String {
        return "[뭉치장] 요청하신 ${displayRegion(region)} $productName 공구가 열렸어요."
    }

    private fun notificationRegionCodes(store: Store): List<String> {
        if (store.region == RegionType.NATIONWIDE || store.district == DistrictType.NATIONWIDE) {
            return listOf(DistrictType.NATIONWIDE.name)
        }

        return listOf(
            DistrictType.NATIONWIDE.name,
            "${store.region.name}_ALL",
            store.district.name,
        ).distinct()
    }

    private fun displayRegion(region: String): String {
        val code = region.trim()
        return DistrictType.entries.firstOrNull { it.name == code }?.label
            ?: RegionType.entries.firstOrNull { it.name == code }?.label
            ?: code
    }

    @Transactional(readOnly = true)
    fun recommendStores(request: StoreRecommendationRequest): StoreRecommendationResponse {
        val startedAt = System.nanoTime()
        val regionLabel = request.region.label
        val items = runCatching {
            naverLocalSearchClient.search(
                keyword = "$regionLabel ${request.productName}",
                display = NAVER_DISPLAY_COUNT
            ).items
        }.onFailure { e ->
            log.warn(
                "store_recommendation fallback=true reason=naver_failure regionLength={} productLength={} latencyMs={}",
                regionLabel.length,
                request.productName.length,
                elapsedMillis(startedAt),
                e
            )
        }.getOrNull() ?: return emptyRecommendation(request, startedAt, fallback = true)

        val candidates = items
            .mapIndexedNotNull { index, item -> item.toCandidateOrNull(index, request) }
            .distinctBy { it.duplicateKey }

        if (candidates.isEmpty()) {
            return emptyRecommendation(request, startedAt, fallback = false)
        }

        val registeredStores = findRegisteredStores(candidates)
        val registeredByName = registeredStores.associateBy { normalize(it.name) }
        val registeredByAddress = registeredStores.associateBy { normalize(it.address) }

        val matchedStoreIds = candidates
            .mapNotNull { candidate ->
                candidate.matchedStoreId(registeredByName, registeredByAddress)
            }
            .toSet()

        val historyStoreIds = if (matchedStoreIds.isEmpty()) {
            emptySet()
        } else {
            groupBuyRepository.findStoreIdsWithGroupBuyHistory(matchedStoreIds).toSet()
        }

        val stores = candidates
            .map { candidate ->
                candidate.withSignals(
                    registeredByName = registeredByName,
                    registeredByAddress = registeredByAddress,
                    historyStoreIds = historyStoreIds
                )
            }
            .sortedWith(
                compareByDescending<ScoredStore> { it.score }
                    .thenBy { it.candidate.naverRank }
                    .thenBy { it.candidate.storeName }
            )
            .take(RECOMMENDATION_LIMIT)
            .map { it.toResponse() }

        log.info(
            "store_recommendation fallback=false recommendationCount={} naverCount={} latencyMs={}",
            stores.size,
            items.size,
            elapsedMillis(startedAt)
        )

        return StoreRecommendationResponse(
            region = regionLabel,
            productName = request.productName,
            stores = stores
        )
    }

    private fun emptyRecommendation(
        request: StoreRecommendationRequest,
        startedAt: Long,
        fallback: Boolean
    ): StoreRecommendationResponse {
        log.info(
            "store_recommendation fallback={} recommendationCount=0 naverCount=0 latencyMs={}",
            fallback,
            elapsedMillis(startedAt)
        )
        return StoreRecommendationResponse(
            region = request.region.label,
            productName = request.productName,
            stores = emptyList()
        )
    }

    private fun NaverLocalSearchItem.toCandidateOrNull(
        index: Int,
        request: StoreRecommendationRequest
    ): StoreRecommendationCandidate? {
        return runCatching {
            val storeName = storeName()
            val roadAddress = roadAddress.trim()
            val lotAddress = address.trim().ifBlank { null }
            val addressMatched = containsNormalized(roadAddress, request.region.label) ||
                containsNormalized(lotAddress.orEmpty(), request.region.label)
            val categoryMatched = isCategoryMatched(category, request.productName)

            StoreRecommendationCandidate(
                naverRank = index,
                placeId = placeId(),
                storeName = storeName,
                roadAddress = roadAddress,
                lotAddress = lotAddress,
                latitude = latitude(),
                longitude = longitude(),
                category = category,
                addressMatched = addressMatched,
                categoryMatched = categoryMatched,
                duplicateKey = duplicateKey(storeName, roadAddress, lotAddress)
            )
        }.getOrNull()
    }

    private fun findRegisteredStores(candidates: List<StoreRecommendationCandidate>): List<Store> {
        val names = candidates.map { normalize(it.storeName) }.filter { it.isNotBlank() }.toSet()
        val addresses = candidates
            .flatMap { listOfNotNull(it.roadAddress.ifBlank { null }, it.lotAddress) }
            .map { normalize(it) }
            .toSet()

        val byName = if (names.isEmpty()) emptyList() else storeRepository.findByNormalizedNameIn(names)
        val byAddress = if (addresses.isEmpty()) emptyList() else storeRepository.findByNormalizedAddressIn(addresses)

        return (byName + byAddress).distinctBy { it.id }
    }

    private fun StoreRecommendationCandidate.withSignals(
        registeredByName: Map<String, Store>,
        registeredByAddress: Map<String, Store>,
        historyStoreIds: Set<Long>
    ): ScoredStore {
        val matchedStoreId = matchedStoreId(registeredByName, registeredByAddress)
        val registeredStore = matchedStoreId != null
        val previousGroupBuyStore = matchedStoreId != null && matchedStoreId in historyStoreIds

        val score =
            (if (addressMatched) 40 else 0) +
                (if (categoryMatched) 25 else 0) +
                (if (registeredStore) 20 else 0) +
                (if (previousGroupBuyStore) 15 else 0)

        return ScoredStore(
            candidate = this,
            registeredStore = registeredStore,
            previousGroupBuyStore = previousGroupBuyStore,
            score = score
        )
    }

    private fun StoreRecommendationCandidate.matchedStoreId(
        registeredByName: Map<String, Store>,
        registeredByAddress: Map<String, Store>
    ): Long? {
        return registeredByName[normalize(storeName)]?.id
            ?: registeredByAddress[normalize(roadAddress)]?.id
            ?: lotAddress?.let { registeredByAddress[normalize(it)]?.id }
    }

    private fun ScoredStore.toResponse(): StoreRecommendationResponse.RecommendedStore {
        return StoreRecommendationResponse.RecommendedStore(
            placeId = candidate.placeId,
            storeName = candidate.storeName,
            roadAddress = candidate.roadAddress,
            lotAddress = candidate.lotAddress,
            latitude = candidate.latitude,
            longitude = candidate.longitude,
            category = candidate.category,
            addressMatched = candidate.addressMatched,
            categoryMatched = candidate.categoryMatched,
            registeredStore = registeredStore,
            previousGroupBuyStore = previousGroupBuyStore
        )
    }

    private fun isCategoryMatched(category: String, productName: String): Boolean {
        val normalizedCategory = normalize(category)
        val normalizedProduct = normalize(productName)
        return NORMALIZED_CATEGORY_KEYWORDS.any { keyword ->
            normalizedCategory.contains(keyword) ||
                (normalizedProduct.contains(keyword) && normalizedCategory.contains(NORMALIZED_FOOD_KEYWORD))
        }
    }

    private fun duplicateKey(storeName: String, roadAddress: String, lotAddress: String?): String {
        val addressKey = roadAddress.ifBlank { lotAddress.orEmpty() }
        return "${normalize(storeName)}:${normalize(addressKey)}"
    }

    private fun containsNormalized(value: String, keyword: String): Boolean {
        val normalizedValue = normalize(value)
        val normalizedKeyword = normalize(keyword)
        return normalizedKeyword.isNotBlank() && normalizedValue.contains(normalizedKeyword)
    }

    private fun elapsedMillis(startedAt: Long): Long =
        ((System.nanoTime() - startedAt) / 1_000_000.0).roundToLong()

    private fun findUser(userId: Long) =
        userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

    private fun requireUserId(openRequest: GroupBuyOpenRequest): Long =
        openRequest.user.id ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

    private data class StoreRecommendationCandidate(
        val naverRank: Int,
        val placeId: String,
        val storeName: String,
        val roadAddress: String,
        val lotAddress: String?,
        val latitude: Double,
        val longitude: Double,
        val category: String,
        val addressMatched: Boolean,
        val categoryMatched: Boolean,
        val duplicateKey: String
    )

    private data class ScoredStore(
        val candidate: StoreRecommendationCandidate,
        val registeredStore: Boolean,
        val previousGroupBuyStore: Boolean,
        val score: Int
    )
}

data class GroupBuyOpenNotificationResult(
    val targetCount: Int,
    val sentCount: Int,
    val failedCount: Int,
    val targetUserIds: List<Long> = emptyList(),
)
