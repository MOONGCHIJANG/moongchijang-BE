package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.GroupBuyOpenRequestService
import com.moongchijang.domain.groupbuy.application.dto.CreateGroupBuyOpenRequestRequest
import com.moongchijang.domain.groupbuy.application.dto.StoreRecommendationRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOpenRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.entity.NotificationStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyOpenRequestRepository
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.notification.infrastructure.aligo.AligoAlimtalkClient
import com.moongchijang.domain.store.application.RecommendedStoreImageService
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.StoreRecommendationRegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.store.domain.repository.StoreRepository
import com.moongchijang.domain.store.infrastructure.naver.NaverLocalSearchClient
import com.moongchijang.domain.store.infrastructure.naver.dto.NaverLocalSearchResponse
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.security.crypto.AesGcmPersonalInfoEncryptor
import com.moongchijang.security.crypto.HmacSha256PersonalInfoHasher
import com.moongchijang.security.crypto.PersonalInfoEncryptionProperties
import com.moongchijang.security.crypto.PersonalInfoManager
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.NaverFixture
import com.moongchijang.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class GroupBuyOpenRequestServiceTest {

    @Mock
    private lateinit var openRequestRepository: GroupBuyOpenRequestRepository

    @Mock
    private lateinit var naverLocalSearchClient: NaverLocalSearchClient

    @Mock
    private lateinit var storeRepository: StoreRepository

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var aligoAlimtalkClient: AligoAlimtalkClient

    @Mock
    private lateinit var notificationEventPublisher: NotificationEventPublisher

    @Mock
    private lateinit var recommendedStoreImageService: RecommendedStoreImageService

    private val personalInfoProperties = PersonalInfoEncryptionProperties(
        secretKey = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
    )
    private val personalInfoManager = PersonalInfoManager(
        AesGcmPersonalInfoEncryptor(personalInfoProperties),
        HmacSha256PersonalInfoHasher(personalInfoProperties),
    )

    private lateinit var service: GroupBuyOpenRequestService

    @BeforeEach
    fun setUp() {
        service = GroupBuyOpenRequestService(
            openRequestRepository = openRequestRepository,
            naverLocalSearchClient = naverLocalSearchClient,
            storeRepository = storeRepository,
            groupBuyRepository = groupBuyRepository,
            userRepository = userRepository,
            aligoAlimtalkClient = aligoAlimtalkClient,
            notificationEventPublisher = notificationEventPublisher,
            recommendedStoreImageService = recommendedStoreImageService,
            personalInfoManager = personalInfoManager,
        )
        lenient().`when`(userRepository.findByIdAndDeletedAtIsNull(anyLong()))
            .thenAnswer { UserFixture.createKakaoUser(id = it.getArgument(0)) }
        lenient().`when`(recommendedStoreImageService.findActiveImageUrls())
            .thenReturn(
                listOf(
                    "https://dkg5euyknlpa.cloudfront.net/dev/recommended-store/1.jpeg",
                    "https://dkg5euyknlpa.cloudfront.net/dev/recommended-store/2.jpeg",
                )
            )
        lenient().`when`(recommendedStoreImageService.imageUrlByIndex(anyInt(), anyList()))
            .thenAnswer {
                val index = it.getArgument<Int>(0)
                val imageUrls = it.getArgument<List<String>>(1)
                imageUrls[index.mod(imageUrls.size)]
            }
    }

    @Test
    fun `정상 알림 신청 시 저장 성공`() {
        val userId = 1L
        val request = CreateGroupBuyOpenRequestRequest(region = "성수", productName = "소금빵")

        `when`(openRequestRepository.existsByUser_IdAndRegionAndProductName(userId, "성수", "소금빵"))
            .thenReturn(false)
        `when`(openRequestRepository.saveAndFlush(any())).thenReturn(
            GroupBuyOpenRequest(user = com.moongchijang.support.UserFixture.createKakaoUser(id = userId), region = "성수", productName = "소금빵").apply { id = 1L }
        )

        service.create(userId, request)

        verify(openRequestRepository).saveAndFlush(any())
    }

    @Test
    fun `중복 알림 신청 시 DUPLICATE_OPEN_REQUEST 예외`() {
        val userId = 1L
        val request = CreateGroupBuyOpenRequestRequest(region = "성수", productName = "소금빵")

        `when`(openRequestRepository.existsByUser_IdAndRegionAndProductName(userId, "성수", "소금빵"))
            .thenReturn(true)

        val ex = assertThrows<CustomException> { service.create(userId, request) }
        assertEquals(ErrorCode.DUPLICATE_OPEN_REQUEST, ex.errorCode)
        verify(openRequestRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `동시 신청으로 UNIQUE 제약 위반 시 DUPLICATE_OPEN_REQUEST 예외`() {
        val userId = 1L
        val request = CreateGroupBuyOpenRequestRequest(region = "성수", productName = "소금빵")

        `when`(openRequestRepository.existsByUser_IdAndRegionAndProductName(userId, "성수", "소금빵"))
            .thenReturn(false)
        `when`(openRequestRepository.saveAndFlush(any<GroupBuyOpenRequest>()))
            .thenThrow(DataIntegrityViolationException("uk_open_req_user_region_product"))

        val ex = assertThrows<CustomException> { service.create(userId, request) }
        assertEquals(ErrorCode.DUPLICATE_OPEN_REQUEST, ex.errorCode)
    }

    @Test
    fun `매장 추천 시 네이버 결과를 중복 제거하고 추천 점수 순으로 반환한다`() {
        val request = StoreRecommendationRequest(region = StoreRecommendationRegionType.SEOUL_SEONGSU, productName = "소금빵")
        val loaf = NaverFixture.naverItem(
            title = "<b>LOAF</b>",
            link = "https://map.naver.com/p/entry/place/100",
            category = "음식점>카페,디저트",
            address = "서울 성동구 성수동1가 1",
            roadAddress = "서울 성동구 성수이로 1"
        )
        val duplicateLoaf = loaf.copy(description = "duplicate")
        val other = NaverFixture.naverItem(
            title = "다른 가게",
            link = "https://map.naver.com/p/entry/place/200",
            category = "생활,편의",
            address = "서울 마포구 망원동 1",
            roadAddress = "서울 마포구 월드컵로 1"
        )
        val registeredStore = GroupBuyFixture.createStore(
            name = "LOAF",
            address = "서울 성동구 성수이로 1"
        ).apply { id = 10L }

        `when`(naverLocalSearchClient.search("성수 소금빵", 20)).thenReturn(
            NaverLocalSearchResponse(
                total = 3,
                start = 1,
                display = 3,
                items = listOf(other, loaf, duplicateLoaf)
            )
        )
        `when`(storeRepository.findByNormalizedNameIn(anyCollection())).thenReturn(listOf(registeredStore))
        `when`(storeRepository.findByNormalizedAddressIn(anyCollection())).thenReturn(emptyList())
        `when`(groupBuyRepository.findStoreIdsWithGroupBuyHistory(setOf(10L))).thenReturn(listOf(10L))

        val response = service.recommendStores(request)

        assertEquals("성수", response.region)
        assertEquals("소금빵", response.productName)
        assertEquals(2, response.stores.size)
        assertEquals("LOAF", response.stores[0].storeName)
        assertEquals("100", response.stores[0].placeId)
        assertTrue(response.stores[0].addressMatched)
        assertTrue(response.stores[0].categoryMatched)
        assertTrue(response.stores[0].registeredStore)
        assertTrue(response.stores[0].previousGroupBuyStore)
        assertThat(response.stores[0].imageUrl).startsWith("https://dkg5euyknlpa.cloudfront.net/")
        assertEquals("다른 가게", response.stores[1].storeName)
        assertFalse(response.stores[1].registeredStore)
        verify(storeRepository).findByNormalizedNameIn(setOf("다른가게", "loaf"))
        verify(storeRepository).findByNormalizedAddressIn(
            setOf(
                "서울마포구월드컵로1",
                "서울마포구망원동1",
                "서울성동구성수이로1",
                "서울성동구성수동1가1"
            )
        )
    }

    @Test
    fun `네이버 API 실패 시 빈 추천 목록으로 fallback 한다`() {
        val request = StoreRecommendationRequest(region = StoreRecommendationRegionType.SEOUL_SEONGSU, productName = "소금빵")
        `when`(naverLocalSearchClient.search("성수 소금빵", 20))
            .thenThrow(RuntimeException("timeout"))

        val response = service.recommendStores(request)

        assertEquals("성수", response.region)
        assertEquals("소금빵", response.productName)
        assertTrue(response.stores.isEmpty())
        verifyNoInteractions(storeRepository, groupBuyRepository)
    }

    @Test
    fun `네이버 결과가 0건이면 빈 추천 목록을 반환한다`() {
        val request = StoreRecommendationRequest(region = StoreRecommendationRegionType.SEOUL_SEONGSU, productName = "소금빵")
        `when`(naverLocalSearchClient.search("성수 소금빵", 20)).thenReturn(
            NaverLocalSearchResponse(total = 0, start = 1, display = 0, items = emptyList())
        )

        val response = service.recommendStores(request)

        assertTrue(response.stores.isEmpty())
        verifyNoInteractions(storeRepository, groupBuyRepository)
    }

    @Test
    fun `공구 개설 알림 발송 성공 시 PENDING 요청을 SENT 처리`() {
        val openRequest = GroupBuyOpenRequest(
            user = com.moongchijang.support.UserFixture.createKakaoUser(id = 1L).apply { phoneNumber = "01012345678" },
            region = "성수",
            productName = "소금빵"
        )

        `when`(
            openRequestRepository.findAllByRegionInAndProductNameAndNotificationStatus(
                listOf("성수"),
                "소금빵",
                NotificationStatus.PENDING,
            )
        ).thenReturn(listOf(openRequest))
        `when`(aligoAlimtalkClient.send("01012345678", "[뭉치장] 요청하신 성수 소금빵 공구가 열렸어요."))
            .thenReturn(true)

        val result = service.notifyOpened(region = "성수", productName = "소금빵")

        assertEquals(NotificationStatus.SENT, openRequest.notificationStatus)
        assertEquals(1, result.targetCount)
        assertEquals(1, result.sentCount)
        assertEquals(0, result.failedCount)
        verify(aligoAlimtalkClient).send("01012345678", "[뭉치장] 요청하신 성수 소금빵 공구가 열렸어요.")
        verify(openRequestRepository).saveAll(listOf(openRequest))
    }

    @Test
    fun `공구 개설 알림 발송 실패 시 FAILED 처리`() {
        val openRequest = GroupBuyOpenRequest(
            user = com.moongchijang.support.UserFixture.createKakaoUser(id = 1L).apply { phoneNumber = "01012345678" },
            region = "성수",
            productName = "소금빵"
        )

        `when`(
            openRequestRepository.findAllByRegionInAndProductNameAndNotificationStatus(
                listOf("성수"),
                "소금빵",
                NotificationStatus.PENDING,
            )
        ).thenReturn(listOf(openRequest))
        `when`(aligoAlimtalkClient.send("01012345678", "[뭉치장] 요청하신 성수 소금빵 공구가 열렸어요."))
            .thenReturn(false)

        val result = service.notifyOpened(region = "성수", productName = "소금빵")

        assertEquals(NotificationStatus.FAILED, openRequest.notificationStatus)
        assertEquals(1, result.targetCount)
        assertEquals(0, result.sentCount)
        assertEquals(1, result.failedCount)
        verify(openRequestRepository).saveAll(listOf(openRequest))
    }

    @Test
    fun `전화번호가 없으면 알리고 호출 없이 FAILED 처리`() {
        val openRequest = GroupBuyOpenRequest(
            user = com.moongchijang.support.UserFixture.createKakaoUser(id = 1L).apply { phoneNumber = null },
            region = "성수",
            productName = "소금빵"
        )

        `when`(
            openRequestRepository.findAllByRegionInAndProductNameAndNotificationStatus(
                listOf("성수"),
                "소금빵",
                NotificationStatus.PENDING,
            )
        ).thenReturn(listOf(openRequest))
        val result = service.notifyOpened(region = "성수", productName = "소금빵")

        assertEquals(NotificationStatus.FAILED, openRequest.notificationStatus)
        assertEquals(1, result.targetCount)
        assertEquals(0, result.sentCount)
        assertEquals(1, result.failedCount)
        verifyNoInteractions(aligoAlimtalkClient)
        verify(openRequestRepository).saveAll(listOf(openRequest))
    }

    @Test
    fun `공구 기준 알림은 매장 지역과 세부지역을 모두 매칭하고 사용자별 중복 발송을 막는다`() {
        val regionRequest = GroupBuyOpenRequest(
            user = com.moongchijang.support.UserFixture.createKakaoUser(id = 1L).apply { phoneNumber = "01012345678" },
            region = "SEOUL_ALL",
            productName = "소금빵"
        ).apply { id = 10L }
        val districtRequest = GroupBuyOpenRequest(
            user = com.moongchijang.support.UserFixture.createKakaoUser(id = 1L).apply { phoneNumber = "01012345678" },
            region = "SEOUL_SEONGSU_GEONDAE_GWANGJIN",
            productName = "소금빵"
        ).apply { id = 11L }
        val groupBuy = createGroupBuy(productName = "소금빵")

        `when`(
            openRequestRepository.findAllByRegionInAndProductNameAndNotificationStatus(
                listOf("NATIONWIDE", "SEOUL_ALL", "SEOUL_SEONGSU_GEONDAE_GWANGJIN"),
                "소금빵",
                NotificationStatus.PENDING,
            )
        ).thenReturn(listOf(regionRequest, districtRequest))
        `when`(aligoAlimtalkClient.send("01012345678", "[뭉치장] 요청하신 서울 전체 소금빵 공구가 열렸어요."))
            .thenReturn(true)

        val result = service.notifyOpened(groupBuy)

        assertEquals(NotificationStatus.SENT, regionRequest.notificationStatus)
        assertEquals(NotificationStatus.SENT, districtRequest.notificationStatus)
        assertEquals(1, result.targetCount)
        assertEquals(1, result.sentCount)
        assertEquals(0, result.failedCount)
        verify(aligoAlimtalkClient).send("01012345678", "[뭉치장] 요청하신 서울 전체 소금빵 공구가 열렸어요.")
        verify(openRequestRepository).saveAll(listOf(regionRequest, districtRequest))
        val invocation = mockingDetails(notificationEventPublisher).invocations
            .first { it.method.name == "publishRequestOpened" }
        assertEquals(200L, invocation.arguments[0])
        assertEquals(listOf(1L), invocation.arguments[1])
        assertTrue(invocation.arguments[2] is LocalDateTime)
    }

    private fun createGroupBuy(productName: String): GroupBuy =
        GroupBuy(
            store = Store(
                name = "테스트 매장",
                address = "서울 성동구 성수동",
                region = RegionType.SEOUL,
                district = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN,
            ),
            groupBuyRequest = GroupBuyRequest(
            user = com.moongchijang.support.UserFixture.createKakaoUser(id = 1L),
                storeName = "테스트 매장",
                productName = productName,
                desiredQuantity = 10,
                desiredPickupDate = LocalDate.now().plusDays(3),
            ).apply { id = 200L },
            productName = productName,
            productDescription = "설명",
            price = 6000,
            targetQuantity = 10,
            maxQuantity = 20,
            status = GroupBuyStatus.IN_PROGRESS,
            recruitmentStartAt = LocalDateTime.now(),
            deadline = LocalDateTime.now().plusDays(1),
            pickupDate = LocalDate.now().plusDays(3),
            pickupTimeStart = LocalTime.of(13, 0),
            pickupTimeEnd = LocalTime.of(15, 0),
            pickupLocation = "매장",
        )
}
