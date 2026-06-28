package com.example.rest;

import com.example.document.article.Article;
import com.example.document.user.Author;
import com.example.document.user.User;
import com.example.service.ArticleService;
import com.example.service.CommentService;
import com.example.service.UserService;
import com.example.utils.ArticleIdPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArticleController.class)
class ArticleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private co.elastic.clients.elasticsearch.ElasticsearchClient elasticsearchClient;

    @Test
    void shouldReturnArticleWhenFindingBySlug() throws Exception {
        User user = new User("author", "test@test.com", "pw", "token", "bio", "image", new byte[0], java.util.List.of());
        Author author = new Author(user, false);
        Article article = new Article("slug", "title", "desc", "body", List.of("tag"), java.time.Instant.now(), java.time.Instant.now(), false, 0, java.util.List.of(), author);

        when(articleService.findArticleBySlug(eq("slug"))).thenReturn(new ArticleIdPair(article, "id"));

        mockMvc.perform(get("/api/articles/slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article.slug").value("slug"))
                .andExpect(jsonPath("$.article.title").value("title"));
    }
}
