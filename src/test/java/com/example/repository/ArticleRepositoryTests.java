package com.example.repository;

import com.example.config.TestConfiguration;
import com.example.document.article.Article;
import com.example.document.article.TagsDTO;
import com.example.document.user.Author;
import com.example.document.user.User;
import com.example.utils.ArticleIdPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ImportTestcontainers(TestConfiguration.class)
@Testcontainers
class ArticleRepositoryTests {

    @Autowired
    private ArticleRepository articleRepository;

    @Test
    void shouldSaveAndFindArticleBySlug() throws IOException {
        User user = new User("testuser", "test@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>());
        Author author = new Author(user, false);
        Article article = new Article("integration-test-article", "Integration Test Article", "Description", "Body text", List.of("test", "integration"), Instant.now(), Instant.now(), false, 0, new ArrayList<>(), author);

        articleRepository.save(article);

        ArticleIdPair found = articleRepository.findBySlug("integration-test-article");
        assertThat(found).isNotNull();
        assertThat(found.article().title()).isEqualTo("Integration Test Article");
    }

    @Test
    void shouldUpdateArticleSuccessfully() throws IOException {
        User user = new User("updater", "update@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>());
        Author author = new Author(user, false);
        Article article = new Article("update-article-test", "Update Article Test", "Desc", "Body", List.of("update"), Instant.now(), Instant.now(), false, 0, new ArrayList<>(), author);

        articleRepository.save(article);

        ArticleIdPair articlePair = articleRepository.findBySlug("update-article-test");
        Article oldArticle = articlePair.article();

        Article updatedArticle = new Article(oldArticle.slug(), "Updated Title Test", oldArticle.description(), oldArticle.body(), oldArticle.tagList(), oldArticle.createdAt(), Instant.now(), oldArticle.favorited(), oldArticle.favoritesCount(), oldArticle.favoritedBy(), oldArticle.author());

        articleRepository.update(articlePair.id(), updatedArticle);

        ArticleIdPair updated = articleRepository.findBySlug("update-article-test");
        assertThat(updated.article().title()).isEqualTo("Updated Title Test");
    }

    @Test
    void shouldDeleteArticleSuccessfully() throws IOException {
        User user = new User("deleter", "del@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>());
        Author author = new Author(user, false);
        Article article = new Article("delete-article-test", "Delete Article Test", "Desc", "Body", List.of("delete"), Instant.now(), Instant.now(), false, 0, new ArrayList<>(), author);

        articleRepository.save(article);
        assertThat(articleRepository.findBySlug("delete-article-test")).isNotNull();

        articleRepository.deleteBySlug("delete-article-test");
        assertThat(articleRepository.findBySlug("delete-article-test")).isNull();
    }

    @Test
    void shouldFindAllTagsSuccessfully() throws IOException {
        User user = new User("tag_author", "tag@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>());
        Author author = new Author(user, false);
        Article article = new Article("tag-test-article", "Tag Test Article", "Desc", "Body", List.of("tag_z", "tag_a"), Instant.now(), Instant.now(), false, 0, new ArrayList<>(), author);

        articleRepository.save(article);

        TagsDTO tags = articleRepository.findAllTags();
        assertThat(tags.tags()).contains("tag_z", "tag_a");
    }
}
