package com.example.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.util.NamedValue;
import com.example.document.article.Article;
import com.example.document.article.TagsDTO;
import com.example.utils.ArticleIdPair;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.constant.Constants.ARTICLES;
import static com.example.constant.Constants.COMMENTS;
import static com.example.utils.Utility.extractId;
import static com.example.utils.Utility.extractSource;

@Repository
public class ArticleRepository {

    private final ElasticsearchClient esClient;

    public ArticleRepository(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void save(Article article) throws IOException {
        IndexRequest<Article> articleReq = IndexRequest.of((id -> id
                .index(ARTICLES.getName())
                .refresh(Refresh.WaitFor)
                .document(article)));
        esClient.index(articleReq);
    }

    public ArticleIdPair findBySlug(String slug) throws IOException {
        SearchResponse<Article> getArticle = esClient.search(ss -> ss
                        .index(ARTICLES.getName())
                        .query(q -> q
                                .term(t -> t
                                        .field("slug.keyword")
                                        .value(slug))
                        )
                , Article.class);

        if (getArticle.hits().hits().isEmpty()) {
            return null;
        }
        return new ArticleIdPair(extractSource(getArticle), extractId(getArticle));
    }

    public void update(String id, Article updatedArticle) throws IOException {
        UpdateResponse<Article> upArticle = esClient.update(up -> up
                        .index(ARTICLES.getName())
                        .id(id)
                        .refresh(Refresh.WaitFor)
                        .doc(updatedArticle)
                , Article.class);
        if (!upArticle.result().name().equals("Updated")) {
            throw new RuntimeException("Article update failed");
        }
    }

    public void deleteBySlug(String slug) throws IOException {
        DeleteByQueryResponse deleteArticle = esClient.deleteByQuery(d -> d
                .index(ARTICLES.getName())
                .waitForCompletion(true)
                .refresh(true)
                .query(q -> q
                        .term(t -> t
                                .field("slug.keyword")
                                .value(slug))
                ));
        if (deleteArticle.deleted() < 1) {
            throw new RuntimeException("Failed to delete article");
        }
    }

    public void deleteCommentsByArticleSlug(String slug) throws IOException {
        esClient.deleteByQuery(d -> d
                .index(COMMENTS.getName())
                .waitForCompletion(true)
                .refresh(true)
                .query(q -> q
                        .term(t -> t
                                .field("articleSlug.keyword")
                                .value(slug))
                ));
    }

    public SearchResponse<Article> searchArticles(Query query, Integer limit, Integer offset) throws IOException {
        return esClient.search(ss -> ss
                        .index(ARTICLES.getName())
                        .size(limit)
                        .from(offset)
                        .query(query)
                        .sort(srt -> srt
                                .field(fld -> fld
                                        .field("updatedAt")
                                        .order(SortOrder.Desc)))
                , Article.class);
    }

    public SearchResponse<Article> findArticlesByAuthors(List<FieldValue> authorsFilter) throws IOException {
        return esClient.search(ss -> ss
                        .index(ARTICLES.getName())
                        .query(q -> q
                                .bool(b -> b
                                        .filter(f -> f
                                                .terms(t -> t
                                                        .field("author.username.keyword")
                                                        .terms(TermsQueryField.of(tqf -> tqf.value(authorsFilter)))
                                                ))))
                        .sort(srt -> srt
                                .field(fld -> fld
                                        .field("updatedAt")
                                        .order(SortOrder.Desc)))
                , Article.class);
    }

    public TagsDTO findAllTags() throws IOException {
        NamedValue<SortOrder> sort = new NamedValue<>("_count", SortOrder.Desc);

        SearchResponse<Void> aggregateTags = esClient.search(s -> s
                        .index(ARTICLES.getName())
                        .size(0)
                        .aggregations("tags", agg -> agg
                                .terms(ter -> ter
                                        .field("tagList.keyword")
                                        .order(sort))
                        ),
                Void.class
        );

        return new TagsDTO(aggregateTags.aggregations().get("tags")
                .sterms().buckets()
                .array().stream()
                .map(st -> st.key().stringValue())
                .collect(Collectors.toList())
        );
    }
}
