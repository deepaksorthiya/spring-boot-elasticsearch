package com.example.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.document.comment.Comment;
import org.springframework.stereotype.Repository;

import java.io.IOException;

import static com.example.constant.Constants.COMMENTS;

@Repository
public class CommentRepository {

    private final ElasticsearchClient esClient;

    public CommentRepository(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public void save(Comment comment) throws IOException {
        IndexRequest<Comment> commentReq = IndexRequest.of((id -> id
                .index(COMMENTS.getName())
                .refresh(Refresh.WaitFor)
                .document(comment)));

        esClient.index(commentReq);
    }

    public void deleteComment(String commentId, String username) throws IOException {
        DeleteByQueryResponse deleteComment = esClient.deleteByQuery(ss -> ss
                .index(COMMENTS.getName())
                .waitForCompletion(true)
                .refresh(true)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .term(t -> t
                                                .field("id")
                                                .value(commentId))
                                ).must(m -> m
                                        .term(t -> t
                                                .field("author.username.keyword")
                                                .value(username))))
                ));
        if (deleteComment.deleted() < 1) {
            throw new RuntimeException("Failed to delete comment");
        }
    }

    public SearchResponse<Comment> findCommentsByArticleSlug(String slug) throws IOException {
        return esClient.search(s -> s
                        .index(COMMENTS.getName())
                        .query(q -> q
                                .term(t -> t
                                        .field("articleSlug.keyword")
                                        .value(slug))
                        )
                , Comment.class);
    }
}
