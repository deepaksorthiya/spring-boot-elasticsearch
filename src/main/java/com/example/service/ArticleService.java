package com.example.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery.Builder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.document.article.*;
import com.example.document.exception.ResourceAlreadyExistsException;
import com.example.document.exception.ResourceNotFoundException;
import com.example.document.exception.UnauthorizedException;
import com.example.document.user.Author;
import com.example.document.user.User;
import com.example.repository.ArticleRepository;
import com.example.utils.ArticleIdPair;
import com.github.slugify.Slugify;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.utils.Utility.isNullOrBlank;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public Article newArticle(ArticleCreationDTO articleDTO, Author author) throws IOException {

        String slug = generateAndCheckSlug(articleDTO.title());

        Instant now = Instant.now();
        Article article = new Article(articleDTO, slug, now, now, author);

        articleRepository.save(article);

        return article;
    }

    public ArticleIdPair findArticleBySlug(String slug) throws IOException {
        return articleRepository.findBySlug(slug);
    }

    public ArticleDTO updateArticle(ArticleUpdateDTO article, String slug, Author author) throws IOException {

        ArticleIdPair articlePair = Optional.ofNullable(findArticleBySlug(slug))
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
        String id = articlePair.id();
        Article oldArticle = articlePair.article();

        if (!oldArticle.author().username().equals(author.username())) {
            throw new UnauthorizedException("Cannot modify article from another author");
        }

        String newSlug = slug;
        if (!isNullOrBlank(article.title()) && !article.title().equals(oldArticle.title())) {
            newSlug = generateAndCheckSlug(article.title());
        }

        Instant updatedAt = Instant.now();

        Article updatedArticle = new Article(newSlug,
                isNullOrBlank(article.title()) ? oldArticle.title() : article.title(),
                isNullOrBlank(article.description()) ? oldArticle.description() : article.description(),
                isNullOrBlank(article.body()) ? oldArticle.body() : article.body(),
                oldArticle.tagList(), oldArticle.createdAt(),
                updatedAt, oldArticle.favorited(), oldArticle.favoritesCount(),
                oldArticle.favoritedBy(), oldArticle.author());

        updateArticle(id, updatedArticle);
        return new ArticleDTO(updatedArticle);
    }

    private void updateArticle(String id, Article updatedArticle) throws IOException {
        articleRepository.update(id, updatedArticle);
    }

    public void deleteArticle(String slug, Author author) throws IOException {

        ArticleIdPair articlePair = Optional.ofNullable(findArticleBySlug(slug))
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
        Article article = articlePair.article();

        if (!article.author().username().equals(author.username())) {
            throw new UnauthorizedException("Cannot delete article from another author");
        }

        articleRepository.deleteBySlug(slug);
        articleRepository.deleteCommentsByArticleSlug(slug);
    }

    public Article markArticleAsFavorite(String slug, String username) throws IOException {
        ArticleIdPair articlePair = Optional.ofNullable(findArticleBySlug(slug))
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
        String id = articlePair.id();
        Article article = articlePair.article();

        if (article.favoritedBy().contains(username)) {
            return article;
        }

        article.favoritedBy().add(username);
        Article updatedArticle = new Article(article.slug(), article.title(),
                article.description(),
                article.body(), article.tagList(), article.createdAt(), article.updatedAt(),
                true, article.favoritesCount() + 1, article.favoritedBy(), article.author());

        updateArticle(id, updatedArticle);
        return updatedArticle;
    }

    public Article removeArticleFromFavorite(String slug, String username) throws IOException {
        ArticleIdPair articlePair = Optional.ofNullable(findArticleBySlug(slug))
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
        String id = articlePair.id();
        Article article = articlePair.article();

        if (!article.favoritedBy().contains(username)) {
            return article;
        }

        article.favoritedBy().remove(username);
        int favoriteCount = article.favoritesCount() - 1;
        boolean favorited = article.favorited();
        if (favoriteCount == 0) {
            favorited = false;
        }

        Article updatedArticle = new Article(article.slug(), article.title(),
                article.description(),
                article.body(), article.tagList(), article.createdAt(), article.updatedAt(), favorited,
                favoriteCount, article.favoritedBy(), article.author());

        updateArticle(id, updatedArticle);
        return updatedArticle;
    }

    public ArticlesDTO findArticles(String tag, String author, String favorited, Integer limit,
                                    Integer offset,
                                    Optional<User> user) throws IOException {
        List<Query> conditions = new ArrayList<>();

        if (!isNullOrBlank(tag)) {
            conditions.add(new Builder()
                    .field("tagList")
                    .query(tag).build()._toQuery());
        }
        if (!isNullOrBlank(author)) {
            conditions.add(new Builder()
                    .field("author.username")
                    .query(author).build()._toQuery());
        }
        if (!isNullOrBlank(favorited)) {
            conditions.add(MatchQuery.of(mq -> mq
                            .field("favoritedBy")
                            .query(favorited))
                    ._toQuery());
        }

        Query query = new Query.Builder().bool(b -> b.should(conditions)).build();

        SearchResponse<Article> getArticle = articleRepository.searchArticles(query, limit, offset);

        return new ArticlesDTO(getArticle.hits().hits()
                .stream()
                .map(Hit::source)
                .peek(a -> {
                    if (!isNullOrBlank(tag) && a.tagList().contains(tag)) {
                        Collections.swap(a.tagList(), a.tagList().indexOf(tag), 0);
                    }
                })
                .map(ArticleForListDTO::new)
                .map(a -> {
                    if (user.isPresent()) {
                        boolean following = user.get().following().contains(a.author().username());
                        return new ArticleForListDTO(a, new Author(a.author().username(),
                                a.author().email(), a.author().bio(), following));
                    }
                    return a;
                })
                .collect(Collectors.toList()), getArticle.hits().hits().size());
    }

    public ArticlesDTO generateArticleFeed(User user) throws IOException {
        List<FieldValue> authorsFilter = user.following().stream()
                .map(FieldValue::of).toList();

        SearchResponse<Article> articlesByAuthors = articleRepository.findArticlesByAuthors(authorsFilter);

        return new ArticlesDTO(articlesByAuthors.hits().hits()
                .stream()
                .map(Hit::source)
                .map(ArticleForListDTO::new)
                .map(a -> {
                    boolean following = user.following().contains(a.author().username());
                    return new ArticleForListDTO(a, new Author(a.author().username(),
                            a.author().email(), a.author().bio(), following));
                })
                .collect(Collectors.toList()), articlesByAuthors.hits().hits().size());
    }


    public TagsDTO findAllTags() throws IOException {
        return articleRepository.findAllTags();
    }

    private String generateAndCheckSlug(String title) throws IOException {
        String slug = Slugify.builder().build().slugify(title);
        if (Objects.nonNull(findArticleBySlug(slug))) {
            throw new ResourceAlreadyExistsException("Article slug already exists, please change the title");
        }
        return slug;
    }

}
