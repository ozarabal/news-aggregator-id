package com.app.news_aggregator.controller;

import com.app.news_aggregator.dto.ApiResponse;
import com.app.news_aggregator.dto.ArticleDto;
import com.app.news_aggregator.model.User;
import com.app.news_aggregator.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bookmarks", description = "Kelola artikel yang disimpan user (butuh JWT)")
@RestController
@RequestMapping("/api/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @Operation(summary = "Daftar bookmark",
               description = "Ambil semua artikel yang di-bookmark user saat ini, urut terbaru",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponse<List<ArticleDto.Summary>>> getBookmarks(
            @AuthenticationPrincipal User currentUser) {
        List<ArticleDto.Summary> bookmarks = bookmarkService.getBookmarks(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Bookmark berhasil diambil", bookmarks));
    }

    @Operation(summary = "Tambah bookmark",
               description = "Bookmark artikel berdasarkan ID",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{articleId}")
    public ResponseEntity<ApiResponse<Void>> addBookmark(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "ID artikel yang ingin di-bookmark") @PathVariable Long articleId) {
        bookmarkService.addBookmark(currentUser.getId(), articleId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Artikel berhasil di-bookmark"));
    }

    @Operation(summary = "Hapus bookmark",
               description = "Hapus bookmark artikel berdasarkan ID",
               security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{articleId}")
    public ResponseEntity<ApiResponse<Void>> removeBookmark(
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "ID artikel yang ingin dihapus dari bookmark") @PathVariable Long articleId) {
        bookmarkService.removeBookmark(currentUser.getId(), articleId);
        return ResponseEntity.ok(ApiResponse.success("Bookmark berhasil dihapus"));
    }
}
