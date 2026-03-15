package com.bootsync.api;

import com.bootsync.snippet.dto.SnippetContentView;
import com.bootsync.snippet.dto.SnippetFormRequest;
import com.bootsync.snippet.service.SnippetCommandService;
import com.bootsync.snippet.service.SnippetQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/snippets")
public class ApiSnippetController {

    private final SnippetQueryService snippetQueryService;
    private final SnippetCommandService snippetCommandService;

    public ApiSnippetController(SnippetQueryService snippetQueryService, SnippetCommandService snippetCommandService) {
        this.snippetQueryService = snippetQueryService;
        this.snippetCommandService = snippetCommandService;
    }

    @GetMapping
    public List<ApiSnippetResponse> snippets(Authentication authentication) {
        return snippetQueryService.allForMember(authentication.getName()).stream()
            .map(this::toResponse)
            .toList();
    }

    @GetMapping("/{snippetId}")
    public ApiSnippetResponse snippet(Authentication authentication, @PathVariable Long snippetId) {
        return toResponse(snippetQueryService.contentForMember(authentication.getName(), snippetId));
    }

    @PostMapping
    public ResponseEntity<ApiSnippetResponse> create(
        Authentication authentication,
        HttpServletRequest request,
        @Valid @RequestBody ApiSnippetSaveRequest snippetRequest
    ) {
        Long snippetId = snippetCommandService.createForMember(
            authentication.getName(),
            request.getSession(true).getId(),
            toFormRequest(snippetRequest)
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toResponse(snippetQueryService.contentForMember(authentication.getName(), snippetId)));
    }

    @PutMapping("/{snippetId}")
    public ApiSnippetResponse update(
        Authentication authentication,
        HttpServletRequest request,
        @PathVariable Long snippetId,
        @Valid @RequestBody ApiSnippetSaveRequest snippetRequest
    ) {
        snippetCommandService.updateForMember(
            authentication.getName(),
            request.getSession(true).getId(),
            snippetId,
            toFormRequest(snippetRequest)
        );
        return toResponse(snippetQueryService.contentForMember(authentication.getName(), snippetId));
    }

    @DeleteMapping("/{snippetId}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long snippetId) {
        snippetCommandService.deleteForMember(authentication.getName(), snippetId);
        return ResponseEntity.noContent().build();
    }

    private SnippetFormRequest toFormRequest(ApiSnippetSaveRequest snippetRequest) {
        List<String> tags = snippetRequest.tags() == null ? List.of() : snippetRequest.tags();
        return new SnippetFormRequest(
            snippetRequest.title(),
            snippetRequest.content(),
            String.join(", ", tags),
            snippetRequest.secretWarningToken()
        );
    }

    private ApiSnippetResponse toResponse(SnippetContentView snippet) {
        return new ApiSnippetResponse(
            snippet.id(),
            snippet.title(),
            snippet.contentMarkdown(),
            snippet.tags(),
            snippet.createdAt(),
            snippet.updatedAt()
        );
    }
}
