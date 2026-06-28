package com.example.service;

import com.example.document.article.*;
import com.example.document.exception.ResourceAlreadyExistsException;
import com.example.document.exception.UnauthorizedException;
import com.example.document.user.Author;
import com.example.document.user.User;
import com.example.repository.ArticleRepository;
import com.example.utils.ArticleIdPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTests {

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ArticleService articleService;

    @Test
    void shouldCreateArticleSuccessfully() throws IOException {
        ArticleCreationDTO creationDTO = new ArticleCreationDTO("Unit Test Article", "Description", "Body text", List.of("test", "unit"));
        User user = new User("testuser", "test@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>());
        Author author = new Author(user, false);

        when(articleRepository.findBySlug("unit-test-article")).thenReturn(null);

        Article created = articleService.newArticle(creationDTO, author);

        assertThat(created).isNotNull();
        assertThat(created.title()).isEqualTo("Unit Test Article");
        assertThat(created.slug()).isEqualTo("unit-test-article");
        assertThat(created.tagList()).containsExactlyInAnyOrder("test", "unit");

        verify(articleRepository).save(any(Article.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingArticleWithExistingSlug() throws IOException {
        ArticleCreationDTO creationDTO = new ArticleCreationDTO("Unit Test Article", "Description", "Body text", List.of("test"));
        Author author = new Author(new User("testuser", "test@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>()), false);

        Article existing = new Article("unit-test-article", "Unit Test Article", "Desc", "Body", List.of(), Instant.now(), Instant.now(), false, 0, new ArrayList<>(), author);
        when(articleRepository.findBySlug("unit-test-article")).thenReturn(new ArticleIdPair(existing, "id"));

        assertThrows(ResourceAlreadyExistsException.class, () -> articleService.newArticle(creationDTO, author));
    }

    @Test
    void shouldUpdateArticleSuccessfully() throws IOException {
        Author author = new Author(new User("updater", "update@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>()), false);
        Article oldArticle = new Article("update-article-test", "Update Article Test", "Desc", "Body", List.of(), Instant.now(), Instant.now(), false, 0, new ArrayList<>(), author);
        when(articleRepository.findBySlug("update-article-test")).thenReturn(new ArticleIdPair(oldArticle, "id1"));
        when(articleRepository.findBySlug("updated-title-test")).thenReturn(null); // No collision

        ArticleUpdateDTO updateDTO = new ArticleUpdateDTO("Updated Title Test", "New Desc", "New Body");
        ArticleDTO updated = articleService.updateArticle(updateDTO, "update-article-test", author);

        assertThat(updated.title()).isEqualTo("Updated Title Test");
        assertThat(updated.slug()).isEqualTo("updated-title-test");
        verify(articleRepository).update(eq("id1"), any(Article.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdateUnauthorized() throws IOException {
        Author author = new Author(new User("author1", "a@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>()), false);
        Article oldArticle = new Article("update-article-test", "Update Article Test", "Desc", "Body", List.of(), Instant.now(), Instant.now(), false, 0, new ArrayList<>(), author);
        when(articleRepository.findBySlug("update-article-test")).thenReturn(new ArticleIdPair(oldArticle, "id1"));

        Author hacker = new Author(new User("hacker", "h@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>()), false);
        ArticleUpdateDTO updateDTO = new ArticleUpdateDTO("Updated Title Test", "New Desc", "New Body");

        assertThrows(UnauthorizedException.class, () -> articleService.updateArticle(updateDTO, "update-article-test", hacker));
    }

    @Test
    void shouldDeleteArticleSuccessfully() throws IOException {
        Author author = new Author(new User("deleter", "del@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>()), false);
        Article article = new Article("delete-article-test", "Delete Article Test", "Desc", "Body", List.of(), Instant.now(), Instant.now(), false, 0, new ArrayList<>(), author);

        when(articleRepository.findBySlug("delete-article-test")).thenReturn(new ArticleIdPair(article, "id1"));

        articleService.deleteArticle("delete-article-test", author);

        verify(articleRepository).deleteBySlug("delete-article-test");
        verify(articleRepository).deleteCommentsByArticleSlug("delete-article-test");
    }

    @Test
    void shouldMarkArticleAsFavorite() throws IOException {
        Author author = new Author(new User("fav_author", "fav@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>()), false);
        Article article = new Article("fav-article-test", "Fav Article Test", "Desc", "Body", List.of(), Instant.now(), Instant.now(), false, 0, new ArrayList<>(), author);

        when(articleRepository.findBySlug("fav-article-test")).thenReturn(new ArticleIdPair(article, "id1"));

        Article marked = articleService.markArticleAsFavorite("fav-article-test", "liker_user");

        assertThat(marked.favoritesCount()).isEqualTo(1);
        assertThat(marked.favoritedBy()).contains("liker_user");
        verify(articleRepository).update(eq("id1"), any(Article.class));
    }

    @Test
    void shouldRemoveArticleFromFavorite() throws IOException {
        Author author = new Author(new User("fav_author", "fav@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>()), false);
        List<String> favoritedBy = new ArrayList<>();
        favoritedBy.add("liker_user");
        Article article = new Article("fav-article-test", "Fav Article Test", "Desc", "Body", List.of(), Instant.now(), Instant.now(), true, 1, favoritedBy, author);

        when(articleRepository.findBySlug("fav-article-test")).thenReturn(new ArticleIdPair(article, "id1"));

        Article removed = articleService.removeArticleFromFavorite("fav-article-test", "liker_user");

        assertThat(removed.favoritesCount()).isEqualTo(0);
        assertThat(removed.favoritedBy()).isEmpty();
        verify(articleRepository).update(eq("id1"), any(Article.class));
    }

    @Test
    void shouldFindAllTagsSuccessfully() throws IOException {
        TagsDTO expectedTags = new TagsDTO(List.of("tag_z", "tag_a"));
        when(articleRepository.findAllTags()).thenReturn(expectedTags);

        TagsDTO tags = articleService.findAllTags();
        assertThat(tags.tags()).containsExactlyElementsOf(expectedTags.tags());
    }
}
