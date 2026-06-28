package com.example.repository;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.config.TestConfiguration;
import com.example.document.comment.Comment;
import com.example.document.user.Author;
import com.example.document.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ImportTestcontainers(TestConfiguration.class)
@Testcontainers
class CommentRepositoryTests {

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void shouldSaveAndFindCommentByArticleSlug() throws IOException {
        User user = new User("commentuser", "comment@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>());
        Author author = new Author(user, false);
        Comment comment = new Comment(1L, Instant.now(), Instant.now(), "This is a comment", author, "test-article-slug");

        commentRepository.save(comment);

        SearchResponse<Comment> response = commentRepository.findCommentsByArticleSlug("test-article-slug");
        assertThat(response.hits().hits()).isNotEmpty();
        assertThat(response.hits().hits().get(0).source().body()).isEqualTo("This is a comment");
    }

    @Test
    void shouldDeleteComment() throws IOException {
        User user = new User("del_commentuser", "del_comment@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>());
        Author author = new Author(user, false);
        Comment comment = new Comment(2L, Instant.now(), Instant.now(), "Delete me", author, "del-test-article-slug");

        commentRepository.save(comment);

        SearchResponse<Comment> response = commentRepository.findCommentsByArticleSlug("del-test-article-slug");
        assertThat(response.hits().hits()).isNotEmpty();

        commentRepository.deleteComment("2", "del_commentuser");

        SearchResponse<Comment> afterDelete = commentRepository.findCommentsByArticleSlug("del-test-article-slug");
        assertThat(afterDelete.hits().hits()).isEmpty();
    }
}
