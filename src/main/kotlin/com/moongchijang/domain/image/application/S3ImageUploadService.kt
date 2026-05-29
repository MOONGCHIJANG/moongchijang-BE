package com.moongchijang.domain.image.application

import com.moongchijang.domain.image.application.dto.ImagePresignedUploadItemRequest
import com.moongchijang.domain.image.application.dto.ImagePresignedUploadItemResponse
import com.moongchijang.domain.image.application.dto.ImagePresignedUploadRequest
import com.moongchijang.domain.image.application.dto.ImagePresignedUploadResponse
import com.moongchijang.domain.image.application.dto.ImageUploadCategory
import com.moongchijang.domain.image.application.dto.ImageDeleteResponse
import com.moongchijang.global.config.AppS3Properties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class S3ImageUploadService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val appS3Properties: AppS3Properties,
) {
    private val log = LoggerFactory.getLogger(S3ImageUploadService::class.java)

    fun issuePresignedUrls(userId: Long, request: ImagePresignedUploadRequest): ImagePresignedUploadResponse {
        validateCount(request.files)
        val responses = request.files.map { issueSingle(request.groupBuyId, it) }
        log.info(
            "[S3ImageUploadService] Presigned URL 발급 완료: userId={}, groupBuyId={}, total={}, thumbnailCount={}, productCount={}",
            userId,
            request.groupBuyId,
            responses.size,
            responses.count { it.category == ImageUploadCategory.THUMBNAIL },
            responses.count { it.category == ImageUploadCategory.PRODUCT },
        )
        return ImagePresignedUploadResponse(items = responses)
    }

    fun deleteImages(userId: Long, keys: List<String>): ImageDeleteResponse {
        val normalizedKeys = keys.map { it.trim().removePrefix("/") }.filter { it.isNotBlank() }.distinct()
        if (normalizedKeys.isEmpty()) {
            throw CustomException(ErrorCode.INVALID_INPUT, "삭제할 이미지 key가 비어 있습니다.")
        }
        val prefix = appS3Properties.prefix.trim('/').let { if (it.isBlank()) "" else "$it/" }
        if (prefix.isNotBlank() && normalizedKeys.any { !it.startsWith(prefix) }) {
            throw CustomException(ErrorCode.INVALID_INPUT, "허용된 경로(prefix)의 이미지 key만 삭제할 수 있습니다.")
        }

        val request = DeleteObjectsRequest.builder()
            .bucket(appS3Properties.bucket)
            .delete(
                Delete.builder()
                    .objects(normalizedKeys.map { ObjectIdentifier.builder().key(it).build() })
                    .build()
            )
            .build()

        val result = s3Client.deleteObjects(request)
        val deleted = result.deleted()?.map { it.key() } ?: emptyList()
        val failed = result.errors()?.map { it.key() } ?: emptyList()
        log.info(
            "[S3ImageUploadService] 이미지 삭제 완료: userId={}, requested={}, deleted={}, failed={}",
            userId,
            normalizedKeys.size,
            deleted.size,
            failed.size,
        )
        return ImageDeleteResponse(
            deletedKeys = deleted,
            failedKeys = failed,
        )
    }

    private fun issueSingle(groupBuyId: Long?, file: ImagePresignedUploadItemRequest): ImagePresignedUploadItemResponse {
        val contentType = file.contentType.trim().lowercase()
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw CustomException(ErrorCode.INVALID_INPUT, "지원하지 않는 이미지 형식입니다.")
        }

        val extension = extensionFrom(file.fileName)
        val folder = if (file.category == ImageUploadCategory.THUMBNAIL) "thumbnail" else "products"
        val datePartition = LocalDate.now(ZoneId.of("Asia/Seoul")).toString().replace("-", "")
        val subject = groupBuyId?.toString() ?: "pending/$datePartition"
        val key = "${appS3Properties.prefix.trim('/')}/group-buys/$subject/$folder/${UUID.randomUUID()}.$extension"

        val putRequest = PutObjectRequest.builder()
            .bucket(appS3Properties.bucket)
            .key(key)
            .contentType(contentType)
            .build()

        val presignedUrl = s3Presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .putObjectRequest(putRequest)
                .signatureDuration(Duration.ofSeconds(appS3Properties.presignExpirationSeconds))
                .build()
        ).url().toString()

        return ImagePresignedUploadItemResponse(
            category = file.category,
            key = key,
            uploadUrl = presignedUrl,
        )
    }

    private fun validateCount(files: List<ImagePresignedUploadItemRequest>) {
        val thumbnailCount = files.count { it.category == ImageUploadCategory.THUMBNAIL }
        val productCount = files.count { it.category == ImageUploadCategory.PRODUCT }
        if (thumbnailCount > 1) {
            throw CustomException(ErrorCode.INVALID_INPUT, "썸네일 이미지는 최대 1장까지 가능합니다.")
        }
        if (productCount > 10) {
            throw CustomException(ErrorCode.INVALID_INPUT, "상품 이미지는 최대 10장까지 가능합니다.")
        }
    }

    private fun extensionFrom(fileName: String): String {
        val ext = fileName.trim().substringAfterLast('.', "").lowercase()
        if (ext !in ALLOWED_EXTENSIONS) {
            throw CustomException(ErrorCode.INVALID_INPUT, "지원하지 않는 파일 확장자입니다.")
        }
        return ext
    }

    private fun String.trim(char: Char): String = this.trim().trimStart(char).trimEnd(char)

    companion object {
        private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    }
}
